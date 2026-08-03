// Copyright 2026 Agenterie. Apache-2.0.
// Models: CC BY-NC-SA 4.0 (openWakeWord / David Scripka).
package com.agenterie.jarvis.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agenterie.jarvis.wake.WakePipeline
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

/**
 * SpikeWakeService — Foreground service « microphone » (JA-T5).
 *
 * Instrument de mesure P0 : écoute en continu, alimente WakePipeline (JA-T4),
 * journalise chaque détection au-dessus du seuil dans un fichier horodaté
 * ET dans Logcat avec le tag [JARVISWAKE] (stable, filtrable par scripts/pull_measures.sh).
 *
 * Format journal (une ligne par détection) :
 *   2026-08-02T21:14:03.412Z detect score=0.930 rms=1234.5 since_boot_ms=123456
 *
 * Restrictions Android 34+ respectées :
 *   - Service DOIT être démarré depuis l'activité visible (SpikeActivity).
 *   - Permissions déclarées dans le manifest : FOREGROUND_SERVICE,
 *     FOREGROUND_SERVICE_MICROPHONE, RECORD_AUDIO, POST_NOTIFICATIONS.
 */
class SpikeWakeService : Service() {

    companion object {
        /** Tag logcat stable — filtré par scripts/pull_measures.sh */
        const val TAG = "JARVISWAKE"

        const val CHANNEL_ID = "spike_wake_channel"
        const val NOTIF_ID   = 1

        /** Action du broadcast envoyé à SpikeActivity pour MAJ temps réel. */
        const val ACTION_SCORE  = "com.agenterie.jarvis.spike.ACTION_SCORE"
        const val EXTRA_SCORE   = "score"
        const val EXTRA_DETECTS = "detect_count"

        /** Intervalle minimum entre deux broadcasts de score (ms). */
        private const val BROADCAST_THROTTLE_MS = 150L

        /** Période réfractaire par défaut si wake.json est illisible. */
        private const val DEFAULT_REFRACTORY_MS = 3_000L
    }

    // ── État partagé (lu aussi par SpikeActivity via liaison indirecte) ─────
    val currentScore   = AtomicReference(0.0f)
    val detectCount    = AtomicInteger(0)
    var journalPath    = ""
        private set

    // ── Internals ────────────────────────────────────────────────────────────
    private val running    = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val f = File(filesDir, "spike-detections.log")
        journalPath = f.absolutePath
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Armé — en écoute…"))

        // Créer le journal dès le démarrage du service (objection Codex HAUTE) :
        // une session valide à zéro détection doit pouvoir être lue par `run-as … cat`.
        // Échec explicite : le service ne reste pas « armé » sans journal accessible.
        val journalFile = File(journalPath)
        if (!journalFile.exists()) {
            try {
                journalFile.createNewFile()
                Log.i(TAG, "Journal créé au démarrage : $journalPath")
            } catch (e: Exception) {
                Log.e(TAG, "Impossible de créer le journal : $e")
                stopSelfWithError("Erreur journal — service arrêté")
                return START_NOT_STICKY
            }
        }

        startCapture()
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Capture audio ────────────────────────────────────────────────────────

    private fun startCapture() {
        if (running.getAndSet(true)) return

        audioThread = Thread {
            runCatching { captureLoop() }
                .onFailure { e ->
                    Log.e(TAG, "captureLoop crashed: ${e.message}", e)
                    // Objection Codex MOYENNE/107 : une panne ne laisse pas le FGS
                    // vivant avec running=true mais sans capture réelle.
                    stopSelfWithError("Erreur capture — service arrêté")
                }
        }.apply {
            isDaemon = true
            name = "SpikeCapture"
            start()
        }
    }

    /** Arrête proprement le service en cas d'erreur et reflète l'état dans la notification. */
    private fun stopSelfWithError(msg: String) {
        running.set(false)
        updateNotification(msg)
        stopSelf()
    }

    private fun stopCapture() {
        running.set(false)
        audioThread?.join(3_000)
        audioThread = null
    }

    /**
     * Boucle principale d'acquisition.
     * Lit des tranches de [WakePipeline.CHUNK_SIZE] = 1280 échantillons (80 ms @ 16 kHz),
     * alimente WakePipeline, applique seuil + réfractaire, journalise les détections.
     */
    private fun captureLoop() {
        // ── Chargement du pipeline ───────────────────────────────────────────
        val pipeline = WakePipeline(
            assets.open("wake/melspectrogram.onnx"),
            assets.open("wake/embedding_model.onnx"),
            assets.open("wake/hey_jarvis_v0.1.onnx")
        )

        // ── Lecture de la config wake.json (seuil + réfractaire) ────────────
        val (threshold, refractoryMs) = runCatching {
            val json = assets.open("wake/wake.json").bufferedReader().readText()
            val th  = Regex(""""threshold"\s*:\s*([\d.]+)""").find(json)!!.groupValues[1].toFloat()
            val rfr = Regex(""""refractory_ms"\s*:\s*(\d+)""").find(json)!!.groupValues[1].toLong()
            Pair(th, rfr)
        }.getOrElse {
            Log.w(TAG, "wake.json illisible, valeurs par défaut appliquées: $it")
            Pair(0.5f, DEFAULT_REFRACTORY_MS)
        }

        Log.i(TAG, "SpikeWakeService démarré — threshold=$threshold refractoryMs=$refractoryMs")

        // ── AudioRecord ──────────────────────────────────────────────────────
        val sampleRate = 16_000
        val chunkSize  = WakePipeline.CHUNK_SIZE   // 1280 échantillons = 80 ms
        val minBuf     = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recBuf = maxOf(chunkSize * 4, minBuf)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recBuf
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord non initialisé — RECORD_AUDIO accordé ?")
            pipeline.close()
            // Objection Codex MOYENNE/107 : on ne laisse pas le FGS vivant sans capture.
            stopSelfWithError("Erreur micro — RECORD_AUDIO manquant ?")
            return
        }
        recorder.startRecording()
        Log.i(TAG, "AudioRecord démarré — journalPath=$journalPath")

        // ── Journalier ──────────────────────────────────────────────────────
        val journalFile = File(journalPath)

        // ── Boucle de capture ────────────────────────────────────────────────
        val samples       = ShortArray(chunkSize)
        // null = aucune détection encore, donc JAMAIS dans la fenêtre réfractaire.
        // Même choix que WakeGate, et pour la même raison : avec Long.MIN_VALUE,
        // `now - lastDetectMs` vaut ~9,22e18 > Long.MAX_VALUE et DÉBORDE en négatif,
        // si bien que la condition ci-dessous restait fausse à jamais — aucune
        // détection n'était journalisée ni comptée (audit Codex du 2026-08-03).
        var lastDetectMs: Long? = null
        var lastBcastMs   = 0L

        try {
            pipeline.use {
                while (running.get()) {
                    val read = recorder.read(samples, 0, chunkSize)
                    if (read < chunkSize) continue

                    // ── Score ────────────────────────────────────────────────
                    val score = pipeline.predict(samples)
                    currentScore.set(score)

                    // ── RMS ──────────────────────────────────────────────────
                    val rms = sqrt(samples.fold(0.0) { acc, s -> acc + s.toLong() * s } / chunkSize).toFloat()

                    // ── Détection ────────────────────────────────────────────
                    val now = System.currentTimeMillis()
                    val dansRefractaire = lastDetectMs?.let { (now - it) < refractoryMs } ?: false
                    if (score >= threshold && !dansRefractaire) {
                        lastDetectMs = now
                        val count      = detectCount.incrementAndGet()
                        val bootMs     = SystemClock.elapsedRealtime()
                        val timestamp  = isoFmt.format(Date(now))
                        // Objection Codex MOYENNE/205 : Locale.US garantit un point décimal
                        // stable quelle que soit la locale du téléphone (fr → virgule sinon).
                        val line = String.format(
                            Locale.US,
                            "%s detect score=%.3f rms=%.1f since_boot_ms=%d",
                            timestamp, score, rms, bootMs
                        )

                        Log.i(TAG, line)
                        runCatching { journalFile.appendText("$line\n") }
                            .onFailure { Log.e(TAG, "Écriture journal échouée: $it") }

                        updateNotification(
                            String.format(Locale.US, "Armé | Détections: %d | score=%.3f", count, score)
                        )
                    }

                    // ── Broadcast throttlé vers l'activité ───────────────────
                    if (now - lastBcastMs >= BROADCAST_THROTTLE_MS) {
                        lastBcastMs = now
                        sendBroadcast(Intent(ACTION_SCORE).apply {
                            `package` = packageName      // RECEIVER_NOT_EXPORTED compat
                            putExtra(EXTRA_SCORE,   score)
                            putExtra(EXTRA_DETECTS, detectCount.get())
                        })
                    }
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Écoute wake-word (spike)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification permanente du service de mesure P0"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, SpikeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis Spike")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(tap)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIF_ID, buildNotification(text))
    }
}

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
import com.agenterie.jarvis.wake.WakeGate
import com.agenterie.jarvis.wake.WakePipeline
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedQueue
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

        // ── Référence statique (weak) — utilisée par InjectWakeReceiver ───────
        private val _instance = AtomicReference<SpikeWakeService?>(null)

        /**
         * Retourne l'instance en cours ou null si le service est arrêté.
         * Utilisé par [InjectWakeReceiver] pour accéder à la file d'injection.
         */
        fun instance(): SpikeWakeService? = _instance.get()

        /**
         * Vrai ssi le service tourne.  Délègue à [SpikeState] pour rester
         * testable en JVM sans contexte Android.
         */
        fun isRunning(): Boolean = SpikeState.isRunning()
    }

    // ── File d'injection PCM (debug uniquement — InjectWakeReceiver) ──────────
    /**
     * Tranches PCM injectées par [InjectWakeReceiver] (25 silences + wav).
     * Thread-safe : le receiver y écrit depuis le thread principal,
     * la boucle de capture y lit depuis SpikeCapture.
     * Traitées sur le thread de capture : WakePipeline n'est pas thread-safe.
     */
    internal val injectQueue: ConcurrentLinkedQueue<ShortArray> = ConcurrentLinkedQueue()

    /** Enfile une liste de tranches PCM pour injection dans la boucle de capture. */
    fun enqueueInject(chunks: List<ShortArray>) {
        injectQueue.addAll(chunks)
        Log.i(TAG, "enqueueInject: ${chunks.size} tranches enfilées")
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
        // Marquer comme actif AVANT tout : SpikeActivity.onResume lit SpikeState.
        _instance.set(this)
        SpikeState.running.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Armé — en écoute…"))

        // Rotation du journal à chaque arm explicite (gate Codex MOYENNE) :
        // le journal est append-only ; sans rotation les collectes successives cumulent
        // les détections historiques et pull_measures.sh surestime la session courante.
        // Intent != null ↔ arm explicite (SpikeActivity.startForegroundService) ;
        // intent == null ↔ redémarrage système (START_STICKY) où les données de la
        // session interrompue sont préservées.
        val journalFile = File(journalPath)
        try {
            if (intent != null) {
                // Nouvelle session P0 : journal vierge et compteur remis à zéro.
                // writeText("") crée le fichier s'il est absent, le tronque s'il existe.
                journalFile.writeText("")
                detectCount.set(0)
                Log.i(TAG, "Journal rotaté — nouvelle session P0 : $journalPath")
            } else if (!journalFile.exists()) {
                // Redémarrage système : créer le journal seulement s'il n'existe pas.
                journalFile.createNewFile()
                Log.i(TAG, "Journal créé (redémarrage système) : $journalPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'initialiser le journal : $e")
            stopSelfWithError("Erreur journal — service arrêté")
            return START_NOT_STICKY
        }

        startCapture()
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        // Effacer la référence statique APRÈS l'arrêt de la capture.
        _instance.compareAndSet(this, null)
        SpikeState.running.set(false)
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
        // Objection Codex MOYENNE — WakeGate.fromConfig lève une IllegalArgumentException
        // si threshold ou refractory_ms est absent ou invalide : plus de fallback silencieux
        // sur 0.5 qui rendrait les mesures P0 intraçables à la configuration prévue.
        // L'exception se propage jusqu'au runCatching de startCapture → stopSelfWithError.
        val gate = WakeGate.fromConfig(assets.open("wake/wake.json"), pipeline)
        val threshold    = gate.threshold
        val refractoryMs = gate.refractoryMs

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

        // ── État local partagé entre processChunk et la boucle ───────────────
        // null = aucune détection encore, donc JAMAIS dans la fenêtre réfractaire.
        // Même choix que WakeGate, et pour la même raison : avec Long.MIN_VALUE,
        // `now - lastDetectMs` vaut ~9,22e18 > Long.MAX_VALUE et DÉBORDE en négatif,
        // si bien que la condition restait fausse à jamais — aucune détection
        // n'était journalisée ni comptée (audit Codex du 2026-08-03).
        var lastDetectMs: Long? = null
        var lastBcastMs   = 0L

        // ── Traitement d'une tranche PCM (voix réelle OU injection debug) ────
        // Fonction locale : capture le pipeline, gate, journal et état ci-dessus.
        // Le résultat (score, journal, notification) emprunte LE MÊME CHEMIN
        // quelle que soit l'origine de la tranche — pas de raccourci vers le journal.
        fun processChunk(s: ShortArray) {
            val score = pipeline.predict(s)
            currentScore.set(score)

            val rms = sqrt(s.fold(0.0) { acc, x -> acc + x.toLong() * x } / chunkSize).toFloat()

            val now = System.currentTimeMillis()
            val dansRefractaire = lastDetectMs?.let { (now - it) < refractoryMs } ?: false
            if (score >= threshold && !dansRefractaire) {
                lastDetectMs = now
                val bootMs    = SystemClock.elapsedRealtime()
                val timestamp = isoFmt.format(Date(now))
                val line = String.format(
                    Locale.US,
                    "%s detect score=%.3f rms=%.1f since_boot_ms=%d",
                    timestamp, score, rms, bootMs
                )
                // Écriture AVANT le comptage : l'échec est fatal (audit Codex HAUTE).
                try {
                    journalFile.appendText("$line\n")
                } catch (e: Exception) {
                    Log.e(TAG, "Écriture journal échouée — service arrêté : $e")
                    throw e
                }
                Log.i(TAG, line)
                val count = detectCount.incrementAndGet()
                updateNotification(
                    String.format(Locale.US, "Armé | Détections: %d | score=%.3f", count, score)
                )
            }

            // Broadcast throttlé vers l'activité
            if (now - lastBcastMs >= BROADCAST_THROTTLE_MS) {
                lastBcastMs = now
                sendBroadcast(Intent(ACTION_SCORE).apply {
                    `package` = packageName
                    putExtra(EXTRA_SCORE,   score)
                    putExtra(EXTRA_DETECTS, detectCount.get())
                })
            }
        }

        // ── Boucle de capture ────────────────────────────────────────────────
        val samples = ShortArray(chunkSize)

        try {
            pipeline.use {
                while (running.get()) {
                    // ── Drainer la file d'injection (InjectWakeReceiver, debug) ──
                    // Priorité avant la lecture micro : l'injection est déterministe
                    // (25 silences + wav) et ne bloque pas — le pipeline tourne sur ce
                    // thread, donc les échantillons injectés empruntent le même chemin
                    // que la voix (seuil, réfractaire, journal, compteur, notification).
                    var injected = injectQueue.poll()
                    while (injected != null) {
                        processChunk(injected)
                        injected = injectQueue.poll()
                    }

                    // ── Lecture micro ─────────────────────────────────────────
                    val read = recorder.read(samples, 0, chunkSize)
                    // Objection Codex HAUTE — les valeurs négatives sont des codes d'erreur
                    // AudioRecord (ERROR=-1, ERROR_INVALID_OPERATION=-3, etc.), pas des lectures
                    // courtes. Les traiter comme `continue` provoque une boucle CPU sans audio.
                    if (read < 0) throw RuntimeException(
                        "AudioRecord.read() erreur=$read — micro perdu ou permission révoquée ?"
                    )
                    if (read < chunkSize) continue

                    processChunk(samples)
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

// Copyright 2026 Agenterie. Apache-2.0.
package com.agenterie.jarvis.spike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.agenterie.jarvis.wake.WakePipeline

/**
 * Point d'injection PCM debug — permet d'observer une VRAIE détection via adb.
 *
 * Usage :
 *   adb shell am broadcast -a com.agenterie.jarvis.spike.INJECT_WAKE \
 *       com.agenterie.jarvis.spike
 *
 * Ce que ça prouve : la détection arrive jusqu'au journal (seuil, réfractaire,
 * écriture, compteur, notification) — pas seulement que le service est vivant.
 *
 * Sécurité :
 *   - [android:exported="true"] obligatoire pour que `adb shell am broadcast` atteigne
 *     le composant (APK jetable : c'est assumé).
 *   - Neutralisé hors build debug via [BuildConfig.DEBUG] : retour immédiat en release.
 *
 * Protocole d'injection :
 *   1. 25 tranches de silence (amorçage, miroir de [WakePipelineTest]).
 *   2. Tranches du WAV `fixtures/hey_jarvis_espeak.wav` (fixture prouvée ≥ 0.5).
 * Les tranches sont enfilées dans [SpikeWakeService.injectQueue] et traitées
 * sur le thread de capture (WakePipeline n'est pas thread-safe) — aucune
 * écriture directe dans le journal.
 */
class InjectWakeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Neutralisé hors build debug — un récepteur exporté qui fabrique une
        // détection n'a rien à faire en production.
        if (!BuildConfig.DEBUG) return

        if (intent.action != ACTION_INJECT_WAKE) return

        val service = SpikeWakeService.instance() ?: run {
            Log.w(TAG, "InjectWakeReceiver: service non actif — broadcast ignoré")
            return
        }

        // ── Charger le WAV depuis les assets ─────────────────────────────────
        // Le module spike inclut app/src/test/resources dans ses assets.srcDirs.
        val wavBytes = try {
            context.assets.open("fixtures/hey_jarvis_espeak.wav").readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "InjectWakeReceiver: impossible d'ouvrir le WAV fixture : $e")
            return
        }

        // ── Parser le WAV → PCM int16 ─────────────────────────────────────────
        val pcm = parseWav(wavBytes) ?: run {
            Log.e(TAG, "InjectWakeReceiver: pas de chunk 'data' dans le WAV")
            return
        }

        // ── Préparer les tranches : 25 silences + tranches WAV ────────────────
        // 25 silences : miroir exact de WakePipelineTest.processWav(warmupChunks=25)
        // → score déterministe (fixture prouvée au-dessus du seuil dans les tests JVM).
        val chunks = mutableListOf<ShortArray>()
        repeat(WARMUP_SILENCE_CHUNKS) {
            chunks.add(ShortArray(WakePipeline.CHUNK_SIZE)) // silence = 0.0f PCM
        }
        var i = 0
        while (i + WakePipeline.CHUNK_SIZE <= pcm.size) {
            chunks.add(pcm.copyOfRange(i, i + WakePipeline.CHUNK_SIZE))
            i += WakePipeline.CHUNK_SIZE
        }

        service.enqueueInject(chunks)
        Log.i(TAG,
            "InjectWakeReceiver: injection enfilée — " +
            "${WARMUP_SILENCE_CHUNKS} silences + ${chunks.size - WARMUP_SILENCE_CHUNKS} " +
            "tranches WAV (total ${chunks.size})"
        )
    }

    companion object {
        /** Action attendue dans le manifest et par `adb shell am broadcast`. */
        const val ACTION_INJECT_WAKE = "com.agenterie.jarvis.spike.INJECT_WAKE"

        private const val TAG = "JARVISWAKE"

        /** Miroir de WakePipelineTest.processWav(warmupChunks = 25). */
        private const val WARMUP_SILENCE_CHUNKS = 25

        /**
         * Lit l'entête RIFF/WAV et retourne les données PCM int16 little-endian,
         * ou null si le chunk « data » est introuvable.
         * Miroir de WakePipelineTest.loadWav — même algorithme, source de vérité unique.
         */
        fun parseWav(bytes: ByteArray): ShortArray? {
            var offset = 12
            while (offset < bytes.size - 8) {
                val id = String(bytes, offset, 4, Charsets.US_ASCII)
                val size = ((bytes[offset + 4].toInt() and 0xFF)) or
                           ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                           ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                           ((bytes[offset + 7].toInt() and 0xFF) shl 24)
                if (id == "data") {
                    val audio = bytes.copyOfRange(offset + 8, offset + 8 + size)
                    return ShortArray(audio.size / 2) { j ->
                        ((audio[2 * j + 1].toInt() shl 8) or (audio[2 * j].toInt() and 0xFF)).toShort()
                    }
                }
                offset += 8 + size
            }
            return null
        }
    }
}

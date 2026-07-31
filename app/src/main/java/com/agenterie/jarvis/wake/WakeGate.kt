package com.agenterie.jarvis.wake

import java.io.InputStream

/**
 * Wraps a [WakePipeline] scorer with a configurable detection threshold and refractory period
 * loaded from [wake.json].  No constants are hard-coded here.
 *
 * @property scorer    Called for each chunk; returns score in [0, 1].
 * @property threshold Minimum score to open a detection session (from wake.json).
 * @property refractoryMs After a detection, further detections are suppressed for this duration
 *                         (from wake.json, key "refractory_ms").
 * @param clock        Injectable time source — defaults to [System.currentTimeMillis] for prod,
 *                     overridable in unit tests.
 */
class WakeGate(
    private val scorer: (ShortArray) -> Float,
    val threshold: Float,
    val refractoryMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    // null = no detection yet → never within refractory (avoids Long overflow)
    private var lastDetectionMs: Long? = null

    /**
     * Feed one audio chunk to the pipeline.
     * @return true if the wake word was detected AND the refractory window has expired.
     */
    fun process(samples: ShortArray): Boolean {
        val score = scorer(samples)
        val now = clock()
        val withinRefractory = lastDetectionMs?.let { (now - it) < refractoryMs } ?: false
        return if (score >= threshold && !withinRefractory) {
            lastDetectionMs = now
            true
        } else {
            false
        }
    }

    companion object {
        /**
         * Build a [WakeGate] from a [WakePipeline] and the configuration stream
         * (e.g. assets/wake/wake.json).  Parses threshold and refractory_ms.
         */
        @JvmStatic
        fun fromConfig(
            configStream: InputStream,
            pipeline: WakePipeline,
            clock: () -> Long = System::currentTimeMillis
        ): WakeGate {
            val json = configStream.bufferedReader().readText()
            val threshold = requireNotNull(
                Regex(""""threshold"\s*:\s*([\d.]+)""").find(json)
            ) { "wake.json must contain 'threshold'" }.groupValues[1].toFloat()
            val refractoryMs = requireNotNull(
                Regex(""""refractory_ms"\s*:\s*(\d+)""").find(json)
            ) { "wake.json must contain 'refractory_ms'" }.groupValues[1].toLong()
            return WakeGate(pipeline::predict, threshold, refractoryMs, clock)
        }
    }
}

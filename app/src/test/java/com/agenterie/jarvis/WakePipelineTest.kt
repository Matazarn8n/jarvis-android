package com.agenterie.jarvis

import com.agenterie.jarvis.wake.WakeGate
import com.agenterie.jarvis.wake.WakePipeline
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * JVM unit tests for the WakePipeline / WakeGate stack.
 * Written before implementation (TDD) — proves parity with the openWakeWord Python reference.
 *
 * Pre-condition: fixtures must exist (run scripts/make_fixtures.sh first).
 * Models are loaded from src/main/assets/wake/ (Gradle working-dir = app/).
 */
class WakePipelineTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun openPipeline() = WakePipeline(
        File("src/main/assets/wake/melspectrogram.onnx").inputStream(),
        File("src/main/assets/wake/embedding_model.onnx").inputStream(),
        File("src/main/assets/wake/hey_jarvis_v0.1.onnx").inputStream()
    )

    /** Reads 16-bit little-endian PCM from a WAV file (skips RIFF header). */
    private fun loadWav(path: String): ShortArray {
        val bytes = File(path).readBytes()
        // Find "data" sub-chunk
        var offset = 12
        while (offset < bytes.size - 8) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = ((bytes[offset + 4].toInt() and 0xFF)) or
                       ((bytes[offset + 5].toInt() and 0xFF) shl 8) or
                       ((bytes[offset + 6].toInt() and 0xFF) shl 16) or
                       ((bytes[offset + 7].toInt() and 0xFF) shl 24)
            if (id == "data") {
                val audio = bytes.copyOfRange(offset + 8, offset + 8 + size)
                return ShortArray(audio.size / 2) { i ->
                    ((audio[2 * i + 1].toInt() shl 8) or (audio[2 * i].toInt() and 0xFF)).toShort()
                }
            }
            offset += 8 + size
        }
        error("No 'data' chunk found in $path")
    }

    /**
     * Processes a wav file through the pipeline in 1280-sample chunks.
     * Pre-warms with [warmupChunks] of silence so both Python and Kotlin share
     * the same feature context when the real audio begins (mirrors parity script).
     */
    private fun processWav(
        wavPath: String,
        pipeline: WakePipeline,
        warmupChunks: Int = 25
    ): List<Float> {
        val silence = ShortArray(WakePipeline.CHUNK_SIZE)
        repeat(warmupChunks) { pipeline.predict(silence) }
        val audio = loadWav(wavPath)
        val scores = mutableListOf<Float>()
        var i = 0
        while (i + WakePipeline.CHUNK_SIZE <= audio.size) {
            scores += pipeline.predict(audio.copyOfRange(i, i + WakePipeline.CHUNK_SIZE))
            i += WakePipeline.CHUNK_SIZE
        }
        return scores
    }

    // ── Test 1: positive sample scores above detection threshold ──────────────

    @Test
    fun `positive sample max score exceeds 0_5 threshold`() {
        openPipeline().use { pipeline ->
            val scores = processWav(
                "src/test/resources/fixtures/hey_jarvis_espeak.wav",
                pipeline
            )
            val maxScore = scores.maxOrNull() ?: 0f
            assertTrue(
                "Expected max score > 0.5 on 'hey jarvis' fixture, got $maxScore",
                maxScore > 0.5f
            )
        }
    }

    // ── Test 2: negative sample stays below detection threshold ───────────────

    @Test
    fun `negative sample max score stays below 0_5 threshold`() {
        openPipeline().use { pipeline ->
            val scores = processWav(
                "src/test/resources/fixtures/negatif_espeak.wav",
                pipeline
            )
            val maxScore = scores.maxOrNull() ?: 0f
            assertTrue(
                "Expected max score < 0.5 on negative fixture, got $maxScore",
                maxScore < 0.5f
            )
        }
    }

    // ── Test 3: refractory gate suppresses immediate second detection ─────────

    @Test
    fun `refractory gate suppresses second detection within window`() {
        // Use a fake clock so we control time precisely
        var fakeTimeMs = 0L

        openPipeline().use { pipeline ->
            val gate = WakeGate.fromConfig(
                File("src/main/assets/wake/wake.json").inputStream(),
                pipeline,
                clock = { fakeTimeMs }
            )

            // Pre-warm pipeline
            val silence = ShortArray(WakePipeline.CHUNK_SIZE)
            repeat(25) { gate.process(silence) }

            val audio = loadWav("src/test/resources/fixtures/hey_jarvis_espeak.wav")

            // First pass — should yield at least one detection
            var firstPassDetections = 0
            var i = 0
            while (i + WakePipeline.CHUNK_SIZE <= audio.size) {
                if (gate.process(audio.copyOfRange(i, i + WakePipeline.CHUNK_SIZE)))
                    firstPassDetections++
                i += WakePipeline.CHUNK_SIZE
            }
            assertTrue("Gate should detect on positive audio", firstPassDetections > 0)

            // Second pass — clock still at 0, well within 3000 ms refractory
            var secondPassDetections = 0
            i = 0
            while (i + WakePipeline.CHUNK_SIZE <= audio.size) {
                if (gate.process(audio.copyOfRange(i, i + WakePipeline.CHUNK_SIZE)))
                    secondPassDetections++
                i += WakePipeline.CHUNK_SIZE
            }
            assertEquals(
                "Gate must suppress all detections within refractory window",
                0,
                secondPassDetections
            )

            // Third pass — advance past refractory (3001 ms)
            fakeTimeMs = 3001L
            var thirdPassDetections = 0
            i = 0
            while (i + WakePipeline.CHUNK_SIZE <= audio.size) {
                if (gate.process(audio.copyOfRange(i, i + WakePipeline.CHUNK_SIZE)))
                    thirdPassDetections++
                i += WakePipeline.CHUNK_SIZE
            }
            assertTrue(
                "Gate should allow detection again after refractory expiry",
                thirdPassDetections > 0
            )
        }
    }
}

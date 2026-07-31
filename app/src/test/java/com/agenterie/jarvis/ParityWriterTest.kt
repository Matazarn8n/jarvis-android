package com.agenterie.jarvis

import com.agenterie.jarvis.wake.WakePipeline
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 * Writes per-chunk scores for both fixtures to build/parity-kotlin.json.
 * Called by parity_check.sh via:
 *   ./gradlew :app:testDebugUnitTest --tests "com.agenterie.jarvis.ParityWriterTest"
 *
 * JSON schema:
 * {
 *   "pos_scores":     [f, f, ...],
 *   "neg_scores":     [f, f, ...],
 *   "pos_max_score":  f,
 *   "neg_max_score":  f
 * }
 */
class ParityWriterTest {

    private fun openPipeline() = WakePipeline(
        File("src/main/assets/wake/melspectrogram.onnx").inputStream(),
        File("src/main/assets/wake/embedding_model.onnx").inputStream(),
        File("src/main/assets/wake/hey_jarvis_v0.1.onnx").inputStream()
    )

    private fun loadWav(path: String): ShortArray {
        val bytes = File(path).readBytes()
        var offset = 12
        while (offset < bytes.size - 8) {
            val id = String(bytes, offset, 4, Charsets.US_ASCII)
            val size = (bytes[offset + 4].toInt() and 0xFF) or
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
        error("No 'data' chunk in $path")
    }

    private fun processWav(wavPath: String, pipeline: WakePipeline): List<Float> {
        // Pre-warm with 25 silence chunks — same as Python parity script
        val silence = ShortArray(WakePipeline.CHUNK_SIZE)
        repeat(25) { pipeline.predict(silence) }
        val audio = loadWav(wavPath)
        val scores = mutableListOf<Float>()
        var i = 0
        while (i + WakePipeline.CHUNK_SIZE <= audio.size) {
            scores += pipeline.predict(audio.copyOfRange(i, i + WakePipeline.CHUNK_SIZE))
            i += WakePipeline.CHUNK_SIZE
        }
        return scores
    }

    @Test
    fun `write parity scores to JSON`() {
        val posFix = "src/test/resources/fixtures/hey_jarvis_espeak.wav"
        val negFix = "src/test/resources/fixtures/negatif_espeak.wav"

        val posScores = openPipeline().use { processWav(posFix, it) }
        val negScores = openPipeline().use { processWav(negFix, it) }

        fun Float.fmt() = String.format(Locale.US, "%.6f", this)
        fun List<Float>.toJsonArray() = joinToString(",", "[", "]") { it.fmt() }

        val json = buildString {
            append("{\n")
            append("""  "pos_scores": ${posScores.toJsonArray()},""").append("\n")
            append("""  "neg_scores": ${negScores.toJsonArray()},""").append("\n")
            append("""  "pos_max_score": ${(posScores.maxOrNull() ?: 0f).fmt()},""").append("\n")
            append("""  "neg_max_score": ${(negScores.maxOrNull() ?: 0f).fmt()}""").append("\n")
            append("}\n")
        }

        // Ensure output directory exists and write
        val outFile = File("build/parity-kotlin.json")
        outFile.parentFile?.mkdirs()
        outFile.writeText(json)

        println("Kotlin parity scores written to ${outFile.absolutePath}")
        println("pos_max_score=${posScores.maxOrNull()}")
        println("neg_max_score=${negScores.maxOrNull()}")
    }
}

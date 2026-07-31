// Copyright 2026 Agenterie. Apache-2.0.
// Models: CC BY-NC-SA 4.0 (openWakeWord / David Scripka).
package com.agenterie.jarvis.wake

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.InputStream
import java.nio.FloatBuffer

/**
 * Stateful streaming pipeline that replicates the openWakeWord Python preprocessing chain:
 *   raw audio (int16, 1280 samples / 80 ms) →
 *   melspectrogram (ONNX, spec/10+2 normalisation) →
 *   embedding (Google speech_embedding, 76-frame sliding window) →
 *   wake-word classifier (hey_jarvis_v0.1) → score [0, 1].
 *
 * Each [predict] call consumes exactly [CHUNK_SIZE] = 1280 samples.
 * The first [WARMUP_FRAMES] = 5 calls always return 0.0 (model warm-up, mirrors Python).
 * AudioRecord must NOT appear here: streams are injected by the caller.
 *
 * Thread safety: not thread-safe, use from a single dedicated thread.
 */
class WakePipeline(
    melspecModelStream: InputStream,
    embeddingModelStream: InputStream,
    classifierModelStream: InputStream
) : AutoCloseable {

    companion object {
        const val CHUNK_SIZE = 1280          // 80 ms @ 16 kHz
        private const val MEL_CONTEXT = 480  // 160*3 samples prepended to mel input
        private const val MEL_WINDOW = 76    // mel frames per embedding window
        private const val N_MELS = 32
        private const val EMBEDDING_DIM = 96
        private const val N_FEATURE_FRAMES = 16   // classifier input depth
        private const val RAW_BUFFER_MAX = 160_000 // 10 s @ 16 kHz
        private const val MEL_BUFFER_MAX = 970    // ≈10 s of mel frames
        private const val FEATURE_BUFFER_MAX = 120 // ≈10 s of embeddings
        private const val WARMUP_FRAMES = 5
    }

    // ONNX sessions — shared environment (singleton, do not close it here)
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val melspecSession: OrtSession
    private val embeddingSession: OrtSession
    private val classifierSession: OrtSession
    private val classifierInputName: String

    // Raw audio ring buffer (primitive, no boxing)
    private val rawRing = ShortArray(RAW_BUFFER_MAX)
    private var rawWriteIdx = 0   // next write position (mod RAW_BUFFER_MAX)
    private var rawTotal = 0      // total samples ever written (unbounded for ring math)

    // Mel spectrogram buffer — list of FloatArray(N_MELS) frames
    // Initialised with 76 rows of 1.0 (mirrors Python: np.ones((76, 32)))
    private val melBuf = ArrayDeque<FloatArray>(MEL_BUFFER_MAX + MEL_WINDOW).also { d ->
        repeat(MEL_WINDOW) { d.addLast(FloatArray(N_MELS) { 1.0f }) }
    }

    // Feature (embedding) buffer — list of FloatArray(EMBEDDING_DIM)
    // Initialised with 16 zero rows (the first 5 frames are zeroed anyway)
    private val featBuf = ArrayDeque<FloatArray>(FEATURE_BUFFER_MAX + N_FEATURE_FRAMES).also { d ->
        repeat(N_FEATURE_FRAMES) { d.addLast(FloatArray(EMBEDDING_DIM)) }
    }

    private var callCount = 0

    init {
        val opts = OrtSession.SessionOptions().apply {
            setInterOpNumThreads(1)
            setIntraOpNumThreads(1)
        }
        melspecSession = env.createSession(melspecModelStream.readBytes(), opts)
        embeddingSession = env.createSession(embeddingModelStream.readBytes(), opts)
        classifierSession = env.createSession(classifierModelStream.readBytes(), opts)
        classifierInputName = classifierSession.inputNames.iterator().next()
    }

    /**
     * Feed one 80-ms chunk and return the detection score.
     * @param samples Exactly [CHUNK_SIZE] = 1280 int16 PCM samples @ 16 kHz.
     * @return Score in [0, 1]. Always 0.0 for the first [WARMUP_FRAMES] calls.
     */
    fun predict(samples: ShortArray): Float {
        require(samples.size == CHUNK_SIZE) {
            "WakePipeline.predict requires $CHUNK_SIZE samples, got ${samples.size}"
        }

        // ── 1. Buffer raw audio ──────────────────────────────────────────────
        for (s in samples) {
            rawRing[rawWriteIdx % RAW_BUFFER_MAX] = s
            rawWriteIdx++
        }
        val rawCount = minOf(rawWriteIdx, RAW_BUFFER_MAX)

        // ── 2. Mel spectrogram from last (CHUNK_SIZE + MEL_CONTEXT) samples ──
        //    Mirrors: list(raw_data_buffer)[-1280 - 160*3 :]
        val contextLen = minOf(rawCount, CHUNK_SIZE + MEL_CONTEXT)
        val rawFloat = FloatArray(contextLen)
        val startAbsolute = rawWriteIdx - contextLen
        for (i in 0 until contextLen) {
            rawFloat[i] = rawRing[(startAbsolute + i) % RAW_BUFFER_MAX].toFloat()
        }
        val newMelFrames = runMelspectrogram(rawFloat)
        for (frame in newMelFrames) {
            melBuf.addLast(frame)
            if (melBuf.size > MEL_BUFFER_MAX) melBuf.removeFirst()
        }

        // ── 3. Embedding from last MEL_WINDOW mel frames ─────────────────────
        if (melBuf.size >= MEL_WINDOW) {
            val offset = melBuf.size - MEL_WINDOW
            val window = Array(MEL_WINDOW) { i -> melBuf[offset + i] }
            val emb = runEmbeddingModel(window)
            featBuf.addLast(emb)
            if (featBuf.size > FEATURE_BUFFER_MAX) featBuf.removeFirst()
        }

        // ── 4. Classifier on last N_FEATURE_FRAMES embeddings ────────────────
        val score = if (featBuf.size >= N_FEATURE_FRAMES) {
            val off = featBuf.size - N_FEATURE_FRAMES
            val feats = Array(N_FEATURE_FRAMES) { i -> featBuf[off + i] }
            runClassifier(feats)
        } else 0.0f

        // ── 5. Zero first WARMUP_FRAMES (mirrors Python prediction_buffer < 5) ─
        return if (callCount < WARMUP_FRAMES) {
            callCount++
            0.0f
        } else {
            score
        }
    }

    // ── Private ONNX helpers ──────────────────────────────────────────────────

    /** Runs melspectrogram.onnx on raw float32 samples.
     *  Input  [1, N] float32 → Output [1, 1, F, 32] float32
     *  Transform applied: spec / 10 + 2  (matches Python AudioFeatures._get_melspectrogram). */
    private fun runMelspectrogram(samples: FloatArray): List<FloatArray> {
        val shape = longArrayOf(1L, samples.size.toLong())
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(samples), shape)
        return try {
            val result = melspecSession.run(mapOf("input" to inputTensor))
            result.use {
                val outTensor = result.iterator().next().value as OnnxTensor
                val buf = outTensor.floatBuffer
                val nFrames = buf.remaining() / N_MELS
                (0 until nFrames).map { i ->
                    FloatArray(N_MELS) { j -> buf[i * N_MELS + j] / 10.0f + 2.0f }
                }
            }
        } finally {
            inputTensor.close()
        }
    }

    /** Runs embedding_model.onnx on a [MEL_WINDOW]×[N_MELS] window.
     *  Input  [1, 76, 32, 1] float32 → Output [1, 1, 1, 96] float32 → squeezed [96]. */
    private fun runEmbeddingModel(window: Array<FloatArray>): FloatArray {
        val flatData = FloatArray(MEL_WINDOW * N_MELS)
        for (i in 0 until MEL_WINDOW)
            for (j in 0 until N_MELS)
                flatData[i * N_MELS + j] = window[i][j]
        val shape = longArrayOf(1L, MEL_WINDOW.toLong(), N_MELS.toLong(), 1L)
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatData), shape)
        return try {
            val result = embeddingSession.run(mapOf("input_1" to inputTensor))
            result.use {
                val outTensor = result.iterator().next().value as OnnxTensor
                val buf = outTensor.floatBuffer
                FloatArray(EMBEDDING_DIM) { i -> buf[i] }
            }
        } finally {
            inputTensor.close()
        }
    }

    /** Runs hey_jarvis_v0.1.onnx on [N_FEATURE_FRAMES]×[EMBEDDING_DIM] features.
     *  Input  [1, 16, 96] float32 → Output [1, 1] float32 → score. */
    private fun runClassifier(features: Array<FloatArray>): Float {
        val flatData = FloatArray(N_FEATURE_FRAMES * EMBEDDING_DIM)
        for (i in 0 until N_FEATURE_FRAMES)
            for (j in 0 until EMBEDDING_DIM)
                flatData[i * EMBEDDING_DIM + j] = features[i][j]
        val shape = longArrayOf(1L, N_FEATURE_FRAMES.toLong(), EMBEDDING_DIM.toLong())
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(flatData), shape)
        return try {
            val result = classifierSession.run(mapOf(classifierInputName to inputTensor))
            result.use {
                val outTensor = result.iterator().next().value as OnnxTensor
                outTensor.floatBuffer[0]
            }
        } finally {
            inputTensor.close()
        }
    }

    override fun close() {
        classifierSession.close()
        embeddingSession.close()
        melspecSession.close()
        // OrtEnvironment is a singleton — do not close it here
    }
}

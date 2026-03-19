package com.lsm.translator.service

import android.content.Context
import android.util.Log
import com.lsm.translator.model.LandmarkFrame
import com.lsm.translator.model.PhraseClassification
import kotlin.math.exp

// ---------------------------------------------------------------------------
// Classifier interface
// ---------------------------------------------------------------------------

interface LSMClassifierInterface {
    fun classify(frames: List<LandmarkFrame>): PhraseClassification
    val windowSize: Int
    var confidenceThreshold: Float
    val isReady: Boolean
}

// ---------------------------------------------------------------------------
// Stub classifier
//
// Cycles through deterministic outputs so every screen works without a real
// trained model. Used by TFLiteClassifier as a fallback.
// ---------------------------------------------------------------------------

class StubLSMClassifier : LSMClassifierInterface {

    override val windowSize            = 30
    override var confidenceThreshold   = PhraseClassification.CONFIDENCE_THRESHOLD
    override val isReady               = true

    private var callCount = 0

    private val cycle = listOf(
        "ph_001" to 0.92f,                                  // Hola
        "ph_001" to 0.94f,
        "ph_001" to 0.91f,
        "ph_016" to 0.87f,                                  // Gracias
        "ph_016" to 0.89f,
        "ph_028" to 0.95f,                                  // Ayuda
        "ph_013" to 0.88f,                                  // Sí
        "ph_014" to 0.84f,                                  // No
        "ph_015" to 0.79f,                                  // Por favor
        PhraseClassification.UNKNOWN_PHRASE_ID to 0.41f,    // low confidence
        "ph_005" to 0.73f,                                  // ¿Cómo estás?
        "ph_006" to 0.82f                                   // Estoy bien
    )

    override fun classify(frames: List<LandmarkFrame>): PhraseClassification {
        val (phraseId, confidence) = cycle[callCount++ % cycle.size]
        val isLow = confidence < confidenceThreshold ||
                phraseId == PhraseClassification.UNKNOWN_PHRASE_ID
        return PhraseClassification(
            phraseId        = if (isLow) PhraseClassification.UNKNOWN_PHRASE_ID else phraseId,
            confidence      = confidence,
            isLowConfidence = isLow,
            topAlternatives = if (isLow) emptyList()
                              else listOf(phraseId to confidence, "ph_002" to 0.12f)
        )
    }
}

// ---------------------------------------------------------------------------
// TFLite classifier (production — swap in after training)
//
// HOW TO ACTIVATE THE REAL MODEL:
//   1. Train the model — see docs/TRAINING_PIPELINE.md
//   2. Copy lsm_phrase_classifier.tflite → app/src/main/assets/
//   3. In app/build.gradle.kts uncomment:
//        implementation("org.tensorflow:tensorflow-lite:2.14.0")
//        implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
//   4. In this file, uncomment every block marked "UNCOMMENT FOR REAL MODEL"
//   5. Commit and push — GitHub Actions will build a new APK
//
// Model contract:
//   Input  shape: [1, 30, 75, 3]  float32
//   Output shape: [1, 106]        float32  (softmax — index 0 = unknown)
// ---------------------------------------------------------------------------

class TFLiteClassifier(
    private val context: Context,
    private val phraseIds: List<String>   // ["unknown", "ph_001", ..., "ph_105"]
) : LSMClassifierInterface {

    override val windowSize          = 30
    override var confidenceThreshold = PhraseClassification.CONFIDENCE_THRESHOLD
    override var isReady             = false

    private val stub = StubLSMClassifier()

    // UNCOMMENT FOR REAL MODEL ──────────────────────────────────────────────
    // private var interpreter: org.tensorflow.lite.Interpreter? = null
    // ───────────────────────────────────────────────────────────────────────

    init { loadModel() }

    private fun loadModel() {
        try {
            val assets = context.assets.list("") ?: emptyArray()
            if ("lsm_phrase_classifier.tflite" !in assets) {
                Log.w(TAG, "Model not found in assets — running in stub mode")
                return
            }
            // UNCOMMENT FOR REAL MODEL ──────────────────────────────────────
            // val buffer = org.tensorflow.lite.support.common.FileUtil
            //     .loadMappedFile(context, "lsm_phrase_classifier.tflite")
            // interpreter = org.tensorflow.lite.Interpreter(
            //     buffer,
            //     org.tensorflow.lite.Interpreter.Options().apply { numThreads = 2 }
            // )
            // isReady = true
            // Log.i(TAG, "TFLite model loaded OK")
            // ─────────────────────────────────────────────────────────────
            Log.d(TAG, "Model file found — uncomment TFLite blocks to activate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
        }
    }

    override fun classify(frames: List<LandmarkFrame>): PhraseClassification {
        if (!isReady) return stub.classify(frames)

        // UNCOMMENT FOR REAL MODEL ──────────────────────────────────────────
        // val input  = buildInputTensor(frames)
        // val output = Array(1) { FloatArray(phraseIds.size) }
        // interpreter!!.run(input, output)
        // return decodeOutput(output[0])
        // ───────────────────────────────────────────────────────────────────

        return stub.classify(frames)
    }

    private fun buildInputTensor(frames: List<LandmarkFrame>): Array<Array<Array<FloatArray>>> {
        val input = Array(1) {
            Array(windowSize) { Array(LandmarkFrame.NUM_LANDMARKS) { FloatArray(3) } }
        }
        frames.takeLast(windowSize).forEachIndexed { t, frame ->
            val flat = frame.flattened()
            for (l in 0 until LandmarkFrame.NUM_LANDMARKS) {
                input[0][t][l][0] = flat[l * 3]
                input[0][t][l][1] = flat[l * 3 + 1]
                input[0][t][l][2] = flat[l * 3 + 2]
            }
        }
        return input
    }

    private fun decodeOutput(logits: FloatArray): PhraseClassification {
        val maxV = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp((it - maxV).toDouble()).toFloat() }
        val sum  = exps.sum()
        val prob = exps.map { it / sum }

        val sorted   = prob.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
        val (idx, conf) = sorted.first()
        val phraseId    = phraseIds.getOrElse(idx) { PhraseClassification.UNKNOWN_PHRASE_ID }
        val isLow       = conf < confidenceThreshold ||
                          phraseId == PhraseClassification.UNKNOWN_PHRASE_ID
        val alts = sorted.drop(1).take(3).mapNotNull { (i, c) ->
            phraseIds.getOrNull(i)?.let { id -> id to c }
        }
        return PhraseClassification(
            phraseId        = if (isLow) PhraseClassification.UNKNOWN_PHRASE_ID else phraseId,
            confidence      = conf,
            isLowConfidence = isLow,
            topAlternatives = alts
        )
    }

    companion object { private const val TAG = "TFLiteClassifier" }
}

// ---------------------------------------------------------------------------
// Sliding-window landmark buffer
// ---------------------------------------------------------------------------

class LandmarkWindowBuffer(val capacity: Int = 30) {
    private val buf = ArrayDeque<LandmarkFrame>(capacity)

    @Synchronized fun append(frame: LandmarkFrame) {
        if (buf.size >= capacity) buf.removeFirst()
        buf.addLast(frame)
    }

    @Synchronized fun frames(): List<LandmarkFrame> = buf.toList()
    @Synchronized fun isFull(): Boolean = buf.size >= capacity
    @Synchronized fun reset()           { buf.clear() }
}

// ---------------------------------------------------------------------------
// EMA smoother + debouncer
//
// Emits only when:
//   • confidence >= threshold
//   • same phrase stable for >= requiredStableWindows consecutive calls
//   • at least emitCooldownMs has elapsed since the last emission
// ---------------------------------------------------------------------------

class ClassificationSmoother {

    var alpha                 = 0.3f    // EMA weight (0 = fully smooth, 1 = no smoothing)
    var requiredStableWindows = 4       // consecutive windows before emitting
    var emitCooldownMs        = 1500L   // minimum ms between emissions

    private val smoothed    = mutableMapOf<String, Float>()
    private val stableCount = mutableMapOf<String, Int>()
    private var lastEmitMs  = 0L

    fun feed(result: PhraseClassification): PhraseClassification? {
        val id = result.phraseId

        // Update EMA for this phrase
        val prev = smoothed[id] ?: 0f
        smoothed[id] = alpha * result.confidence + (1f - alpha) * prev

        // Decay all other entries
        smoothed.keys.filter { it != id }.forEach {
            smoothed[it] = (smoothed[it] ?: 0f) * 0.8f
        }

        if (result.isLowConfidence) {
            stableCount.clear()
            return null
        }

        stableCount[id] = (stableCount[id] ?: 0) + 1
        stableCount.keys.filter { it != id }.forEach { stableCount[it] = 0 }

        if ((stableCount[id] ?: 0) < requiredStableWindows)      return null
        if (System.currentTimeMillis() - lastEmitMs < emitCooldownMs) return null

        lastEmitMs = System.currentTimeMillis()
        stableCount[id] = 0

        return PhraseClassification(
            phraseId        = id,
            confidence      = smoothed[id] ?: result.confidence,
            isLowConfidence = false,
            topAlternatives = result.topAlternatives
        )
    }

    fun reset() {
        smoothed.clear()
        stableCount.clear()
        lastEmitMs = 0L
    }
}

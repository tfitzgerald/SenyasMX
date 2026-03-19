package com.lsm.translator.model

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Phrase pack data
// ---------------------------------------------------------------------------

data class Phrase(
    val id: String,
    val category: String,
    val spanish: String,
    val synonyms: List<String>,
    @SerializedName("video_file")       val videoFile: String,
    @SerializedName("duration_seconds") val durationSeconds: Double,
    val difficulty: String,
    val tags: List<String>
) {
    /** Combined lowercase text used for fuzzy search */
    val searchableText: String
        get() = (listOf(spanish) + synonyms).joinToString(" ").lowercase()
}

data class PhraseCategory(
    val id: String,
    val name: String,
    @SerializedName("name_en") val nameEn: String,
    val icon: String,
    val color: String
)

data class PhrasePack(
    val version: String,
    val language: String,
    @SerializedName("total_phrases") val totalPhrases: Int,
    val categories: List<PhraseCategory>,
    val phrases: List<Phrase>
)

// ---------------------------------------------------------------------------
// ML classification result
// ---------------------------------------------------------------------------

data class PhraseClassification(
    val phraseId: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isLowConfidence: Boolean,
    val topAlternatives: List<Pair<String, Float>> = emptyList()
) {
    companion object {
        const val CONFIDENCE_THRESHOLD = 0.65f
        const val UNKNOWN_PHRASE_ID    = "unknown"

        fun lowConfidence() = PhraseClassification(
            phraseId       = UNKNOWN_PHRASE_ID,
            confidence     = 0f,
            isLowConfidence = true
        )
    }
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

data class RecognitionHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val phrase: Phrase,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// Search result
// ---------------------------------------------------------------------------

data class FuzzyMatchResult(val phrase: Phrase, val score: Double)

// ---------------------------------------------------------------------------
// Landmark types (used by the ML pipeline)
// ---------------------------------------------------------------------------

data class Landmark3D(val x: Float, val y: Float, val z: Float) {
    companion object { val ZERO = Landmark3D(0f, 0f, 0f) }
}

data class LandmarkFrame(
    val poseLandmarks:      List<Landmark3D>,   // 33 points
    val leftHandLandmarks:  List<Landmark3D>,   // 21 points
    val rightHandLandmarks: List<Landmark3D>    // 21 points
) {
    companion object {
        const val NUM_LANDMARKS = 75   // 33 + 21 + 21
        const val POSE_COUNT    = 33
        const val HAND_COUNT    = 21

        fun empty() = LandmarkFrame(
            poseLandmarks      = List(POSE_COUNT) { Landmark3D.ZERO },
            leftHandLandmarks  = List(HAND_COUNT) { Landmark3D.ZERO },
            rightHandLandmarks = List(HAND_COUNT) { Landmark3D.ZERO }
        )
    }

    /** Flatten all landmarks to a FloatArray of size 75 × 3 = 225 */
    fun flattened(): FloatArray {
        val result = FloatArray(NUM_LANDMARKS * 3)
        var i = 0
        for (pt in poseLandmarks + leftHandLandmarks + rightHandLandmarks) {
            result[i++] = pt.x
            result[i++] = pt.y
            result[i++] = pt.z
        }
        return result
    }
}

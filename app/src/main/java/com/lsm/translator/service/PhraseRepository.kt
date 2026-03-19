package com.lsm.translator.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.lsm.translator.model.FuzzyMatchResult
import com.lsm.translator.model.Phrase
import com.lsm.translator.model.PhraseCategory
import com.lsm.translator.model.PhrasePack
import com.lsm.translator.model.PhraseClassification
import java.text.Normalizer

class PhraseRepository(private val context: Context) {

    private var pack: PhrasePack? = null
    private val index = mutableMapOf<String, Phrase>()   // id -> Phrase

    val phrases: List<Phrase>
        get() = pack?.phrases ?: emptyList()

    val categories: List<PhraseCategory>
        get() = pack?.categories ?: emptyList()

    /** Ordered list starting with "unknown" then ph_001…ph_105 */
    val orderedPhraseIds: List<String>
        get() = listOf(PhraseClassification.UNKNOWN_PHRASE_ID) + phrases.map { it.id }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    fun load(): Result<PhrasePack> = runCatching {
        val json   = context.assets.open("phrases.json").bufferedReader().readText()
        val loaded = Gson().fromJson(json, PhrasePack::class.java)
        pack = loaded
        index.clear()
        index.putAll(loaded.phrases.associateBy { it.id })
        Log.i(TAG, "Loaded ${loaded.phrases.size} phrases (v${loaded.version})")
        loaded
    }.onFailure { e ->
        Log.e(TAG, "Failed to load phrases.json: ${e.message}")
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    fun phraseById(id: String): Phrase? = index[id]

    fun phrasesByCategory(categoryId: String): List<Phrase> =
        phrases.filter { it.category == categoryId }

    // -------------------------------------------------------------------------
    // Offline fuzzy search  (Dice bigram coefficient + prefix bonus)
    // -------------------------------------------------------------------------

    fun search(query: String): List<FuzzyMatchResult> {
        if (query.isBlank()) return emptyList()
        val q = query.normalise()
        return phrases
            .map { FuzzyMatchResult(it, score(q, it.searchableText.normalise())) }
            .filter { it.score > 0.1 }
            .sortedByDescending { it.score }
            .take(10)
    }

    fun topMatches(query: String, limit: Int = 3): List<FuzzyMatchResult> =
        search(query).take(limit)

    private fun score(q: String, target: String): Double {
        if (target == q)              return 1.0
        if (target.contains(q))      return 0.97
        if (target.startsWith(q))    return 0.95
        val qBig = bigrams(q)
        val tBig = bigrams(target).toHashSet()
        if (qBig.isEmpty() || tBig.isEmpty()) return 0.0
        val inter = qBig.count { it in tBig }
        return (2.0 * inter) / (qBig.size + tBig.size)
    }

    private fun bigrams(s: String): List<String> =
        if (s.length < 2) listOf(s)
        else (0 until s.length - 1).map { s.substring(it, it + 2) }

    // -------------------------------------------------------------------------
    // Video URI  (loaded from app assets at runtime)
    // -------------------------------------------------------------------------

    fun videoUri(phrase: Phrase): String =
        "file:///android_asset/videos/${phrase.videoFile}"

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun String.normalise(): String =
        Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    companion object { private const val TAG = "PhraseRepository" }
}

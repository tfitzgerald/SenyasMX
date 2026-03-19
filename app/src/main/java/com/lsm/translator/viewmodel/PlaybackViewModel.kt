package com.lsm.translator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lsm.translator.model.FuzzyMatchResult
import com.lsm.translator.model.Phrase
import com.lsm.translator.model.PhraseCategory
import com.lsm.translator.service.PhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class PlaybackUiState(
    val searchQuery:      String               = "",
    val selectedCategory: String?              = null,
    val selectedPhrase:   Phrase?              = null,
    val displayedPhrases: List<Phrase>         = emptyList(),
    val categories:       List<PhraseCategory> = emptyList(),
    val fuzzyMatches:     List<FuzzyMatchResult> = emptyList(),
    val playbackRate:     Float                = 1.0f,
    val isPlaying:        Boolean              = false
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PhraseRepository(application).also { it.load() }

    private val _uiState = MutableStateFlow(
        PlaybackUiState(
            displayedPhrases = repo.phrases,
            categories       = repo.categories
        )
    )
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val phrases = when {
                query.isBlank() ->
                    if (state.selectedCategory != null)
                        repo.phrasesByCategory(state.selectedCategory)
                    else
                        repo.phrases
                else -> repo.search(query).map { it.phrase }
            }
            state.copy(
                searchQuery      = query,
                displayedPhrases = phrases,
                fuzzyMatches     = if (query.isNotBlank()) repo.topMatches(query)
                                   else emptyList()
            )
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { state ->
            val phrases = if (categoryId == null) repo.phrases
                          else repo.phrasesByCategory(categoryId)
            state.copy(
                selectedCategory = categoryId,
                searchQuery      = "",
                displayedPhrases = phrases,
                fuzzyMatches     = emptyList()
            )
        }
    }

    fun onPhraseSelected(phrase: Phrase) {
        _uiState.update { it.copy(selectedPhrase = phrase, isPlaying = true) }
    }

    fun onPlaybackRateChanged(rate: Float) {
        _uiState.update { it.copy(playbackRate = rate) }
    }

    fun onPlaybackEnded() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun videoUri(phrase: Phrase): String = repo.videoUri(phrase)
}

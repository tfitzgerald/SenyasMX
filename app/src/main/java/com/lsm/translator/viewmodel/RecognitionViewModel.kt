package com.lsm.translator.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsm.translator.model.LandmarkFrame
import com.lsm.translator.model.RecognitionHistoryItem
import com.lsm.translator.service.ClassificationSmoother
import com.lsm.translator.service.LandmarkWindowBuffer
import com.lsm.translator.service.PhraseRepository
import com.lsm.translator.service.TFLiteClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class RecognitionUiState(
    val isRunning:               Boolean                    = false,
    val currentPhrase:           String?                    = null,
    val currentConfidence:       Float                      = 0f,
    val isLowConfidence:         Boolean                    = false,
    val partialHypothesis:       String?                    = null,
    val history:                 List<RecognitionHistoryItem> = emptyList(),
    val cameraPermissionGranted: Boolean                    = false,
    val isLowLight:              Boolean                    = false,
    val errorMessage:            String?                    = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo       = PhraseRepository(application).also { it.load() }
    private val classifier = TFLiteClassifier(application, repo.orderedPhraseIds)
    private val buffer     = LandmarkWindowBuffer(capacity = 30)
    private val smoother   = ClassificationSmoother()

    private val _uiState = MutableStateFlow(RecognitionUiState())
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "MX"))
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                           result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ttsReady) Log.w(TAG, "TTS: es-MX not supported on this device")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public API called from the UI
    // -------------------------------------------------------------------------

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
    }

    fun startRecognition() {
        buffer.reset()
        smoother.reset()
        _uiState.update { it.copy(
            isRunning     = true,
            currentPhrase = null,
            errorMessage  = null,
            isLowConfidence = false
        )}
    }

    fun stopRecognition() {
        _uiState.update { it.copy(isRunning = false, partialHypothesis = null) }
    }

    /**
     * Called by CameraPreview on every sampled frame (~15 fps).
     * Runs ML inference on a background coroutine.
     */
    fun onFrame(frame: LandmarkFrame) {
        if (!_uiState.value.isRunning) return
        buffer.append(frame)
        if (!buffer.isFull()) return

        viewModelScope.launch(Dispatchers.Default) {
            val raw = classifier.classify(buffer.frames())

            // Show partial hypothesis while building up stable windows
            val partial = if (!raw.isLowConfidence) repo.phraseById(raw.phraseId)?.spanish
                          else null
            _uiState.update { it.copy(partialHypothesis = partial) }

            val stable = smoother.feed(raw) ?: return@launch
            val phrase = repo.phraseById(stable.phraseId) ?: return@launch

            val item = RecognitionHistoryItem(phrase = phrase, confidence = stable.confidence)
            _uiState.update { state ->
                state.copy(
                    currentPhrase     = phrase.spanish,
                    currentConfidence = stable.confidence,
                    isLowConfidence   = false,
                    partialHypothesis = null,
                    history           = listOf(item) + state.history
                )
            }

            if (ttsReady) {
                tts?.speak(phrase.spanish, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun onLightLevel(luminance: Double) {
        _uiState.update { it.copy(isLowLight = luminance < 30.0) }
    }

    fun speakPhrase(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    companion object { private const val TAG = "RecognitionViewModel" }
}

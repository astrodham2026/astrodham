package com.astrodham.astroagent.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.astrodham.astroagent.util.Constants
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages voice input using Android's SpeechRecognizer.
 *
 * Features:
 * - Start/stop speech recognition
 * - Wake phrase detection ("Hey Astro")
 * - Continuous listening mode
 * - Emits recognized text via SharedFlow
 *
 * LIMITATIONS:
 * - Online recognition requires internet (uses Google's speech service)
 * - Offline recognition is available on some devices but with lower accuracy
 * - SpeechRecognizer must be created and used on the Main thread
 * - Continuous listening requires restarting after each recognition result
 *
 * NOTE: For truly always-on wake word detection, a dedicated on-device
 * wake word engine (like Porcupine) would be more battery-efficient.
 * The current implementation uses standard Android APIs.
 */
class VoiceCommandManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false
    private var continuousMode = false

    /** Whether SpeechRecognizer is available on this device */
    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    /** Emits recognized speech text (after wake phrase filtering if enabled) */
    private val _recognizedText = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    /** Emits raw partial results for UI feedback */
    private val _partialResults = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val partialResults: SharedFlow<String> = _partialResults.asSharedFlow()

    /**
     * Initialize the SpeechRecognizer.
     * MUST be called on the Main thread.
     */
    fun initialize() {
        if (!isAvailable) {
            Logger.w("SpeechRecognizer not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(createListener())
        Logger.i("VoiceCommandManager initialized")
    }

    /**
     * Starts listening for speech input.
     * MUST be called on the Main thread.
     *
     * @param continuous If true, restarts listening after each result
     */
    fun startListening(continuous: Boolean = false) {
        if (!isAvailable) {
            Logger.w("Cannot start listening: SpeechRecognizer not available")
            return
        }

        continuousMode = continuous
        isListeningActive = true
        _isListening.value = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Try to enable offline recognition if available
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            Logger.i("Voice listening started (continuous=$continuous)")
        } catch (e: Exception) {
            Logger.e("Failed to start listening", e)
            _isListening.value = false
            isListeningActive = false
        }
    }

    /**
     * Stops listening for speech input.
     */
    fun stopListening() {
        isListeningActive = false
        continuousMode = false
        _isListening.value = false

        try {
            speechRecognizer?.stopListening()
            Logger.i("Voice listening stopped")
        } catch (e: Exception) {
            Logger.e("Error stopping listener", e)
        }
    }

    /**
     * Releases all resources. Call when done with voice input.
     */
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        Logger.i("VoiceCommandManager destroyed")
    }

    // ── Private ──

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Logger.d("Voice: ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Logger.d("Voice: speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed — could be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Logger.d("Voice: speech ended")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error ($error)"
            }
            Logger.w("Voice error: $errorMessage")
            _isListening.value = false

            // In continuous mode, restart listening after non-fatal errors
            if (continuousMode && isListeningActive && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                Logger.d("Restarting continuous listening after error")
                startListening(continuous = true)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestMatch = matches?.firstOrNull()

            if (!bestMatch.isNullOrBlank()) {
                Logger.i("Voice recognized: $bestMatch")
                processRecognizedText(bestMatch)
            }

            _isListening.value = false

            // In continuous mode, restart listening
            if (continuousMode && isListeningActive) {
                startListening(continuous = true)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                _partialResults.tryEmit(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Processes recognized text, handling wake phrase detection.
     *
     * If the text starts with the wake phrase, it strips the phrase
     * and emits only the command portion.
     */
    private fun processRecognizedText(text: String) {
        val lowerText = text.lowercase().trim()

        // Check for wake phrase
        if (lowerText.startsWith(Constants.WAKE_PHRASE)) {
            val command = text.substring(Constants.WAKE_PHRASE.length).trim()
            if (command.isNotBlank()) {
                Logger.i("Wake phrase detected, command: $command")
                _recognizedText.tryEmit(command)
            } else {
                Logger.d("Wake phrase detected but no command followed")
            }
        } else {
            // No wake phrase — emit the full text as command
            _recognizedText.tryEmit(text)
        }
    }
}

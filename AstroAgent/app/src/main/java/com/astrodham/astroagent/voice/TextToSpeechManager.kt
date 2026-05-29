package com.astrodham.astroagent.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.astrodham.astroagent.util.Logger
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale
import java.util.UUID

/**
 * Manages Text-to-Speech output for the agent.
 *
 * Provides:
 * - Speech output with queue support
 * - Suspending speak function for sequential workflows
 * - Language/locale configuration
 * - Proper lifecycle management
 */
class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    /**
     * Initialize the TTS engine.
     * Call this early (e.g., in Application.onCreate or Activity.onCreate).
     */
    fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isReady) {
                    Logger.i("TextToSpeech initialized successfully")
                    // Set slightly faster speech rate
                    tts?.setSpeechRate(1.1f)
                } else {
                    Logger.w("TextToSpeech: Language not supported")
                }
            } else {
                Logger.e("TextToSpeech initialization failed with status: $status")
            }
        }
    }

    /**
     * Speaks the given text asynchronously (fire-and-forget).
     *
     * @param text Text to speak
     * @param flush If true, interrupts any current speech. If false, queues after current.
     */
    fun speak(text: String, flush: Boolean = false) {
        if (!isReady) {
            Logger.w("TTS not ready, cannot speak")
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()

        tts?.speak(text, queueMode, null, utteranceId)
        Logger.d("TTS speaking: ${text.take(50)}...")
    }

    /**
     * Speaks the given text and suspends until speech is complete.
     * Useful for sequential workflows where you need to wait for the speech to finish.
     *
     * @param text Text to speak
     * @return true if speech completed, false if interrupted or error
     */
    suspend fun speakAndWait(text: String): Boolean {
        if (!isReady) {
            Logger.w("TTS not ready, cannot speak")
            return false
        }

        val deferred = CompletableDeferred<Boolean>()
        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) deferred.complete(true)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) deferred.complete(false)
            }
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) deferred.complete(false)
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        return try {
            deferred.await()
        } catch (e: Exception) {
            Logger.e("TTS speakAndWait failed", e)
            false
        }
    }

    /**
     * Stops any ongoing speech immediately.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Releases TTS resources. Call when the component is destroyed.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        Logger.i("TextToSpeech shut down")
    }
}

package com.akproleter.mobile.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TextToSpeech initialized successfully")
                _isReady.value = true
            } else {
                Log.e(TAG, "TextToSpeech initialization failed, status=$status")
                _isReady.value = false
            }
        }
    }

    /**
     * Speaks the given text in the requested locale.
     * Suspends until the utterance finishes or is cancelled.
     *
     * @param text     The string to be spoken.
     * @param language BCP-47 language tag, e.g. "en-US" or "sr-RS".
     */
    suspend fun speak(text: String, language: String = "en-US") {
        if (!_isReady.value || tts == null) {
            Log.w(TAG, "TTS not ready, skipping speak('$text')")
            return
        }

        val locale = Locale.forLanguageTag(language)
        val setLangResult = tts!!.setLanguage(locale)
        if (setLangResult == TextToSpeech.LANG_MISSING_DATA ||
            setLangResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w(TAG, "Language '$language' not supported by TTS engine, falling back to en-US")
            tts!!.setLanguage(Locale.US)
        }

        val utteranceId = UUID.randomUUID().toString()

        suspendCancellableCoroutine<Unit> { cont ->
            tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    Log.d(TAG, "TTS utterance started: $id")
                }

                override fun onDone(id: String?) {
                    if (id == utteranceId && cont.isActive) {
                        Log.d(TAG, "TTS utterance done: $id")
                        cont.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    Log.e(TAG, "TTS utterance error: $id")
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS error code=$errorCode for utterance=$utteranceId")
                    if (cont.isActive) cont.resume(Unit)
                }
            })

            val result = tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "tts.speak() returned ERROR")
                if (cont.isActive) cont.resume(Unit)
            }

            cont.invokeOnCancellation {
                tts?.stop()
            }
        }
    }

    /** Stop any current speech immediately. */
    fun stop() {
        tts?.stop()
    }

    /** Release TTS engine resources. Call from Application.onTerminate() if needed. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}

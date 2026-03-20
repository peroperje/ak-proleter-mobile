package com.akproleter.mobile.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceManager @Inject constructor() {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState = _voiceState.asStateFlow()

    fun startListening(context: Context, language: String = "en-US") {
        Log.d("VoiceManager", "startListening called with language: $language")
        
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e("VoiceManager", "startListening called from non-UI thread!")
            _voiceState.value = VoiceState.Error("Must be called from UI thread")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceManager", "Speech recognition NOT available on this device")
            _voiceState.value = VoiceState.Error("Speech recognition not available")
            return
        }

        // Clean up any stale recognizer
        destroy()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        Log.d("VoiceManager", "Creating new SpeechRecognizer instance...")
        
        // Android 12+ bug: "Android System Intelligence" can hook the default SpeechRecognizer and fail with Error 5
        // after recording. We prioritize Google's TTS RecognitionService if available.
        val componentName = ComponentName(
            "com.google.android.tts",
            "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
        )
        
        var lastPartialText = ""

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, componentName)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to create explicit SpeechRecognizer", e)
        }
        
        if (speechRecognizer == null) {
            Log.d("VoiceManager", "Explicit ComponentName failed/null, falling back to default createSpeechRecognizer")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.Listening
                lastPartialText = ""
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // WORKAROUND FOR ANDROID 12+ BUG:
                // Calling stopListening() often incorrectly triggers ERROR_CLIENT (5).
                // If we already have partial text, treat it as a successful recognition.
                if (error == SpeechRecognizer.ERROR_CLIENT && lastPartialText.isNotBlank()) {
                    Log.d("VoiceManager", "Ignoring Error 5 due to existing partial text.")
                    _voiceState.value = VoiceState.Success(lastPartialText)
                    return
                }

                val errorMsg = getErrorMessage(error)
                Log.e("VoiceManager", "SpeechRecognizer Error: $errorMsg (code $error)")
                _voiceState.value = VoiceState.Error(errorMsg)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _voiceState.value = VoiceState.Success(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    lastPartialText = matches[0]
                    _voiceState.value = VoiceState.Partial(lastPartialText)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        Log.d("VoiceManager", "Invoking startListening(intent)")
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        Log.d("VoiceManager", "stopListening called")
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        Log.d("VoiceManager", "destroying speechRecognizer")
        speechRecognizer?.apply {
            setRecognitionListener(null)
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error (5): Binding failed. Please assure the Google App is installed."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions (9). RECORD_AUDIO required."
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            10 -> "Too many requests / Language not supported (10)"
            else -> "Error code: $errorCode"
        }
    }
}

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Partial(val text: String) : VoiceState()
    data class Success(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

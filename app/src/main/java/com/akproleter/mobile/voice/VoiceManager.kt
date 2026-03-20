package com.akproleter.mobile.voice

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * VoiceManager encapsulates android SpeechRecognizer.
 *
 * ⚠️ NOTE: SpeechRecognizer is NOT thread-safe and MUST be used from the main thread only.
 * This class is intentionally NOT a @Singleton — it should be scoped to the screen/Activity.
 * The ViewModel holds the instance and destroys it in onCleared().
 */
class VoiceManager @Inject constructor() {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState = _voiceState.asStateFlow()

    fun startListening(context: Context, language: String = "en-US") {
        Log.d(TAG, "startListening called, language=$language")

        // Guard: must be on main thread
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            Log.e(TAG, "startListening called from a background thread!")
            _voiceState.value = VoiceState.Error("Must be called from the UI thread")
            return
        }

        // Guard: permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            _voiceState.value = VoiceState.Error("Microphone permission required")
            return
        }

        // Guard: recognition service available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "No speech recognition service available on this device")
            _voiceState.value = VoiceState.Error("Speech recognition not available on this device")
            return
        }

        // Clean up any stale recognizer before creating a new one
        destroyRecognizer()

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        // Primary: try Google Search app recognizer (the canonical STT service, distinct from the
        // TTS app "com.google.android.tts" which was previously used by mistake and caused Error 5).
        // Fallback: system default recognizer.
        val googleSearchComponent = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
        )

        speechRecognizer = try {
            val sr = SpeechRecognizer.createSpeechRecognizer(context, googleSearchComponent)
            Log.d(TAG, "Created SpeechRecognizer with Google Search component")
            sr
        } catch (e: Exception) {
            Log.w(TAG, "Google Search component failed, falling back to default recognizer", e)
            null
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d(TAG, "Using default system SpeechRecognizer")
        }

        var lastPartialText = ""

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lastPartialText = ""
                _voiceState.value = VoiceState.Listening
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) { /* unused */ }
            override fun onBufferReceived(buffer: ByteArray?) { /* unused */ }
            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                // Android 12+ quirk: calling stopListening() triggers ERROR_CLIENT (5).
                // If we already have partial text by then, treat it as success.
                if (error == SpeechRecognizer.ERROR_CLIENT && lastPartialText.isNotBlank()) {
                    Log.d(TAG, "Received Error 5 after partial result — treating as success")
                    _voiceState.value = VoiceState.Success(lastPartialText)
                    return
                }

                val errorMsg = mapError(error)
                Log.e(TAG, "SpeechRecognizer error: $errorMsg (code $error)")
                _voiceState.value = VoiceState.Error(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "onResults: $matches")
                if (!matches.isNullOrEmpty()) {
                    _voiceState.value = VoiceState.Success(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    lastPartialText = matches[0]
                    _voiceState.value = VoiceState.Partial(lastPartialText)
                    Log.d(TAG, "onPartialResults: $lastPartialText")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) { /* unused */ }
        })

        Log.d(TAG, "Calling speechRecognizer.startListening()")
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun stopListening() {
        Log.d(TAG, "stopListening called")
        speechRecognizer?.stopListening()
    }

    fun resetToIdle() {
        _voiceState.value = VoiceState.Idle
    }

    /** Call this when the owning ViewModel is cleared (onCleared). */
    fun destroy() {
        Log.d(TAG, "destroy() — cleaning up SpeechRecognizer")
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        speechRecognizer?.apply {
            setRecognitionListener(null)
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    private fun mapError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error (1)"
        SpeechRecognizer.ERROR_CLIENT -> "Client error (5): Service binding failed"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error (9): RECORD_AUDIO required"
        SpeechRecognizer.ERROR_NETWORK -> "Network error (2)"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout (6)"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found (7)"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy (8)"
        SpeechRecognizer.ERROR_SERVER -> "Server error (4)"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected (6)"
        10 -> "Too many requests or unsupported language (10)"
        else -> "Unknown error (code $errorCode)"
    }

    companion object {
        private const val TAG = "VoiceManager"
    }
}

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Partial(val text: String) : VoiceState()
    data class Success(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

package com.akproleter.mobile.voice

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VoiceManager encapsulates speech recognition.
 *
 * ─ Online mode  → Google cloud SpeechRecognizer (streaming, partial results)
 * ─ Offline mode → AudioCapture + WhisperRecognizer (batch, ~2-5 s delay)
 *
 * ⚠️ NOTE: SpeechRecognizer (online path) MUST be called from the main thread.
 * This class is intentionally NOT a @Singleton — scoped to the ViewModel.
 */
class VoiceManager @Inject constructor(
    private val whisperRecognizer: WhisperRecognizer
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioCapture: AudioCapture? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState = _voiceState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun startListening(context: Context, language: String = "en-US") {
        Log.d(TAG, "startListening — language=$language, online=${isNetworkAvailable(context)}")

        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            Log.e(TAG, "startListening called from a background thread!")
            _voiceState.value = VoiceState.Error("Must be called from the UI thread")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _voiceState.value = VoiceState.Error("Microphone permission required")
            return
        }

        if (isNetworkAvailable(context)) {
            startOnlineListening(context, language)
        } else {
            startOfflineRecording(context, language)
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening")
        // Online path
        speechRecognizer?.stopListening()
        // Offline path: stop capture → triggers Whisper inference
        stopOfflineRecording()
    }

    fun resetToIdle() {
        _voiceState.value = VoiceState.Idle
    }

    fun destroy() {
        Log.d(TAG, "destroy()")
        destroyOnlineRecognizer()
        captureJob?.cancel()
        audioCapture = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Online path — Google cloud SpeechRecognizer (unchanged from before)
    // ─────────────────────────────────────────────────────────────────────────

    private fun startOnlineListening(context: Context, language: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("Speech recognition not available on this device")
            return
        }

        destroyOnlineRecognizer()

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        val googleSearchComponent = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
        )

        speechRecognizer = try {
            SpeechRecognizer.createSpeechRecognizer(context, googleSearchComponent)
                .also { Log.d(TAG, "Created Google Search SpeechRecognizer") }
        } catch (e: Exception) {
            Log.w(TAG, "Google Search component unavailable, using default", e)
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        var lastPartialText = ""

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lastPartialText = ""
                _voiceState.value = VoiceState.Listening
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                // Android 12+ quirk: stopListening() fires ERROR_CLIENT (5).
                // If we already have partial text, treat it as success.
                if (error == SpeechRecognizer.ERROR_CLIENT && lastPartialText.isNotBlank()) {
                    Log.d(TAG, "Error 5 after partial result — treating as success")
                    _voiceState.value = VoiceState.Success(lastPartialText)
                    return
                }
                val msg = mapError(error)
                Log.e(TAG, "SpeechRecognizer error: $msg (code $error)")
                _voiceState.value = VoiceState.Error(msg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) _voiceState.value = VoiceState.Success(matches[0])
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    lastPartialText = matches[0]
                    _voiceState.value = VoiceState.Partial(lastPartialText)
                }
            }
        })

        speechRecognizer?.startListening(recognizerIntent)
        Log.d(TAG, "Online SpeechRecognizer started")
    }

    private fun destroyOnlineRecognizer() {
        speechRecognizer?.apply { setRecognitionListener(null); cancel(); destroy() }
        speechRecognizer = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Offline path — AudioCapture + Whisper ONNX
    // ─────────────────────────────────────────────────────────────────────────

    private fun startOfflineRecording(context: Context, language: String) {
        Log.d(TAG, "Offline mode — starting AudioCapture")
        audioCapture = AudioCapture()
        audioCapture?.start()
        _voiceState.value = VoiceState.Listening

        captureJob = scope.launch(Dispatchers.IO) {
            audioCapture?.recordLoop()
        }

        // store language for use in stopOfflineRecording
        _currentOfflineLanguage = language
    }

    private var _currentOfflineLanguage = "en-US"

    private fun stopOfflineRecording() {
        val capture = audioCapture ?: return
        captureJob?.cancel()
        captureJob = null

        val language = _currentOfflineLanguage
        scope.launch {
            _voiceState.value = VoiceState.Processing
            try {
                val pcm  = capture.stop()
                audioCapture = null
                val text = whisperRecognizer.transcribe(pcm, language)
                if (text.isBlank()) {
                    _voiceState.value = VoiceState.Error("No speech detected")
                } else {
                    _voiceState.value = VoiceState.Success(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Whisper transcription failed", e)
                _voiceState.value = VoiceState.Error("Transcription failed: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun mapError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO              -> "Audio recording error (1)"
        SpeechRecognizer.ERROR_CLIENT             -> "Client error (5)"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error (9)"
        SpeechRecognizer.ERROR_NETWORK            -> "Network error (2)"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT    -> "Network timeout (6)"
        SpeechRecognizer.ERROR_NO_MATCH           -> "No speech match found (7)"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY    -> "Recognizer is busy (8)"
        SpeechRecognizer.ERROR_SERVER             -> "Server error (4)"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT     -> "No speech detected (6)"
        10                                        -> "Too many requests or unsupported language (10)"
        else                                      -> "Unknown error (code $errorCode)"
    }

    companion object {
        private const val TAG = "VoiceManager"
    }
}

sealed class VoiceState {
    object Idle       : VoiceState()
    object Listening  : VoiceState()
    object Processing : VoiceState()   // ← new: Whisper is transcribing offline
    data class Partial(val text: String) : VoiceState()
    data class Success(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

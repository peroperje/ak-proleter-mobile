package com.akproleter.mobile.ui.voice

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akproleter.mobile.data.repositories.VoiceRepository
import com.akproleter.mobile.voice.TextToSpeechManager
import com.akproleter.mobile.voice.VoiceManager
import com.akproleter.mobile.voice.VoiceState
import com.akproleter.mobile.location.AppLocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProcessState {
    object Idle : ProcessState()
    object Processing : ProcessState()
    data class Success(val message: String) : ProcessState()
    data class Error(val message: String) : ProcessState()
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val voiceRepository: VoiceRepository,
    private val ttsManager: TextToSpeechManager,
    private val locationHelper: AppLocationManager
) : ViewModel() {

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    private val _processState = MutableStateFlow<ProcessState>(ProcessState.Idle)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    /** Currently selected recognition & TTS language (BCP-47 tag). */
    private val _selectedLanguage = MutableStateFlow("en-US")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            voiceState.collect { state ->
                if (state is VoiceState.Success) {
                    Log.d(TAG, "Voice success: '${state.text}', triggering TTS + processCommand")
                    // Speak back the recognized text for auditory confirmation (Phase 3)
                    ttsManager.speak(state.text, _selectedLanguage.value)
                    // Then process the command against the backend / offline queue
                    processVoiceText(state.text)
                }
            }
        }
    }

    fun startListening(context: Context) {
        voiceManager.startListening(context, _selectedLanguage.value)
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    /** Toggle the recognition/TTS language between English and Serbian. */
    fun toggleLanguage() {
        _selectedLanguage.value = if (_selectedLanguage.value == "en-US") "sr-RS" else "en-US"
        Log.d(TAG, "Language toggled to ${_selectedLanguage.value}")
        voiceManager.resetToIdle()
    }

    /** Reset the UI state back to Idle (e.g., after showing an error). */
    fun reset() {
        voiceManager.resetToIdle()
        _processState.value = ProcessState.Idle
    }

    fun clearProcessState() {
        _processState.value = ProcessState.Idle
    }

    private fun processVoiceText(text: String) {
        viewModelScope.launch {
            _processState.value = ProcessState.Processing
            val locationResult = locationHelper.getCurrentLocation()

            val result = voiceRepository.processVoiceCommand(
                text = text,
                language = _selectedLanguage.value,
                timestamp = System.currentTimeMillis(),
                lat = locationResult?.lat,
                lon = locationResult?.lon,
                location = locationResult?.locationText
            )
            result.onSuccess { msg ->
                _processState.value = ProcessState.Success(msg)
            }.onFailure { e ->
                Log.w(TAG, "processVoiceCommand failed: ${e.message}")
                _processState.value = ProcessState.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        Log.d(TAG, "ViewModel cleared — VoiceManager destroyed")
    }

    companion object {
        private const val TAG = "VoiceViewModel"
    }
}

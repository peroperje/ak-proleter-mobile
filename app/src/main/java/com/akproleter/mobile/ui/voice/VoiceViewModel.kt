package com.akproleter.mobile.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akproleter.mobile.data.repositories.VoiceRepository
import com.akproleter.mobile.voice.VoiceManager
import com.akproleter.mobile.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    fun toggleListening() {
        if (voiceState.value is VoiceState.Listening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        // If we have a final result, send it to the repository
        val currentState = voiceState.value
        if (currentState is VoiceState.Success) {
            processVoiceText(currentState.text)
        }
    }

    private fun processVoiceText(text: String) {
        viewModelScope.launch {
            voiceRepository.processVoiceCommand(
                text = text,
                language = "sr-RS", // Default, should be managed by LanguageManager
                role = "ADMIN", // Placeholder, from SessionManager
                contextHints = emptyMap() // To be populated with current event, etc.
            )
        }
    }
}

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

    init {
        viewModelScope.launch {
            voiceState.collect { state ->
                if (state is VoiceState.Success) {
                    processVoiceText(state.text)
                }
            }
        }
    }

    fun toggleListening(context: android.content.Context) {
        if (voiceState.value is VoiceState.Listening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening(context)
        }
    }

    fun startListening(context: android.content.Context, language: String = "en-US") {
        voiceManager.startListening(context, language)
    }

    fun stopListening() {
        voiceManager.stopListening()
        // The Success state will be caught asynchronously by the flow collector in init block
    }

    private fun processVoiceText(text: String) {
        viewModelScope.launch {
            voiceRepository.processVoiceCommand(
                text = text,
                //language = "sr-RS", // Default, should be managed by LanguageManager
                language = "en-US", // Default, should be managed by LanguageManager
                role = "ADMIN", // Placeholder, from SessionManager
                contextHints = emptyMap() // To be populated with current event, etc.
            )
        }
    }
}

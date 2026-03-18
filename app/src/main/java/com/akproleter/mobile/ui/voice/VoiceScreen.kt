package com.akproleter.mobile.ui.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akproleter.mobile.R
import com.akproleter.mobile.ui.voice.components.PushToTalkButton
import com.akproleter.mobile.voice.VoiceState

@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AK PROLETER",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Voice Assistant",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        FeedbackDisplay(voiceState)

        Spacer(modifier = Modifier.height(64.dp))

        PushToTalkButton(
            voiceState = voiceState,
            onStart = { viewModel.startListening() },
            onStop = { viewModel.stopListening() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.voice_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun FeedbackDisplay(voiceState: VoiceState) {
    val text = when (voiceState) {
        is VoiceState.Idle -> "Wait for command..."
        is VoiceState.Listening -> "Listening..."
        is VoiceState.Partial -> voiceState.text
        is VoiceState.Success -> voiceState.text
        is VoiceState.Error -> "Error: ${voiceState.message}"
    }

    val color = if (voiceState is VoiceState.Success) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 100.dp)
    )
}

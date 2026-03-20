package com.akproleter.mobile.ui.voice.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.akproleter.mobile.voice.VoiceState

@Composable
fun PushToTalkButton(
    voiceState: VoiceState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isListening = voiceState is VoiceState.Listening || voiceState is VoiceState.Partial

    // Trigger a success haptic whenever recognition completes successfully (Phase 3)
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Success) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val color by animateColorAsState(
        targetValue = when (voiceState) {
            is VoiceState.Listening, is VoiceState.Partial -> MaterialTheme.colorScheme.error
            is VoiceState.Success -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "color"
    )

    // Pulsing ring visible only while actively listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated pulsing ring visible while listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
        }

        // Main mic button — press-and-hold to record
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Haptic on press (interaction feedback)
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onStart()
                            tryAwaitRelease()
                            // Haptic on release (ended recording)
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onStop()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

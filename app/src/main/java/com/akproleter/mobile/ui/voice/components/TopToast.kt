package com.akproleter.mobile.ui.voice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akproleter.mobile.ui.voice.ProcessState

@Composable
fun TopToast(
    processState: ProcessState,
    modifier: Modifier = Modifier
) {
    val isVisible = processState is ProcessState.Success || processState is ProcessState.Error
    val message = when (processState) {
        is ProcessState.Success -> processState.message
        is ProcessState.Error -> processState.message
        else -> ""
    }
    val backgroundColor = when (processState) {
        is ProcessState.Success -> Color(0xFF4CAF50) // Light Green
        is ProcessState.Error -> Color(0xFFF44336)   // Red
        else -> Color.Transparent
    }
    val contentColor = when (processState) {
        is ProcessState.Success -> Color.White
        is ProcessState.Error -> Color.White
        else -> Color.Transparent
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

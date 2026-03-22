package com.akproleter.mobile.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akproleter.mobile.R
import com.akproleter.mobile.ui.voice.components.PushToTalkButton
import com.akproleter.mobile.ui.voice.components.TopToast
import com.akproleter.mobile.voice.VoiceState
import com.akproleter.mobile.voice.WhisperModelManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    userName: String?,
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val processState by viewModel.processState.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val transcription by viewModel.transcribedText.collectAsStateWithLifecycle()
    val modelState by viewModel.modelDownloadState.collectAsStateWithLifecycle()

    LaunchedEffect(processState) {
        if (processState is ProcessState.Success || processState is ProcessState.Error) {
            delay(5000)
            viewModel.clearProcessState()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            viewModel.startListening(context)
        } else {
            android.widget.Toast.makeText(
                context,
                "Microphone permission is required to use voice assistant",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AK PROLETER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (userName != null) {
                            Text(
                                text = "Welcome, $userName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.History,
                            contentDescription = "Recording History"
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            Text(
                text = "Voice Assistant",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Language toggle chip: EN / SR
            LanguageToggleChip(
                selectedLanguage = selectedLanguage,
                onToggle = { viewModel.toggleLanguage() },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (transcription.isNotBlank() && processState is ProcessState.Idle) {
                EditableTranscription(
                    text = transcription,
                    onTextChange = { viewModel.updateTranscription(it) },
                    onSave = { viewModel.saveTranscription() },
                    onCancel = { viewModel.cancelTranscription() }
                )
            } else {
                when (val pState = processState) {
                    is ProcessState.Success -> { /* Handled by TopToast */ }
                    is ProcessState.Error   -> { /* Handled by TopToast */ }
                    is ProcessState.Processing -> {
                        CircularProgressIndicator()
                        Text(
                            text = "Processing...",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    else -> {
                        // Show Whisper "transcribing" spinner when offline
                        if (voiceState is VoiceState.Processing) {
                            CircularProgressIndicator()
                            Text(
                                text = "Transcribing...",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else {
                            FeedbackDisplay(voiceState)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            if (transcription.isBlank() || processState !is ProcessState.Idle) {
                PushToTalkButton(
                    voiceState = voiceState,
                    onStart = {
                        val audioCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        val locationCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (audioCheck == PackageManager.PERMISSION_GRANTED && locationCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.startListening(context)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
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

            TopToast(
                processState = processState
            )

            // Model download banner — shown only during first-time setup
            if (modelState is WhisperModelManager.DownloadState.Progress) {
                val progress = modelState as WhisperModelManager.DownloadState.Progress
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Setting up offline voice (one-time setup)...",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${progress.fileName}: ${progress.percent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        LinearProgressIndicator(
                            progress = { progress.percent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                }
            }

            if (modelState is WhisperModelManager.DownloadState.Error) {
                val err = modelState as WhisperModelManager.DownloadState.Error
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "Offline voice setup failed: ${err.message}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditableTranscription(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            label = { Text("Correct the text") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
        )

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(id = R.string.cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.save))
            }
        }
    }
}

@Composable
private fun LanguageToggleChip(
    selectedLanguage: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnglish = selectedLanguage.startsWith("en")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = isEnglish,
            onClick = { if (!isEnglish) onToggle() },
            label = { Text("EN") }
        )
        FilterChip(
            selected = !isEnglish,
            onClick = { if (isEnglish) onToggle() },
            label = { Text("SR") }
        )
    }
}

@Composable
fun FeedbackDisplay(voiceState: VoiceState) {
    val text = when (voiceState) {
        is VoiceState.Idle -> "Wait for command..."
        is VoiceState.Listening -> "Listening..."
        is VoiceState.Processing -> ""
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

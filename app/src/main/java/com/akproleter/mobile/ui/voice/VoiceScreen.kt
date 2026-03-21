package com.akproleter.mobile.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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
import com.akproleter.mobile.voice.VoiceState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    userName: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val processState by viewModel.processState.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(processState) {
        if (processState is ProcessState.Success || processState is ProcessState.Error) {
            delay(5000)
            viewModel.clearProcessState()
            viewModel.reset() // ensure recognized voice text is hidden as well
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
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            when (val pState = processState) {
                is ProcessState.Success -> {
                    Text(
                        text = pState.message,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(min = 100.dp)
                    )
                }
                is ProcessState.Error -> {
                    Text(
                        text = pState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(min = 100.dp)
                    )
                }
                else -> {
                    FeedbackDisplay(voiceState)
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

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

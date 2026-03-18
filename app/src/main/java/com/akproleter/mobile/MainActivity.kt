package com.akproleter.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akproleter.mobile.ui.auth.AuthState
import com.akproleter.mobile.ui.auth.AuthViewModel
import com.akproleter.mobile.ui.auth.LoginScreen
import com.akproleter.mobile.ui.theme.AKProleterMobileTheme
import com.akproleter.mobile.ui.voice.VoiceScreen
import com.akproleter.mobile.ui.voice.VoiceViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AKProleterMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AkProleterAppContent()
                }
            }
        }
    }
}

@Composable
fun AkProleterAppContent() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    Crossfade(targetState = authState, label = "screenTransition") { state ->
        when (state) {
            is AuthState.Authenticated -> {
                val voiceViewModel: VoiceViewModel = hiltViewModel()
                VoiceScreen(viewModel = voiceViewModel)
            }
            else -> {
                LoginScreen(viewModel = authViewModel)
            }
        }
    }
}

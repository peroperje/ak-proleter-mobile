package com.akproleter.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akproleter.mobile.data.local.SessionManager
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.models.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkSession()
    }

    fun login(email: String, pwhash: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email, pwhash))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    sessionManager.saveToken(body.token)
                    body.user.name?.let { sessionManager.saveName(it) }
                    _authState.value = AuthState.Authenticated(body.user.name)
                } else {
                    _authState.value = AuthState.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    private fun checkSession() {
        if (sessionManager.getToken() != null) {
            _authState.value = AuthState.Authenticated(sessionManager.getName())
        }
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val userName: String?) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

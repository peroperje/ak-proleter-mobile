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

    private fun checkSession() {
        if (sessionManager.getToken() != null) {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, pwhash: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // In a real app, we'd hash the password or use NextAuth's CSRF/Login flow
                val response = apiService.login(LoginRequest(email, pwhash))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    sessionManager.saveToken(loginResponse.token)
                    _authState.value = AuthState.Authenticated
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
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

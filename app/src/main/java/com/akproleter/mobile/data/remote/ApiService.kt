package com.akproleter.mobile.data.remote

import com.akproleter.mobile.data.remote.models.LoginRequest
import com.akproleter.mobile.data.remote.models.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login") // Need to check correct NextAuth credentials endpoint
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/voice/process")
    suspend fun processVoice(
        @Body voiceRequest: VoiceRequest
    ): Response<VoiceResponse>
}

data class VoiceRequest(
    val text: String,
    val language: String,
    val role: String,
    val contextHints: Map<String, Any?>?
)

data class VoiceResponse(
    val status: String,
    val message: String?,
    val data: Map<String, Any?>?
)

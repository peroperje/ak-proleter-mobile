package com.akproleter.mobile.data.remote

import com.akproleter.mobile.data.remote.models.LoginRequest
import com.akproleter.mobile.data.remote.models.LoginResponse
import com.akproleter.mobile.util.Constants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST(Constants.LOGIN_ENDPOINT)
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST(Constants.VOICE_PROCESS_ENDPOINT)
    suspend fun processVoice(
        @Body voiceRequest: VoiceRequest
    ): Response<VoiceResponse>

    @POST(Constants.SUBMIT_RESULT_ENDPOINT)
    suspend fun submitResult(
        @Body result: PendingResultRequest
    ): Response<Unit>
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

data class PendingResultRequest(
    val athleteId: String,
    val eventId: String,
    val disciplineId: String,
    val score: String?,
    val notes: String?,
    val timestamp: Long
)

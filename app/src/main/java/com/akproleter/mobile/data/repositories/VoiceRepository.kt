package com.akproleter.mobile.data.repositories

import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.VoiceRequest
import com.akproleter.mobile.voice.VoiceState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: AkProleterDao
) {
    suspend fun processVoiceCommand(
        text: String,
        language: String,
        role: String,
        contextHints: Map<String, Any?>?
    ): Result<String> {
        return try {
            val response = apiService.processVoice(
                VoiceRequest(text, language, role, contextHints)
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Success")
            } else {
                Result.failure(Exception(response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

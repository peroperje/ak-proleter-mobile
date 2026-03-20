package com.akproleter.mobile.data.repositories

import android.util.Log
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.local.entities.PendingResultEntity
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.VoiceRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: AkProleterDao
) {
    /**
     * Sends a voice command to the backend for AI processing.
     * Returns the AI-generated response message on success.
     */
    suspend fun processVoiceCommand(
        text: String,
        language: String,
        role: String,
        timestamp: Long,
        lat: Float?,
        lon: Float?,
        location: String?
    ): Result<String> {
        return try {
            val response = apiService.processVoice(
                VoiceRequest(text, language, role, timestamp, lat, lon, location)
            )
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "OK"
                Log.d(TAG, "processVoiceCommand success: $message")
                Result.success(message)
            } else {
                val errBody = response.errorBody()?.string()
                Log.w(TAG, "processVoiceCommand HTTP error: $errBody")
                Result.failure(Exception("Server error: $errBody"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "processVoiceCommand network failure — saving as PendingResult", e)
            // Offline-first: persist the utterance locally so SyncWorker can retry later.
            // The text is stored in 'notes' since we don't yet have structured field parsing here.
            savePendingResult(
                athleteId = "",
                eventId = "",
                disciplineId = "",
                score = null,
                notes = text,
                lat = lat,
                lon = lon,
                location = location
            )
            Result.failure(e)
        }
    }


    /**
     * Saves a record that failed to sync to the local Room database.
     * SyncWorker will retry uploading it when connectivity is restored.
     */
    suspend fun savePendingResult(
        athleteId: String,
        eventId: String,
        disciplineId: String,
        score: String?,
        notes: String?,
        lat: Float? = null,
        lon: Float? = null,
        location: String? = null
    ) {
        val entity = PendingResultEntity(
            athleteId = athleteId,
            eventId = eventId,
            disciplineId = disciplineId,
            score = score,
            notes = notes,
            lat = lat,
            lon = lon,
            location = location,
            isSynced = false
        )
        dao.insertPendingResult(entity)
        Log.d(TAG, "Saved PendingResultEntity (localId will be auto-generated)")
    }

    companion object {
        private const val TAG = "VoiceRepository"
    }
}

package com.akproleter.mobile.data.repositories

import android.util.Log
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.local.entities.PendingResultEntity
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.VoiceRequest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import com.akproleter.mobile.data.local.entities.VoiceRecordEntity
import com.akproleter.mobile.data.local.entities.RecordStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: AkProleterDao,
    @ApplicationContext private val context: Context
) {
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Deletes a voice record.
     * For PENDING status: Delete locally only.
     * For SAVED status: Delete via API first (if online), then locally.
     */
    suspend fun deleteVoiceRecord(record: VoiceRecordEntity): Result<Unit> {
        return try {
            when (record.status) {
                RecordStatus.PENDING -> {
                    dao.deleteVoiceRecord(record)
                    Result.success(Unit)
                }
                RecordStatus.SAVED -> {
                    if (!isOnline()) {
                        return Result.failure(Exception("Cannot delete saved record while offline"))
                    }
                    val resultId = record.resultId ?: return Result.failure(Exception("Missing backend result ID"))
                    
                    val response = apiService.deleteResult(resultId)
                    if (response.isSuccessful) {
                        dao.deleteVoiceRecord(record)
                        Result.success(Unit)
                    } else {
                        val errMsg = response.errorBody()?.string() ?: "Unknown error"
                        Result.failure(Exception("Failed to delete from server: $errMsg"))
                    }
                }
                RecordStatus.PROCESSING -> {
                    Result.failure(Exception("Cannot delete record while it is being processed"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting record: ${e.message}", e)
            Result.failure(e)
        }
    }
    /**
     * Sends a voice command to the backend for AI processing.
     * Returns the AI-generated response message on success.
     */
    suspend fun processVoiceCommand(
        text: String,
        language: String,
        timestamp: Long,
        lat: Float?,
        lon: Float?,
        location: String?
    ): Result<String> {
        if (!isOnline()) {
            Log.d(TAG, "Offline mode: Saving transcript as PENDING")
            val entity = VoiceRecordEntity(
                voiceInput = text,
                status = RecordStatus.PENDING
            )
            dao.insertVoiceRecord(entity)
            return Result.success("Saved locally (offline)")
        }

        return try {
            val response = apiService.processVoice(
                VoiceRequest(text, language, timestamp, lat, lon, location)
            )
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "OK"
                val responseData = response.body()?.data
                
                // Online Success -> Save to DB as SAVED
                val entity = VoiceRecordEntity(
                    resultId = responseData?.get("id") as? String,
                    discipline = responseData?.get("discipline") as? String,
                    voiceLogId = responseData?.get("voiceLogId") as? String,
                    score = responseData?.get("score")?.toString(),
                    formattedScore = responseData?.get("formattedScore") as? String,
                    voiceInput = text,
                    status = RecordStatus.SAVED
                )
                dao.insertVoiceRecord(entity)
                
                Log.d(TAG, "processVoiceCommand success: $message")
                Result.success(message)
            } else {
                val errBody = response.errorBody()?.string()
                Log.w(TAG, "processVoiceCommand HTTP error: $errBody")
                // Online Error -> Do NOT save to local DB
                Result.failure(Exception("Server error: $errBody"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "processVoiceCommand network exception — saving as PENDING", e)
            val entity = VoiceRecordEntity(
                voiceInput = text,
                status = RecordStatus.PENDING
            )
            dao.insertVoiceRecord(entity)
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

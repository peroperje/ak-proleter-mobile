package com.akproleter.mobile.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.PendingResultRequest
import com.akproleter.mobile.data.remote.VoiceRequest
import com.akproleter.mobile.data.local.entities.RecordStatus
import kotlinx.coroutines.delay
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: AkProleterDao,
    private val apiService: ApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started — querying unsynced results")
        val unsyncedResults = dao.getUnsyncedResults()

        if (unsyncedResults.isEmpty()) {
            Log.d(TAG, "No unsynced results — nothing to do")
            return Result.success()
        }

        Log.d(TAG, "Found ${unsyncedResults.size} unsynced result(s) to submit")
        var hasFailure = false

        for (entity in unsyncedResults) {
            try {
                val response = apiService.submitResult(
                    PendingResultRequest(
                        athleteId = entity.athleteId,
                        eventId = entity.eventId,
                        disciplineId = entity.disciplineId,
                        score = entity.score,
                        notes = entity.notes,
                        timestamp = entity.timestamp,
                        lat = entity.lat,
                        lon = entity.lon,
                        location = entity.location
                    )
                )
                if (response.isSuccessful) {
                    // Mark as synced in local DB
                    dao.updatePendingResult(entity.copy(isSynced = true))
                    Log.d(TAG, "Synced PendingResult id=${entity.localId} successfully")
                } else {
                    Log.w(TAG, "Server rejected PendingResult id=${entity.localId}: ${response.errorBody()?.string()}")
                    hasFailure = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error syncing PendingResult id=${entity.localId}: ${e.message}")
                hasFailure = true
            }
        }

        // --- Process VoiceRecordEntity with PENDING status ---
        val pendingVoiceRecords = dao.getVoiceRecordsByStatus(RecordStatus.PENDING)
        if (pendingVoiceRecords.isNotEmpty()) {
            Log.d(TAG, "Found ${pendingVoiceRecords.size} PENDING voice record(s)")
            for ((index, record) in pendingVoiceRecords.withIndex()) {
                try {
                    // Mark PROCESSING
                    dao.updateVoiceRecord(record.copy(status = RecordStatus.PROCESSING))
                    
                    val response = apiService.processVoice(
                        VoiceRequest(
                            text = record.voiceInput,
                            language = "en-US", // Defaulting to en-US for offline saved records 
                            timestamp = record.createdAt,
                            lat = null, lon = null, location = null
                        )
                    )
                    
                    if (response.isSuccessful) {
                        val responseData = response.body()?.data
                        val updatedRecord = record.copy(
                            resultId = responseData?.get("id") as? String,
                            discipline = responseData?.get("discipline") as? String,
                            voiceLogId = responseData?.get("voiceLogId") as? String,
                            score = responseData?.get("score")?.toString(),
                            formattedScore = responseData?.get("formattedScore") as? String,
                            status = RecordStatus.SAVED,
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.updateVoiceRecord(updatedRecord)
                        Log.d(TAG, "Synced VoiceRecord ${record.uuid} successfully")
                    } else {
                        Log.w(TAG, "Server rejected VoiceRecord ${record.uuid}. Reverting to PENDING.")
                        dao.updateVoiceRecord(record.copy(status = RecordStatus.PENDING))
                        hasFailure = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network error syncing VoiceRecord ${record.uuid}: ${e.message}")
                    dao.updateVoiceRecord(record.copy(status = RecordStatus.PENDING))
                    hasFailure = true
                }
                
                // Add 1-min pause if there are more items to process
                if (index < pendingVoiceRecords.size - 1) {
                    Log.d(TAG, "Waiting 1 minute before syncing next voice record...")
                    delay(60_000L)
                }
            }
        }

        return if (hasFailure) {
            Log.w(TAG, "SyncWorker completed with some failures — will retry")
            Result.retry()
        } else {
            Log.d(TAG, "SyncWorker completed successfully")
            Result.success()
        }
    }

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "ak_proleter_sync_worker"
    }
}

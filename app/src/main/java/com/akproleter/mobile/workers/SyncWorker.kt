package com.akproleter.mobile.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.remote.ApiService
import com.akproleter.mobile.data.remote.PendingResultRequest
import com.akproleter.mobile.data.repositories.MetadataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: AkProleterDao,
    private val apiService: ApiService,
    private val metadataRepository: MetadataRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started — syncing metadata first")
        // We sync metadata first so that result submission might use up-to-date IDs
        // and voice processing has latest context hints.
        metadataRepository.syncMetadata()

        Log.d(TAG, "Querying unsynced results")
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
                        timestamp = entity.timestamp
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

package com.akproleter.mobile.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akproleter.mobile.data.local.AkProleterDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: AkProleterDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "CleanupWorker started")
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
        return try {
            dao.deleteRecordsOlderThan(cutoffTime)
            Log.d(TAG, "CleanupWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during CleanupWorker", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "CleanupWorker"
        const val WORK_NAME = "ak_proleter_cleanup_worker"
    }
}

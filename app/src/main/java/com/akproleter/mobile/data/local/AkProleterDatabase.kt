package com.akproleter.mobile.data.local

import androidx.room.*
import com.akproleter.mobile.data.local.entities.PendingResultEntity
import com.akproleter.mobile.data.local.entities.RecordStatus
import com.akproleter.mobile.data.local.entities.VoiceRecordEntity

@Dao
interface AkProleterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingResult(result: PendingResultEntity)

    @Query("SELECT * FROM pending_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<PendingResultEntity>

    @Update
    suspend fun updatePendingResult(result: PendingResultEntity)

    // VoiceRecord functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceRecord(record: VoiceRecordEntity)

    @Update
    suspend fun updateVoiceRecord(record: VoiceRecordEntity)

    @Query("SELECT * FROM voice_records ORDER BY createdAt DESC")
    suspend fun getAllVoiceRecords(): List<VoiceRecordEntity>

    @Query("SELECT * FROM voice_records WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getVoiceRecordsByStatus(status: RecordStatus): List<VoiceRecordEntity>

    @Query("DELETE FROM voice_records WHERE createdAt < :cutoffTime")
    suspend fun deleteRecordsOlderThan(cutoffTime: Long)

    @Delete
    suspend fun deleteVoiceRecord(record: VoiceRecordEntity)
}

@Database(
    entities = [
        PendingResultEntity::class,
        VoiceRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AkProleterDatabase : RoomDatabase() {
    abstract fun dao(): AkProleterDao

    companion object {
        const val DATABASE_NAME = "ak_proleter_db"
    }
}

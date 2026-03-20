package com.akproleter.mobile.data.local

import androidx.room.*
import com.akproleter.mobile.data.local.entities.PendingResultEntity

@Dao
interface AkProleterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingResult(result: PendingResultEntity)

    @Query("SELECT * FROM pending_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<PendingResultEntity>

    @Update
    suspend fun updatePendingResult(result: PendingResultEntity)
}

@Database(
    entities = [
        PendingResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AkProleterDatabase : RoomDatabase() {
    abstract fun dao(): AkProleterDao

    companion object {
        const val DATABASE_NAME = "ak_proleter_db"
    }
}

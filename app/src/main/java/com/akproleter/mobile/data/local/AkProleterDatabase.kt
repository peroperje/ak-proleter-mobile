package com.akproleter.mobile.data.local

import androidx.room.*
import com.akproleter.mobile.data.local.entities.AthleteEntity
import com.akproleter.mobile.data.local.entities.DisciplineEntity
import com.akproleter.mobile.data.local.entities.EventEntity
import com.akproleter.mobile.data.local.entities.PendingResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AkProleterDao {
    @Query("SELECT * FROM athletes")
    fun getAllAthletes(): Flow<List<AthleteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAthletes(athletes: List<AthleteEntity>)

    @Query("SELECT * FROM disciplines")
    fun getAllDisciplines(): Flow<List<DisciplineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplines(disciplines: List<DisciplineEntity>)

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingResult(result: PendingResultEntity)

    @Query("SELECT * FROM pending_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<PendingResultEntity>

    @Update
    suspend fun updatePendingResult(result: PendingResultEntity)
}

@Database(
    entities = [
        AthleteEntity::class,
        DisciplineEntity::class,
        EventEntity::class,
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

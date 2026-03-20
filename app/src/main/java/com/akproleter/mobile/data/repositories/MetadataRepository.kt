package com.akproleter.mobile.data.repositories

import android.util.Log
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.local.entities.AthleteEntity
import com.akproleter.mobile.data.local.entities.DisciplineEntity
import com.akproleter.mobile.data.local.entities.EventEntity
import com.akproleter.mobile.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepository @Inject constructor(
    private val apiService: ApiService,
    private val dao: AkProleterDao
) {
    suspend fun syncMetadata(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting metadata sync...")
            
            val athletesRes = apiService.getAthletes()
            if (athletesRes.isSuccessful) {
                val entities = athletesRes.body()?.map { dto ->
                    AthleteEntity(dto.id, dto.name, dto.categoryId, dto.gender)
                } ?: emptyList()
                dao.insertAthletes(entities)
                Log.d(TAG, "Synced ${entities.size} athletes")
            }

            val disciplinesRes = apiService.getDisciplines()
            if (disciplinesRes.isSuccessful) {
                val entities = disciplinesRes.body()?.map { dto ->
                    DisciplineEntity(dto.id, dto.name, dto.internationalSign)
                } ?: emptyList()
                dao.insertDisciplines(entities)
                Log.d(TAG, "Synced ${entities.size} disciplines")
            }

            val eventsRes = apiService.getEvents()
            if (eventsRes.isSuccessful) {
                val entities = eventsRes.body()?.map { dto ->
                    EventEntity(dto.id, dto.title, dto.location, dto.startDate)
                } ?: emptyList()
                dao.insertEvents(entities)
                Log.d(TAG, "Synced ${entities.size} events")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Metadata sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun isMetadataEmpty(): Boolean {
        return dao.getAthletesOnce().isEmpty()
    }

    companion object {
        private const val TAG = "MetadataRepository"
    }
}

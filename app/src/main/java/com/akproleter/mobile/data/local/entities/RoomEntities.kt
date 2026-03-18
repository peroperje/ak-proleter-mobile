package com.akproleter.mobile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athletes")
data class AthleteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String?,
    val gender: String
)

@Entity(tableName = "disciplines")
data class DisciplineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val internationalSign: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val location: String,
    val startDate: Long // UNIX timestamp
)

@Entity(tableName = "pending_results")
data class PendingResultEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val athleteId: String,
    val eventId: String,
    val disciplineId: String,
    val score: String?,
    val notes: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

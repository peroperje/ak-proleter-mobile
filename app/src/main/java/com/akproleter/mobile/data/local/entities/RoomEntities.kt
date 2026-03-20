package com.akproleter.mobile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_results")
data class PendingResultEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val athleteId: String,
    val eventId: String,
    val disciplineId: String,
    val score: String?,
    val notes: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val lat: Float? = null,
    val lon: Float? = null,
    val location: String? = null,
    val isSynced: Boolean = false
)

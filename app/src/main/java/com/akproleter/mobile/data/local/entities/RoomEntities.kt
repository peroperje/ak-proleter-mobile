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

enum class RecordStatus {
    SAVED, PENDING, PROCESSING
}

@Entity(tableName = "voice_records")
data class VoiceRecordEntity(
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resultId: String? = null,
    val discipline: String? = null,
    val voiceLogId: String? = null,
    val score: String? = null,
    val formattedScore: String? = null,
    val voiceInput: String,
    val status: RecordStatus,
    val markedIncorrect: Boolean = false
)

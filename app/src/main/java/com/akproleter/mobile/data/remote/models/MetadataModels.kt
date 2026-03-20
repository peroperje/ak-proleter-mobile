package com.akproleter.mobile.data.remote.models

data class AthleteDto(
    val id: String,
    val name: String,
    val categoryId: String?,
    val gender: String
)

data class DisciplineDto(
    val id: String,
    val name: String,
    val internationalSign: String
)

data class EventDto(
    val id: String,
    val title: String,
    val location: String,
    val startDate: Long // UNIX timestamp
)

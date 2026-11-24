package com.hariku.feature.mood.domain.model

data class MoodEntry(
    val id: Long = 0L,
    val mood: MoodType,
    val timestamp: Long,
    val note: String = ""
)

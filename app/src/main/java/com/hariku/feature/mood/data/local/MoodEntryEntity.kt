package com.hariku.feature.mood.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    val moodType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
)

package com.hariku.feature.mood.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hariku.core.auth.LOCAL_GUEST_USER_ID

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    val moodType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val userId: String = LOCAL_GUEST_USER_ID,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
)

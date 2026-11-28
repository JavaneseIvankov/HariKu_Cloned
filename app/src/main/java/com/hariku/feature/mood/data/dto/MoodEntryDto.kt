package com.hariku.feature.mood.data.dto

import androidx.annotation.Keep
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType

/**
 * Representasi dokumen mood pada Cloud Firestore di `users/{userId}/moods/{id}`.
 */
@Keep
data class MoodEntryDto(
    val id: Long = 0L,
    val userId: String = "",
    val moodType: String = "",
    val timestamp: Long = 0L,
    val note: String = "",
    val updatedAt: Long = 0L
) {
    fun toDomain(): MoodEntry? {
        if (id <= 0L) return null
        val rawMoodType: String? = moodType
        val mood = rawMoodType?.let(MoodType::fromLabel) ?: return null
        val safeNote: String = (note as String?).orEmpty()
        return MoodEntry(
            id = id,
            mood = mood,
            timestamp = timestamp,
            note = safeNote
        )
    }
}

fun MoodEntry.toDto(
    userId: String,
    updatedAt: Long = timestamp
): MoodEntryDto = MoodEntryDto(
    id = id,
    userId = userId,
    moodType = mood.label,
    timestamp = timestamp,
    note = note,
    updatedAt = updatedAt
)

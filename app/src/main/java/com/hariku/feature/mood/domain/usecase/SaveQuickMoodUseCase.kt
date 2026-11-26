package com.hariku.feature.mood.domain.usecase

import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository

class SaveQuickMoodUseCase(
    private val repository: MoodRepository
) {
    suspend operator fun invoke(
        mood: MoodType,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Long> = repository.saveMood(
        mood = mood,
        note = MoodTrackerLogic.sanitizeNote(note),
        timestamp = timestamp
    )
}

package com.hariku.feature.mood.domain.usecase

import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository

class UpdateMoodEntryUseCase(
    private val repository: MoodRepository
) {
    suspend operator fun invoke(
        entryId: Long,
        mood: MoodType,
        note: String
    ): Result<Unit> = repository.updateMood(
        entryId = entryId,
        mood = mood,
        note = MoodTrackerLogic.sanitizeNote(note)
    )
}

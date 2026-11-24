package com.hariku.feature.mood.domain.usecase

import com.hariku.feature.mood.domain.repository.MoodRepository

class DeleteMoodEntryUseCase(
    private val repository: MoodRepository
) {
    suspend operator fun invoke(entryId: Long): Result<Unit> = repository.deleteMood(entryId)
}

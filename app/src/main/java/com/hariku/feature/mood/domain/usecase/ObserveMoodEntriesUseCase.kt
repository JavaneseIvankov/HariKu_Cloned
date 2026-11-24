package com.hariku.feature.mood.domain.usecase

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow

class ObserveMoodEntriesUseCase(
    private val repository: MoodRepository
) {
    operator fun invoke(): Flow<List<MoodEntry>> = repository.observeMoodEntries()
}

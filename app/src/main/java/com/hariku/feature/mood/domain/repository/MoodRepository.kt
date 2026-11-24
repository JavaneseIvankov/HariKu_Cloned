package com.hariku.feature.mood.domain.repository

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import kotlinx.coroutines.flow.Flow

interface MoodRepository {
    fun observeMoodEntries(): Flow<List<MoodEntry>>

    suspend fun saveMood(
        mood: MoodType,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Result<Long>

    suspend fun deleteMood(entryId: Long): Result<Unit>
}

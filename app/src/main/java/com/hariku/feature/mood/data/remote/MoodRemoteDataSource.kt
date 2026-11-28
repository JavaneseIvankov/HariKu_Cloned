package com.hariku.feature.mood.data.remote

import com.hariku.feature.mood.data.dto.MoodEntryDto
import kotlinx.coroutines.flow.Flow

interface MoodRemoteDataSource {
    fun observeAll(userId: String): Flow<List<MoodEntryDto>>
    suspend fun save(entry: MoodEntryDto, userId: String): Long?
    suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String)
    suspend fun update(
        entryId: Long,
        userId: String,
        moodType: String,
        note: String,
        updatedAt: Long
    ): Boolean
    suspend fun delete(entryId: Long, userId: String): Boolean
}

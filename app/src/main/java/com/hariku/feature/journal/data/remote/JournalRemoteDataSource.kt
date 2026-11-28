package com.hariku.feature.journal.data.remote

import com.hariku.feature.journal.data.dto.JournalEntryDto
import kotlinx.coroutines.flow.Flow

interface JournalRemoteDataSource {
    fun observeAll(userId: String): Flow<List<JournalEntryDto>>
    fun observeLatestTag(userId: String): Flow<String?>
    suspend fun getById(id: Long, userId: String): JournalEntryDto?
    suspend fun save(entry: JournalEntryDto, userId: String): Long?
    suspend fun upsertBatch(entries: List<JournalEntryDto>, userId: String)
    suspend fun delete(id: Long, userId: String): Boolean
    suspend fun togglePin(id: Long, userId: String): Boolean
}

package com.hariku.feature.journal.domain.repository

import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeEntries(): Flow<List<JournalEntry>>

    /** Tag entri jurnal terakhir; null bila belum ada entri. */
    fun observeLatestTag(): Flow<JournalCategoryTag?>

    suspend fun getById(id: Long): Result<JournalEntry>

    /** Insert (id == 0) atau update; mengembalikan id entri. */
    suspend fun save(entry: JournalEntry): Result<Long>

    suspend fun delete(id: Long): Result<Unit>

    suspend fun togglePin(id: Long): Result<Unit>
}

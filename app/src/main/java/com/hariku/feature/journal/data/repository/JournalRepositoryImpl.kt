package com.hariku.feature.journal.data.repository

import android.database.sqlite.SQLiteException
import com.hariku.feature.journal.data.local.JournalDao
import com.hariku.feature.journal.data.mapper.JournalMapper
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.repository.JournalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JournalRepositoryImpl(
    private val journalDao: JournalDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JournalRepository {

    override fun observeEntries(): Flow<List<JournalEntry>> =
        journalDao.observeAll()
            .map { entities -> entities.map(JournalMapper::toDomain) }
            .flowOn(ioDispatcher)

    override fun observeLatestTag(): Flow<JournalCategoryTag?> =
        journalDao.getLatestTag()
            .map { tagName -> tagName?.let(JournalCategoryTag::fromNameOrDefault) }
            .flowOn(ioDispatcher)

    override suspend fun getById(id: Long): Result<JournalEntry> = withContext(ioDispatcher) {
        try {
            val entity = journalDao.getById(id)
                ?: return@withContext Result.failure(NoSuchElementException("Jurnal dengan id $id tidak ditemukan"))
            Result.success(JournalMapper.toDomain(entity))
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun save(entry: JournalEntry): Result<Long> = withContext(ioDispatcher) {
        try {
            val entity = JournalMapper.toEntity(entry)
            val savedId = if (entry.id == 0L) {
                journalDao.insert(entity)
            } else {
                journalDao.update(entity)
                entry.id
            }
            Result.success(savedId)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun delete(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            journalDao.delete(id)
            Result.success(Unit)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun togglePin(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            journalDao.togglePin(id)
            Result.success(Unit)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }
}

package com.hariku.feature.journal.data.repository

import android.database.sqlite.SQLiteException
import com.hariku.core.auth.toScopedUserId
import com.hariku.feature.journal.data.local.JournalDao
import com.hariku.feature.journal.data.mapper.JournalMapper
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.repository.JournalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class JournalRepositoryImpl(
    private val journalDao: JournalDao,
    private val userIdFlow: Flow<String?> = flowOf(null),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JournalRepository {

    private fun scopedUserIds(): Flow<String> =
        userIdFlow.map { it.toScopedUserId() }.distinctUntilChanged()

    private suspend fun currentUserId(): String = userIdFlow.first().toScopedUserId()

    override fun observeEntries(): Flow<List<JournalEntry>> =
        scopedUserIds()
            .flatMapLatest { userId ->
                journalDao.observeAll(userId)
                    .map { entities -> entities.map(JournalMapper::toDomain) }
            }
            .flowOn(ioDispatcher)

    override fun observeLatestTag(): Flow<JournalCategoryTag?> =
        scopedUserIds()
            .flatMapLatest { userId ->
                journalDao.getLatestTag(userId)
                    .map { tagName -> tagName?.let(JournalCategoryTag::fromNameOrDefault) }
            }
            .flowOn(ioDispatcher)

    override suspend fun getById(id: Long): Result<JournalEntry> = withContext(ioDispatcher) {
        try {
            val entity = journalDao.getById(id, currentUserId())
                ?: return@withContext Result.failure(NoSuchElementException("Jurnal dengan id $id tidak ditemukan"))
            Result.success(JournalMapper.toDomain(entity))
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun save(entry: JournalEntry): Result<Long> = withContext(ioDispatcher) {
        try {
            val userId = currentUserId()
            val savedId = if (entry.id == 0L) {
                journalDao.insert(JournalMapper.toEntity(entry, userId))
            } else {
                journalDao.getById(entry.id, userId)
                    ?: return@withContext Result.failure(NoSuchElementException("Jurnal dengan id ${entry.id} tidak ditemukan"))
                journalDao.update(JournalMapper.toEntity(entry, userId))
                entry.id
            }
            Result.success(savedId)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun delete(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            journalDao.delete(id, currentUserId())
            Result.success(Unit)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }

    override suspend fun togglePin(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            journalDao.togglePin(id, currentUserId())
            Result.success(Unit)
        } catch (error: SQLiteException) {
            Result.failure(error)
        }
    }
}

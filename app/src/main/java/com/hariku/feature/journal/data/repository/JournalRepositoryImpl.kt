package com.hariku.feature.journal.data.repository

import com.hariku.feature.journal.data.dto.toDto
import com.hariku.feature.journal.data.local.JournalLocalMigrator
import com.hariku.feature.journal.data.remote.JournalRemoteDataSource
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.repository.JournalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class JournalRepositoryImpl(
    private val remoteDataSource: JournalRemoteDataSource,
    private val userIdFlow: Flow<String?> = flowOf(null),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val localMigrator: JournalLocalMigrator? = null
) : JournalRepository {

    private fun scopedUserIds(): Flow<String?> =
        userIdFlow
            .map { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinctUntilChanged()

    private suspend fun currentUserIdOrNull(): String? =
        userIdFlow.first()?.trim()?.takeIf(String::isNotEmpty)

    override fun observeEntries(): Flow<List<JournalEntry>> =
        scopedUserIds()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    flow<List<JournalEntry>> {
                        localMigrator?.migrateIfNeeded(userId)
                        emitAll(
                            remoteDataSource.observeAll(userId)
                                .map { entries -> entries.map { it.toDomain() } }
                        )
                    }
                }
            }
            .flowOn(ioDispatcher)

    override fun observeLatestTag(): Flow<JournalCategoryTag?> =
        scopedUserIds()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    flow<JournalCategoryTag?> {
                        localMigrator?.migrateIfNeeded(userId)
                        emitAll(
                            remoteDataSource.observeLatestTag(userId)
                                .map { tagName -> tagName?.let(JournalCategoryTag::fromNameOrDefault) }
                        )
                    }
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun getById(id: Long): Result<JournalEntry> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            localMigrator?.migrateIfNeeded(userId)
            val dto = remoteDataSource.getById(id, userId)
                ?: return@withContext Result.failure(NoSuchElementException("Jurnal dengan id $id tidak ditemukan"))
            Result.success(dto.toDomain())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun save(entry: JournalEntry): Result<Long> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            localMigrator?.migrateIfNeeded(userId)
            val savedId = remoteDataSource.save(entry.toDto(userId), userId)
                ?: return@withContext Result.failure(NoSuchElementException("Jurnal dengan id ${entry.id} tidak ditemukan"))
            Result.success(savedId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun delete(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            localMigrator?.migrateIfNeeded(userId)
            val deleted = remoteDataSource.delete(id, userId)
            if (!deleted) {
                return@withContext Result.failure(NoSuchElementException("Jurnal dengan id $id tidak ditemukan"))
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun togglePin(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            localMigrator?.migrateIfNeeded(userId)
            val updated = remoteDataSource.togglePin(id, userId)
            if (!updated) {
                return@withContext Result.failure(NoSuchElementException("Jurnal dengan id $id tidak ditemukan"))
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

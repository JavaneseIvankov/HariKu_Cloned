package com.hariku.feature.mood.data.repository

import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.local.MoodLocalMigrator
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
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
class MoodRepositoryImpl(
    private val remoteDataSource: MoodRemoteDataSource,
    private val userIdFlow: Flow<String?> = flowOf(null),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val localMigrator: MoodLocalMigrator? = null
) : MoodRepository {

    private fun scopedUserIds(): Flow<String?> =
        userIdFlow
            .map { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinctUntilChanged()

    private suspend fun currentUserIdOrNull(): String? =
        userIdFlow.first()?.trim()?.takeIf(String::isNotEmpty)

    override fun observeMoodEntries(): Flow<List<MoodEntry>> =
        scopedUserIds()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    flow {
                        localMigrator?.migrateIfNeeded(userId)
                        emitAll(
                            remoteDataSource.observeAll(userId)
                                .map { entries -> entries.mapNotNull { it.toDomain() } }
                        )
                    }
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun saveMood(
        mood: MoodType,
        note: String,
        timestamp: Long
    ): Result<Long> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            val savedId = remoteDataSource.save(
                entry = MoodEntryDto(
                    id = 0L,
                    userId = userId,
                    moodType = mood.label,
                    timestamp = timestamp,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                ),
                userId = userId
            ) ?: return@withContext Result.failure(IllegalStateException("Gagal menyimpan catatan mood"))
            Result.success(savedId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun updateMood(
        entryId: Long,
        mood: MoodType,
        note: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            val updated = remoteDataSource.update(
                entryId = entryId,
                userId = userId,
                moodType = mood.label,
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            if (!updated) {
                return@withContext Result.failure(NoSuchElementException("Catatan mood dengan id $entryId tidak ditemukan"))
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun deleteMood(entryId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Pengguna belum login"))
            val deleted = remoteDataSource.delete(entryId, userId)
            if (!deleted) {
                return@withContext Result.failure(NoSuchElementException("Catatan mood dengan id $entryId tidak ditemukan"))
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

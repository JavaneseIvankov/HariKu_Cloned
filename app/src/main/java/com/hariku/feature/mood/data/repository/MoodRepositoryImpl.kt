package com.hariku.feature.mood.data.repository

import android.database.sqlite.SQLiteException
import com.hariku.core.auth.toScopedUserId
import com.hariku.feature.mood.data.local.MoodDao
import com.hariku.feature.mood.data.local.MoodEntryEntity
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class MoodRepositoryImpl(
    private val moodDao: MoodDao,
    private val userIdFlow: Flow<String?> = flowOf(null)
) : MoodRepository {

    private fun scopedUserIds(): Flow<String> =
        userIdFlow.map { it.toScopedUserId() }.distinctUntilChanged()

    private suspend fun currentUserId(): String = userIdFlow.first().toScopedUserId()

    override fun observeMoodEntries(): Flow<List<MoodEntry>> =
        scopedUserIds().flatMapLatest { userId ->
            moodDao.observeEntries(userId).map { entities ->
                entities.mapNotNull { entity ->
                    val mood = MoodType.fromLabel(entity.moodType) ?: return@mapNotNull null
                    MoodEntry(
                        id = entity.id,
                        mood = mood,
                        timestamp = entity.timestamp,
                        note = entity.note
                    )
                }
            }
        }

    override suspend fun saveMood(
        mood: MoodType,
        note: String,
        timestamp: Long
    ): Result<Long> = try {
        val insertedId = moodDao.insert(
            MoodEntryEntity(
                moodType = mood.label,
                timestamp = timestamp,
                note = note,
                userId = currentUserId()
            )
        )
        Result.success(insertedId)
    } catch (error: SQLiteException) {
        Result.failure(error)
    }

    override suspend fun updateMood(
        entryId: Long,
        mood: MoodType,
        note: String
    ): Result<Unit> = try {
        moodDao.updateById(
            entryId = entryId,
            userId = currentUserId(),
            moodType = mood.label,
            note = note
        )
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(error)
    }

    override suspend fun deleteMood(entryId: Long): Result<Unit> = try {
        moodDao.deleteById(entryId, currentUserId())
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(error)
    }
}

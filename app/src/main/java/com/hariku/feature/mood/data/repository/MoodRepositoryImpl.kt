package com.hariku.feature.mood.data.repository

import android.database.sqlite.SQLiteException
import com.hariku.feature.mood.data.local.MoodDao
import com.hariku.feature.mood.data.local.MoodEntryEntity
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MoodRepositoryImpl(
    private val moodDao: MoodDao
) : MoodRepository {

    override fun observeMoodEntries(): Flow<List<MoodEntry>> =
        moodDao.observeEntries().map { entities ->
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

    override suspend fun saveMood(
        mood: MoodType,
        note: String,
        timestamp: Long
    ): Result<Long> = try {
        val insertedId = moodDao.insert(
            MoodEntryEntity(
                moodType = mood.label,
                timestamp = timestamp,
                note = note
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
            moodType = mood.label,
            note = note
        )
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(error)
    }

    override suspend fun deleteMood(entryId: Long): Result<Unit> = try {
        moodDao.deleteById(entryId)
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(error)
    }
}

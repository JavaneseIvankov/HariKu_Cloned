package com.hariku.feature.mood.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntryEntity): Long

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC, id DESC")
    fun observeEntries(): Flow<List<MoodEntryEntity>>

    @Query("UPDATE mood_entries SET moodType = :moodType, note = :note WHERE id = :entryId")
    suspend fun updateById(entryId: Long, moodType: String, note: String)

    @Query("DELETE FROM mood_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)
}

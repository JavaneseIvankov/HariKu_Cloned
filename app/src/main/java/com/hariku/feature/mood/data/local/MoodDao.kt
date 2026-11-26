package com.hariku.feature.mood.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntryEntity): Long

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY timestamp DESC, id DESC")
    fun observeEntries(userId: String): Flow<List<MoodEntryEntity>>

    @Query("UPDATE mood_entries SET moodType = :moodType, note = :note WHERE id = :entryId AND userId = :userId")
    suspend fun updateById(entryId: Long, userId: String, moodType: String, note: String)

    @Query("DELETE FROM mood_entries WHERE id = :entryId AND userId = :userId")
    suspend fun deleteById(entryId: Long, userId: String)
}

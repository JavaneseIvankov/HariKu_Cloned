package com.hariku.feature.journal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY isPinned DESC, dateMillis DESC, id DESC")
    fun observeAll(userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: Long, userId: String): JournalEntryEntity?

    @Query("SELECT tag FROM journal_entries WHERE userId = :userId ORDER BY updatedAt DESC, id DESC LIMIT 1")
    fun getLatestTag(userId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity): Int

    @Query("DELETE FROM journal_entries WHERE id = :id AND userId = :userId")
    suspend fun delete(id: Long, userId: String): Int

    @Query("UPDATE journal_entries SET isPinned = NOT isPinned WHERE id = :id AND userId = :userId")
    suspend fun togglePin(id: Long, userId: String): Int
}

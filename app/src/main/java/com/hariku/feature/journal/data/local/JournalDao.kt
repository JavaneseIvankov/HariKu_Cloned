package com.hariku.feature.journal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY isPinned DESC, dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): JournalEntryEntity?

    @Query("SELECT tag FROM journal_entries ORDER BY updatedAt DESC, id DESC LIMIT 1")
    fun getLatestTag(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity): Int

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE journal_entries SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long)
}

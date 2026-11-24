package com.hariku.feature.journal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    val title: String,
    val content: String,
    val dateMillis: Long,
    val tag: String,
    val templateType: String,
    val promptUsed: String = "",
    val isPinned: Boolean = false,
    val updatedAt: Long = dateMillis,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
)

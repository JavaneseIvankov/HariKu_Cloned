package com.hariku.feature.journal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hariku.core.auth.LOCAL_GUEST_USER_ID

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
    val userId: String = LOCAL_GUEST_USER_ID,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L
)

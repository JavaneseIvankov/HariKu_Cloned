package com.hariku.feature.journal.data.dto

import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry

/**
 * Representasi dokumen jurnal pada Cloud Firestore di `users/{userId}/journals/{id}`.
 */
data class JournalEntryDto(
    val id: Long = 0L,
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val dateMillis: Long = 0L,
    val tag: String = JournalCategoryTag.UMUM.name,
    val templateType: String = CbtTemplateType.GENERAL_REFLECTION.name,
    val promptUsed: String = "",
    @field:JvmField
    val isPinned: Boolean = false,
    val updatedAt: Long = 0L
) {
    fun toDomain(): JournalEntry = JournalEntry(
        id = id,
        title = title,
        content = content,
        dateMillis = dateMillis,
        tag = JournalCategoryTag.fromNameOrDefault(tag),
        templateType = CbtTemplateType.fromNameOrDefault(templateType),
        promptUsed = promptUsed,
        isPinned = isPinned,
        updatedAt = updatedAt
    )
}

fun JournalEntry.toDto(userId: String): JournalEntryDto = JournalEntryDto(
    id = id,
    userId = userId,
    title = title,
    content = content,
    dateMillis = dateMillis,
    tag = tag.name,
    templateType = templateType.name,
    promptUsed = promptUsed,
    isPinned = isPinned,
    updatedAt = updatedAt
)

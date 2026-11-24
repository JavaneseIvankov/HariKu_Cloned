package com.hariku.feature.journal.domain.model

data class JournalEntry(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val dateMillis: Long,
    val tag: JournalCategoryTag = JournalCategoryTag.UMUM,
    val templateType: CbtTemplateType = CbtTemplateType.GENERAL_REFLECTION,
    val promptUsed: String = "",
    val isPinned: Boolean = false,
    val updatedAt: Long = dateMillis
)

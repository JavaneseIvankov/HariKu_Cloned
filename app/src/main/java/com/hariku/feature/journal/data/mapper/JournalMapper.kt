package com.hariku.feature.journal.data.mapper

import com.hariku.core.auth.LOCAL_GUEST_USER_ID
import com.hariku.feature.journal.data.local.JournalEntryEntity
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry

object JournalMapper {

    fun toDomain(entity: JournalEntryEntity): JournalEntry = JournalEntry(
        id = entity.id,
        title = entity.title,
        content = entity.content,
        dateMillis = entity.dateMillis,
        tag = JournalCategoryTag.fromNameOrDefault(entity.tag),
        templateType = CbtTemplateType.fromNameOrDefault(entity.templateType),
        promptUsed = entity.promptUsed,
        isPinned = entity.isPinned,
        updatedAt = entity.updatedAt
    )

    fun toEntity(model: JournalEntry, userId: String = LOCAL_GUEST_USER_ID): JournalEntryEntity = JournalEntryEntity(
        id = model.id,
        userId = userId,
        title = model.title,
        content = model.content,
        dateMillis = model.dateMillis,
        tag = model.tag.name,
        templateType = model.templateType.name,
        promptUsed = model.promptUsed,
        isPinned = model.isPinned,
        updatedAt = model.updatedAt
    )
}

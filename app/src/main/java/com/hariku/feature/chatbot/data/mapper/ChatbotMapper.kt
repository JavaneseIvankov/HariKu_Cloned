package com.hariku.feature.chatbot.data.mapper

import com.hariku.feature.chatbot.data.dto.ChatbotDto
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatbotAvatarCatalog

object ChatbotMapper {

    fun toDomain(dto: ChatbotDto, userId: String = ""): Chatbot {
        val resolvedResId = ChatbotAvatarCatalog.resolveDrawableResId(
            avatarKey = dto.avatarKey,
            legacyResId = dto.avatarResId,
            botName = dto.name
        )
        val resolvedKey = dto.avatarKey?.takeIf { it.isNotBlank() }
            ?: ChatbotAvatarCatalog.keyForDrawable(resolvedResId)
            ?: ChatbotAvatarCatalog.DEFAULT_AVATAR_KEY

        return Chatbot(
            id = dto.id.orEmpty(),
            name = dto.name.orEmpty(),
            subtitle = dto.subtitle.orEmpty(),
            description = dto.description.orEmpty(),
            avatarResId = resolvedResId,
            avatarKey = resolvedKey,
            createdAt = dto.createdAt ?: 0L,
            userId = userId
        )
    }

    fun toDto(model: Chatbot): ChatbotDto {
        val resolvedKey = resolveAvatarKey(model)
        return ChatbotDto(
            id = model.id,
            name = model.name,
            subtitle = model.subtitle,
            description = model.description,
            avatarResId = null,
            avatarKey = resolvedKey,
            createdAt = model.createdAt
        )
    }

    fun toMap(model: Chatbot): Map<String, Any> {
        return mapOf(
            "id" to model.id,
            "name" to model.name,
            "subtitle" to model.subtitle,
            "description" to model.description,
            "avatarKey" to resolveAvatarKey(model),
            "createdAt" to model.createdAt
        )
    }

    private fun resolveAvatarKey(model: Chatbot): String {
        return model.avatarKey.takeIf { it.isNotBlank() }
            ?: ChatbotAvatarCatalog.keyForDrawable(model.avatarResId)
            ?: ChatbotAvatarCatalog.keyForPresetName(model.name)
            ?: ChatbotAvatarCatalog.DEFAULT_AVATAR_KEY
    }
}
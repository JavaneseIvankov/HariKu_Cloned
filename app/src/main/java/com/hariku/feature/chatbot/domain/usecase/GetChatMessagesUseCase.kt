package com.hariku.feature.chatbot.domain.usecase

import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import kotlinx.coroutines.flow.Flow

class GetChatMessagesUseCase(
    private val repository: ChatbotRepository
) {
    operator fun invoke(chatbotId: String, userId: String): Flow<Result<List<ChatMessage>>> {
        return repository.getChatMessages(chatbotId, userId)
    }
}


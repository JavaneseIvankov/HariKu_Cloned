package com.hariku.feature.chatbot.domain.usecase

import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import kotlinx.coroutines.flow.Flow

class GetChatbotsWithHistoryUseCase(
    private val repository: ChatbotRepository
) {
    operator fun invoke(userId: String): Flow<Result<List<ChatbotWithHistory>>> {
        return repository.getChatbotsWithHistory(userId)
    }
}


package com.hariku.feature.chatbot.domain.usecase

import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository

class GetChatbotByIdUseCase(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(chatbotId: String, userId: String): Result<Chatbot> {
        return repository.getChatbotById(chatbotId, userId)
    }
}
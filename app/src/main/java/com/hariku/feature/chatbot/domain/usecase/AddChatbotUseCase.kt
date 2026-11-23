package com.hariku.feature.chatbot.domain.usecase

import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository

class AddChatbotUseCase(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(chatbot: Chatbot): Result<Unit> {
        return repository.addChatbot(chatbot)
    }
}
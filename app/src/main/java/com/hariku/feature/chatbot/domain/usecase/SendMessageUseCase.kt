package com.hariku.feature.chatbot.domain.usecase

import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository

class SendMessageUseCase(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(message: ChatMessage): Result<Unit> {
        return repository.sendMessage(message)
    }
}
package com.hariku.feature.chatbot.data.dto

data class ChatMessageDto(
    val id: String = "",
    val chatbotId: String = "",
    val message: String = "",
    val fromUser: Boolean = true,
    val timestamp: Long = 0L,
    val read: Boolean = false
)
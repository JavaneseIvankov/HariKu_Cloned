package com.hariku.feature.chatbot.data.dto

import com.google.firebase.firestore.PropertyName

data class ChatMessageDto(
    @get:PropertyName("id")
    @PropertyName("id")
    val id: String = "",
    @get:PropertyName("chatbotId")
    @PropertyName("chatbotId")
    val chatbotId: String = "",
    @get:PropertyName("message")
    @PropertyName("message")
    val message: String = "",
    @get:PropertyName("fromUser")
    @PropertyName("fromUser")
    val fromUser: Boolean = true,
    @get:PropertyName("timestamp")
    @PropertyName("timestamp")
    val timestamp: Long = 0L,
    @get:PropertyName("read")
    @PropertyName("read")
    val read: Boolean = false
)
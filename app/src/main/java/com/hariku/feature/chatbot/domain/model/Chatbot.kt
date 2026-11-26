package com.hariku.feature.chatbot.domain.model

data class Chatbot(
    val id: String = "",
    val name: String = "",
    val subtitle: String = "",
    val description: String = "",
    val avatarResId: Int = 0,
    val avatarKey: String = "",
    val createdAt: Long = 0L,
    val userId: String = ""
)
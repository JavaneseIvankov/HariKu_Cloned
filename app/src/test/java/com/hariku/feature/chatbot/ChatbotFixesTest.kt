package com.hariku.feature.chatbot

import com.hariku.R
import com.hariku.feature.chatbot.data.dto.ChatMessageDto
import com.hariku.feature.chatbot.data.dto.ChatbotDto
import com.hariku.feature.chatbot.data.mapper.ChatMessageMapper
import com.hariku.feature.chatbot.data.mapper.ChatbotMapper
import com.hariku.feature.chatbot.data.remote.ChatbotFirebaseService
import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatbotAvatarCatalog
import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import com.hariku.feature.chatbot.domain.usecase.GetChatMessagesUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotByIdUseCase
import com.hariku.feature.chatbot.domain.usecase.MarkMessagesAsReadUseCase
import com.hariku.feature.chatbot.domain.usecase.SendMessageUseCase
import com.hariku.feature.chatbot.presentation.detail.ChatbotDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatbotFixesTest {

    @Test
    fun chatbotMapper_persistsStableAvatarKeyInsteadOfVolatileResourceInt() {
        val model = Chatbot(
            id = "bot-1",
            name = "Luna",
            subtitle = "CERIA",
            description = "Desc",
            avatarResId = R.drawable.ic_customize_cat_02,
            createdAt = 1_000L,
            userId = "u1"
        )

        val dto = ChatbotMapper.toDto(model)
        assertEquals("cat_02", dto.avatarKey)
        assertNull(dto.avatarResId)

        val map = ChatbotMapper.toMap(model)
        assertEquals("cat_02", map["avatarKey"])
        assertFalse(map.containsKey("avatarResId"))

        val roundTrip = ChatbotMapper.toDomain(dto, userId = "u1")
        assertEquals(R.drawable.ic_customize_cat_02, roundTrip.avatarResId)
        assertEquals("cat_02", roundTrip.avatarKey)
    }

    @Test
    fun chatbotMapper_handlesStaleOrMissingLegacyAvatarResIdWithoutCrashing() {
        val stalePresetDto = ChatbotDto(
            id = "bot-preset",
            name = "Mochi",
            avatarResId = 999_999_999,
            avatarKey = null
        )
        val resolvedPreset = ChatbotMapper.toDomain(stalePresetDto, userId = "u1")
        assertEquals(R.drawable.ic_customize_cat_04, resolvedPreset.avatarResId)
        assertEquals("cat_04", resolvedPreset.avatarKey)

        val staleCustomDto = ChatbotDto(
            id = "bot-custom",
            name = "Kucingku",
            avatarResId = 999_999_999,
            avatarKey = null
        )
        val resolvedCustom = ChatbotMapper.toDomain(staleCustomDto, userId = "u1")
        assertEquals(ChatbotAvatarCatalog.DEFAULT_AVATAR_RES_ID, resolvedCustom.avatarResId)
        assertEquals(ChatbotAvatarCatalog.DEFAULT_AVATAR_KEY, resolvedCustom.avatarKey)

        assertEquals(
            ChatbotAvatarCatalog.DEFAULT_AVATAR_RES_ID,
            ChatbotAvatarCatalog.safeDrawableResId(0)
        )
        assertEquals(
            ChatbotAvatarCatalog.DEFAULT_AVATAR_RES_ID,
            ChatbotAvatarCatalog.safeDrawableResId(-1)
        )
    }

    @Test
    fun chatMessageDtoAndFirebaseFieldConstantsMatch() {
        val dtoFields = ChatMessageDto::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(ChatbotFirebaseService.FIELD_FROM_USER in dtoFields)
        assertTrue(ChatbotFirebaseService.FIELD_READ in dtoFields)
        assertEquals("fromUser", ChatbotFirebaseService.FIELD_FROM_USER)
        assertEquals("read", ChatbotFirebaseService.FIELD_READ)

        val message = ChatMessage(
            id = "msg-1",
            chatbotId = "bot-1",
            userId = "u1",
            message = "Halo",
            isFromUser = false,
            timestamp = 1_000L,
            isRead = false
        )
        val dto = ChatMessageMapper.toDto(message)
        assertFalse(dto.fromUser)
        assertFalse(dto.read)

        val mappedBack = ChatMessageMapper.toDomain(dto, userId = "u1", chatbotId = "bot-1")
        assertFalse(mappedBack.isFromUser)
        assertFalse(mappedBack.isRead)
    }

    @Test
    fun chatbotDetailViewModel_marksIncomingUnreadBotMessagesAsRead() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = FakeReadTrackingChatbotRepository()
            val vm = ChatbotDetailViewModel(
                getChatMessagesUseCase = GetChatMessagesUseCase(repo),
                getChatbotByIdUseCase = GetChatbotByIdUseCase(repo),
                sendMessageUseCase = SendMessageUseCase(repo),
                markMessagesAsReadUseCase = MarkMessagesAsReadUseCase(repo),
                userIdFlow = MutableStateFlow("user_A"),
                coroutineScopeOverride = scope
            )

            vm.initialize("bot-1")
            assertEquals(1, repo.markReadCalls)

            // Incoming unread bot reply while detail screen is open should trigger mark-as-read
            repo.emitBotReply("bot-reply-1", "Hai, aku di sini")
            assertEquals(2, repo.markReadCalls)
            assertTrue(vm.uiState.messages.last().isRead)
        } finally {
            scope.cancel()
        }
    }

    private class FakeReadTrackingChatbotRepository : ChatbotRepository {
        private val messagesFlow = MutableStateFlow<Result<List<ChatMessage>>>(
            Result.success(
                listOf(
                    ChatMessage(
                        id = "u-1",
                        chatbotId = "bot-1",
                        userId = "user_A",
                        message = "Halo",
                        isFromUser = true,
                        timestamp = 1_000L,
                        isRead = true
                    )
                )
            )
        )
        var markReadCalls = 0

        fun emitBotReply(id: String, text: String) {
            val current = messagesFlow.value.getOrNull().orEmpty()
            messagesFlow.value = Result.success(
                current + ChatMessage(
                    id = id,
                    chatbotId = "bot-1",
                    userId = "user_A",
                    message = text,
                    isFromUser = false,
                    timestamp = 2_000L,
                    isRead = false
                )
            )
        }

        override fun getChatbots(userId: String): Flow<Result<List<Chatbot>>> =
            MutableStateFlow(Result.success(emptyList()))

        override fun getChatbotsWithHistory(userId: String): Flow<Result<List<ChatbotWithHistory>>> =
            MutableStateFlow(Result.success(emptyList()))

        override suspend fun addChatbot(chatbot: Chatbot): Result<Unit> = Result.success(Unit)
        override suspend fun deleteChatbot(chatbotId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getChatbotById(chatbotId: String, userId: String): Result<Chatbot> =
            Result.success(Chatbot(id = chatbotId, name = "HariKu", userId = userId))

        override fun getChatMessages(chatbotId: String, userId: String): Flow<Result<List<ChatMessage>>> =
            messagesFlow

        override suspend fun sendMessage(message: ChatMessage): Result<Unit> = Result.success(Unit)

        override suspend fun markMessagesAsRead(chatbotId: String, userId: String): Result<Unit> {
            markReadCalls++
            val current = messagesFlow.value.getOrNull().orEmpty()
            messagesFlow.value = Result.success(
                current.map { msg ->
                    if (!msg.isFromUser && !msg.isRead) msg.copy(isRead = true) else msg
                }
            )
            return Result.success(Unit)
        }
    }
}

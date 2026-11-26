package com.hariku.feature.chatbot.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.usecase.GetChatMessagesUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotByIdUseCase
import com.hariku.feature.chatbot.domain.usecase.MarkMessagesAsReadUseCase
import com.hariku.feature.chatbot.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotDetailViewModel(
    private val getChatMessagesUseCase: GetChatMessagesUseCase,
    private val getChatbotByIdUseCase: GetChatbotByIdUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val userIdFlow: Flow<String?>,
    private val coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = coroutineScopeOverride ?: viewModelScope

    var uiState by mutableStateOf(ChatbotDetailUiState())
        private set

    private val currentChatbotIdFlow = MutableStateFlow<String?>(null)
    private var activeUserId: String? = null

    init {
        val normalizedUserIds = userIdFlow
            .map { it?.takeIf(String::isNotBlank) }
            .distinctUntilChanged()

        combine(currentChatbotIdFlow, normalizedUserIds) { chatbotId, userId ->
            chatbotId to userId
        }
            .distinctUntilChanged()
            .flatMapLatest { (chatbotId, userId) ->
                activeUserId = userId
                channelFlow<Unit> {
                    if (chatbotId.isNullOrBlank()) {
                        uiState = ChatbotDetailUiState(isLoading = false)
                        return@channelFlow
                    }
                    if (userId == null) {
                        uiState = ChatbotDetailUiState(
                            isLoading = false,
                            error = "User tidak terautentikasi"
                        )
                        return@channelFlow
                    }

                    uiState = ChatbotDetailUiState(isLoading = true)

                    launch {
                        getChatbotByIdUseCase(chatbotId, userId).fold(
                            onSuccess = { chatbot ->
                                uiState = uiState.copy(chatbot = chatbot)
                            },
                            onFailure = { exception ->
                                uiState = uiState.copy(
                                    error = exception.message ?: "Gagal memuat chatbot"
                                )
                            }
                        )
                    }

                    launch {
                        markMessagesAsReadUseCase(chatbotId, userId)
                    }

                    getChatMessagesUseCase(chatbotId, userId).collect { result ->
                        result.fold(
                            onSuccess = { messages ->
                                uiState = uiState.copy(
                                    messages = messages,
                                    isLoading = false,
                                    error = null
                                )
                            },
                            onFailure = { exception ->
                                uiState = uiState.copy(
                                    isLoading = false,
                                    error = exception.message ?: "Gagal memuat pesan"
                                )
                            }
                        )
                    }
                }
            }
            .launchIn(scope)
    }

    fun initialize(chatbotId: String) {
        currentChatbotIdFlow.value = chatbotId
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank() || uiState.isSending) return
        val chatbotId = currentChatbotIdFlow.value?.takeIf(String::isNotBlank) ?: return

        scope.launch {
            val userId = activeUserId ?: userIdFlow.firstOrNull()?.takeIf(String::isNotBlank)
            if (userId == null) {
                uiState = uiState.copy(error = "User tidak terautentikasi")
                return@launch
            }

            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatbotId = chatbotId,
                userId = userId,
                message = messageText.trim(),
                isFromUser = true,
                timestamp = System.currentTimeMillis(),
                isRead = true
            )

            uiState = uiState.copy(isSending = true, error = null)

            sendMessageUseCase(message).fold(
                onSuccess = {
                    uiState = uiState.copy(isSending = false)
                },
                onFailure = { exception ->
                    uiState = uiState.copy(
                        isSending = false,
                        error = exception.message ?: "Gagal mengirim pesan"
                    )
                }
            )
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class ChatbotDetailUiState(
    val chatbot: Chatbot? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

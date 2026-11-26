package com.hariku.feature.chatbot.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.usecase.GetChatbotsWithHistoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModel(
    private val getChatbotsWithHistoryUseCase: GetChatbotsWithHistoryUseCase,
    private val userIdFlow: Flow<String?>,
    private val coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = coroutineScopeOverride ?: viewModelScope

    var uiState by mutableStateOf(ChatbotUiState())
        private set

    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeChatbots()
    }

    private fun observeChatbots() {
        val normalizedUserIds = userIdFlow
            .map { it?.takeIf(String::isNotBlank) }
            .distinctUntilChanged()

        combine(normalizedUserIds, refreshTrigger) { userId, _ -> userId }
            .flatMapLatest { userId ->
                if (userId == null) {
                    uiState = ChatbotUiState(
                        chatbots = emptyList(),
                        isLoading = false,
                        error = null
                    )
                    flowOf(Result.success(emptyList<ChatbotWithHistory>()))
                } else {
                    uiState = ChatbotUiState(
                        chatbots = emptyList(),
                        isLoading = true,
                        error = null
                    )
                    getChatbotsWithHistoryUseCase(userId)
                }
            }
            .onEach { result ->
                result.fold(
                    onSuccess = { chatbots ->
                        uiState = uiState.copy(
                            chatbots = chatbots,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        uiState = uiState.copy(
                            isLoading = false,
                            error = exception.message ?: "Gagal memuat chatbot"
                        )
                    }
                )
            }
            .launchIn(scope)
    }

    fun refresh() {
        uiState = uiState.copy(isLoading = true, error = null)
        refreshTrigger.value += 1
    }
}

data class ChatbotUiState(
    val chatbots: List<ChatbotWithHistory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

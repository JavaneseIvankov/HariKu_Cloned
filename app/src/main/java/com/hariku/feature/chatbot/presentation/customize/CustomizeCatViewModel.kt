package com.hariku.feature.chatbot.presentation.customize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatbotAvatarCatalog
import com.hariku.feature.chatbot.domain.usecase.AddChatbotUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class CustomizeCatViewModel(
    private val addChatbotUseCase: AddChatbotUseCase,
    private val userIdFlow: Flow<String?>
) : ViewModel() {

    var uiState by mutableStateOf(CustomizeCatUiState())
        private set

    init {
        userIdFlow
            .map { it?.takeIf(String::isNotBlank) }
            .distinctUntilChanged()
            .drop(1)
            .onEach { resetState() }
            .launchIn(viewModelScope)
    }

    fun addChatbot(
        name: String,
        subtitle: String,
        description: String,
        avatarResId: Int
    ) {
        viewModelScope.launch {
            val userId = userIdFlow.firstOrNull()?.takeIf(String::isNotBlank)
            if (userId == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = "User tidak terautentikasi"
                )
                return@launch
            }

            uiState = uiState.copy(isLoading = true, error = null)

            val safeAvatarResId = ChatbotAvatarCatalog.safeDrawableResId(avatarResId)
            val chatbot = Chatbot(
                id = UUID.randomUUID().toString(),
                name = name,
                subtitle = subtitle,
                description = description,
                avatarResId = safeAvatarResId,
                avatarKey = ChatbotAvatarCatalog.keyForDrawable(safeAvatarResId)
                    ?: ChatbotAvatarCatalog.DEFAULT_AVATAR_KEY,
                createdAt = System.currentTimeMillis(),
                userId = userId
            )

            addChatbotUseCase(chatbot).fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isSuccess = false,
                        error = exception.message ?: "Gagal menambahkan chatbot"
                    )
                }
            )
        }
    }

    fun resetState() {
        uiState = CustomizeCatUiState()
    }
}

data class CustomizeCatUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

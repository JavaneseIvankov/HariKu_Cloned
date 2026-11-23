package com.hariku.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LoginUseCase
import com.hariku.feature.auth.presentation.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null
)

class LoginScreenViewModel(
    private val loginUseCase: LoginUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        // A restored session goes straight through, same as the old behaviour.
        LoginUiState(loginSuccess = getCurrentUserUseCase() != null)
    )
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onLoginClicked() {
        if (_uiState.value.isLoading) return

        val current = _uiState.value
        val problem = AuthValidation.validateEmail(current.email)
            ?: AuthValidation.validatePassword(current.password)
        if (problem != null) {
            _uiState.update { it.copy(error = problem) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            loginUseCase(current.email, current.password)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, loginSuccess = true)
                    }
                }
                .onFailure { exception ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = AuthValidation.describe(
                                exception.javaClass.simpleName,
                                exception.message
                            )
                        )
                    }
                }
        }
    }

    /** Called by the UI once the error has been shown, so it does not repeat. */
    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}

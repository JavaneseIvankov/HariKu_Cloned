package com.hariku.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.auth.domain.usecase.SignUpUseCase
import com.hariku.feature.auth.presentation.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false,
    val error: String? = null
)

class RegisterScreenViewModel(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onConfirmationChange(confirmation: String) {
        _uiState.update { it.copy(confirmation = confirmation, error = null) }
    }

    fun onRegisterClicked() {
        if (_uiState.value.isLoading) return

        val current = _uiState.value
        val problem = AuthValidation.validateRegistration(
            name = current.name,
            email = current.email,
            password = current.password,
            confirmation = current.confirmation
        )
        if (problem != null) {
            _uiState.update { it.copy(error = problem) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            signUpUseCase(current.name, current.email, current.password)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, registerSuccess = true)
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

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}

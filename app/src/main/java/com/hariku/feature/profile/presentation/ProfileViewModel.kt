package com.hariku.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase
import com.hariku.feature.auth.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {
    val currentUser: StateFlow<AuthUser?> = observeAuthStateUseCase()
        .stateIn(
            scope = coroutineScopeOverride ?: viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = getCurrentUserUseCase()
        )

    fun logout() = logoutUseCase()
}

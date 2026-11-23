package com.hariku.feature.profile.presentation

import androidx.lifecycle.ViewModel
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase

class ProfileViewModel(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    val currentUser: AuthUser? = getCurrentUserUseCase()

    fun logout() = logoutUseCase()
}

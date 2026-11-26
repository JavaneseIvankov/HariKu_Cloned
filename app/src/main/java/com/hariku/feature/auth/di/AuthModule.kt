package com.hariku.feature.auth.di

import com.google.firebase.FirebaseApp
import com.hariku.feature.auth.data.remote.AuthRemoteDataSource
import com.hariku.feature.auth.data.repository.AuthRepositoryImpl
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository
import com.hariku.feature.auth.domain.usecase.CheckLoginStatusUseCase
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LoginUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase
import com.hariku.feature.auth.domain.usecase.ObserveAuthStateUseCase
import com.hariku.feature.auth.domain.usecase.SignUpUseCase
import com.hariku.feature.auth.presentation.login.LoginScreenViewModel
import com.hariku.feature.auth.presentation.register.RegisterScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    // Data
    single {
        AuthRemoteDataSource(
            auth = get(),
            firestore = get()
        )
    }

    single<AuthRepository> {
        if (FirebaseApp.getApps(androidContext()).isNotEmpty()) {
            AuthRepositoryImpl(remoteDataSource = get())
        } else {
            OfflineAuthRepository
        }
    }

    // Domain
    factory { LoginUseCase(repository = get()) }
    factory { SignUpUseCase(repository = get()) }
    factory { GetCurrentUserUseCase(repository = get()) }
    factory { CheckLoginStatusUseCase(repository = get()) }
    factory { ObserveAuthStateUseCase(repository = get()) }
    factory { LogoutUseCase(repository = get()) }

    // Presentation
    viewModel {
        LoginScreenViewModel(
            loginUseCase = get(),
            getCurrentUserUseCase = get()
        )
    }

    viewModel {
        RegisterScreenViewModel(signUpUseCase = get())
    }
}

private object OfflineAuthRepository : AuthRepository {
    private val unconfiguredError =
        IllegalStateException("Firebase belum dikonfigurasi di perangkat ini")

    override suspend fun login(email: String, password: String): Result<AuthUser> =
        Result.failure(unconfiguredError)

    override suspend fun signUp(email: String, password: String, name: String): Result<AuthUser> =
        Result.failure(unconfiguredError)

    override fun getCurrentUser(): AuthUser? = null

    override fun getAuthState(): Flow<AuthUser?> = flowOf(null)

    override fun logout() = Unit
}

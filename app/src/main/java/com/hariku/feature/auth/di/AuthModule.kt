package com.hariku.feature.auth.di

import com.hariku.feature.auth.data.remote.AuthRemoteDataSource
import com.hariku.feature.auth.data.repository.AuthRepositoryImpl
import com.hariku.feature.auth.domain.repository.AuthRepository
import com.hariku.feature.auth.domain.usecase.CheckLoginStatusUseCase
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LoginUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase
import com.hariku.feature.auth.domain.usecase.SignUpUseCase
import com.hariku.feature.auth.presentation.login.LoginScreenViewModel
import com.hariku.feature.auth.presentation.register.RegisterScreenViewModel
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
        AuthRepositoryImpl(remoteDataSource = get())
    }

    // Domain
    factory { LoginUseCase(repository = get()) }
    factory { SignUpUseCase(repository = get()) }
    factory { GetCurrentUserUseCase(repository = get()) }
    factory { CheckLoginStatusUseCase(repository = get()) }
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

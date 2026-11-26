package com.hariku.feature.chatbot.di

import com.hariku.core.auth.CurrentUserSession
import com.hariku.feature.chatbot.data.mapper.ChatMessageMapper
import com.hariku.feature.chatbot.data.mapper.ChatbotMapper
import com.hariku.feature.chatbot.data.remote.ChatbotFirebaseService
import com.hariku.feature.chatbot.data.remote.GeminiApiService
import com.hariku.feature.chatbot.data.repository.ChatbotRepositoryImpl
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import com.hariku.feature.chatbot.domain.usecase.AddChatbotUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotByIdUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotsUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotsWithHistoryUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatMessagesUseCase
import com.hariku.feature.chatbot.domain.usecase.MarkMessagesAsReadUseCase
import com.hariku.feature.chatbot.domain.usecase.SendMessageUseCase
import com.hariku.feature.chatbot.presentation.ChatbotViewModel
import com.hariku.feature.chatbot.presentation.customize.CustomizeCatViewModel
import com.hariku.feature.chatbot.presentation.customize.CustomizeNewCatViewModel
import com.hariku.feature.chatbot.presentation.customize.CustomizePersonalCatViewModel
import com.hariku.feature.chatbot.presentation.detail.ChatbotDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatbotModule = module {
    
    single {
        ChatbotMapper
    }
    
    single {
        ChatMessageMapper
    }
    
    single {
        ChatbotFirebaseService(
            firestore = get(),
            isAuthenticated = { get<CurrentUserSession>().currentUserIdOrNull() != null }
        )
    }
    
    single {
        val apiKey = com.hariku.BuildConfig.GEMINI_API_KEY
        GeminiApiService(apiKey)
    }
    
    single<ChatbotRepository> {
        ChatbotRepositoryImpl(
            firebaseService = get(),
            geminiApiService = get(),
            mapper = get(),
            messageMapper = get()
        )
    }
    
    factory {
        AddChatbotUseCase(repository = get())
    }
    
    factory {
        GetChatbotsUseCase(repository = get())
    }
    
    factory {
        GetChatbotsWithHistoryUseCase(repository = get())
    }
    
    factory {
        GetChatbotByIdUseCase(repository = get())
    }
    
    factory {
        GetChatMessagesUseCase(repository = get())
    }
    
    factory {
        SendMessageUseCase(repository = get())
    }
    
    factory {
        MarkMessagesAsReadUseCase(repository = get())
    }
    
    viewModel {
        CustomizeCatViewModel(
            addChatbotUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
    
    viewModel {
        CustomizeNewCatViewModel()
    }
    
    viewModel {
        CustomizePersonalCatViewModel(
            addChatbotUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
    
    viewModel {
        ChatbotViewModel(
            getChatbotsWithHistoryUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
    
    viewModel {
        ChatbotDetailViewModel(
            getChatMessagesUseCase = get(),
            getChatbotByIdUseCase = get(),
            sendMessageUseCase = get(),
            markMessagesAsReadUseCase = get(),
            userIdFlow = get<CurrentUserSession>().userIdFlow
        )
    }
}
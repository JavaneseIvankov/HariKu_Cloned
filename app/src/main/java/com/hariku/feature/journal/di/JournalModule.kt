package com.hariku.feature.journal.di

import com.google.firebase.auth.FirebaseAuth
import com.hariku.core.auth.observeCurrentUserIdFlow
import com.hariku.feature.journal.data.local.JournalDatabase
import com.hariku.feature.journal.data.provider.PsychologicalContextProviderImpl
import com.hariku.feature.journal.data.repository.JournalRepositoryImpl
import com.hariku.feature.journal.domain.provider.PsychologicalContextProvider
import com.hariku.feature.journal.domain.repository.JournalRepository
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.GetJournalByIdUseCase
import com.hariku.feature.journal.domain.usecase.ObserveJournalEntriesUseCase
import com.hariku.feature.journal.domain.usecase.SaveJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import com.hariku.feature.journal.presentation.JournalViewModel
import com.hariku.feature.journal.presentation.create.GuidedJournalEditorViewModel
import com.hariku.feature.journal.presentation.detail.JournalDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val journalModule = module {
    single { JournalDatabase.open(androidContext()) }
    single { get<JournalDatabase>().journalDao() }
    single<JournalRepository> {
        JournalRepositoryImpl(
            journalDao = get(),
            userIdFlow = observeCurrentUserIdFlow { runCatching { get<FirebaseAuth>() }.getOrNull() }
        )
    }
    single<PsychologicalContextProvider> {
        PsychologicalContextProviderImpl(
            moodRepository = get(),
            chatbotRepository = get(),
            userIdFlow = PsychologicalContextProviderImpl.observeFirebaseUserIdFlow {
                runCatching { get<FirebaseAuth>() }.getOrNull()
            }
        )
    }

    factory { ObserveJournalEntriesUseCase(repository = get()) }
    factory { GetJournalByIdUseCase(repository = get()) }
    factory { SaveJournalEntryUseCase(repository = get()) }
    factory { DeleteJournalEntryUseCase(repository = get()) }
    factory { TogglePinJournalUseCase(repository = get()) }
    factory {
        GetAdaptivePromptUseCase(
            contextProvider = get(),
            journalRepository = get()
        )
    }

    viewModel {
        JournalViewModel(
            observeJournalEntriesUseCase = get(),
            getAdaptivePromptUseCase = get(),
            togglePinJournalUseCase = get(),
            deleteJournalEntryUseCase = get()
        )
    }
    viewModel {
        GuidedJournalEditorViewModel(
            getAdaptivePromptUseCase = get(),
            getJournalByIdUseCase = get(),
            saveJournalEntryUseCase = get()
        )
    }
    viewModel {
        JournalDetailViewModel(
            getJournalByIdUseCase = get(),
            togglePinJournalUseCase = get(),
            deleteJournalEntryUseCase = get()
        )
    }
}

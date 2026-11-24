package com.hariku.feature.mood.di

import com.hariku.feature.mood.data.local.MoodDatabase
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.repository.MoodRepository
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
import com.hariku.feature.mood.presentation.QuickMoodViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val moodModule = module {
    single { MoodDatabase.open(androidContext()) }
    single { get<MoodDatabase>().moodDao() }
    single<MoodRepository> { MoodRepositoryImpl(moodDao = get()) }

    factory { ObserveMoodEntriesUseCase(repository = get()) }
    factory { SaveQuickMoodUseCase(repository = get()) }
    factory { DeleteMoodEntryUseCase(repository = get()) }
    factory { UpdateMoodEntryUseCase(repository = get()) }

    viewModel {
        QuickMoodViewModel(
            observeMoodEntriesUseCase = get(),
            saveQuickMoodUseCase = get(),
            deleteMoodEntryUseCase = get(),
            updateMoodEntryUseCase = get()
        )
    }
}

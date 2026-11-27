package com.hariku.feature_statistic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * Feeds the statistics tab from the mood entries owned by the mood feature, so
 * recording and reporting share one store instead of two parallel databases.
 */
class StatisticViewModel(
    observeMoodEntriesUseCase: ObserveMoodEntriesUseCase
) : ViewModel() {
    /** Null until the first snapshot arrives, which the report renders as loading. */
    val entries: StateFlow<List<MoodEntry>?> = observeMoodEntriesUseCase()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

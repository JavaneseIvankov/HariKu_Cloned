package com.hariku.feature_statistic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.mood.Mood
import com.hariku.feature.mood.MoodRecord
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Feeds the statistics tab from the mood entries owned by the mood feature, so
 * recording and reporting share one store instead of two parallel databases.
 */
class StatisticViewModel(
    observeMoodEntriesUseCase: ObserveMoodEntriesUseCase
) : ViewModel() {
    /** Null until the first snapshot arrives, which the report renders as loading. */
    val records: StateFlow<List<MoodRecord>?> = observeMoodEntriesUseCase()
        .map { entries -> entries.mapNotNull { it.toRecord() } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun MoodEntry.toRecord(): MoodRecord? {
        val mapped = Mood.entries.firstOrNull { it.label == mood.label } ?: return null
        return MoodRecord(mood = mapped, timestamp = timestamp, id = id)
    }
}

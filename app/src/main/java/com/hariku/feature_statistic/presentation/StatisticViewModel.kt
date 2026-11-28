package com.hariku.feature_statistic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class StatisticUiState(
    val isLoading: Boolean = true,
    val entries: List<MoodEntry> = emptyList(),
    val loadError: String? = null
)

/**
 * Feeds the statistics tab from the mood entries owned by the mood feature, so
 * recording and reporting share one Firestore store.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticViewModel(
    observeMoodEntriesUseCase: ObserveMoodEntriesUseCase,
    userIdFlow: Flow<String?> = flowOf(null),
    coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    private val scopedUsers = userIdFlow
        .map { it?.trim()?.takeIf(String::isNotEmpty) }
        .distinctUntilChanged()

    val uiState: StateFlow<StatisticUiState> = combine(scopedUsers, retryTrigger) { userId, trigger ->
        userId to trigger
    }
        .flatMapLatest {
            flow {
                emit(StatisticUiState(isLoading = true))
                observeMoodEntriesUseCase()
                    .map { entries ->
                        StatisticUiState(
                            isLoading = false,
                            entries = entries,
                            loadError = null
                        )
                    }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(
                            StatisticUiState(
                                isLoading = false,
                                entries = emptyList(),
                                loadError = "Statistik mood gagal dimuat."
                            )
                        )
                    }
                    .collect { emit(it) }
            }
        }
        .stateIn(
            scope = coroutineScopeOverride ?: viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticUiState(isLoading = true)
        )

    fun retryLoad() {
        retryTrigger.update { it + 1 }
    }
}

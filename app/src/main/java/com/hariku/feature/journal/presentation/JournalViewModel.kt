package com.hariku.feature.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.ObserveJournalEntriesUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    observeJournalEntriesUseCase: ObserveJournalEntriesUseCase,
    getAdaptivePromptUseCase: GetAdaptivePromptUseCase,
    private val togglePinJournalUseCase: TogglePinJournalUseCase,
    private val deleteJournalEntryUseCase: DeleteJournalEntryUseCase,
    private val coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    private data class JournalLocalState(
        val searchQuery: String = "",
        val selectedCategory: JournalCategoryTag? = null,
        val promptVariantIndex: Int = 0,
        val isTemplateSheetVisible: Boolean = false,
        val pendingDeleteEntry: JournalEntry? = null,
        val pendingMutationIds: Set<Long> = emptySet()
    )

    private data class EntriesLoadState(
        val isLoading: Boolean = true,
        val entries: List<JournalEntry> = emptyList(),
        val errorMessage: String? = null
    )

    private val scope: CoroutineScope
        get() = coroutineScopeOverride ?: viewModelScope

    private val localState = MutableStateFlow(JournalLocalState())

    private val _events = Channel<JournalEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val entriesLoadFlow = observeJournalEntriesUseCase()
        .map { entries -> EntriesLoadState(isLoading = false, entries = entries, errorMessage = null) }
        .catch { throwable ->
            emit(
                EntriesLoadState(
                    isLoading = false,
                    entries = emptyList(),
                    errorMessage = throwable.message ?: "Gagal memuat daftar jurnal."
                )
            )
        }

    private val recommendationFlow = localState
        .map { it.promptVariantIndex }
        .flatMapLatest { variant -> getAdaptivePromptUseCase(variantIndex = variant) }
        .catch {
            emit(AdaptivePromptEngine.recommend(metrics = null, latestTag = null))
        }

    val uiState: StateFlow<JournalUiState> = combine(
        entriesLoadFlow,
        recommendationFlow,
        localState
    ) { loadState, recommendation, local ->
        val filtered = filterEntries(loadState.entries, local.searchQuery, local.selectedCategory)
        JournalUiState(
            isLoading = loadState.isLoading,
            allEntries = loadState.entries,
            filteredEntries = filtered,
            pinnedEntries = filtered.filter { it.isPinned },
            recentEntries = filtered.filterNot { it.isPinned },
            searchQuery = local.searchQuery,
            selectedCategory = local.selectedCategory,
            recommendation = recommendation,
            promptVariantIndex = local.promptVariantIndex,
            isTemplateSheetVisible = local.isTemplateSheetVisible,
            pendingDeleteEntry = local.pendingDeleteEntry,
            pendingMutationIds = local.pendingMutationIds,
            errorMessage = loadState.errorMessage
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JournalUiState()
    )

    fun onSearchQueryChange(query: String) {
        localState.update { it.copy(searchQuery = query) }
    }

    fun onSelectCategory(category: JournalCategoryTag?) {
        localState.update { state ->
            val next = if (state.selectedCategory == category) null else category
            state.copy(selectedCategory = next)
        }
    }

    fun onClearFilters() {
        localState.update { it.copy(searchQuery = "", selectedCategory = null) }
    }

    fun onCyclePromptVariant() {
        localState.update { it.copy(promptVariantIndex = it.promptVariantIndex + 1) }
    }

    fun onShowTemplateSheet() {
        localState.update { it.copy(isTemplateSheetVisible = true) }
    }

    fun onDismissTemplateSheet() {
        localState.update { it.copy(isTemplateSheetVisible = false) }
    }

    fun onTogglePin(entryId: Long) {
        if (entryId in localState.value.pendingMutationIds) return
        val wasPinned = uiState.value.allEntries.firstOrNull { it.id == entryId }?.isPinned == true
        localState.update { it.copy(pendingMutationIds = it.pendingMutationIds + entryId) }

        scope.launch {
            try {
                togglePinJournalUseCase(entryId).fold(
                    onSuccess = {
                        _events.send(
                            JournalEvent.ShowMessage(
                                if (wasPinned) "Sematan jurnal dilepas" else "Jurnal disematkan di atas"
                            )
                        )
                    },
                    onFailure = {
                        _events.send(JournalEvent.ShowMessage("Gagal memperbarui status sematan jurnal."))
                    }
                )
            } finally {
                localState.update { it.copy(pendingMutationIds = it.pendingMutationIds - entryId) }
            }
        }
    }

    fun onRequestDelete(entry: JournalEntry) {
        localState.update { it.copy(pendingDeleteEntry = entry) }
    }

    fun onDismissDeleteDialog() {
        localState.update { it.copy(pendingDeleteEntry = null) }
    }

    fun onConfirmDelete() {
        val target = localState.value.pendingDeleteEntry ?: return
        if (target.id in localState.value.pendingMutationIds) return
        localState.update {
            it.copy(
                pendingDeleteEntry = null,
                pendingMutationIds = it.pendingMutationIds + target.id
            )
        }

        scope.launch {
            try {
                deleteJournalEntryUseCase(target.id).fold(
                    onSuccess = {
                        _events.send(JournalEvent.ShowMessage("Jurnal \"${target.title}\" dihapus"))
                    },
                    onFailure = {
                        _events.send(JournalEvent.ShowMessage("Gagal menghapus jurnal."))
                    }
                )
            } finally {
                localState.update { it.copy(pendingMutationIds = it.pendingMutationIds - target.id) }
            }
        }
    }

    private fun filterEntries(
        entries: List<JournalEntry>,
        rawQuery: String,
        category: JournalCategoryTag?
    ): List<JournalEntry> {
        val trimmedQuery = rawQuery.trim()
        return entries.filter { entry ->
            val matchesCategory = category == null || entry.tag == category
            val matchesQuery = trimmedQuery.isEmpty() ||
                entry.title.contains(trimmedQuery, ignoreCase = true) ||
                entry.content.contains(trimmedQuery, ignoreCase = true) ||
                entry.tag.label.contains(trimmedQuery, ignoreCase = true) ||
                entry.templateType.title.contains(trimmedQuery, ignoreCase = true) ||
                entry.promptUsed.contains(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }
}

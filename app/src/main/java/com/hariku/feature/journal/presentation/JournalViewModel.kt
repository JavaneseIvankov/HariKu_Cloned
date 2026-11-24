package com.hariku.feature.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.ObserveJournalEntriesUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val observeJournalEntriesUseCase: ObserveJournalEntriesUseCase,
    private val getAdaptivePromptUseCase: GetAdaptivePromptUseCase,
    private val togglePinJournalUseCase: TogglePinJournalUseCase,
    private val deleteJournalEntryUseCase: DeleteJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<JournalCategoryTag?>(null)
    private val variantIndexFlow = MutableStateFlow(0)

    init {
        observeEntriesAndFilters()
        observeAdaptiveRecommendation()
    }

    private fun observeEntriesAndFilters() {
        viewModelScope.launch {
            combine(
                observeJournalEntriesUseCase(),
                searchQueryFlow,
                selectedCategoryFlow
            ) { entries, query, category ->
                Triple(entries, query, category)
            }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Gagal memuat daftar jurnal."
                        )
                    }
                }
                .collect { (entries, query, category) ->
                    val filtered = filterEntries(entries, query, category)
                    val pinned = filtered.filter { it.isPinned }
                    val recent = filtered.filterNot { it.isPinned }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            allEntries = entries,
                            filteredEntries = filtered,
                            pinnedEntries = pinned,
                            recentEntries = recent,
                            searchQuery = query,
                            selectedCategory = category,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun observeAdaptiveRecommendation() {
        viewModelScope.launch {
            variantIndexFlow
                .flatMapLatest { variant -> getAdaptivePromptUseCase(variantIndex = variant) }
                .catch { /* Pertahankan rekomendasi netral default bila terjadi kendala */ }
                .collect { recommendation ->
                    _uiState.update { state ->
                        state.copy(recommendation = recommendation)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectCategory(category: JournalCategoryTag?) {
        val next = if (selectedCategoryFlow.value == category) null else category
        selectedCategoryFlow.value = next
        _uiState.update { it.copy(selectedCategory = next) }
    }

    fun onClearFilters() {
        searchQueryFlow.value = ""
        selectedCategoryFlow.value = null
        _uiState.update { it.copy(searchQuery = "", selectedCategory = null) }
    }

    fun onCyclePromptVariant() {
        val nextVariant = variantIndexFlow.value + 1
        variantIndexFlow.value = nextVariant
        _uiState.update { it.copy(promptVariantIndex = nextVariant) }
    }

    fun onShowTemplateSheet() {
        _uiState.update { it.copy(isTemplateSheetVisible = true) }
    }

    fun onDismissTemplateSheet() {
        _uiState.update { it.copy(isTemplateSheetVisible = false) }
    }

    fun onTogglePin(entryId: Long) {
        viewModelScope.launch {
            togglePinJournalUseCase(entryId).fold(
                onSuccess = {
                    val wasPinned = _uiState.value.allEntries.firstOrNull { it.id == entryId }?.isPinned == true
                    _uiState.update { state ->
                        state.copy(
                            feedbackMessage = if (wasPinned) {
                                "Sematan jurnal dilepas"
                            } else {
                                "Jurnal disematkan di atas"
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(errorMessage = "Gagal memperbarui status sematan jurnal.")
                    }
                }
            )
        }
    }

    fun onRequestDelete(entry: JournalEntry) {
        _uiState.update { it.copy(pendingDeleteEntry = entry) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteEntry = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.pendingDeleteEntry ?: return
        _uiState.update { it.copy(pendingDeleteEntry = null) }
        viewModelScope.launch {
            deleteJournalEntryUseCase(target.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(feedbackMessage = "Jurnal \"${target.title}\" dihapus")
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(errorMessage = "Gagal menghapus jurnal.")
                    }
                }
            )
        }
    }

    fun onDismissMessage() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
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

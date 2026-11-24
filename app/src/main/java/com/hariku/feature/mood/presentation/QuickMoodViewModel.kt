package com.hariku.feature.mood.presentation

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class QuickMoodViewModel(
    private val observeMoodEntriesUseCase: ObserveMoodEntriesUseCase,
    private val saveQuickMoodUseCase: SaveQuickMoodUseCase,
    private val deleteMoodEntryUseCase: DeleteMoodEntryUseCase,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickMoodUiState())
    val uiState: StateFlow<QuickMoodUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeMoodEntries()
    }

    fun retryLoad() {
        observeMoodEntries()
    }

    fun onNoteInputChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                noteInput = value.take(MoodTrackerLogic.MAX_NOTE_LENGTH),
                actionError = null
            )
        }
    }

    fun onToggleNoteInput() {
        _uiState.update { state ->
            state.copy(isNoteExpanded = !state.isNoteExpanded)
        }
    }

    fun onToggleHistory() {
        _uiState.update { state ->
            state.copy(isHistoryExpanded = !state.isHistoryExpanded)
        }
    }

    fun onSelectMood(mood: MoodType) {
        if (_uiState.value.isSaving) return

        val noteToSave = _uiState.value.noteInput
        _uiState.update { state ->
            state.copy(
                isSaving = true,
                actionError = null,
                feedbackMessage = null
            )
        }

        viewModelScope.launch {
            val result = saveQuickMoodUseCase(
                mood = mood,
                note = noteToSave,
                timestamp = nowProvider()
            )
            result.fold(
                onSuccess = { insertedId ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            noteInput = "",
                            isNoteExpanded = false,
                            feedbackMessage = "Mood ${mood.label} tersimpan",
                            lastSavedEntryId = insertedId,
                            actionError = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            actionError = "Mood gagal disimpan. Coba lagi."
                        )
                    }
                }
            )
        }
    }

    fun onUndoLastSaved() {
        val entryId = _uiState.value.lastSavedEntryId ?: return
        onDeleteEntry(entryId = entryId, fromUndo = true)
    }

    fun onDeleteEntry(entryId: Long, fromUndo: Boolean = false) {
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true, actionError = null) }
        viewModelScope.launch {
            deleteMoodEntryUseCase(entryId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val clearedUndo = if (state.lastSavedEntryId == entryId) null else state.lastSavedEntryId
                        state.copy(
                            isSaving = false,
                            lastSavedEntryId = clearedUndo,
                            feedbackMessage = if (fromUndo) "Catatan mood dibatalkan" else "Catatan mood dihapus"
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            actionError = "Gagal menghapus catatan mood."
                        )
                    }
                }
            )
        }
    }

    fun onDismissFeedback() {
        _uiState.update { state ->
            state.copy(
                feedbackMessage = null,
                lastSavedEntryId = null,
                actionError = null
            )
        }
    }

    private fun observeMoodEntries() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }

        observeJob = viewModelScope.launch {
            observeMoodEntriesUseCase()
                .catch { error ->
                    if (error !is SQLiteException) throw error
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = "Catatan mood gagal dimuat."
                        )
                    }
                }
                .collect { entries ->
                    val summary = MoodTrackerLogic.summarizeToday(
                        entries = entries,
                        today = todayProvider(),
                        zone = zoneId
                    )
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = null,
                            latestTodayEntry = summary.latestEntry,
                            todayEntries = summary.todayEntries
                        )
                    }
                }
        }
    }
}

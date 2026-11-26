package com.hariku.feature.mood.presentation

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
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
    private val updateMoodEntryUseCase: UpdateMoodEntryUseCase,
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

    fun onToggleQuickTag(tagLabel: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { state ->
            state.copy(
                noteInput = MoodTrackerLogic.toggleContextTag(state.noteInput, tagLabel),
                actionError = null
            )
        }
    }

    fun onToggleHistory() {
        _uiState.update { state ->
            val nextExpanded = !state.isHistoryExpanded
            state.copy(
                isHistoryExpanded = nextExpanded,
                historyPage = if (nextExpanded) state.historyPage else 0
            )
        }
    }

    fun onPreviousHistoryPage() {
        _uiState.update { state ->
            state.copy(historyPage = (state.currentHistoryPage - 1).coerceAtLeast(0))
        }
    }

    fun onNextHistoryPage() {
        _uiState.update { state ->
            val maxPage = (state.totalHistoryPages - 1).coerceAtLeast(0)
            state.copy(historyPage = (state.currentHistoryPage + 1).coerceAtMost(maxPage))
        }
    }

    fun onStartEditEntry(entry: MoodEntry) {
        if (_uiState.value.isSaving) return
        _uiState.update { state ->
            state.copy(
                editingEntry = entry,
                editMood = entry.mood,
                editNote = entry.note,
                actionError = null
            )
        }
    }

    fun onEditMoodSelected(mood: MoodType) {
        _uiState.update { state ->
            state.copy(editMood = mood, actionError = null)
        }
    }

    fun onEditNoteChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                editNote = value.take(MoodTrackerLogic.MAX_NOTE_LENGTH),
                actionError = null
            )
        }
    }

    fun onToggleEditQuickTag(tagLabel: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { state ->
            state.copy(
                editNote = MoodTrackerLogic.toggleContextTag(state.editNote, tagLabel),
                actionError = null
            )
        }
    }

    fun onDismissEditEntry() {
        if (_uiState.value.isSaving) return
        _uiState.update { state ->
            state.copy(
                editingEntry = null,
                editMood = null,
                editNote = "",
                actionError = null
            )
        }
    }

    fun onConfirmEditEntry() {
        val current = _uiState.value
        val targetEntry = current.editingEntry ?: return
        if (current.isSaving) return

        val targetMood = current.editMood ?: targetEntry.mood
        val targetNote = current.editNote

        _uiState.update { state ->
            state.copy(
                isSaving = true,
                actionError = null,
                feedbackMessage = null
            )
        }

        viewModelScope.launch {
            updateMoodEntryUseCase(
                entryId = targetEntry.id,
                mood = targetMood,
                note = targetNote
            ).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val clearedUndo = if (state.lastSavedEntryId == targetEntry.id) null else state.lastSavedEntryId
                        state.copy(
                            isSaving = false,
                            editingEntry = null,
                            editMood = null,
                            editNote = "",
                            lastSavedEntryId = clearedUndo,
                            feedbackMessage = "Catatan mood ${targetMood.label} diperbarui",
                            actionError = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            actionError = "Catatan mood gagal diperbarui. Coba lagi."
                        )
                    }
                }
            )
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
                            historyPage = 0,
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
                        val clearedEditing = state.editingEntry?.id == entryId
                        state.copy(
                            isSaving = false,
                            editingEntry = if (clearedEditing) null else state.editingEntry,
                            editMood = if (clearedEditing) null else state.editMood,
                            editNote = if (clearedEditing) "" else state.editNote,
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
                        val clampedPage = QuickMoodUiState.clampHistoryPage(
                            page = state.historyPage,
                            itemCount = summary.todayEntries.size
                        )
                        state.copy(
                            isLoading = false,
                            loadError = null,
                            latestTodayEntry = summary.latestEntry,
                            todayEntries = summary.todayEntries,
                            streakDays = summary.streak.currentDays,
                            hasCheckedInToday = summary.streak.checkedInToday,
                            historyPage = clampedPage
                        )
                    }
                }
        }
    }
}

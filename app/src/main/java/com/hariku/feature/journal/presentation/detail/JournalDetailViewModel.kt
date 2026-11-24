package com.hariku.feature.journal.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetJournalByIdUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JournalDetailViewModel(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val togglePinJournalUseCase: TogglePinJournalUseCase,
    private val deleteJournalEntryUseCase: DeleteJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<JournalDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentId: Long? = null

    fun loadJournal(journalId: Long) {
        currentId = journalId
        if (journalId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ID jurnal tidak valid."
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(isLoading = state.entry == null, errorMessage = null)
        }

        viewModelScope.launch {
            getJournalByIdUseCase(journalId).fold(
                onSuccess = { loaded ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            entry = loaded,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Jurnal tidak ditemukan."
                        )
                    }
                }
            )
        }
    }

    fun onTogglePin() {
        val entry = _uiState.value.entry ?: return
        if (_uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            togglePinJournalUseCase(entry.id).fold(
                onSuccess = {
                    val nextPinned = !entry.isPinned
                    _uiState.update { state ->
                        state.copy(
                            isWorking = false,
                            entry = entry.copy(isPinned = nextPinned),
                            feedbackMessage = if (nextPinned) {
                                "Jurnal disematkan"
                            } else {
                                "Sematan jurnal dilepas"
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            isWorking = false,
                            feedbackMessage = "Gagal mengubah status sematan."
                        )
                    }
                }
            )
        }
    }

    fun onRequestDelete() {
        if (_uiState.value.entry != null) {
            _uiState.update { it.copy(isDeleteDialogVisible = true) }
        }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(isDeleteDialogVisible = false) }
    }

    fun onConfirmDelete() {
        val entry = _uiState.value.entry ?: return
        if (_uiState.value.isWorking) return

        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                isWorking = true
            )
        }

        viewModelScope.launch {
            deleteJournalEntryUseCase(entry.id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isWorking = false) }
                    _events.send(JournalDetailEvent.DeletedAndNavigateBack)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            feedbackMessage = "Gagal menghapus jurnal. Coba lagi."
                        )
                    }
                }
            )
        }
    }

    fun onDismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}

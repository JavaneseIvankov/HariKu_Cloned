package com.hariku.feature.journal.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.core.auth.toScopedUserId
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetJournalByIdUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalDetailViewModel(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val togglePinJournalUseCase: TogglePinJournalUseCase,
    private val deleteJournalEntryUseCase: DeleteJournalEntryUseCase,
    userIdFlow: Flow<String?> = flowOf(null),
    private val coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = coroutineScopeOverride ?: viewModelScope

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<JournalDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadRequest = MutableStateFlow<Pair<Long, Int>?>(null)
    private var requestCounter = 0

    init {
        val scopedUserIds = userIdFlow
            .map { it.toScopedUserId() }
            .distinctUntilChanged()

        combine(loadRequest, scopedUserIds) { request, userId ->
            request?.let { it.first to (it.second to userId) }
        }
            .distinctUntilChanged()
            .mapLatest { combined ->
                val (journalId, _) = combined ?: return@mapLatest
                if (journalId <= 0L) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entry = null,
                            isWorking = false,
                            isDeleteDialogVisible = false,
                            errorMessage = "ID jurnal tidak valid."
                        )
                    }
                    return@mapLatest
                }

                _uiState.update { state ->
                    val sameEntry = state.entry?.id == journalId
                    state.copy(
                        isLoading = !sameEntry,
                        isWorking = false,
                        isDeleteDialogVisible = false,
                        errorMessage = null
                    )
                }

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
                                entry = null,
                                errorMessage = error.message ?: "Jurnal tidak ditemukan."
                            )
                        }
                    }
                )
            }
            .launchIn(scope)
    }

    fun loadJournal(journalId: Long) {
        requestCounter += 1
        loadRequest.value = journalId to requestCounter
    }

    fun onTogglePin() {
        val entry = _uiState.value.entry ?: return
        if (_uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true) }
        scope.launch {
            togglePinJournalUseCase(entry.id).fold(
                onSuccess = {
                    val nextPinned = !entry.isPinned
                    _uiState.update { state ->
                        state.copy(
                            isWorking = false,
                            entry = entry.copy(isPinned = nextPinned)
                        )
                    }
                    _events.send(
                        JournalDetailEvent.ShowMessage(
                            if (nextPinned) "Jurnal disematkan" else "Sematan jurnal dilepas"
                        )
                    )
                },
                onFailure = {
                    _uiState.update { it.copy(isWorking = false) }
                    _events.send(JournalDetailEvent.ShowMessage("Gagal mengubah status sematan."))
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

        scope.launch {
            deleteJournalEntryUseCase(entry.id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isWorking = false) }
                    _events.send(JournalDetailEvent.DeletedAndNavigateBack)
                },
                onFailure = {
                    _uiState.update { it.copy(isWorking = false) }
                    _events.send(JournalDetailEvent.ShowMessage("Gagal menghapus jurnal. Coba lagi."))
                }
            )
        }
    }
}

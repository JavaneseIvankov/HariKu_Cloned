package com.hariku.feature.journal.presentation.detail

import com.hariku.feature.journal.domain.model.JournalEntry

data class JournalDetailUiState(
    val isLoading: Boolean = true,
    val entry: JournalEntry? = null,
    val isWorking: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface JournalDetailEvent {
    data object DeletedAndNavigateBack : JournalDetailEvent
}

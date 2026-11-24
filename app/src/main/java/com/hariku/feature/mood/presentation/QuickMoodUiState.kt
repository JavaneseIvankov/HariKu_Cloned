package com.hariku.feature.mood.presentation

import com.hariku.feature.mood.domain.model.MoodEntry

data class QuickMoodUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val latestTodayEntry: MoodEntry? = null,
    val todayEntries: List<MoodEntry> = emptyList(),
    val noteInput: String = "",
    val isNoteExpanded: Boolean = false,
    val isHistoryExpanded: Boolean = false,
    val feedbackMessage: String? = null,
    val lastSavedEntryId: Long? = null,
    val loadError: String? = null,
    val actionError: String? = null
) {
    val todayCount: Int get() = todayEntries.size
}

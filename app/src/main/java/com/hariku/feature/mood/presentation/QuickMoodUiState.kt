package com.hariku.feature.mood.presentation

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType

data class QuickMoodUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val latestTodayEntry: MoodEntry? = null,
    val todayEntries: List<MoodEntry> = emptyList(),
    val streakDays: Int = 0,
    val hasCheckedInToday: Boolean = false,
    val noteInput: String = "",
    val isNoteExpanded: Boolean = false,
    val isHistoryExpanded: Boolean = false,
    val historyPage: Int = 0,
    val editingEntry: MoodEntry? = null,
    val editMood: MoodType? = null,
    val editNote: String = "",
    val feedbackMessage: String? = null,
    val lastSavedEntryId: Long? = null,
    val loadError: String? = null,
    val actionError: String? = null
) {
    val todayCount: Int get() = todayEntries.size

    val totalHistoryPages: Int
        get() = totalPagesFor(todayEntries.size)

    val currentHistoryPage: Int
        get() = clampHistoryPage(historyPage, todayEntries.size)

    val pagedTodayEntries: List<MoodEntry>
        get() {
            if (todayEntries.isEmpty()) return emptyList()
            val startIndex = currentHistoryPage * HISTORY_PAGE_SIZE
            val endIndex = (startIndex + HISTORY_PAGE_SIZE).coerceAtMost(todayEntries.size)
            return todayEntries.subList(startIndex, endIndex)
        }

    val canGoToPreviousHistoryPage: Boolean
        get() = currentHistoryPage > 0

    val canGoToNextHistoryPage: Boolean
        get() = currentHistoryPage < totalHistoryPages - 1

    companion object {
        const val HISTORY_PAGE_SIZE = 5

        fun totalPagesFor(itemCount: Int): Int =
            if (itemCount <= 0) 1 else (itemCount + HISTORY_PAGE_SIZE - 1) / HISTORY_PAGE_SIZE

        fun clampHistoryPage(page: Int, itemCount: Int): Int =
            page.coerceIn(0, (totalPagesFor(itemCount) - 1).coerceAtLeast(0))
    }
}

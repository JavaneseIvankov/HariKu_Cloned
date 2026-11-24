package com.hariku.feature.journal.presentation

import androidx.compose.ui.graphics.Color
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation

/**
 * Design tokens penyelarasan Figma Guided Journaling (Bagian 5).
 */
object JournalDesignTokens {
    val CanvasBackground = Color(0xFFF2F2F2)
    val CardWhite = Color(0xFFFDFCFC)
    val CardSoft = Color(0xFFF9F9F9)
    val PeachHighlight = Color(0xFFFFDFC2)
    val PeachSoft = Color(0xFFFFEFE0)
    val PromptItemSurface = Color(0x80EDE9E7) // #EDE9E7 dengan 50% alpha
    val PromptItemBorder = Color(0xFFE0DAD6)
    val PrimaryCta = Color(0xFFC97D50)
    val TextPrimary = Color(0xFF242424)
    val TextMuted = Color(0xFF9F9F9F)
    val TextSecondary = Color(0xFF5E5652)
    val SageBadgeBg = Color(0xFFEAF1E8)
    val SageBadgeInk = Color(0xFF4B7F54)
    val WarningBadgeBg = Color(0xFFFFF1E3)
    val WarningBadgeInk = Color(0xFFB35F2E)
    val ErrorBg = Color(0xFFFDECEB)
    val ErrorInk = Color(0xFFB33A32)
}

data class JournalUiState(
    val isLoading: Boolean = true,
    val allEntries: List<JournalEntry> = emptyList(),
    val filteredEntries: List<JournalEntry> = emptyList(),
    val pinnedEntries: List<JournalEntry> = emptyList(),
    val recentEntries: List<JournalEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: JournalCategoryTag? = null,
    val recommendation: JournalPromptRecommendation = AdaptivePromptEngine.recommend(
        metrics = null,
        latestTag = null
    ),
    val promptVariantIndex: Int = 0,
    val isTemplateSheetVisible: Boolean = false,
    val pendingDeleteEntry: JournalEntry? = null,
    val pendingMutationIds: Set<Long> = emptySet(),
    val errorMessage: String? = null
) {
    val hasActiveFilter: Boolean
        get() = searchQuery.isNotBlank() || selectedCategory != null
}

sealed interface JournalEvent {
    data class ShowMessage(val message: String) : JournalEvent
}

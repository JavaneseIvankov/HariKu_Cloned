package com.hariku.feature.journal.presentation.create

import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.domain.model.JournalStateSnapshot

enum class GuidedJournalStep(val stepNumber: Int) {
    CHECK_IN(1),
    SELECT_PROMPT(2),
    EDITOR(3)
}

object GuidedFeelingCatalog {
    /** Daftar 15 chip perasaan sesuai spesifikasi Figma Node 1450:13390. */
    val defaultFeelings: List<String> = listOf(
        "Senang",
        "Sedih",
        "Cemas",
        "Marah",
        "Takut",
        "Stress",
        "Lelah",
        "Kecewa",
        "Termotivasi",
        "Ingin Fokus",
        "Bosan",
        "Kesepian",
        "Damai",
        "Malu",
        "Muak"
    )
}

data class GuidedJournalEditorUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val editingJournalId: Long? = null,
    val skipFlowMode: Boolean = false,
    val currentStep: GuidedJournalStep = GuidedJournalStep.CHECK_IN,
    val selectedFeelings: Set<String> = emptySet(),
    val customFeelingNote: String = "",
    val recommendation: JournalPromptRecommendation = AdaptivePromptEngine.recommend(
        metrics = null,
        latestTag = null
    ),
    val selectedTemplate: CbtTemplateType = CbtTemplateType.GENERAL_REFLECTION,
    val isTemplateManuallyOverridden: Boolean = false,
    val dynamicPrompts: List<String> = AdaptivePromptEngine.questionsFor(CbtTemplateType.GENERAL_REFLECTION)
        .map { AdaptivePromptEngine.injectContext(it, null) },
    val selectedPrompt: String = "",
    val title: String = "",
    val content: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val selectedTag: JournalCategoryTag = JournalCategoryTag.UMUM,
    val hasUserPickedTag: Boolean = false,
    val isPinned: Boolean = false,
    val isTemplateSheetVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val titleError: String? = null,
    val contentError: String? = null,
    val saveError: String? = null,
    val lastSavedSnapshot: JournalStateSnapshot? = null
) {
    val isEditMode: Boolean
        get() = editingJournalId != null
}

sealed interface GuidedJournalEditorEvent {
    data object NavigateBack : GuidedJournalEditorEvent
    data class JournalSaved(val isEditMode: Boolean) : GuidedJournalEditorEvent
}

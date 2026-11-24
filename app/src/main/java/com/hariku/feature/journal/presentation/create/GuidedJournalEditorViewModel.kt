package com.hariku.feature.journal.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.GetJournalByIdUseCase
import com.hariku.feature.journal.domain.usecase.JournalValidationException
import com.hariku.feature.journal.domain.usecase.SaveJournalEntryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuidedJournalEditorViewModel(
    private val getAdaptivePromptUseCase: GetAdaptivePromptUseCase,
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuidedJournalEditorUiState())
    val uiState: StateFlow<GuidedJournalEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<GuidedJournalEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isInitialized = false

    init {
        observeAdaptivePrompts()
    }

    fun initialize(
        journalId: Long?,
        skipToEditor: Boolean = false,
        templateOverride: String? = null,
        prefillTitle: String? = null
    ) {
        if (isInitialized) return
        isInitialized = true

        if (journalId != null && journalId > 0L) {
            loadExistingJournal(journalId)
            return
        }

        val parsedTemplate = templateOverride
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> CbtTemplateType.entries.firstOrNull { it.name == raw } }

        _uiState.update { state ->
            val effectiveTemplate = parsedTemplate ?: state.recommendation.template
            val nextState = state.copy(
                skipFlowMode = skipToEditor,
                currentStep = if (skipToEditor) GuidedJournalStep.EDITOR else GuidedJournalStep.CHECK_IN,
                selectedTemplate = effectiveTemplate,
                isTemplateManuallyOverridden = parsedTemplate != null,
                title = prefillTitle?.take(SaveJournalEntryUseCase.MAX_TITLE_LENGTH) ?: state.title
            )
            nextState.copy(dynamicPrompts = computeDynamicPrompts(nextState))
        }
    }

    private fun loadExistingJournal(journalId: Long) {
        _uiState.update {
            it.copy(
                isLoading = true,
                editingJournalId = journalId,
                currentStep = GuidedJournalStep.EDITOR
            )
        }
        viewModelScope.launch {
            getJournalByIdUseCase(journalId).fold(
                onSuccess = { entry ->
                    _uiState.update { state ->
                        val updated = state.copy(
                            isLoading = false,
                            editingJournalId = entry.id,
                            currentStep = GuidedJournalStep.EDITOR,
                            title = entry.title,
                            content = entry.content,
                            dateMillis = entry.dateMillis,
                            selectedTag = entry.tag,
                            hasUserPickedTag = true,
                            selectedTemplate = entry.templateType,
                            isTemplateManuallyOverridden = true,
                            selectedPrompt = entry.promptUsed,
                            isPinned = entry.isPinned,
                            saveError = null
                        )
                        updated.copy(dynamicPrompts = computeDynamicPrompts(updated))
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            saveError = error.message ?: "Data jurnal tidak ditemukan."
                        )
                    }
                }
            )
        }
    }

    private fun observeAdaptivePrompts() {
        viewModelScope.launch {
            getAdaptivePromptUseCase()
                .catch { /* Gunakan fallback netral jika stream gagal */ }
                .collect { recommendation ->
                    _uiState.update { state ->
                        val effectiveTemplate = if (state.isTemplateManuallyOverridden || state.isEditMode) {
                            state.selectedTemplate
                        } else {
                            resolveTemplateWithFeelings(recommendation, state.selectedFeelings)
                        }
                        val nextState = state.copy(
                            recommendation = recommendation,
                            selectedTemplate = effectiveTemplate
                        )
                        nextState.copy(dynamicPrompts = computeDynamicPrompts(nextState))
                    }
                }
        }
    }

    fun onToggleFeeling(feeling: String) {
        _uiState.update { state ->
            val nextFeelings = if (feeling in state.selectedFeelings) {
                state.selectedFeelings - feeling
            } else {
                state.selectedFeelings + feeling
            }
            val suggestedTag = if (state.hasUserPickedTag) {
                state.selectedTag
            } else {
                inferCategoryFromFeelings(nextFeelings) ?: state.selectedTag
            }
            val effectiveTemplate = if (state.isTemplateManuallyOverridden || state.isEditMode) {
                state.selectedTemplate
            } else {
                resolveTemplateWithFeelings(state.recommendation, nextFeelings)
            }
            val nextState = state.copy(
                selectedFeelings = nextFeelings,
                selectedTag = suggestedTag,
                selectedTemplate = effectiveTemplate
            )
            nextState.copy(dynamicPrompts = computeDynamicPrompts(nextState))
        }
    }

    fun onCustomFeelingNoteChange(value: String) {
        _uiState.update { state ->
            val nextState = state.copy(customFeelingNote = value.take(160))
            nextState.copy(dynamicPrompts = computeDynamicPrompts(nextState))
        }
    }

    fun onProceedFromCheckIn() {
        _uiState.update { state ->
            val updated = state.copy(currentStep = GuidedJournalStep.SELECT_PROMPT)
            updated.copy(dynamicPrompts = computeDynamicPrompts(updated))
        }
    }

    fun onSkipCheckIn() {
        _uiState.update { state ->
            val effectiveTemplate = if (state.isTemplateManuallyOverridden) {
                state.selectedTemplate
            } else {
                state.recommendation.template
            }
            val updated = state.copy(
                selectedFeelings = emptySet(),
                customFeelingNote = "",
                selectedTemplate = effectiveTemplate,
                currentStep = GuidedJournalStep.SELECT_PROMPT
            )
            updated.copy(dynamicPrompts = computeDynamicPrompts(updated))
        }
    }

    fun onSelectPrompt(prompt: String) {
        _uiState.update { state ->
            val nextTitle = if (state.title.isBlank()) {
                deriveTitleFromPrompt(prompt, state.selectedTemplate)
            } else {
                state.title
            }
            state.copy(
                selectedPrompt = prompt,
                title = nextTitle,
                titleError = null,
                currentStep = GuidedJournalStep.EDITOR
            )
        }
    }

    fun onSkipPromptSelection() {
        _uiState.update { state ->
            val fallbackTemplate = if (state.isTemplateManuallyOverridden) {
                state.selectedTemplate
            } else {
                CbtTemplateType.GENERAL_REFLECTION
            }
            state.copy(
                selectedPrompt = "",
                selectedTemplate = fallbackTemplate,
                currentStep = GuidedJournalStep.EDITOR
            )
        }
    }

    fun onBackPressed() {
        val state = _uiState.value
        when {
            state.isEditMode || state.skipFlowMode -> sendEvent(GuidedJournalEditorEvent.NavigateBack)
            state.currentStep == GuidedJournalStep.EDITOR -> {
                _uiState.update { it.copy(currentStep = GuidedJournalStep.SELECT_PROMPT) }
            }
            state.currentStep == GuidedJournalStep.SELECT_PROMPT -> {
                _uiState.update { it.copy(currentStep = GuidedJournalStep.CHECK_IN) }
            }
            else -> sendEvent(GuidedJournalEditorEvent.NavigateBack)
        }
    }

    fun onTitleChange(newTitle: String) {
        if (newTitle.length <= SaveJournalEntryUseCase.MAX_TITLE_LENGTH) {
            _uiState.update {
                it.copy(
                    title = newTitle,
                    titleError = null,
                    saveError = null
                )
            }
        }
    }

    fun onContentChange(newContent: String) {
        if (newContent.length <= SaveJournalEntryUseCase.MAX_CONTENT_LENGTH) {
            _uiState.update {
                it.copy(
                    content = newContent,
                    contentError = null,
                    saveError = null
                )
            }
        }
    }

    fun onSelectCategoryTag(tag: JournalCategoryTag) {
        _uiState.update {
            it.copy(
                selectedTag = tag,
                hasUserPickedTag = true,
                saveError = null
            )
        }
    }

    fun onShowTemplateSheet() {
        _uiState.update { it.copy(isTemplateSheetVisible = true) }
    }

    fun onDismissTemplateSheet() {
        _uiState.update { it.copy(isTemplateSheetVisible = false) }
    }

    fun onSelectTemplate(template: CbtTemplateType) {
        _uiState.update { state ->
            val updated = state.copy(
                selectedTemplate = template,
                isTemplateManuallyOverridden = true,
                isTemplateSheetVisible = false
            )
            updated.copy(dynamicPrompts = computeDynamicPrompts(updated))
        }
    }

    fun onShowDatePicker() {
        _uiState.update { it.copy(isDatePickerVisible = true) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(isDatePickerVisible = false) }
    }

    fun onDateSelected(selectedDateMillis: Long) {
        _uiState.update {
            it.copy(
                dateMillis = selectedDateMillis,
                isDatePickerVisible = false
            )
        }
    }

    fun onInsertTemplateFramework() {
        _uiState.update { state ->
            val frameworkBlock = state.selectedTemplate.sections.joinToString(separator = "\n\n") { section ->
                "• $section:\n"
            }
            val combined = if (state.content.isBlank()) {
                frameworkBlock
            } else {
                "${state.content.trimEnd()}\n\n$frameworkBlock"
            }
            state.copy(
                content = combined.take(SaveJournalEntryUseCase.MAX_CONTENT_LENGTH),
                contentError = null
            )
        }
    }

    fun onSaveJournal() {
        val current = _uiState.value
        if (current.isSaving) return

        val trimmedTitle = current.title.trim()
        val trimmedContent = current.content.trim()

        val titleErr = when {
            trimmedTitle.isEmpty() -> "Judul jurnal tidak boleh kosong"
            trimmedTitle.length > SaveJournalEntryUseCase.MAX_TITLE_LENGTH ->
                "Judul maksimal ${SaveJournalEntryUseCase.MAX_TITLE_LENGTH} karakter"
            else -> null
        }
        val contentErr = when {
            trimmedContent.isEmpty() -> "Isi teks jurnal tidak boleh kosong"
            trimmedContent.length > SaveJournalEntryUseCase.MAX_CONTENT_LENGTH ->
                "Isi teks maksimal ${SaveJournalEntryUseCase.MAX_CONTENT_LENGTH} karakter"
            else -> null
        }

        if (titleErr != null || contentErr != null) {
            _uiState.update {
                it.copy(
                    titleError = titleErr,
                    contentError = contentErr
                )
            }
            return
        }

        val finalContent = buildFinalContentWithCheckIn(
            rawContent = trimmedContent,
            feelings = current.selectedFeelings,
            customFeeling = current.customFeelingNote,
            isEditMode = current.isEditMode
        )

        val entryToSave = JournalEntry(
            id = current.editingJournalId ?: 0L,
            title = trimmedTitle,
            content = finalContent,
            dateMillis = current.dateMillis,
            tag = current.selectedTag,
            templateType = current.selectedTemplate,
            promptUsed = current.selectedPrompt,
            isPinned = current.isPinned
        )

        _uiState.update {
            it.copy(
                isSaving = true,
                titleError = null,
                contentError = null,
                saveError = null
            )
        }

        viewModelScope.launch {
            saveJournalEntryUseCase(
                entry = entryToSave,
                metrics = current.recommendation.metrics
            ).fold(
                onSuccess = { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            lastSavedSnapshot = snapshot
                        )
                    }
                    sendEvent(GuidedJournalEditorEvent.JournalSaved(isEditMode = current.isEditMode))
                },
                onFailure = { error ->
                    val message = if (error is JournalValidationException) {
                        error.message ?: "Periksa kembali input jurnalmu."
                    } else {
                        "Gagal menyimpan jurnal. Silakan coba lagi."
                    }
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            saveError = message
                        )
                    }
                }
            )
        }
    }

    private fun resolveTemplateWithFeelings(
        recommendation: JournalPromptRecommendation,
        feelings: Set<String>
    ): CbtTemplateType {
        // Prioritas utama selalu metrik EVI & ADS aktual sesuai Flowchart 2.2.3
        if (!recommendation.metrics.isDefaultFallback) {
            return recommendation.template
        }
        // Jika pengguna baru belum punya riwayat EVI/ADS, check-in Step 1 membantu mempersonalisasi template
        return when {
            feelings.any { it in setOf("Cemas", "Takut") } -> CbtTemplateType.COGNITIVE_REFRAMING
            feelings.any { it in setOf("Marah", "Muak", "Sedih") } -> CbtTemplateType.BEHAVIORAL_ACTIVATION
            feelings.any { it in setOf("Stress", "Kecewa", "Malu", "Lelah") } -> CbtTemplateType.THOUGHT_RECORD
            else -> recommendation.template
        }
    }

    private fun inferCategoryFromFeelings(feelings: Set<String>): JournalCategoryTag? = when {
        feelings.any { it in setOf("Cemas", "Takut") } -> JournalCategoryTag.KECEMASAN
        feelings.any { it in setOf("Stress", "Muak") } -> JournalCategoryTag.STRES
        feelings.any { it == "Lelah" } -> JournalCategoryTag.TIDUR
        feelings.any { it in setOf("Termotivasi", "Ingin Fokus", "Bosan") } -> JournalCategoryTag.PRODUKTIVITAS
        feelings.any { it in setOf("Kesepian", "Kecewa", "Malu") } -> JournalCategoryTag.HUBUNGAN
        feelings.any { it in setOf("Senang", "Damai") } -> JournalCategoryTag.POSITIVITAS
        else -> null
    }

    private fun computeDynamicPrompts(state: GuidedJournalEditorUiState): List<String> {
        val contextSlotValue = state.recommendation.contextTag
        val baseQuestions = AdaptivePromptEngine.questionsFor(state.selectedTemplate)
            .map { question -> AdaptivePromptEngine.injectContext(question, contextSlotValue) }

        val feelingLabels = buildList {
            addAll(state.selectedFeelings.map { it.lowercase() })
            state.customFeelingNote.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
        }

        if (feelingLabels.isEmpty()) {
            return baseQuestions
        }

        val feelingSummary = feelingLabels.take(3).joinToString(", ")
        val personalizedPrompt = when (state.selectedTemplate) {
            CbtTemplateType.COGNITIVE_REFRAMING ->
                "Kamu sedang merasa $feelingSummary. Pikiran apa terkait $contextSlotValue yang paling memicu rasa itu, dan bagaimana fakta nyatanya?"
            CbtTemplateType.BEHAVIORAL_ACTIVATION ->
                "Saat merasa $feelingSummary, langkah kecil apa terkait $contextSlotValue yang bisa membuatmu sedikit lebih tenang hari ini?"
            CbtTemplateType.THOUGHT_RECORD ->
                "Kamu mencatat perasaan $feelingSummary. Situasi apa pada $contextSlotValue yang memicunya, dan apa respon alternatif yang lebih seimbang?"
            CbtTemplateType.GENERAL_REFLECTION ->
                "Kamu merasa $feelingSummary hari ini. Bagaimana $contextSlotValue memengaruhi harimu dan apa yang patut kamu apresiasi dari dirimu?"
        }

        return listOf(personalizedPrompt) + baseQuestions
    }

    private fun deriveTitleFromPrompt(prompt: String, template: CbtTemplateType): String {
        val clean = prompt.replace("?", "").trim()
        if (clean.length <= SaveJournalEntryUseCase.MAX_TITLE_LENGTH) {
            return clean
        }
        val prefix = "${template.title}: "
        val remaining = SaveJournalEntryUseCase.MAX_TITLE_LENGTH - prefix.length
        val truncated = clean.take(remaining).substringBeforeLast(' ').ifBlank { clean.take(remaining) }
        return (prefix + truncated).take(SaveJournalEntryUseCase.MAX_TITLE_LENGTH)
    }

    private fun buildFinalContentWithCheckIn(
        rawContent: String,
        feelings: Set<String>,
        customFeeling: String,
        isEditMode: Boolean
    ): String {
        if (isEditMode || (feelings.isEmpty() && customFeeling.isBlank())) {
            return rawContent
        }
        val feelingParts = buildList {
            if (feelings.isNotEmpty()) add(feelings.joinToString(", "))
            if (customFeeling.isNotBlank()) add(customFeeling.trim())
        }.joinToString(" · ")

        val header = "[Perasaan awal: $feelingParts]\n\n"
        return if (rawContent.startsWith("[Perasaan awal:")) {
            rawContent
        } else {
            (header + rawContent).take(SaveJournalEntryUseCase.MAX_CONTENT_LENGTH)
        }
    }

    private fun sendEvent(event: GuidedJournalEditorEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

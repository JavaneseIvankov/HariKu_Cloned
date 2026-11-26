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
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

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
            applyAdaptivePromptResolution(nextState)
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
                        applyAdaptivePromptResolution(updated)
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
                        val nextState = state.copy(recommendation = recommendation)
                        applyAdaptivePromptResolution(nextState)
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
            applyAdaptivePromptResolution(state.copy(selectedFeelings = nextFeelings))
        }
    }

    fun onCustomFeelingNoteChange(value: String) {
        _uiState.update { state ->
            applyAdaptivePromptResolution(state.copy(customFeelingNote = value.take(160)))
        }
    }

    fun onProceedFromCheckIn() {
        _uiState.update { state ->
            applyAdaptivePromptResolution(state.copy(currentStep = GuidedJournalStep.SELECT_PROMPT))
        }
    }

    fun onSkipCheckIn() {
        _uiState.update { state ->
            applyAdaptivePromptResolution(
                state.copy(
                    selectedFeelings = emptySet(),
                    customFeelingNote = "",
                    currentStep = GuidedJournalStep.SELECT_PROMPT
                )
            )
        }
    }

    fun onSelectPrompt(prompt: String) {
        _uiState.update { state ->
            state.copy(
                selectedPrompt = prompt,
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
        _uiState.update { it.copy(activeModal = EditorModal.TemplateSelector) }
    }

    fun onDismissTemplateSheet() {
        _uiState.update { it.copy(activeModal = EditorModal.None) }
    }

    fun onSelectTemplate(template: CbtTemplateType) {
        _uiState.update { state ->
            val updated = state.copy(
                selectedTemplate = template,
                isTemplateManuallyOverridden = true,
                activeModal = EditorModal.None
            )
            applyAdaptivePromptResolution(updated)
        }
    }

    fun onShowDatePicker() {
        _uiState.update { it.copy(activeModal = EditorModal.DatePicker) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(activeModal = EditorModal.None) }
    }

    fun onDateSelected(selectedUtcDateMillis: Long) {
        _uiState.update { state ->
            state.copy(
                dateMillis = mergeUtcDateWithLocalTime(
                    utcDateMillis = selectedUtcDateMillis,
                    referenceMillis = state.dateMillis
                ),
                activeModal = EditorModal.None
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
            val invalidField = if (titleErr != null) {
                JournalEditorField.TITLE
            } else {
                JournalEditorField.CONTENT
            }
            sendEvent(GuidedJournalEditorEvent.FocusInvalidField(invalidField))
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

    private fun applyAdaptivePromptResolution(state: GuidedJournalEditorUiState): GuidedJournalEditorUiState {
        val latestTag = JournalCategoryTag.entries.firstOrNull {
            AdaptivePromptEngine.resolveContextTag(it) == state.recommendation.contextTag
        }
        val overrideTemplate = if (state.isTemplateManuallyOverridden || state.isEditMode) {
            state.selectedTemplate
        } else {
            null
        }
        val resolved = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = state.recommendation.metrics,
            latestTag = latestTag,
            selectedFeelings = state.selectedFeelings,
            customFeelingNote = state.customFeelingNote,
            templateOverride = overrideTemplate
        )
        val nextTag = if (state.hasUserPickedTag) {
            state.selectedTag
        } else {
            resolved.suggestedCategory ?: JournalCategoryTag.UMUM
        }
        return state.copy(
            selectedTemplate = overrideTemplate ?: resolved.template,
            selectedTag = nextTag,
            dynamicPrompts = resolved.prompts,
            checkInReason = resolved.reason
        )
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

    companion object {
        /** Material3 DatePicker mengembalikan UTC 00:00; gabungkan dengan jam lokal agar tanggal tidak bergeser. */
        fun mergeUtcDateWithLocalTime(
            utcDateMillis: Long,
            referenceMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val pickedDate = Instant.ofEpochMilli(utcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            val localTime = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalTime()
            return pickedDate.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
        }
    }
}

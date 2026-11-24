package com.hariku.feature.journal.presentation.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.core.ui.components.CustomizeTopBar
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.components.TemplateSelectorSheet
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun GuidedJournalEditorScreen(
    journalId: Long? = null,
    skipToEditor: Boolean = false,
    templateOverride: String? = null,
    prefillTitle: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCompleted: () -> Unit,
    viewModel: GuidedJournalEditorViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToCompleted by rememberUpdatedState(onNavigateToCompleted)

    SideEffect {
        viewModel.initialize(
            journalId = journalId,
            skipToEditor = skipToEditor,
            templateOverride = templateOverride,
            prefillTitle = prefillTitle
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                GuidedJournalEditorEvent.NavigateBack -> currentOnNavigateBack()
                is GuidedJournalEditorEvent.JournalSaved -> {
                    if (event.isEditMode) {
                        currentOnNavigateBack()
                    } else {
                        currentOnNavigateToCompleted()
                    }
                }
            }
        }
    }

    BackHandler(
        enabled = !state.isEditMode &&
            !state.skipFlowMode &&
            state.currentStep != GuidedJournalStep.CHECK_IN
    ) {
        viewModel.onBackPressed()
    }

    GuidedJournalEditorContent(
        state = state,
        onBackClick = viewModel::onBackPressed,
        onToggleFeeling = viewModel::onToggleFeeling,
        onCustomFeelingNoteChange = viewModel::onCustomFeelingNoteChange,
        onProceedFromCheckIn = viewModel::onProceedFromCheckIn,
        onSkipCheckIn = viewModel::onSkipCheckIn,
        onSelectPrompt = viewModel::onSelectPrompt,
        onSkipPromptSelection = viewModel::onSkipPromptSelection,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        onSelectCategoryTag = viewModel::onSelectCategoryTag,
        onShowTemplateSheet = viewModel::onShowTemplateSheet,
        onDismissTemplateSheet = viewModel::onDismissTemplateSheet,
        onSelectTemplate = viewModel::onSelectTemplate,
        onShowDatePicker = viewModel::onShowDatePicker,
        onDismissDatePicker = viewModel::onDismissDatePicker,
        onDateSelected = viewModel::onDateSelected,
        onInsertTemplateFramework = viewModel::onInsertTemplateFramework,
        onSaveJournal = viewModel::onSaveJournal
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedJournalEditorContent(
    state: GuidedJournalEditorUiState,
    onBackClick: () -> Unit,
    onToggleFeeling: (String) -> Unit,
    onCustomFeelingNoteChange: (String) -> Unit,
    onProceedFromCheckIn: () -> Unit,
    onSkipCheckIn: () -> Unit,
    onSelectPrompt: (String) -> Unit,
    onSkipPromptSelection: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSelectCategoryTag: (JournalCategoryTag) -> Unit,
    onShowTemplateSheet: () -> Unit,
    onDismissTemplateSheet: () -> Unit,
    onSelectTemplate: (CbtTemplateType) -> Unit,
    onShowDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onInsertTemplateFramework: () -> Unit,
    onSaveJournal: () -> Unit
) {
    val topBarTitle = when {
        state.isEditMode -> "Edit Jurnal"
        state.currentStep == GuidedJournalStep.CHECK_IN -> "Guided Journaling"
        state.currentStep == GuidedJournalStep.SELECT_PROMPT -> "Buat Prompt AI"
        else -> "Buat Jurnal"
    }

    when (state.activeModal) {
        EditorModal.None -> Unit
        EditorModal.TemplateSelector -> {
            TemplateSelectorSheet(
                recommendedTemplate = state.recommendation.template,
                selectedTemplate = state.selectedTemplate,
                onSelectTemplate = onSelectTemplate,
                onDismiss = onDismissTemplateSheet
            )
        }
        EditorModal.DatePicker -> {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.dateMillis)
            DatePickerDialog(
                onDismissRequest = onDismissDatePicker,
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis ?: state.dateMillis
                            onDateSelected(millis)
                        }
                    ) {
                        Text("Pilih", color = JournalDesignTokens.PrimaryCta, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDatePicker) {
                        Text("Batal", color = JournalDesignTokens.TextSecondary)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    Scaffold(
        containerColor = JournalDesignTokens.CanvasBackground,
        topBar = {
            CustomizeTopBar(
                title = topBarTitle,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .background(JournalDesignTokens.CanvasBackground)
                .imePadding()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = JournalDesignTokens.PrimaryCta,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!state.isEditMode && !state.skipFlowMode) {
                        GuidedStepIndicator(currentStep = state.currentStep)
                    }

                    when (state.currentStep) {
                        GuidedJournalStep.CHECK_IN -> CheckInStepSection(
                            selectedFeelings = state.selectedFeelings,
                            customFeelingNote = state.customFeelingNote,
                            onToggleFeeling = onToggleFeeling,
                            onCustomFeelingNoteChange = onCustomFeelingNoteChange,
                            onProceed = onProceedFromCheckIn,
                            onSkip = onSkipCheckIn
                        )

                        GuidedJournalStep.SELECT_PROMPT -> SelectPromptStepSection(
                            state = state,
                            onSelectPrompt = onSelectPrompt,
                            onSkip = onSkipPromptSelection,
                            onOpenTemplateSheet = onShowTemplateSheet
                        )

                        GuidedJournalStep.EDITOR -> JournalEditorStepSection(
                            state = state,
                            onTitleChange = onTitleChange,
                            onContentChange = onContentChange,
                            onSelectCategoryTag = onSelectCategoryTag,
                            onOpenTemplateSheet = onShowTemplateSheet,
                            onShowDatePicker = onShowDatePicker,
                            onInsertTemplateFramework = onInsertTemplateFramework,
                            onSave = onSaveJournal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedStepIndicator(currentStep: GuidedJournalStep) {
    val steps = listOf(
        GuidedJournalStep.CHECK_IN to "1. Perasaan",
        GuidedJournalStep.SELECT_PROMPT to "2. Prompt AI",
        GuidedJournalStep.EDITOR to "3. Tulis Jurnal"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { (step, label) ->
            val isActive = step == currentStep
            val isDone = step.stepNumber < currentStep.stepNumber
            val bgColor = when {
                isActive -> JournalDesignTokens.PrimaryCta
                isDone -> JournalDesignTokens.PeachHighlight
                else -> JournalDesignTokens.CardWhite
            }
            val textColor = when {
                isActive -> Color.White
                isDone -> JournalDesignTokens.TextPrimary
                else -> JournalDesignTokens.TextMuted
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidedJournalCheckInPreview() {
    HariKuTheme {
        GuidedJournalEditorContent(
            state = GuidedJournalEditorUiState(
                selectedFeelings = setOf("Cemas", "Ingin Fokus")
            ),
            onBackClick = {},
            onToggleFeeling = {},
            onCustomFeelingNoteChange = {},
            onProceedFromCheckIn = {},
            onSkipCheckIn = {},
            onSelectPrompt = {},
            onSkipPromptSelection = {},
            onTitleChange = {},
            onContentChange = {},
            onSelectCategoryTag = {},
            onShowTemplateSheet = {},
            onDismissTemplateSheet = {},
            onSelectTemplate = {},
            onShowDatePicker = {},
            onDismissDatePicker = {},
            onDateSelected = {},
            onInsertTemplateFramework = {},
            onSaveJournal = {}
        )
    }
}

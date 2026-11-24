package com.hariku.feature.journal.presentation.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.R
import com.hariku.core.ui.components.CustomizeTopBar
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.usecase.SaveJournalEntryUseCase
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.components.JournalCategoryCatalog
import com.hariku.feature.journal.presentation.components.MetricStatusPill
import com.hariku.feature.journal.presentation.components.TemplateSelectorSheet
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    LaunchedEffect(journalId, skipToEditor, templateOverride, prefillTitle) {
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
                GuidedJournalEditorEvent.NavigateBack -> onNavigateBack()
                is GuidedJournalEditorEvent.JournalSaved -> {
                    if (event.isEditMode) {
                        onNavigateBack()
                    } else {
                        onNavigateToCompleted()
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

    if (state.isTemplateSheetVisible) {
        TemplateSelectorSheet(
            recommendedTemplate = state.recommendation.template,
            selectedTemplate = state.selectedTemplate,
            onSelectTemplate = onSelectTemplate,
            onDismiss = onDismissTemplateSheet
        )
    }

    if (state.isDatePickerVisible) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInStepSection(
    selectedFeelings: Set<String>,
    customFeelingNote: String,
    onToggleFeeling: (String) -> Unit,
    onCustomFeelingNoteChange: (String) -> Unit,
    onProceed: () -> Unit,
    onSkip: () -> Unit
) {
    // Header ilustrasi Step 1 (prompt 2)
    HeaderIllustrationBanner(
        title = "Kenali Emosimu Sejenak",
        subtitle = "Pilih satu atau beberapa perasaan yang paling menggambarkan kondisimu saat ini.",
        iconRes = R.drawable.ic_cat_handing_book
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Bagaimana Perasaanmu?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = JournalDesignTokens.TextPrimary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GuidedFeelingCatalog.defaultFeelings.forEach { feeling ->
                    val isSelected = feeling in selectedFeelings
                    val chipShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(
                                if (isSelected) {
                                    JournalDesignTokens.PeachHighlight
                                } else {
                                    JournalDesignTokens.PeachHighlight.copy(alpha = 0.55f)
                                }
                            )
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) {
                                    JournalDesignTokens.PrimaryCta
                                } else {
                                    Color.Transparent
                                },
                                shape = chipShape
                            )
                            .clickable { onToggleFeeling(feeling) }
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = JournalDesignTokens.PrimaryCta,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = feeling,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = JournalDesignTokens.TextPrimary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customFeelingNote,
                onValueChange = onCustomFeelingNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text(
                        text = "Tulis di sini apabila ada perasaan lain yang ingin kamu tambahkan....",
                        fontSize = 13.sp,
                        color = JournalDesignTokens.TextMuted,
                        lineHeight = 18.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JournalDesignTokens.CardSoft,
                    unfocusedContainerColor = JournalDesignTokens.CardSoft,
                    focusedBorderColor = JournalDesignTokens.PrimaryCta,
                    unfocusedBorderColor = Color(0xFFE5DFDC),
                    focusedTextColor = JournalDesignTokens.TextPrimary,
                    unfocusedTextColor = JournalDesignTokens.TextPrimary,
                    cursorColor = JournalDesignTokens.PrimaryCta
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onProceed,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JournalDesignTokens.PrimaryCta,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text(
                        text = "Lanjut",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(JournalDesignTokens.PrimaryCta)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = JournalDesignTokens.PrimaryCta
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Lewati",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectPromptStepSection(
    state: GuidedJournalEditorUiState,
    onSelectPrompt: (String) -> Unit,
    onSkip: () -> Unit,
    onOpenTemplateSheet: () -> Unit
) {
    val metrics = state.recommendation.metrics

    // Header ilustrasi Step 2 (prompt 1)
    HeaderIllustrationBanner(
        title = "Pertanyaan Pemantik Adaptif",
        subtitle = "Disusun otomatis dari indeks EVI, ADS, konteks jurnal terakhir, dan perasaanmu.",
        iconRes = R.drawable.positivitas_hal_baik
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JournalDesignTokens.PeachHighlight)
                        .clickable(onClick = onOpenTemplateSheet)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = state.selectedTemplate.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Ubah template",
                        tint = JournalDesignTokens.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricStatusPill(
                    label = "EVI: ${metrics.eviLevel.label} (${String.format(Locale.US, "%.2f", metrics.eviScore)})",
                    isElevated = metrics.eviLevel == EviLevel.VOLATIL
                )
                MetricStatusPill(
                    label = "ADS: ${metrics.adsLevel.label} (${(metrics.adsScore * 100).toInt()}%)",
                    isElevated = metrics.adsLevel != AdsLevel.RENDAH
                )
            }

            Text(
                text = "Pilih salah satu pertanyaan pemantik di bawah untuk mulai berefleksi:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = JournalDesignTokens.TextPrimary,
                lineHeight = 21.sp
            )

            Text(
                text = buildString {
                    append(state.recommendation.reason)
                    append(" Konteks topik: ${state.recommendation.contextTag}.")
                    if (state.selectedFeelings.isNotEmpty()) {
                        append(" Perasaan dipilih: ${state.selectedFeelings.joinToString(", ")}.")
                    }
                },
                fontSize = 12.sp,
                color = JournalDesignTokens.TextSecondary,
                lineHeight = 17.sp
            )

            // Daftar item prompt (#EDE9E7 50% alpha + chevron kanan)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.dynamicPrompts.forEach { promptItem ->
                    val itemShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(JournalDesignTokens.PromptItemSurface)
                            .border(
                                width = 1.dp,
                                color = JournalDesignTokens.PromptItemBorder,
                                shape = itemShape
                            )
                            .clickable { onSelectPrompt(promptItem) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = promptItem,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalDesignTokens.TextPrimary,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "Pilih prompt",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(JournalDesignTokens.PrimaryCta)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = JournalDesignTokens.PrimaryCta
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Lewati (Tulis Gaya Bebas)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalEditorStepSection(
    state: GuidedJournalEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSelectCategoryTag: (JournalCategoryTag) -> Unit,
    onOpenTemplateSheet: () -> Unit,
    onShowDatePicker: () -> Unit,
    onInsertTemplateFramework: () -> Unit,
    onSave: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    }
    val formattedDate = remember(state.dateMillis) {
        Instant.ofEpochMilli(state.dateMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    // Header ilustrasi Step 3 (header journal 1)
    val categoryVisual = JournalCategoryCatalog.of(state.selectedTag)
    HeaderIllustrationBanner(
        title = if (state.isEditMode) "Perbarui Catatan Jurnalmu" else "Ruang Refleksi Pribadimu",
        subtitle = state.selectedTemplate.description,
        iconRes = categoryVisual.iconRes
    )

    // Kartu utama editor (#F9F9F9 sesuai spesifikasi Figma Node 1451:11822)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Baris Tanggal Jurnal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(JournalDesignTokens.CardWhite)
                    .border(1.dp, Color(0xFFE5DFDC), RoundedCornerShape(14.dp))
                    .clickable(enabled = !state.isSaving, onClick = onShowDatePicker)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalDesignTokens.TextPrimary
                    )
                }
                Text(
                    text = "Ubah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JournalDesignTokens.PrimaryCta
                )
            }

            // Input Judul Journal (maks 60 karakter, field #FFDFC2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Judul Journal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Text(
                        text = "${state.title.length}/${SaveJournalEntryUseCase.MAX_TITLE_LENGTH}",
                        fontSize = 12.sp,
                        color = JournalDesignTokens.TextMuted
                    )
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    enabled = !state.isSaving,
                    singleLine = true,
                    isError = state.titleError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text(
                            text = "Masukkan judul jurnal...",
                            fontSize = 14.sp,
                            color = JournalDesignTokens.TextSecondary.copy(alpha = 0.7f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JournalDesignTokens.PeachHighlight,
                        unfocusedContainerColor = JournalDesignTokens.PeachHighlight,
                        disabledContainerColor = JournalDesignTokens.PeachHighlight.copy(alpha = 0.6f),
                        focusedBorderColor = JournalDesignTokens.PrimaryCta,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = JournalDesignTokens.ErrorInk,
                        focusedTextColor = JournalDesignTokens.TextPrimary,
                        unfocusedTextColor = JournalDesignTokens.TextPrimary,
                        cursorColor = JournalDesignTokens.PrimaryCta
                    )
                )

                if (state.titleError != null) {
                    Text(
                        text = state.titleError,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.ErrorInk
                    )
                }
            }

            // Pemilihan Tag Kategori
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tag Kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalDesignTokens.TextPrimary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JournalCategoryTag.entries.forEach { tag ->
                        val isSelected = tag == state.selectedTag
                        val shape = RoundedCornerShape(12.dp)
                        Row(
                            modifier = Modifier
                                .clip(shape)
                                .background(
                                    if (isSelected) {
                                        JournalDesignTokens.PeachHighlight
                                    } else {
                                        JournalDesignTokens.CardWhite
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        JournalDesignTokens.PrimaryCta
                                    } else {
                                        Color(0xFFE5DFDC)
                                    },
                                    shape = shape
                                )
                                .clickable(enabled = !state.isSaving) { onSelectCategoryTag(tag) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = JournalCategoryCatalog.of(tag).iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = tag.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = JournalDesignTokens.TextPrimary
                            )
                        }
                    }
                }
            }

            // Panduan Kerangka Template CBT Terpilih + Prompt Aktif
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JournalDesignTokens.PromptItemSurface)
                    .border(1.dp, JournalDesignTokens.PromptItemBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = JournalDesignTokens.PrimaryCta,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Panduan CBT: ${state.selectedTemplate.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalDesignTokens.TextPrimary
                        )
                    }
                    Text(
                        text = "Ganti",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.clickable(enabled = !state.isSaving, onClick = onOpenTemplateSheet)
                    )
                }

                if (state.selectedPrompt.isNotBlank()) {
                    Text(
                        text = "\"${state.selectedPrompt}\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalDesignTokens.TextPrimary,
                        lineHeight = 18.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.selectedTemplate.sections.forEachIndexed { index, section ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(JournalDesignTokens.PeachHighlight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalDesignTokens.PrimaryCta
                                )
                            }
                            Text(
                                text = section,
                                fontSize = 12.sp,
                                color = JournalDesignTokens.TextSecondary
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onInsertTemplateFramework,
                    enabled = !state.isSaving,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sisipkan poin kerangka ke teks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.PrimaryCta
                    )
                }
            }

            // Input Teks Jurnal (maks 50.000 karakter, background #FFDFC2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Teks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Text(
                        text = "${state.content.length}/${SaveJournalEntryUseCase.MAX_CONTENT_LENGTH}",
                        fontSize = 12.sp,
                        color = JournalDesignTokens.TextMuted
                    )
                }

                OutlinedTextField(
                    value = state.content,
                    onValueChange = onContentChange,
                    enabled = !state.isSaving,
                    isError = state.contentError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            text = "Isi Jurnalmu di sini",
                            fontSize = 14.sp,
                            color = JournalDesignTokens.TextSecondary.copy(alpha = 0.7f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JournalDesignTokens.PeachHighlight,
                        unfocusedContainerColor = JournalDesignTokens.PeachHighlight,
                        disabledContainerColor = JournalDesignTokens.PeachHighlight.copy(alpha = 0.6f),
                        focusedBorderColor = JournalDesignTokens.PrimaryCta,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = JournalDesignTokens.ErrorInk,
                        focusedTextColor = JournalDesignTokens.TextPrimary,
                        unfocusedTextColor = JournalDesignTokens.TextPrimary,
                        cursorColor = JournalDesignTokens.PrimaryCta
                    )
                )

                if (state.contentError != null) {
                    Text(
                        text = state.contentError,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.ErrorInk
                    )
                }
            }

            if (state.saveError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JournalDesignTokens.ErrorBg)
                        .padding(12.dp)
                ) {
                    Text(
                        text = state.saveError,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.ErrorInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JournalDesignTokens.PrimaryCta,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Selesai", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderIllustrationBanner(
    title: String,
    subtitle: String,
    iconRes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(JournalDesignTokens.PeachSoft)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = JournalDesignTokens.TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = JournalDesignTokens.TextSecondary,
                lineHeight = 17.sp
            )
        }
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit
        )
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

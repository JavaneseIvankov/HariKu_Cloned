package com.hariku.feature.journal.presentation.create

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.usecase.SaveJournalEntryUseCase
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.components.JournalCategoryCatalog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalEditorStepSection(
    state: GuidedJournalEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSelectCategoryTag: (JournalCategoryTag) -> Unit,
    onOpenTemplateSheet: () -> Unit,
    onShowDatePicker: () -> Unit,
    onInsertTemplateFramework: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    titleBringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() },
    contentBringIntoViewRequester: BringIntoViewRequester = remember { BringIntoViewRequester() },
    titleFocusRequester: FocusRequester = remember { FocusRequester() },
    contentFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    }
    val formattedDate = remember(state.dateMillis) {
        Instant.ofEpochMilli(state.dateMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    val categoryVisual = JournalCategoryCatalog.of(state.selectedTag)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderIllustrationBanner(
            title = if (state.isEditMode) "Perbarui Catatan Jurnalmu" else "Ruang Refleksi Pribadimu",
            subtitle = state.selectedTemplate.description,
            iconRes = categoryVisual.iconRes
        )

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

                Column(
                    modifier = Modifier.bringIntoViewRequester(titleBringIntoViewRequester),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                        singleLine = false,
                        maxLines = 2,
                        isError = state.titleError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = {
                            Text(
                                text = "Tulis judul singkat jurnalmu...",
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(JournalDesignTokens.PromptItemSurface)
                        .border(1.dp, JournalDesignTokens.PromptItemBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = JournalDesignTokens.PrimaryCta,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Panduan CBT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JournalDesignTokens.PrimaryCta
                                )
                            }
                            Text(
                                text = "Ganti",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JournalDesignTokens.PrimaryCta,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !state.isSaving, onClick = onOpenTemplateSheet)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = state.selectedTemplate.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalDesignTokens.TextPrimary,
                            lineHeight = 19.sp
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

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.selectedTemplate.sections.forEachIndexed { index, section ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(JournalDesignTokens.PeachHighlight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = JournalDesignTokens.PrimaryCta,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    text = section,
                                    fontSize = 12.sp,
                                    color = JournalDesignTokens.TextSecondary,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
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

                Column(
                    modifier = Modifier.bringIntoViewRequester(contentBringIntoViewRequester),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            .height(220.dp)
                            .focusRequester(contentFocusRequester),
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
}

@Preview(showBackground = true)
@Composable
private fun JournalEditorStepSectionPreview() {
    HariKuTheme {
        JournalEditorStepSection(
            state = GuidedJournalEditorUiState(
                currentStep = GuidedJournalStep.EDITOR,
                title = "Refleksi malam",
                content = "Hari ini terasa lebih tenang setelah menyusun prioritas."
            ),
            onTitleChange = {},
            onContentChange = {},
            onSelectCategoryTag = {},
            onOpenTemplateSheet = {},
            onShowDatePicker = {},
            onInsertTemplateFramework = {},
            onSave = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

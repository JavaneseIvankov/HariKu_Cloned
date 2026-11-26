package com.hariku.feature.journal.presentation.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.components.MetricStatusPill
import java.util.Locale

@Composable
internal fun SelectPromptStepSection(
    state: GuidedJournalEditorUiState,
    onSelectPrompt: (String) -> Unit,
    onSkip: () -> Unit,
    onOpenTemplateSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = state.recommendation.metrics

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    text = state.checkInReason,
                    fontSize = 12.sp,
                    color = JournalDesignTokens.TextSecondary,
                    lineHeight = 17.sp
                )

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
}

@Preview(showBackground = true)
@Composable
private fun SelectPromptStepSectionPreview() {
    HariKuTheme {
        SelectPromptStepSection(
            state = GuidedJournalEditorUiState(
                currentStep = GuidedJournalStep.SELECT_PROMPT,
                selectedFeelings = setOf("Cemas")
            ),
            onSelectPrompt = {},
            onSkip = {},
            onOpenTemplateSheet = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

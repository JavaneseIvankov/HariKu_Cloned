package com.hariku.feature.journal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.presentation.JournalDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectorSheet(
    recommendedTemplate: CbtTemplateType,
    selectedTemplate: CbtTemplateType = recommendedTemplate,
    onSelectTemplate: (CbtTemplateType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = JournalDesignTokens.CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pilih Kerangka Template CBT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JournalDesignTokens.TextPrimary
            )
            Text(
                text = "Template direkomendasikan otomatis berdasarkan indeks EVI (Mood) dan ADS (Percakapan), namun kamu bebas memilih kerangka lain.",
                fontSize = 12.sp,
                color = JournalDesignTokens.TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            CbtTemplateType.entries.forEach { template ->
                val isSelected = template == selectedTemplate
                val isRecommended = template == recommendedTemplate
                val shape = RoundedCornerShape(16.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (isSelected) JournalDesignTokens.PeachSoft else JournalDesignTokens.CardSoft
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                JournalDesignTokens.PrimaryCta
                            } else {
                                Color(0xFFE6E0DC)
                            },
                            shape = shape
                        )
                        .clickable { onSelectTemplate(template) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalDesignTokens.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isRecommended) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(JournalDesignTokens.PeachHighlight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Adaptif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JournalDesignTokens.PrimaryCta
                                )
                            }
                        }
                    }

                    Text(
                        text = template.description,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.TextSecondary,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = template.sections.joinToString(" → "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalDesignTokens.PrimaryCta
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TemplateSelectorSheetPreview() {
    HariKuTheme {
        TemplateSelectorSheet(
            recommendedTemplate = CbtTemplateType.COGNITIVE_REFRAMING,
            onSelectTemplate = {},
            onDismiss = {}
        )
    }
}

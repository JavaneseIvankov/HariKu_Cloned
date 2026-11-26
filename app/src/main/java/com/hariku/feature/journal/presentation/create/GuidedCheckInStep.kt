package com.hariku.feature.journal.presentation.create

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.presentation.JournalDesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CheckInStepSection(
    selectedFeelings: Set<String>,
    customFeelingNote: String,
    onToggleFeeling: (String) -> Unit,
    onCustomFeelingNoteChange: (String) -> Unit,
    onProceed: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
}

@Composable
internal fun HeaderIllustrationBanner(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
private fun CheckInStepSectionPreview() {
    HariKuTheme {
        CheckInStepSection(
            selectedFeelings = setOf("Cemas", "Ingin Fokus"),
            customFeelingNote = "",
            onToggleFeeling = {},
            onCustomFeelingNoteChange = {},
            onProceed = {},
            onSkip = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

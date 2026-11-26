package com.hariku.feature.journal.presentation.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.presentation.JournalDesignTokens
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptivePromptBannerCard(
    recommendation: JournalPromptRecommendation,
    onStartGuidedReflection: () -> Unit,
    onStartFreeWrite: () -> Unit,
    onCyclePrompt: () -> Unit,
    onOpenTemplateSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = recommendation.metrics
    val eviElevated = metrics.eviLevel == EviLevel.VOLATIL
    val adsElevated = metrics.adsLevel != AdsLevel.RENDAH

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = recommendation.template.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Lihat template CBT",
                        tint = JournalDesignTokens.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(onClick = onCyclePrompt) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Ganti variasi pertanyaan",
                        tint = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Indikator status EVI & ADS (Flowchart 2.2.3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricStatusPill(
                    label = "EVI: ${metrics.eviLevel.label} (${String.format(Locale.US, "%.2f", metrics.eviScore)})",
                    isElevated = eviElevated
                )
                MetricStatusPill(
                    label = "ADS: ${metrics.adsLevel.label} (${(metrics.adsScore * 100).toInt()}%)",
                    isElevated = adsElevated
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rekomendasi Refleksi Hari Ini",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Text(
                        text = recommendation.reason,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.TextSecondary,
                        lineHeight = 17.sp
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_cat_handing_book),
                    contentDescription = "Ilustrasi Jurnal Terpandu",
                    modifier = Modifier.size(62.dp)
                )
            }

            // Kotak pertanyaan pemantik dinamis (#EDE9E7 50% alpha)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JournalDesignTokens.PromptItemSurface)
                    .border(
                        width = 1.dp,
                        color = JournalDesignTokens.PromptItemBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "\"${recommendation.prompt}\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalDesignTokens.TextPrimary,
                        lineHeight = 19.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alur CBT:",
                            style = TextStyle(
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JournalDesignTokens.TextMuted,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        recommendation.sections.forEachIndexed { index, section ->
                            Text(
                                text = section,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = JournalDesignTokens.TextMuted,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                            if (index < recommendation.sections.lastIndex) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = JournalDesignTokens.TextMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartGuidedReflection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JournalDesignTokens.PrimaryCta,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Mulai Refleksi",
                        style = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedButton(
                    onClick = onStartFreeWrite,
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(JournalDesignTokens.PrimaryCta)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = JournalDesignTokens.PrimaryCta
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Tulis Bebas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MetricStatusPill(
    label: String,
    isElevated: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isElevated) JournalDesignTokens.WarningBadgeBg else JournalDesignTokens.SageBadgeBg
    val ink = if (isElevated) JournalDesignTokens.WarningBadgeInk else JournalDesignTokens.SageBadgeInk
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdaptivePromptBannerCardPreview() {
    HariKuTheme {
        AdaptivePromptBannerCard(
            recommendation = AdaptivePromptEngine.recommend(null, null),
            onStartGuidedReflection = {},
            onStartFreeWrite = {},
            onCyclePrompt = {},
            onOpenTemplateSheet = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

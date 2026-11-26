package com.hariku.feature.journal.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.R
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.JournalViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun JournalHomeSection(
    onStartGuidedJournal: () -> Unit,
    onOpenJournalTab: () -> Unit,
    onOpenEntryDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    JournalHomeCard(
        recommendation = state.recommendation,
        latestEntry = state.allEntries.firstOrNull(),
        totalCount = state.allEntries.size,
        onStartGuidedJournal = onStartGuidedJournal,
        onOpenJournalTab = onOpenJournalTab,
        onOpenEntryDetail = onOpenEntryDetail,
        modifier = modifier
    )
}

@Composable
fun JournalHomeCard(
    recommendation: JournalPromptRecommendation,
    latestEntry: JournalEntry?,
    totalCount: Int,
    onStartGuidedJournal: () -> Unit,
    onOpenJournalTab: () -> Unit,
    onOpenEntryDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = recommendation.metrics

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Refleksi harian",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.PrimaryCta
                    )
                }

                Text(
                    text = if (totalCount > 0) "Lihat semua ($totalCount)" else "Buka Jurnal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JournalDesignTokens.PrimaryCta,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenJournalTab)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                        text = recommendation.template.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalDesignTokens.TextPrimary
                    )
                    Text(
                        text = recommendation.reason,
                        fontSize = 12.sp,
                        color = JournalDesignTokens.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_cat_handing_book),
                    contentDescription = "Jurnal Terpandu",
                    modifier = Modifier.size(56.dp)
                )
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
                Text(
                    text = "\"${recommendation.prompt}\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalDesignTokens.TextPrimary,
                    lineHeight = 19.sp
                )
            }

            if (latestEntry != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(JournalDesignTokens.PeachSoft)
                        .clickable { onOpenEntryDetail(latestEntry.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Catatan terakhir · ${latestEntry.tag.label}",
                            fontSize = 11.sp,
                            color = JournalDesignTokens.PrimaryCta,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = latestEntry.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalDesignTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka detail jurnal",
                        tint = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Button(
                onClick = onStartGuidedJournal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JournalDesignTokens.PrimaryCta,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Mulai Refleksi Terpandu",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalHomeCardPreview() {
    HariKuTheme {
        JournalHomeCard(
            recommendation = AdaptivePromptEngine.recommend(null, null),
            latestEntry = null,
            totalCount = 0,
            onStartGuidedJournal = {},
            onOpenJournalTab = {},
            onOpenEntryDetail = {}
        )
    }
}

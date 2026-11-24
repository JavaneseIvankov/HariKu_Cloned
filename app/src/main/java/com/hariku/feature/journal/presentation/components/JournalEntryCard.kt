package com.hariku.feature.journal.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.presentation.JournalDesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID"))
    }
    val formattedDate = remember(entry.dateMillis) {
        Instant.ofEpochMilli(entry.dateMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }
    val categoryVisual = JournalCategoryCatalog.of(entry.tag)
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (entry.isPinned) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = JournalDesignTokens.PrimaryCta.copy(alpha = 0.6f),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = categoryVisual.iconRes),
                    contentDescription = entry.tag.label,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalDesignTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = JournalDesignTokens.TextMuted
                    )
                }
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (entry.isPinned) "Lepas sematan" else "Sematkan jurnal",
                        tint = if (entry.isPinned) {
                            JournalDesignTokens.PrimaryCta
                        } else {
                            JournalDesignTokens.TextMuted
                        },
                        modifier = Modifier.size(19.dp)
                    )
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Hapus jurnal",
                            tint = JournalDesignTokens.TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Text(
                text = entry.content,
                fontSize = 13.sp,
                color = JournalDesignTokens.TextSecondary,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(JournalDesignTokens.PeachHighlight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.tag.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalDesignTokens.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEDE9E7))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.templateType.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalDesignTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalEntryCardPreview() {
    HariKuTheme {
        JournalEntryCard(
            entry = JournalEntry(
                id = 1L,
                title = "Refleksi setelah presentasi",
                content = "Awalnya gugup, tapi setelah mempersiapkan poin utama semuanya berjalan lancar.",
                dateMillis = 1763971200000L,
                tag = JournalCategoryTag.AKADEMIK,
                templateType = CbtTemplateType.COGNITIVE_REFRAMING,
                isPinned = true
            ),
            onClick = {},
            onTogglePin = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

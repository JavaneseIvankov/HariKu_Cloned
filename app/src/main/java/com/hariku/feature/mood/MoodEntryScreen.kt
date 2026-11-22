package com.hariku.feature.mood

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val moodIcons = listOf(
    Mood.HAPPY to R.drawable.ic_emote_senang,
    Mood.NEUTRAL to R.drawable.ic_emote_biasa,
    Mood.SAD to R.drawable.ic_emote_sedih,
    Mood.ANGRY to R.drawable.ic_emote_marah,
    Mood.ANXIOUS to R.drawable.ic_emote_cemas,
    Mood.TIRED to R.drawable.ic_emote_lelah,
    Mood.DISAPPOINTED to R.drawable.ic_emote_kecewa,
    Mood.AFRAID to R.drawable.ic_emote_takut,
    Mood.EMPTY to R.drawable.ic_emote_hampa,
    Mood.EXCITED to R.drawable.ic_emote_semangat
)

@Composable
fun MoodEntryScreen(
    entries: List<MoodEntry>?,
    saving: Boolean,
    onSave: (Mood) -> Unit
) {
    val latestMood = entries?.firstOrNull()?.takeIf { entry ->
        Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() ==
            LocalDate.now()
    }?.moodType

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = latestMood?.let { "Mood Terakhir: $it" } ?: "Bagaimana Suasana Hatimu Hari Ini?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (latestMood == null) Color.Black else Color(0xFF71A77A)
            )
            Spacer(Modifier.height(16.dp))
            if (entries == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    moodIcons.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { (mood, icon) ->
                                MoodItem(
                                    mood = mood,
                                    isSelected = latestMood == mood.label,
                                    isDisabled = saving,
                                    iconRes = icon,
                                    onClick = { onSave(mood) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodItem(
    mood: Mood,
    isSelected: Boolean,
    isDisabled: Boolean,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (isDisabled && !isSelected) 0.4f else 1f)
            .clickable(enabled = !isDisabled, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF71a77a).copy(alpha = 0.2f), CircleShape)
                        .border(3.dp, Color(0xFF71a77a), CircleShape)
                )
            }
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = mood.label,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mood.label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF71a77a) else Color.Black
        )
    }
}

package com.hariku.feature_statistic.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.presentation.MoodVisualCatalog

private val emptyMoodBorderColor = Color(0xFF9F9F9F)

@Composable
fun CalendarDayCell(day: Int, mood: MoodType?) {
    val visual = mood?.let { MoodVisualCatalog.of(it) }
    val bgColor = visual?.accentColor ?: Color.Transparent
    val borderColor = visual?.accentColor ?: emptyMoodBorderColor
    val iconRes = visual?.iconRes ?: R.drawable.ic_cross

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(256.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 1.dp, width = 0.dp)
                .background(borderColor)
        )

        Text(
            text = day.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            modifier = Modifier.fillMaxWidth().background(Color.White),
            maxLines = 1
        )
    }
}

@Preview
@Composable
private fun CalendarDayCellPreview() {
    CalendarDayCell(
        day = 1,
        mood = MoodType.SENANG
    )
}

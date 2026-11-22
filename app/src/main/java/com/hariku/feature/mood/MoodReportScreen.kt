package com.hariku.feature.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hariku.feature_statistic.domain.model.MoodStatItem
import com.hariku.feature_statistic.domain.model.SentimentData
import com.hariku.feature_statistic.domain.model.WeeklySentiment
import com.hariku.feature_statistic.presentation.components.CalendarView
import com.hariku.feature_statistic.presentation.components.ChartBarCard
import com.hariku.feature_statistic.presentation.components.ChartPieCard
import com.hariku.feature_statistic.presentation.components.MoodOverallCard
import com.hariku.feature_statistic.presentation.components.StatisticsMood
import com.hariku.feature_statistic.presentation.components.WarningCard
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id-ID")

@Composable
fun MoodReportScreen(entries: List<MoodEntry>?) {
    if (entries == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var monthValue by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthValue)
    val report = remember(entries, month) { MoodReport.from(entries, month) }
    val monthly = report.monthly
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM", indonesian) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { WarningCard() }
        item {
            val dominant = monthly.counts.maxByOrNull { it.value }?.key
            val change = report.positiveChange
            val title = when {
                change == null -> "Ringkasan Mood"
                change > 0 -> "Sentimen Positif Meningkat!"
                change < 0 -> "Sentimen Positif Menurun"
                else -> "Sentimen Positif Tetap"
            }
            val summary = if (dominant == null) {
                "Belum ada catatan mood pada bulan ini."
            } else {
                val moodSummary = "Mood yang paling sering kamu catat bulan ini adalah ${dominant.label}."
                if (change == null || change == 0f) {
                    moodSummary
                } else {
                    val points = NumberFormat.getNumberInstance(indonesian).apply {
                        maximumFractionDigits = 1
                    }.format(kotlin.math.abs(change) * 100f)
                    "Sentimen positif ${if (change > 0) "naik" else "turun"} $points poin persentase " +
                        "dibanding bulan lalu. $moodSummary"
                }
            }
            MoodOverallCard(title, summary)
        }
        item {
            CalendarView(
                moodData = report.dailyMoods.mapValues { it.value.label },
                currentYear = month.year,
                currentMonth = month.monthValue - 1,
                onMonthChange = { year, zeroBasedMonth ->
                    monthValue = YearMonth.of(year, zeroBasedMonth + 1).toString()
                }
            )
        }
        item {
            ChartPieCard(SentimentData(
                negative = monthly.percent(Sentiment.NEGATIVE),
                positive = monthly.percent(Sentiment.POSITIVE),
                neutral = monthly.percent(Sentiment.NEUTRAL)
            ))
        }
        item {
            StatisticsMood(monthly.counts.entries.sortedByDescending { it.value }.map { (mood, count) ->
                MoodStatItem(mood.label, count, monthly.fraction(count) * 100f)
            })
        }
        item {
            ChartBarCard(report.weekly.filter { it.breakdown.total > 0 }.map { week ->
                WeeklySentiment(
                    weekLabel = month.atDay(week.days.first).format(dateFormat),
                    negative = week.breakdown.percent(Sentiment.NEGATIVE),
                    positive = week.breakdown.percent(Sentiment.POSITIVE),
                    neutral = week.breakdown.percent(Sentiment.NEUTRAL)
                )
            })
        }
    }
}

private fun MoodBreakdown.percent(sentiment: Sentiment): Float = fraction(count(sentiment)) * 100f

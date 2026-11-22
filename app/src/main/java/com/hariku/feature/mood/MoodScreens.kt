package com.hariku.feature.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id-ID")

@Composable
fun MoodEntryScreen(
    saving: Boolean,
    onSave: (Mood, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by rememberSaveable { mutableStateOf<Mood?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Bagaimana perasaanmu hari ini?", style = MaterialTheme.typography.headlineSmall) }
        item {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { mood ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = selected == mood,
                                onClick = { selected = mood },
                                enabled = !saving,
                                label = { Text(mood.label) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                enabled = selected != null && !saving,
                onClick = { selected?.let { mood -> onSave(mood) { selected = null } } }
            ) {
                Text(if (saving) "Menyimpan…" else "Simpan mood")
            }
        }
    }
}

@Composable
fun MoodReportScreen(entries: List<MoodEntry>?, modifier: Modifier = Modifier) {
    val currentMonth = YearMonth.now()
    var monthValue by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val month = YearMonth.parse(monthValue)
    val report = remember(entries, month) { entries?.let { MoodReport.from(it, month) } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Statistik Mood", style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    monthValue = month.minusMonths(1).toString()
                    selectedDay = null
                }) { Text("‹", Modifier.semantics { contentDescription = "Bulan sebelumnya" }) }
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", indonesian)))
                OutlinedButton(
                    enabled = month < currentMonth,
                    onClick = {
                        monthValue = month.plusMonths(1).toString()
                        selectedDay = null
                    }
                ) { Text("›", Modifier.semantics { contentDescription = "Bulan berikutnya" }) }
            }
        }
        if (report == null) {
            item { CircularProgressIndicator() }
        } else {
            item {
                ReportCard("Kalender mood") {
                    MoodCalendar(month, report.dailyMoods, selectedDay) { selectedDay = it }
                }
            }
            if (report.monthly.total == 0) {
                item { Text("Belum ada catatan mood pada bulan ini.") }
            } else {
                item {
                    ReportCard("Ringkasan bulanan") {
                        Text("${report.monthly.total} catatan mood")
                        val dominant = report.monthly.counts.maxBy { it.value }.key
                        Text("Mood terbanyak: ${dominant.label}")
                        report.positiveChange?.let { change ->
                            val points = NumberFormat.getNumberInstance(indonesian).apply {
                                maximumFractionDigits = 1
                            }.format(change * 100)
                            Text("Perubahan sentimen positif dari bulan lalu: $points poin persentase")
                        }
                    }
                }
                item {
                    ReportCard("Sentimen bulanan") {
                        SentimentBreakdown(report.monthly)
                    }
                }
                item {
                    ReportCard("Distribusi mood") {
                        report.monthly.counts.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
                            val fraction = report.monthly.fraction(count)
                            Text("${mood.label} · $count (${percent(fraction)})")
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = sentimentColor(mood.sentiment)
                            )
                        }
                    }
                }
                item {
                    ReportCard("Tren sentimen mingguan") {
                        report.weekly.forEach { week ->
                            Text("Tanggal ${week.days.first}–${week.days.last}", style = MaterialTheme.typography.titleSmall)
                            if (week.breakdown.total == 0) {
                                Text("Belum ada catatan")
                            } else {
                                SentimentBreakdown(week.breakdown, pie = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun MoodCalendar(
    month: YearMonth,
    moods: Map<Int, Mood>,
    selectedDay: Int?,
    onSelect: (Int) -> Unit
) {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian)
    val cells = (0 until offset + month.lengthOfMonth()).chunked(7)
    Row(Modifier.fillMaxWidth()) {
        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach { label ->
            Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
    }
    Column(Modifier.selectableGroup()) {
        cells.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = week.getOrNull(column)?.minus(offset)?.plus(1)
                    if (day == null || day < 1) {
                        Box(Modifier.weight(1f))
                    } else {
                        val mood = moods[day]
                        val date = month.atDay(day)
                        val label = "${date.format(dateFormat)}, ${mood?.label ?: "belum ada mood"}"
                        Box(
                            modifier = Modifier.weight(1f).height(44.dp)
                                .selectable(
                                    selected = selectedDay == day,
                                    enabled = date <= LocalDate.now(),
                                    role = Role.Button,
                                    onClick = { onSelect(day) }
                                )
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.toString(),
                                color = mood?.let { sentimentColor(it.sentiment) } ?: MaterialTheme.colorScheme.onSurface,
                                style = if (selectedDay == day) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    selectedDay?.let { day ->
        Text("${month.atDay(day).format(dateFormat)} · ${moods[day]?.label ?: "Belum ada mood"}")
    }
}

@Composable
private fun SentimentBreakdown(breakdown: MoodBreakdown, pie: Boolean = true) {
    val positive = sentimentColor(Sentiment.POSITIVE)
    val neutral = sentimentColor(Sentiment.NEUTRAL)
    val negative = sentimentColor(Sentiment.NEGATIVE)
    val colors = listOf(positive, neutral, negative)
    val fractions = Sentiment.entries.map { breakdown.fraction(breakdown.count(it)) }

    if (pie) {
        Canvas(Modifier.size(112.dp)) {
            var start = -90f
            fractions.forEachIndexed { index, fraction ->
                val sweep = fraction * 360f
                drawArc(colors[index], start, sweep, useCenter = true)
                start += sweep
            }
        }
    } else {
        Row(Modifier.fillMaxWidth().height(8.dp)) {
            fractions.forEachIndexed { index, fraction ->
                if (fraction > 0f) {
                    Canvas(Modifier.weight(fraction).height(8.dp)) { drawRect(colors[index]) }
                }
            }
        }
    }
    Sentiment.entries.forEachIndexed { index, sentiment ->
        Text("${sentiment.label} · ${percent(fractions[index])}", color = colors[index])
    }
}

@Composable
private fun sentimentColor(sentiment: Sentiment) = when (sentiment) {
    Sentiment.POSITIVE -> MaterialTheme.colorScheme.primary
    Sentiment.NEUTRAL -> MaterialTheme.colorScheme.tertiary
    Sentiment.NEGATIVE -> MaterialTheme.colorScheme.error
}

private fun percent(fraction: Float): String =
    NumberFormat.getPercentInstance(indonesian).apply { maximumFractionDigits = 1 }.format(fraction)

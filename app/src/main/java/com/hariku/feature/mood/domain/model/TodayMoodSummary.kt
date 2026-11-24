package com.hariku.feature.mood.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayMoodSummary(
    val latestEntry: MoodEntry?,
    val todayEntries: List<MoodEntry>
) {
    val todayCount: Int get() = todayEntries.size
}

object MoodTrackerLogic {
    const val MAX_NOTE_LENGTH = 140

    fun summarizeToday(
        entries: List<MoodEntry>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): TodayMoodSummary {
        val todayEntries = entries
            .filter { entry ->
                Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate() == today
            }
            .sortedWith(
                compareByDescending<MoodEntry> { it.timestamp }
                    .thenByDescending { it.id }
            )

        return TodayMoodSummary(
            latestEntry = todayEntries.firstOrNull(),
            todayEntries = todayEntries
        )
    }

    fun sanitizeNote(raw: String, maxLength: Int = MAX_NOTE_LENGTH): String =
        raw.trim().take(maxLength)
}

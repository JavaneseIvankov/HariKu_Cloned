package com.hariku.feature.mood.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MoodStreak(
    val currentDays: Int = 0,
    val checkedInToday: Boolean = false
)

enum class QuickContextTagId {
    KULIAH,
    TUGAS,
    KURANG_TIDUR,
    TEMAN,
    KELUARGA,
    ISTIRAHAT
}

data class QuickContextTag(
    val id: QuickContextTagId,
    val label: String
)

data class TodayMoodSummary(
    val latestEntry: MoodEntry?,
    val todayEntries: List<MoodEntry>,
    val streak: MoodStreak = MoodStreak()
) {
    val todayCount: Int get() = todayEntries.size
}

object MoodTrackerLogic {
    const val MAX_NOTE_LENGTH = 140

    val quickContextTags: List<QuickContextTag> = listOf(
        QuickContextTag(QuickContextTagId.KULIAH, "Kuliah"),
        QuickContextTag(QuickContextTagId.TUGAS, "Tugas"),
        QuickContextTag(QuickContextTagId.KURANG_TIDUR, "Kurang tidur"),
        QuickContextTag(QuickContextTagId.TEMAN, "Teman"),
        QuickContextTag(QuickContextTagId.KELUARGA, "Keluarga"),
        QuickContextTag(QuickContextTagId.ISTIRAHAT, "Istirahat")
    )

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
            todayEntries = todayEntries,
            streak = computeStreak(entries = entries, today = today, zone = zone)
        )
    }

    fun computeStreak(
        entries: List<MoodEntry>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): MoodStreak {
        if (entries.isEmpty()) return MoodStreak()

        val entryDates = entries
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .filter { !it.isAfter(today) }
            .toSet()

        if (entryDates.isEmpty()) return MoodStreak()

        val checkedInToday = today in entryDates
        val anchorDate = when {
            checkedInToday -> today
            today.minusDays(1) in entryDates -> today.minusDays(1)
            else -> return MoodStreak(currentDays = 0, checkedInToday = false)
        }

        var streakDays = 0
        var cursor = anchorDate
        while (cursor in entryDates) {
            streakDays += 1
            cursor = cursor.minusDays(1)
        }

        return MoodStreak(
            currentDays = streakDays,
            checkedInToday = checkedInToday
        )
    }

    fun hasContextTag(note: String, tagLabel: String): Boolean {
        val cleanTag = tagLabel.trim()
        if (cleanTag.isEmpty()) return false
        return splitNoteSegments(note).any { it.equals(cleanTag, ignoreCase = true) }
    }

    fun toggleContextTag(
        currentNote: String,
        tagLabel: String,
        maxLength: Int = MAX_NOTE_LENGTH
    ): String {
        val cleanTag = tagLabel.trim()
        if (cleanTag.isEmpty()) return currentNote.take(maxLength)

        val segments = splitNoteSegments(currentNote)
        if (segments.any { it.equals(cleanTag, ignoreCase = true) }) {
            return segments
                .filterNot { it.equals(cleanTag, ignoreCase = true) }
                .joinToString(", ")
                .take(maxLength)
        }

        val candidate = if (segments.isEmpty()) {
            cleanTag
        } else {
            (segments + cleanTag).joinToString(", ")
        }

        return if (candidate.length <= maxLength) {
            candidate
        } else if (segments.isEmpty()) {
            cleanTag.take(maxLength)
        } else {
            currentNote.take(maxLength)
        }
    }

    fun sanitizeNote(raw: String, maxLength: Int = MAX_NOTE_LENGTH): String =
        raw.trim().take(maxLength)

    private fun splitNoteSegments(note: String): List<String> =
        note.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

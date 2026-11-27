package com.hariku.feature.mood

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodSentiment
import com.hariku.feature.mood.domain.model.MoodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class MoodReportTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val month = YearMonth.of(2024, 2)

    @Test
    fun monthlyAndWeeklyTotalsUseLocalDates() {
        val report = MoodReport.from(
            listOf(
                entry("Marah", "2024-01-31T16:59:59Z"),
                entry("Senang", "2024-01-31T17:00:00Z"),
                entry("Biasa", "2024-02-07T17:00:00Z"),
                entry("Semangat", "2024-02-29T16:59:59Z"),
                entry("Sedih", "2024-02-29T17:00:00Z")
            ),
            month,
            jakarta
        )

        assertEquals(mapOf(1 to MoodType.SENANG, 8 to MoodType.BIASA, 29 to MoodType.SEMANGAT), report.dailyMoods)
        assertEquals(3, report.monthly.total)
        assertEquals(2, report.monthly.count(MoodSentiment.POSITIVE))
        assertEquals(1, report.monthly.count(MoodSentiment.NEUTRAL))
        assertEquals(0, report.monthly.count(MoodSentiment.NEGATIVE))
        assertEquals(listOf(1, 1, 0, 0, 1), report.weekly.map { it.breakdown.total })
        assertEquals(29..29, report.weekly.last().days)
    }

    @Test
    fun dailyMoodUsesFrequencyThenLatestEntryRegardlessOfInputOrder() {
        val report = MoodReport.from(
            listOf(
                entry("Sedih", "2024-02-01T12:00:00Z"),
                entry("Senang", "2024-02-01T10:00:00Z"),
                entry("Senang", "2024-02-01T11:00:00Z"),
                entry("Marah", "2024-02-02T10:00:00Z"),
                entry("Lelah", "2024-02-02T11:00:00Z"),
                entry("Biasa", "2024-02-03T10:00:00Z", id = 5),
                entry("Cemas", "2024-02-03T10:00:00Z", id = 4)
            ),
            month,
            jakarta
        )

        assertEquals(mapOf(1 to MoodType.SENANG, 2 to MoodType.LELAH, 3 to MoodType.BIASA), report.dailyMoods)
    }

    @Test
    fun comparisonUsesPercentagePointChangeFromPreviousMonth() {
        val report = MoodReport.from(
            listOf(
                entry("Senang", "2023-12-15T00:00:00Z"),
                entry("Sedih", "2023-12-20T00:00:00Z"),
                entry("Semangat", "2024-01-01T00:00:00Z")
            ),
            YearMonth.of(2024, 1),
            jakarta
        )

        assertEquals(0.5f, requireNotNull(report.positiveChange), 0.0001f)
    }

    @Test
    fun missingDataDoesNotProduceAComparison() {
        val moment = entry("Senang", "2024-02-01T00:00:00Z")
        val empty = MoodReport.from(emptyList(), month, jakarta)
        val currentOnly = MoodReport.from(listOf(moment), month, jakarta)
        val previousOnly = MoodReport.from(listOf(moment), month.plusMonths(1), jakarta)

        assertEquals(0, empty.monthly.total)
        assertEquals(0f, empty.monthly.fraction(0), 0f)
        assertNull(currentOnly.positiveChange)
        assertNull(previousOnly.positiveChange)
    }

    private fun entry(label: String, time: String, id: Long = 0) =
        MoodEntry(
            id = id,
            mood = requireNotNull(MoodType.fromLabel(label)),
            timestamp = Instant.parse(time).toEpochMilli()
        )
}

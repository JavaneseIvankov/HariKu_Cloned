package com.hariku.feature.mood

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodSentiment
import com.hariku.feature.mood.domain.model.MoodType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MoodBreakdown(val counts: Map<MoodType, Int>) {
    val total: Int get() = counts.values.sum()

    fun count(sentiment: MoodSentiment): Int =
        counts.entries.sumOf { (mood, count) -> if (mood.sentiment == sentiment) count else 0 }

    fun fraction(count: Int): Float = if (total == 0) 0f else count.toFloat() / total
}

data class MoodWeek(val days: IntRange, val breakdown: MoodBreakdown)

data class MoodReport(
    val monthly: MoodBreakdown,
    val dailyMoods: Map<Int, MoodType>,
    val weekly: List<MoodWeek>,
    val positiveChange: Float?
) {
    companion object {
        fun from(
            entries: List<MoodEntry>,
            month: YearMonth,
            zone: ZoneId = ZoneId.systemDefault()
        ): MoodReport {
            val previousMonth = month.minusMonths(1)
            val entriesByDay = mutableMapOf<Int, MutableList<MoodEntry>>()
            val previousMoods = mutableListOf<MoodType>()

            for (entry in entries) {
                val date = Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()
                when (YearMonth.from(date)) {
                    month -> entriesByDay.getOrPut(date.dayOfMonth) { mutableListOf() }.add(entry)
                    previousMonth -> previousMoods.add(entry.mood)
                }
            }

            val moodsByDay = entriesByDay.mapValues { (_, dayEntries) -> dayEntries.map { it.mood } }
            val monthly = breakdown(moodsByDay.values.flatten())
            val previous = breakdown(previousMoods)
            val weekly = (1..month.lengthOfMonth() step 7).map { start ->
                val days = start..minOf(start + 6, month.lengthOfMonth())
                MoodWeek(days, breakdown(days.flatMap { moodsByDay[it].orEmpty() }))
            }
            val positiveChange = if (monthly.total > 0 && previous.total > 0) {
                monthly.fraction(monthly.count(MoodSentiment.POSITIVE)) -
                    previous.fraction(previous.count(MoodSentiment.POSITIVE))
            } else {
                null
            }

            return MoodReport(
                monthly = monthly,
                dailyMoods = entriesByDay.mapValues { (_, dayEntries) ->
                    val counts = dayEntries.groupingBy { it.mood }.eachCount()
                    dayEntries.maxWith(
                        compareBy({ counts.getValue(it.mood) }, { it.timestamp }, { it.id })
                    ).mood
                },
                weekly = weekly,
                positiveChange = positiveChange
            )
        }

        private fun breakdown(moods: List<MoodType>) = MoodBreakdown(moods.groupingBy { it }.eachCount())
    }
}

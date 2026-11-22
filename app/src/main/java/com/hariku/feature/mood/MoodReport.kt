package com.hariku.feature.mood

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class Sentiment(val label: String) {
    POSITIVE("Positif"), NEUTRAL("Netral"), NEGATIVE("Negatif")
}

enum class Mood(val label: String, val sentiment: Sentiment) {
    HAPPY("Senang", Sentiment.POSITIVE),
    EXCITED("Semangat", Sentiment.POSITIVE),
    NEUTRAL("Biasa", Sentiment.NEUTRAL),
    SAD("Sedih", Sentiment.NEGATIVE),
    ANGRY("Marah", Sentiment.NEGATIVE),
    AFRAID("Takut", Sentiment.NEGATIVE),
    ANXIOUS("Cemas", Sentiment.NEGATIVE),
    DISAPPOINTED("Kecewa", Sentiment.NEGATIVE),
    TIRED("Lelah", Sentiment.NEGATIVE),
    EMPTY("Hampa", Sentiment.NEGATIVE)
}

data class MoodBreakdown(val counts: Map<Mood, Int>) {
    val total: Int get() = counts.values.sum()

    fun count(sentiment: Sentiment): Int =
        counts.entries.sumOf { (mood, count) -> if (mood.sentiment == sentiment) count else 0 }

    fun fraction(count: Int): Float = if (total == 0) 0f else count.toFloat() / total
}

data class MoodWeek(val days: IntRange, val breakdown: MoodBreakdown)

data class MoodReport(
    val monthly: MoodBreakdown,
    val dailyMoods: Map<Int, Mood>,
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
            val moodsByLabel = Mood.entries.associateBy { it.label }
            val moodsByDay = mutableMapOf<Int, MutableList<Mood>>()
            val previousMoods = mutableListOf<Mood>()

            // The DAO orders entries newest first, so ties favor the most recent mood.
            for (entry in entries) {
                val mood = moodsByLabel[entry.moodType] ?: continue
                val date = Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()
                when (YearMonth.from(date)) {
                    month -> moodsByDay.getOrPut(date.dayOfMonth) { mutableListOf() }.add(mood)
                    previousMonth -> previousMoods.add(mood)
                }
            }

            val monthly = breakdown(moodsByDay.values.flatten())
            val previous = breakdown(previousMoods)
            val weekly = (1..month.lengthOfMonth() step 7).map { start ->
                val days = start..minOf(start + 6, month.lengthOfMonth())
                MoodWeek(days, breakdown(days.flatMap { moodsByDay[it].orEmpty() }))
            }
            val positiveChange = if (monthly.total > 0 && previous.total > 0) {
                monthly.fraction(monthly.count(Sentiment.POSITIVE)) -
                    previous.fraction(previous.count(Sentiment.POSITIVE))
            } else {
                null
            }

            return MoodReport(
                monthly = monthly,
                dailyMoods = moodsByDay.mapValues { (_, moods) ->
                    moods.groupingBy { it }.eachCount().maxBy { it.value }.key
                },
                weekly = weekly,
                positiveChange = positiveChange
            )
        }

        private fun breakdown(moods: List<Mood>) = MoodBreakdown(moods.groupingBy { it }.eachCount())
    }
}

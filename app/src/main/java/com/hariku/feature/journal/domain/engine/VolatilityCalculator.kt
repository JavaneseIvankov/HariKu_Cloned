package com.hariku.feature.journal.domain.engine

import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import kotlin.math.abs

/** Menghitung 7-day SMA dan EVI dari riwayat mood. Pure Kotlin. */
object VolatilityCalculator {
    const val VOLATILE_THRESHOLD = 0.85
    const val WINDOW_DAYS = 7
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Konversi MoodType ke skor 1..5 (5 = sangat positif). */
    fun moodScore(mood: MoodType): Int = when (mood) {
        MoodType.SENANG, MoodType.SEMANGAT -> 5
        MoodType.BIASA -> 3
        MoodType.KECEWA, MoodType.LELAH, MoodType.HAMPA -> 2
        MoodType.SEDIH, MoodType.MARAH, MoodType.TAKUT, MoodType.CEMAS -> 1
    }

    /** SMA skor mood dalam 7 hari terakhir hingga entri terbaru; null bila kosong. */
    fun calculateSma7(
        entries: List<MoodEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double? {
        val from = nowMillis - WINDOW_DAYS * DAY_MILLIS
        val window = entries.filter { it.timestamp in (from + 1)..nowMillis }
        if (window.isEmpty()) return null
        return window.map { moodScore(it.mood) }.average()
    }

    /** EVI = |skor mood terbaru dalam 7 hari terakhir - SMA7|. Tanpa data = 0.0. */
    fun calculateEvi(
        entries: List<MoodEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {
        val from = nowMillis - WINDOW_DAYS * DAY_MILLIS
        val latestEntry = entries
            .filter { it.timestamp in (from + 1)..nowMillis }
            .maxByOrNull { it.timestamp }
            ?: return 0.0
        val sma = calculateSma7(entries, nowMillis) ?: return 0.0
        return abs(moodScore(latestEntry.mood) - sma)
    }

    fun classify(evi: Double): EviLevel =
        if (evi > VOLATILE_THRESHOLD) EviLevel.VOLATIL else EviLevel.STABIL
}

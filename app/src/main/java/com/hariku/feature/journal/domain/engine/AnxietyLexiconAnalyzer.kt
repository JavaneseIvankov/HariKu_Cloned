package com.hariku.feature.journal.domain.engine

import com.hariku.feature.journal.domain.model.AdsLevel

/** Menghitung Anxiety Density Score (ADS) dari rasio kata kunci cemas. Pure Kotlin. */
object AnxietyLexiconAnalyzer {
    const val LOW_THRESHOLD = 0.15
    const val HIGH_THRESHOLD = 0.35
    const val FULL_WEIGHT_MAX_AGE_MILLIS = 48L * 60 * 60 * 1000
    const val DECAY_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    data class TimedMessage(
        val text: String,
        val timestampMillis: Long
    )

    val lexicon: Set<String> = setOf(
        // Indonesia
        "cemas", "khawatir", "takut", "gelisah", "panik", "gugup", "resah", "was-was", "waswas",
        "tegang", "stres", "stress", "overthinking", "tertekan", "cemasnya", "bingung", "ngeri",
        "deg-degan", "degdegan", "putus", "asa", "pesimis", "terancam", "insomnia", "sesak",
        // English
        "anxious", "anxiety", "worried", "worry", "afraid", "fear", "scared", "panic", "nervous",
        "stressed", "overwhelmed", "uneasy", "dread", "tense", "restless"
    )

    private val tokenRegex = Regex("[\\p{L}\\p{N}'-]+")

    private fun tokenize(text: String): List<String> =
        tokenRegex.findAll(text.lowercase()).map { it.value.trim('-', '\'') }.filter { it.isNotEmpty() }.toList()

    /** ADS satu teks: kata cemas / total kata, di-clamp 0.0..1.0. */
    fun analyze(text: String): Double {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return 0.0
        val hits = tokens.count { it in lexicon }
        return (hits.toDouble() / tokens.size).coerceIn(0.0, 1.0)
    }

    /** ADS pesan pengguna terbaru (urutan list = kronologis, terakhir = terbaru). */
    fun analyzeLatest(messages: List<String>): Double {
        val latest = messages.lastOrNull { it.isNotBlank() } ?: return 0.0
        return analyze(latest)
    }

    /**
     * ADS pesan terbaru dengan time-decay:
     * <= 48 jam = bobot 100%, > 48 jam s.d. 7 hari = bobot 50%, > 7 hari = 0.0.
     */
    fun analyzeLatestWithDecay(
        messages: List<TimedMessage>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {
        val latest = messages.lastOrNull { it.text.isNotBlank() } ?: return 0.0
        val rawAds = analyze(latest.text)
        if (latest.timestampMillis <= 0L) return rawAds
        val ageMillis = (nowMillis - latest.timestampMillis).coerceAtLeast(0L)
        val multiplier = when {
            ageMillis <= FULL_WEIGHT_MAX_AGE_MILLIS -> 1.0
            ageMillis <= DECAY_MAX_AGE_MILLIS -> 0.5
            else -> 0.0
        }
        return (rawAds * multiplier).coerceIn(0.0, 1.0)
    }

    fun classify(ads: Double): AdsLevel = when {
        ads > HIGH_THRESHOLD -> AdsLevel.TINGGI
        ads < LOW_THRESHOLD -> AdsLevel.RENDAH
        else -> AdsLevel.SEDANG
    }
}

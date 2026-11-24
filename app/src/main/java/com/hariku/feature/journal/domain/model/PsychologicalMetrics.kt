package com.hariku.feature.journal.domain.model

enum class AdsLevel(val label: String) { RENDAH("Rendah"), SEDANG("Sedang"), TINGGI("Tinggi") }

enum class EviLevel(val label: String) { STABIL("Stabil"), VOLATIL("Volatil") }

/**
 * Parameter psikologis pengguna.
 * [eviScore]: deviasi mood terbaru terhadap 7-day SMA (skala mood 1..5).
 * [adsScore]: Anxiety Density Score 0.0..1.0.
 */
data class PsychologicalMetrics(
    val eviScore: Double = 0.0,
    val eviLevel: EviLevel = EviLevel.STABIL,
    val adsScore: Double = 0.0,
    val adsLevel: AdsLevel = AdsLevel.RENDAH,
    val isDefaultFallback: Boolean = false
) {
    companion object {
        /** Default netral untuk pengguna baru (data belum tersedia). */
        val NEUTRAL = PsychologicalMetrics(isDefaultFallback = true)
    }
}

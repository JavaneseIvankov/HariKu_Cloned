package com.hariku.feature.mood.domain.model

enum class MoodSentiment(val label: String) {
    POSITIVE("Positif"),
    NEUTRAL("Netral"),
    NEGATIVE("Negatif")
}

enum class MoodType(
    val label: String,
    val sentiment: MoodSentiment
) {
    SENANG("Senang", MoodSentiment.POSITIVE),
    SEMANGAT("Semangat", MoodSentiment.POSITIVE),
    BIASA("Biasa", MoodSentiment.NEUTRAL),
    SEDIH("Sedih", MoodSentiment.NEGATIVE),
    MARAH("Marah", MoodSentiment.NEGATIVE),
    TAKUT("Takut", MoodSentiment.NEGATIVE),
    CEMAS("Cemas", MoodSentiment.NEGATIVE),
    KECEWA("Kecewa", MoodSentiment.NEGATIVE),
    LELAH("Lelah", MoodSentiment.NEGATIVE),
    HAMPA("Hampa", MoodSentiment.NEGATIVE);

    companion object {
        private val byLabel = entries.associateBy { it.label.lowercase() }

        fun fromLabel(label: String): MoodType? = byLabel[label.trim().lowercase()]
    }
}

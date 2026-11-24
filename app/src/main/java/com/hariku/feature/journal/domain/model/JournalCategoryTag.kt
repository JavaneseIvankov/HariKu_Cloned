package com.hariku.feature.journal.domain.model

enum class JournalCategoryTag(val label: String) {
    AKADEMIK("Akademik"),
    KECEMASAN("Kecemasan"),
    STRES("Stres"),
    TIDUR("Tidur"),
    HUBUNGAN("Hubungan"),
    PRODUKTIVITAS("Produktivitas"),
    POSITIVITAS("Positivitas"),
    UMUM("Umum");

    companion object {
        fun fromLabel(label: String?): JournalCategoryTag? =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }

        fun fromNameOrDefault(name: String?): JournalCategoryTag =
            entries.firstOrNull { it.name == name } ?: UMUM
    }
}

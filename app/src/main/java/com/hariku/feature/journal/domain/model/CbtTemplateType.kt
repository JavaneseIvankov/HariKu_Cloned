package com.hariku.feature.journal.domain.model

/** Empat template CBT hasil pemetaan rule-based (Flowchart 2.2.3). */
enum class CbtTemplateType(
    val title: String,
    val description: String,
    val sections: List<String>
) {
    COGNITIVE_REFRAMING(
        title = "Cognitive Reframing",
        description = "Menantang pikiran cemas berlebihan dengan bukti nyata dan sudut pandang seimbang.",
        sections = listOf("Pikiran yang muncul", "Bukti nyata", "Sudut pandang lebih seimbang")
    ),
    BEHAVIORAL_ACTIVATION(
        title = "Behavioral Activation & Grounding",
        description = "Menstabilkan fluktuasi emosi lewat satu aksi kecil yang konkret.",
        sections = listOf("Kondisi saat ini", "Pemicu perubahan mood", "1 langkah kecil hari ini")
    ),
    THOUGHT_RECORD(
        title = "Thought Record",
        description = "Mencatat situasi, emosi, dan pikiran otomatis secara objektif.",
        sections = listOf("Situasi", "Emosi yang dirasakan", "Pikiran otomatis", "Respon alternatif")
    ),
    GENERAL_REFLECTION(
        title = "Refleksi Umum",
        description = "Refleksi harian dan apresiasi diri.",
        sections = listOf("Hal yang terjadi hari ini", "Perasaan dominan", "Satu hal yang disyukuri / dipelajari")
    );

    companion object {
        fun fromNameOrDefault(name: String?): CbtTemplateType =
            entries.firstOrNull { it.name == name } ?: GENERAL_REFLECTION
    }
}

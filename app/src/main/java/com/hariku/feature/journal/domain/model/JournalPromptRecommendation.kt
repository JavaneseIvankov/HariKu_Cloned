package com.hariku.feature.journal.domain.model

data class JournalPromptRecommendation(
    val template: CbtTemplateType,
    val prompt: String,
    val sections: List<String>,
    val reason: String,
    val metrics: PsychologicalMetrics,
    val contextTag: String
)

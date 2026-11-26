package com.hariku.feature.journal.domain.model

/** State jurnal akhir (template, pertanyaan, entri) untuk dikonsumsi Mood Analytics & Reporting. */
data class JournalStateSnapshot(
    val template: CbtTemplateType,
    val prompt: String,
    val entry: JournalEntry,
    val metrics: PsychologicalMetrics = PsychologicalMetrics.NEUTRAL
)

package com.hariku.feature.journal.domain.engine

import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.domain.model.PsychologicalMetrics

/** Dynamic CBT Adaptive Prompting Engine (Flowchart 2.2.3). Pure Kotlin & deterministik. */
object AdaptivePromptEngine {
    const val CONTEXT_SLOT = "{contextTag}"
    const val CONTEXT_FALLBACK = "aktivitasmu belakangan ini"

    private val questionBank: Map<CbtTemplateType, List<String>> = mapOf(
        CbtTemplateType.COGNITIVE_REFRAMING to listOf(
            "Terkait topik {contextTag} yang terakhir kamu catat, pikiran apa yang paling membebanimu saat ini?",
            "Adakah bukti nyata yang mendukung atau menentang kekhawatiranmu tentang {contextTag}?",
            "Bagaimana sudut pandang yang lebih seimbang tentang {contextTag}?"
        ),
        CbtTemplateType.BEHAVIORAL_ACTIVATION to listOf(
            "Mood-mu terasa naik turun. Apa yang berubah saat {contextTag}, dan satu langkah kecil apa yang bisa kamu lakukan hari ini?",
            "Sebutkan 3 hal di sekitarmu sekarang, lalu tuliskan satu aktivitas kecil yang menenangkan terkait {contextTag}.",
            "Apa pemicu perubahan perasaanmu belakangan ini saat {contextTag}?"
        ),
        CbtTemplateType.THOUGHT_RECORD to listOf(
            "Ceritakan situasi saat {contextTag}: apa yang kamu rasakan dan pikiran otomatis apa yang muncul?",
            "Tuliskan satu pikiran otomatis tentang {contextTag} dan respon alternatif yang lebih objektif.",
            "Emosi apa yang paling kuat saat {contextTag}, dan apa penyebabnya?"
        ),
        CbtTemplateType.GENERAL_REFLECTION to listOf(
            "Bagaimana harimu terkait {contextTag}? Apa yang paling kamu syukuri atau pelajari?",
            "Perasaan apa yang paling dominan hari ini saat {contextTag}?",
            "Satu hal baik apa yang terjadi hari ini, dan apa yang kamu pelajari dari {contextTag}?"
        )
    )

    fun selectTemplate(metrics: PsychologicalMetrics): CbtTemplateType = when {
        metrics.adsLevel == AdsLevel.TINGGI -> CbtTemplateType.COGNITIVE_REFRAMING
        metrics.eviLevel == EviLevel.VOLATIL -> CbtTemplateType.BEHAVIORAL_ACTIVATION
        metrics.adsLevel == AdsLevel.SEDANG -> CbtTemplateType.THOUGHT_RECORD
        else -> CbtTemplateType.GENERAL_REFLECTION
    }

    fun questionsFor(template: CbtTemplateType): List<String> = questionBank.getValue(template)

    /** Isi slot {contextTag}; fallback ramah bila tag kosong / belum ada entri. */
    fun injectContext(question: String, contextTag: String?): String =
        question.replace(CONTEXT_SLOT, contextTag?.trim()?.takeIf { it.isNotEmpty() } ?: CONTEXT_FALLBACK)

    fun resolveContextTag(latestTag: JournalCategoryTag?): String? =
        latestTag?.label?.lowercase()

    /**
     * @param metrics null = data tidak tersedia -> default netral.
     * @param variantIndex memilih variasi pertanyaan dari bank (di-modulo).
     */
    fun recommend(
        metrics: PsychologicalMetrics?,
        latestTag: JournalCategoryTag?,
        variantIndex: Int = 0
    ): JournalPromptRecommendation {
        val effective = metrics ?: PsychologicalMetrics.NEUTRAL
        val template = selectTemplate(effective)
        val bank = questionsFor(template)
        val tag = resolveContextTag(latestTag)
        val prompt = injectContext(bank[Math.floorMod(variantIndex, bank.size)], tag)
        return JournalPromptRecommendation(
            template = template,
            prompt = prompt,
            sections = template.sections,
            reason = reasonFor(template, effective),
            metrics = effective,
            contextTag = tag ?: CONTEXT_FALLBACK
        )
    }

    private fun reasonFor(template: CbtTemplateType, m: PsychologicalMetrics): String = when (template) {
        CbtTemplateType.COGNITIVE_REFRAMING -> "Kecemasan pada percakapanmu terdeteksi tinggi."
        CbtTemplateType.BEHAVIORAL_ACTIVATION -> "Suasana hatimu terdeteksi berfluktuasi belakangan ini."
        CbtTemplateType.THOUGHT_RECORD -> "Kecemasan pada percakapanmu terdeteksi sedang."
        CbtTemplateType.GENERAL_REFLECTION ->
            if (m.isDefaultFallback) "Belum ada data cukup, mulai dengan refleksi umum."
            else "Kondisimu stabil, saatnya refleksi harian."
    }
}

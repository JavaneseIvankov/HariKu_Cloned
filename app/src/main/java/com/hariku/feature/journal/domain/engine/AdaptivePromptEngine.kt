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

    enum class EmotionValence { POSITIVE, NEUTRAL, HEAVY }
    enum class PromptTone { POSITIVE, MIXED, SUPPORTIVE }

    private data class FeelingProfile(
        val valence: EmotionValence,
        val weights: Map<CbtTemplateType, Int>,
        val suggestedTag: JournalCategoryTag
    )

    data class GuidedPromptResult(
        val template: CbtTemplateType,
        val tone: PromptTone,
        val prompts: List<String>,
        val reason: String,
        val contextPhrase: String,
        val suggestedCategory: JournalCategoryTag?
    )

    private val clinicalPriority = listOf(
        CbtTemplateType.COGNITIVE_REFRAMING,
        CbtTemplateType.BEHAVIORAL_ACTIVATION,
        CbtTemplateType.THOUGHT_RECORD,
        CbtTemplateType.GENERAL_REFLECTION
    )

    private val feelingProfiles: Map<String, FeelingProfile> = mapOf(
        "Senang" to FeelingProfile(
            valence = EmotionValence.POSITIVE,
            weights = mapOf(CbtTemplateType.GENERAL_REFLECTION to 2),
            suggestedTag = JournalCategoryTag.POSITIVITAS
        ),
        "Damai" to FeelingProfile(
            valence = EmotionValence.POSITIVE,
            weights = mapOf(CbtTemplateType.GENERAL_REFLECTION to 2),
            suggestedTag = JournalCategoryTag.POSITIVITAS
        ),
        "Termotivasi" to FeelingProfile(
            valence = EmotionValence.POSITIVE,
            weights = mapOf(CbtTemplateType.GENERAL_REFLECTION to 2),
            suggestedTag = JournalCategoryTag.PRODUKTIVITAS
        ),
        "Ingin Fokus" to FeelingProfile(
            valence = EmotionValence.POSITIVE,
            weights = mapOf(CbtTemplateType.GENERAL_REFLECTION to 2),
            suggestedTag = JournalCategoryTag.PRODUKTIVITAS
        ),
        "Bosan" to FeelingProfile(
            valence = EmotionValence.NEUTRAL,
            weights = mapOf(
                CbtTemplateType.BEHAVIORAL_ACTIVATION to 2,
                CbtTemplateType.GENERAL_REFLECTION to 1
            ),
            suggestedTag = JournalCategoryTag.PRODUKTIVITAS
        ),
        "Cemas" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(
                CbtTemplateType.COGNITIVE_REFRAMING to 3,
                CbtTemplateType.THOUGHT_RECORD to 1
            ),
            suggestedTag = JournalCategoryTag.KECEMASAN
        ),
        "Takut" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(
                CbtTemplateType.COGNITIVE_REFRAMING to 3,
                CbtTemplateType.THOUGHT_RECORD to 1
            ),
            suggestedTag = JournalCategoryTag.KECEMASAN
        ),
        "Stress" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(
                CbtTemplateType.THOUGHT_RECORD to 2,
                CbtTemplateType.COGNITIVE_REFRAMING to 2
            ),
            suggestedTag = JournalCategoryTag.STRES
        ),
        "Marah" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(
                CbtTemplateType.BEHAVIORAL_ACTIVATION to 3,
                CbtTemplateType.THOUGHT_RECORD to 1
            ),
            suggestedTag = JournalCategoryTag.STRES
        ),
        "Muak" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(
                CbtTemplateType.BEHAVIORAL_ACTIVATION to 3,
                CbtTemplateType.THOUGHT_RECORD to 1
            ),
            suggestedTag = JournalCategoryTag.STRES
        ),
        "Sedih" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(CbtTemplateType.BEHAVIORAL_ACTIVATION to 3),
            suggestedTag = JournalCategoryTag.HUBUNGAN
        ),
        "Kesepian" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(CbtTemplateType.BEHAVIORAL_ACTIVATION to 3),
            suggestedTag = JournalCategoryTag.HUBUNGAN
        ),
        "Lelah" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(CbtTemplateType.BEHAVIORAL_ACTIVATION to 3),
            suggestedTag = JournalCategoryTag.TIDUR
        ),
        "Kecewa" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(CbtTemplateType.THOUGHT_RECORD to 3),
            suggestedTag = JournalCategoryTag.HUBUNGAN
        ),
        "Malu" to FeelingProfile(
            valence = EmotionValence.HEAVY,
            weights = mapOf(CbtTemplateType.THOUGHT_RECORD to 3),
            suggestedTag = JournalCategoryTag.HUBUNGAN
        )
    )

    private val questionBank: Map<CbtTemplateType, List<String>> = mapOf(
        CbtTemplateType.COGNITIVE_REFRAMING to listOf(
            "Terkait {contextTag}, pikiran apa yang paling membebanimu saat ini?",
            "Adakah bukti nyata yang mendukung atau justru mematahkan kekhawatiranmu?",
            "Jika sahabatmu mengalami hal yang sama, sudut pandang lebih seimbang apa yang akan kamu katakan padanya?"
        ),
        CbtTemplateType.BEHAVIORAL_ACTIVATION to listOf(
            "Di tengah {contextTag}, satu langkah kecil yang realistis dan menenangkan apa yang bisa kamu lakukan hari ini?",
            "Sebutkan 3 hal di sekitarmu sekarang yang membuatmu merasa lebih aman dan membumi.",
            "Aktivitas ringan apa yang biasanya paling cepat membantu memulihkan energimu?"
        ),
        CbtTemplateType.THOUGHT_RECORD to listOf(
            "Ceritakan situasi seputar {contextTag}: apa yang memicunya dan pikiran otomatis apa yang muncul?",
            "Emosi apa yang paling terasa saat ini, dan respon alternatif apa yang lebih objektif?",
            "Bagian mana dari situasi ini yang masih berada dalam kendalimu hari ini?"
        ),
        CbtTemplateType.GENERAL_REFLECTION to listOf(
            "Bagaimana harimu terkait {contextTag}? Hal apa yang paling kamu syukuri atau pelajari?",
            "Momen kecil apa hari ini yang membuatmu tersenyum atau merasa lega?",
            "Satu hal baik apa yang ingin kamu apresiasi dari dirimu sendiri hari ini?"
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

    /** Pemetaan frasa kontekstual natural Bahasa Indonesia per kategori jurnal. */
    fun resolveContextTag(latestTag: JournalCategoryTag?): String? = when (latestTag) {
        JournalCategoryTag.AKADEMIK -> "urusan kuliah atau tugasmu"
        JournalCategoryTag.KECEMASAN -> "rasa cemas yang sempat muncul"
        JournalCategoryTag.STRES -> "tekanan yang sedang kamu hadapi"
        JournalCategoryTag.TIDUR -> "kualitas tidur dan istirahatmu"
        JournalCategoryTag.HUBUNGAN -> "interaksimu dengan orang terdekat"
        JournalCategoryTag.PRODUKTIVITAS -> "fokus dan produktivitasmu"
        JournalCategoryTag.POSITIVITAS -> "hal-hal baik yang kamu syukuri"
        JournalCategoryTag.UMUM, null -> null
    }

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

    /**
     * Resolusi gabungan antara metrik historis (EVI & ADS), tag jurnal terakhir,
     * serta check-in perasaan real-time Step 1 memakai Weighted CBT Scoring.
     */
    fun recommendWithCheckIn(
        metrics: PsychologicalMetrics?,
        latestTag: JournalCategoryTag?,
        selectedFeelings: Set<String>,
        customFeelingNote: String = "",
        templateOverride: CbtTemplateType? = null
    ): GuidedPromptResult {
        val effective = metrics ?: PsychologicalMetrics.NEUTRAL
        val profiles = selectedFeelings.mapNotNull { feelingProfiles[it] }

        val positiveFeelings = selectedFeelings.filter { feelingProfiles[it]?.valence == EmotionValence.POSITIVE }
        val heavyFeelings = selectedFeelings.filter { feelingProfiles[it]?.valence == EmotionValence.HEAVY }

        val tone = when {
            heavyFeelings.isEmpty() && positiveFeelings.isNotEmpty() -> PromptTone.POSITIVE
            positiveFeelings.isNotEmpty() && heavyFeelings.isNotEmpty() -> PromptTone.MIXED
            else -> PromptTone.SUPPORTIVE
        }

        val template = templateOverride ?: resolveWeightedTemplate(
            metrics = effective,
            profiles = profiles,
            tone = tone
        )

        val trimmedNote = customFeelingNote.trim()
        val isNegativeHistoryTag = latestTag in setOf(
            JournalCategoryTag.KECEMASAN,
            JournalCategoryTag.STRES
        )
        val contextPhrase = when {
            trimmedNote.isNotEmpty() -> trimmedNote
            tone == PromptTone.POSITIVE && isNegativeHistoryTag -> "hal-hal baik hari ini"
            else -> resolveContextTag(latestTag) ?: CONTEXT_FALLBACK
        }

        val prompts = buildGuidedPrompts(
            template = template,
            tone = tone,
            metrics = effective,
            selectedFeelings = selectedFeelings,
            positiveFeelings = positiveFeelings,
            heavyFeelings = heavyFeelings,
            customNote = trimmedNote,
            contextPhrase = contextPhrase
        )

        val suggestedCategory = resolveSuggestedCategory(profiles)
        val reason = buildCombinedReason(
            template = template,
            metrics = effective,
            tone = tone,
            selectedFeelings = selectedFeelings,
            contextPhrase = contextPhrase
        )

        return GuidedPromptResult(
            template = template,
            tone = tone,
            prompts = prompts,
            reason = reason,
            contextPhrase = contextPhrase,
            suggestedCategory = suggestedCategory
        )
    }

    private fun resolveWeightedTemplate(
        metrics: PsychologicalMetrics,
        profiles: List<FeelingProfile>,
        tone: PromptTone
    ): CbtTemplateType {
        val scores = CbtTemplateType.entries.associateWith { 0 }.toMutableMap()

        // Bobot dasar dari metrik historis (EVI & ADS).
        // Bila user check-in Murni Positif di Step 1, tekanan historis diturunkan agar tidak memaksa prompt cemas.
        val historicalSoftened = tone == PromptTone.POSITIVE
        when {
            metrics.adsLevel == AdsLevel.TINGGI ->
                scores[CbtTemplateType.COGNITIVE_REFRAMING] = (scores[CbtTemplateType.COGNITIVE_REFRAMING] ?: 0) +
                    if (historicalSoftened) 1 else 4
            metrics.eviLevel == EviLevel.VOLATIL ->
                scores[CbtTemplateType.BEHAVIORAL_ACTIVATION] = (scores[CbtTemplateType.BEHAVIORAL_ACTIVATION] ?: 0) +
                    if (historicalSoftened) 1 else 3
            metrics.adsLevel == AdsLevel.SEDANG ->
                scores[CbtTemplateType.THOUGHT_RECORD] = (scores[CbtTemplateType.THOUGHT_RECORD] ?: 0) +
                    if (historicalSoftened) 1 else 2
            else ->
                scores[CbtTemplateType.GENERAL_REFLECTION] = (scores[CbtTemplateType.GENERAL_REFLECTION] ?: 0) + 1
        }

        // Bobot kontribusi dari setiap chip perasaan Step 1.
        profiles.forEach { profile ->
            profile.weights.forEach { (type, weight) ->
                scores[type] = (scores[type] ?: 0) + weight
            }
        }

        val maxScore = scores.values.maxOrNull() ?: 0
        return clinicalPriority.firstOrNull { scores[it] == maxScore } ?: CbtTemplateType.GENERAL_REFLECTION
    }

    private fun resolveSuggestedCategory(profiles: List<FeelingProfile>): JournalCategoryTag? {
        if (profiles.isEmpty()) return null
        val counts = profiles.groupingBy { it.suggestedTag }.eachCount()
        val maxCount = counts.values.maxOrNull() ?: return null
        val priority = listOf(
            JournalCategoryTag.KECEMASAN,
            JournalCategoryTag.STRES,
            JournalCategoryTag.TIDUR,
            JournalCategoryTag.HUBUNGAN,
            JournalCategoryTag.PRODUKTIVITAS,
            JournalCategoryTag.POSITIVITAS
        )
        return priority.firstOrNull { counts[it] == maxCount } ?: counts.keys.firstOrNull()
    }

    private fun buildGuidedPrompts(
        template: CbtTemplateType,
        tone: PromptTone,
        metrics: PsychologicalMetrics,
        selectedFeelings: Set<String>,
        positiveFeelings: List<String>,
        heavyFeelings: List<String>,
        customNote: String,
        contextPhrase: String
    ): List<String> {
        val bank = questionsFor(template).map { injectContext(it, contextPhrase) }
        val hasStep1Input = selectedFeelings.isNotEmpty() || customNote.isNotEmpty()
        if (!hasStep1Input) return bank

        val hasElevatedHistory = metrics.eviLevel == EviLevel.VOLATIL || metrics.adsLevel != AdsLevel.RENDAH
        val leadPrompt = when (tone) {
            PromptTone.POSITIVE -> {
                val positiveSummary = positiveFeelings.take(3).joinToString(", ") { it.lowercase() }
                when {
                    customNote.isNotEmpty() ->
                        "Kamu merasa $positiveSummary dan menuliskan tentang \"$customNote\". Hal apa dari momen ini yang paling ingin kamu syukuri atau pertahankan?"
                    hasElevatedHistory ->
                        "Belakangan ini kondisimu sempat naik turun, tapi hari ini kamu merasa $positiveSummary. Apa yang membantumu merasa lebih baik?"
                    else ->
                        "Senang melihatmu merasa $positiveSummary hari ini! Apa hal terbaik yang terjadi dan bagaimana kamu ingin menjaga energi positif ini?"
                }
            }
            PromptTone.MIXED -> {
                val posPart = positiveFeelings.take(2).joinToString(", ") { it.lowercase() }
                val heavyPart = heavyFeelings.take(2).joinToString(", ") { it.lowercase() }
                if (customNote.isNotEmpty()) {
                    "Di satu sisi kamu merasa $posPart, namun ada rasa $heavyPart terkait \"$customNote\". Apa langkah paling realistis untuk merawat dirimu sekarang?"
                } else {
                    "Di satu sisi kamu merasa $posPart, namun ada rasa $heavyPart yang muncul. Pikiran atau situasi apa yang paling memengaruhi keseimbanganmu hari ini?"
                }
            }
            PromptTone.SUPPORTIVE -> {
                val feelingsSummary = selectedFeelings.take(3).joinToString(", ") { it.lowercase() }
                val focus = if (customNote.isNotEmpty()) "\"$customNote\"" else contextPhrase
                when (template) {
                    CbtTemplateType.COGNITIVE_REFRAMING ->
                        "Saat merasa $feelingsSummary seputar $focus, pikiran otomatis apa yang paling kuat muncul, dan bagaimana fakta sebenarnya?"
                    CbtTemplateType.BEHAVIORAL_ACTIVATION ->
                        "Kamu sedang merasa $feelingsSummary terkait $focus. Satu aksi kecil apa yang bisa memberi sedikit rasa lega hari ini?"
                    CbtTemplateType.THOUGHT_RECORD ->
                        "Kamu mencatat rasa $feelingsSummary tentang $focus. Situasi apa yang memicunya dan sudut pandang alternatif apa yang lebih adil bagimu?"
                    CbtTemplateType.GENERAL_REFLECTION ->
                        "Kamu merasa $feelingsSummary hari ini terkait $focus. Apa yang paling kamu butuhkan untuk dirimu saat ini?"
                }
            }
        }

        val secondaryBank = if (tone == PromptTone.POSITIVE && template != CbtTemplateType.GENERAL_REFLECTION) {
            questionsFor(CbtTemplateType.GENERAL_REFLECTION).map { injectContext(it, contextPhrase) }
        } else {
            bank
        }

        return (listOf(leadPrompt) + secondaryBank).distinct().take(4)
    }

    private fun buildCombinedReason(
        template: CbtTemplateType,
        metrics: PsychologicalMetrics,
        tone: PromptTone,
        selectedFeelings: Set<String>,
        contextPhrase: String
    ): String {
        val baseReason = reasonFor(template, metrics)
        if (selectedFeelings.isEmpty()) {
            return "$baseReason Fokus konteks: $contextPhrase."
        }
        val feelingText = selectedFeelings.joinToString(", ")
        return when (tone) {
            PromptTone.POSITIVE ->
                "Kamu merasa $feelingText hari ini, sehingga pertanyaan difokuskan pada apresiasi diri & menjaga energi positif."
            PromptTone.MIXED ->
                "$baseReason Menyeimbangkan perasaanmu ($feelingText) lewat template ${template.title}."
            PromptTone.SUPPORTIVE ->
                "$baseReason Disesuaikan dengan perasaanmu ($feelingText) pada konteks $contextPhrase."
        }
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

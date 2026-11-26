package com.hariku.feature.journal.domain.usecase

import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.model.JournalPromptRecommendation
import com.hariku.feature.journal.domain.provider.PsychologicalContextProvider
import com.hariku.feature.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Menggabungkan metrik psikologis + tag entri terakhir -> rekomendasi prompt adaptif. */
class GetAdaptivePromptUseCase(
    private val contextProvider: PsychologicalContextProvider,
    private val journalRepository: JournalRepository
) {
    operator fun invoke(variantIndex: Int = 0): Flow<JournalPromptRecommendation> =
        combine(
            contextProvider.observeMetrics(),
            journalRepository.observeLatestTag()
        ) { metrics, latestTag ->
            AdaptivePromptEngine.recommend(metrics, latestTag, variantIndex)
        }
}

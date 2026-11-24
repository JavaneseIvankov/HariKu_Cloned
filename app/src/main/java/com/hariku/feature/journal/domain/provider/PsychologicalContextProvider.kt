package com.hariku.feature.journal.domain.provider

import com.hariku.feature.journal.domain.model.PsychologicalMetrics
import kotlinx.coroutines.flow.Flow

/** Penyedia EVI (Mood Tracker) & ADS (Chat Assistant). Wajib fallback ke netral bila data kosong. */
interface PsychologicalContextProvider {
    fun observeMetrics(): Flow<PsychologicalMetrics>
}

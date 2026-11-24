package com.hariku.feature.journal.domain.usecase

import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class ObserveJournalEntriesUseCase(private val repository: JournalRepository) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.observeEntries()
}

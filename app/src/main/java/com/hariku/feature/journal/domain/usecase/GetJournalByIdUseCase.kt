package com.hariku.feature.journal.domain.usecase

import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.repository.JournalRepository

class GetJournalByIdUseCase(private val repository: JournalRepository) {
    suspend operator fun invoke(id: Long): Result<JournalEntry> = repository.getById(id)
}

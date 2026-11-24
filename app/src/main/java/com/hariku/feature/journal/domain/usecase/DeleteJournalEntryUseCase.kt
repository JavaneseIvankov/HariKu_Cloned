package com.hariku.feature.journal.domain.usecase

import com.hariku.feature.journal.domain.repository.JournalRepository

class DeleteJournalEntryUseCase(private val repository: JournalRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.delete(id)
}

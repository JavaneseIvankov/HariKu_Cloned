package com.hariku.feature.journal.domain.usecase

import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.model.JournalStateSnapshot
import com.hariku.feature.journal.domain.model.PsychologicalMetrics
import com.hariku.feature.journal.domain.repository.JournalRepository

class JournalValidationException(message: String) : IllegalArgumentException(message)

/** Validasi domain (judul, isi, tanggal, tag) lalu insert/update. Mengembalikan snapshot state jurnal. */
class SaveJournalEntryUseCase(private val repository: JournalRepository) {

    suspend operator fun invoke(
        entry: JournalEntry,
        metrics: PsychologicalMetrics = PsychologicalMetrics.NEUTRAL,
        now: Long = System.currentTimeMillis()
    ): Result<JournalStateSnapshot> {
        val sanitized = entry.copy(
            title = entry.title.trim(),
            content = entry.content.trim(),
            updatedAt = now
        )
        validate(sanitized)?.let { return Result.failure(JournalValidationException(it)) }

        return repository.save(sanitized).map { id ->
            val saved = sanitized.copy(id = if (sanitized.id == 0L) id else sanitized.id)
            JournalStateSnapshot(
                template = saved.templateType,
                prompt = saved.promptUsed,
                entry = saved,
                metrics = metrics
            )
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val MAX_CONTENT_LENGTH = 50_000

        /** Mengembalikan pesan error atau null jika valid. */
        fun validate(entry: JournalEntry): String? = when {
            entry.title.isBlank() -> "Judul tidak boleh kosong"
            entry.title.length > MAX_TITLE_LENGTH -> "Judul maksimal $MAX_TITLE_LENGTH karakter"
            entry.content.isBlank() -> "Isi jurnal tidak boleh kosong"
            entry.content.length > MAX_CONTENT_LENGTH -> "Isi jurnal maksimal $MAX_CONTENT_LENGTH karakter"
            entry.dateMillis <= 0L -> "Tanggal tidak valid"
            else -> null
        }
    }
}

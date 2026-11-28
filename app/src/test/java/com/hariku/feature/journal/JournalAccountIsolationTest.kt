package com.hariku.feature.journal

import com.hariku.feature.journal.data.dto.JournalEntryDto
import com.hariku.feature.journal.data.remote.JournalRemoteDataSource
import com.hariku.feature.journal.data.repository.JournalRepositoryImpl
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalAccountIsolationTest {

    @Test
    fun moodRepository_isolatesEntriesAcrossAccountSwitches() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = MoodRepositoryImpl(FakeMoodRemoteDataSource(), userIdFlow, Dispatchers.Unconfined)

        repository.saveMood(MoodType.SENANG, "milik A", 1_000L)
        assertEquals(listOf("milik A"), repository.observeMoodEntries().first().map { it.note })

        userIdFlow.value = "user_B"
        assertTrue(repository.observeMoodEntries().first().isEmpty())

        repository.saveMood(MoodType.SEDIH, "milik B", 2_000L)
        assertEquals(listOf("milik B"), repository.observeMoodEntries().first().map { it.note })

        userIdFlow.value = "user_A"
        assertEquals(listOf("milik A"), repository.observeMoodEntries().first().map { it.note })
    }

    @Test
    fun moodRepository_cannotUpdateOrDeleteOtherAccountsEntry() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = MoodRepositoryImpl(FakeMoodRemoteDataSource(), userIdFlow, Dispatchers.Unconfined)
        val idA = repository.saveMood(MoodType.SENANG, "milik A", 1_000L).getOrThrow()

        userIdFlow.value = "user_B"
        assertTrue(repository.updateMood(idA, MoodType.MARAH, "overwrite").isFailure)
        assertTrue(repository.deleteMood(idA).isFailure)
        assertTrue(repository.deleteMood(idA + 100).isFailure)

        userIdFlow.value = "user_A"
        val entries = repository.observeMoodEntries().first()
        assertEquals(1, entries.size)
        assertEquals("milik A", entries.first().note)
    }

    @Test
    fun moodRepository_requiresAuthenticatedUserWithoutGuestFallback() = runBlocking {
        val remote = FakeMoodRemoteDataSource()
        val repository = MoodRepositoryImpl(remote, MutableStateFlow<String?>(null), Dispatchers.Unconfined)

        assertTrue(repository.observeMoodEntries().first().isEmpty())
        assertTrue(repository.saveMood(MoodType.BIASA, "", 1_000L).isFailure)
        assertTrue(repository.updateMood(1L, MoodType.SENANG, "x").isFailure)
        assertTrue(repository.deleteMood(1L).isFailure)
    }

    @Test
    fun journalRepository_mutationsFailWhenEntryMissingOrOwnedByAnotherAccount() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = JournalRepositoryImpl(FakeJournalRemoteDataSource(), userIdFlow)
        val idA = repository.save(
            JournalEntry(title = "A", content = "private", dateMillis = 1_000L)
        ).getOrThrow()

        userIdFlow.value = "user_B"
        assertTrue(repository.save(JournalEntry(id = idA, title = "overwrite", content = "x", dateMillis = 2_000L)).isFailure)
        assertTrue(repository.delete(idA).isFailure)
        assertTrue(repository.togglePin(idA).isFailure)
        assertTrue(repository.delete(idA + 100).isFailure)
        assertTrue(repository.togglePin(idA + 100).isFailure)

        userIdFlow.value = "user_A"
        assertEquals("A", repository.getById(idA).getOrThrow().title)
    }

    @Test
    fun journalRepository_requiresAuthenticatedUserWithoutGuestFallback() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>(null)
        val repository = JournalRepositoryImpl(FakeJournalRemoteDataSource(), userIdFlow)

        assertTrue(repository.observeEntries().first().isEmpty())
        assertNull(repository.observeLatestTag().first())
        assertTrue(repository.getById(1L).isFailure)
        assertTrue(repository.save(JournalEntry(title = "Guest", content = "x", dateMillis = 1_000L)).isFailure)
        assertTrue(repository.delete(1L).isFailure)
        assertTrue(repository.togglePin(1L).isFailure)
    }

    @Test
    fun journalRepository_isolatesEntriesAndLatestTagAcrossAccountSwitches() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = JournalRepositoryImpl(FakeJournalRemoteDataSource(), userIdFlow)

        val idA = repository.save(
            JournalEntry(title = "A", content = "isi A", dateMillis = 1_000L, tag = JournalCategoryTag.AKADEMIK)
        ).getOrThrow()
        assertEquals(JournalCategoryTag.AKADEMIK, repository.observeLatestTag().first())

        userIdFlow.value = "user_B"
        assertTrue(repository.observeEntries().first().isEmpty())
        assertNull(repository.observeLatestTag().first())
        assertTrue(repository.getById(idA).isFailure)
        assertTrue(
            repository.save(JournalEntry(id = idA, title = "hack", content = "x", dateMillis = 1L)).isFailure
        )

        userIdFlow.value = "user_A"
        val entries = repository.observeEntries().first()
        assertEquals(1, entries.size)
        assertEquals("A", entries.first().title)
    }

    private class FakeMoodRemoteDataSource : MoodRemoteDataSource {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryDto>>(emptyList())

        override fun observeAll(userId: String): Flow<List<MoodEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun save(entry: MoodEntryDto, userId: String): Long? {
            val id = if (entry.id == 0L) nextId++ else entry.id
            items.value = items.value + entry.copy(id = id, userId = userId)
            return id
        }

        override suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String) {
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            items.value = items.value.filterNot { it.userId == userId && it.id in byId } + byId.values
        }

        override suspend fun update(
            entryId: Long,
            userId: String,
            moodType: String,
            note: String,
            updatedAt: Long
        ): Boolean {
            var updated = false
            items.value = items.value.map {
                if (it.id == entryId && it.userId == userId) {
                    updated = true
                    it.copy(moodType = moodType, note = note, updatedAt = updatedAt)
                } else {
                    it
                }
            }
            return updated
        }

        override suspend fun delete(entryId: Long, userId: String): Boolean {
            val before = items.value.size
            items.value = items.value.filterNot { it.id == entryId && it.userId == userId }
            return before > items.value.size
        }
    }

    private class FakeJournalRemoteDataSource : JournalRemoteDataSource {
        private var nextId = 1L
        private val items = MutableStateFlow<List<JournalEntryDto>>(emptyList())

        override fun observeAll(userId: String): Flow<List<JournalEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun getById(id: Long, userId: String): JournalEntryDto? =
            items.value.firstOrNull { it.id == id && it.userId == userId }

        override fun observeLatestTag(userId: String): Flow<String?> =
            items.map { list ->
                list.filter { it.userId == userId }
                    .maxWithOrNull(compareBy<JournalEntryDto> { it.updatedAt }.thenBy { it.id })
                    ?.tag
            }

        override suspend fun save(entry: JournalEntryDto, userId: String): Long? {
            return if (entry.id == 0L) {
                val id = nextId++
                items.value = items.value + entry.copy(id = id, userId = userId)
                id
            } else {
                val existingIndex = items.value.indexOfFirst { it.id == entry.id && it.userId == userId }
                if (existingIndex < 0) return null
                items.value = items.value.map {
                    if (it.id == entry.id && it.userId == userId) entry.copy(userId = userId) else it
                }
                entry.id
            }
        }

        override suspend fun upsertBatch(entries: List<JournalEntryDto>, userId: String) {
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            val remaining = items.value.filterNot { it.userId == userId && it.id in byId }
            items.value = remaining + byId.values
        }

        override suspend fun delete(id: Long, userId: String): Boolean {
            val before = items.value.size
            items.value = items.value.filterNot { it.id == id && it.userId == userId }
            return before > items.value.size
        }

        override suspend fun togglePin(id: Long, userId: String): Boolean {
            var changed = false
            items.value = items.value.map {
                if (it.id == id && it.userId == userId) {
                    changed = true
                    it.copy(isPinned = !it.isPinned)
                } else {
                    it
                }
            }
            return changed
        }
    }
}

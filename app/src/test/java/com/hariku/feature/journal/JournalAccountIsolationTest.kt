package com.hariku.feature.journal

import com.hariku.core.auth.LOCAL_GUEST_USER_ID
import com.hariku.feature.journal.data.local.JournalDao
import com.hariku.feature.journal.data.local.JournalEntryEntity
import com.hariku.feature.journal.data.repository.JournalRepositoryImpl
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.mood.data.local.MoodDao
import com.hariku.feature.mood.data.local.MoodEntryEntity
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodType
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
        val repository = MoodRepositoryImpl(FakeMoodDao(), userIdFlow)

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
    fun moodRepository_cannotDeleteOtherAccountsEntry() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = MoodRepositoryImpl(FakeMoodDao(), userIdFlow)
        val idA = repository.saveMood(MoodType.SENANG, "milik A", 1_000L).getOrThrow()

        userIdFlow.value = "user_B"
        repository.deleteMood(idA)

        userIdFlow.value = "user_A"
        assertEquals(1, repository.observeMoodEntries().first().size)
    }

    @Test
    fun moodRepository_usesLocalGuestScopeWhenNoUser() = runBlocking {
        val dao = FakeMoodDao()
        val repository = MoodRepositoryImpl(dao, MutableStateFlow<String?>(null))
        repository.saveMood(MoodType.BIASA, "", 1_000L)
        assertEquals(1, dao.observeEntries(LOCAL_GUEST_USER_ID).first().size)
    }

    @Test
    fun journalRepository_mutationsFailWhenEntryMissingOrOwnedByAnotherAccount() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = JournalRepositoryImpl(FakeJournalDao(), userIdFlow)
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
    fun journalRepository_isolatesEntriesAndLatestTagAcrossAccountSwitches() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = JournalRepositoryImpl(FakeJournalDao(), userIdFlow)

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

    private class FakeMoodDao : MoodDao {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insert(entry: MoodEntryEntity): Long {
            val id = nextId++
            items.value = items.value + entry.copy(id = id)
            return id
        }

        override fun observeEntries(userId: String): Flow<List<MoodEntryEntity>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun updateById(entryId: Long, userId: String, moodType: String, note: String) {
            items.value = items.value.map {
                if (it.id == entryId && it.userId == userId) it.copy(moodType = moodType, note = note) else it
            }
        }

        override suspend fun deleteById(entryId: Long, userId: String) {
            items.value = items.value.filterNot { it.id == entryId && it.userId == userId }
        }
    }

    private class FakeJournalDao : JournalDao {
        private var nextId = 1L
        private val items = MutableStateFlow<List<JournalEntryEntity>>(emptyList())

        override fun observeAll(userId: String): Flow<List<JournalEntryEntity>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun getById(id: Long, userId: String): JournalEntryEntity? =
            items.value.firstOrNull { it.id == id && it.userId == userId }

        override fun getLatestTag(userId: String): Flow<String?> =
            items.map { list ->
                list.filter { it.userId == userId }
                    .maxWithOrNull(compareBy<JournalEntryEntity> { it.updatedAt }.thenBy { it.id })
                    ?.tag
            }

        override suspend fun insert(entry: JournalEntryEntity): Long {
            val id = nextId++
            items.value = items.value + entry.copy(id = id)
            return id
        }

        override suspend fun update(entry: JournalEntryEntity): Int {
            val existingIndex = items.value.indexOfFirst { it.id == entry.id && it.userId == entry.userId }
            if (existingIndex < 0) return 0
            items.value = items.value.map {
                if (it.id == entry.id && it.userId == entry.userId) entry else it
            }
            return 1
        }

        override suspend fun delete(id: Long, userId: String): Int {
            val before = items.value.size
            items.value = items.value.filterNot { it.id == id && it.userId == userId }
            return before - items.value.size
        }

        override suspend fun togglePin(id: Long, userId: String): Int {
            var changed = 0
            items.value = items.value.map {
                if (it.id == id && it.userId == userId) {
                    changed++
                    it.copy(isPinned = !it.isPinned)
                } else {
                    it
                }
            }
            return changed
        }
    }
}

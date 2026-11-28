package com.hariku.feature.journal

import com.hariku.feature.journal.data.dto.JournalEntryDto
import com.hariku.feature.journal.data.local.LocalJournalStore
import com.hariku.feature.journal.data.local.RoomJournalLocalMigrator
import com.hariku.feature.journal.data.remote.JournalRemoteDataSource
import com.hariku.feature.journal.data.repository.JournalRepositoryImpl
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalLocalMigratorTest {

    @Test
    fun migrator_uploadsMatchingUsersLegacyEntriesAndCleansLocalRowsIdempotently() = runBlocking {
        val localStore = FakeLocalJournalStore(
            mutableListOf(
                JournalEntryDto(
                    id = 1L,
                    userId = "user_A",
                    title = "Lama A",
                    content = "Catatan A",
                    dateMillis = 1_000L,
                    tag = JournalCategoryTag.AKADEMIK.name,
                    updatedAt = 1_000L
                ),
                JournalEntryDto(
                    id = 2L,
                    userId = "user_B",
                    title = "Lama B",
                    content = "Catatan B",
                    dateMillis = 2_000L,
                    tag = JournalCategoryTag.STRES.name,
                    updatedAt = 2_000L
                )
            )
        )
        val remote = FakeRemoteDataSource()
        val migrator = RoomJournalLocalMigrator(localStore, remote)
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = JournalRepositoryImpl(
            remoteDataSource = remote,
            userIdFlow = userIdFlow,
            ioDispatcher = Dispatchers.Unconfined,
            localMigrator = migrator
        )

        val entriesA = repository.observeEntries().first()
        assertEquals(listOf("Lama A"), entriesA.map { it.title })
        assertTrue(localStore.isMigrated("user_A"))
        assertFalse(localStore.isMigrated("user_B"))
        assertEquals(listOf(2L), localStore.remainingIds())

        // Idempotent second run should not upload again
        migrator.migrateIfNeeded("user_A")
        assertEquals(1, remote.upsertCalls)

        // Switching to user_B migrates only user_B's rows
        userIdFlow.value = "user_B"
        val entriesB = repository.observeEntries().first()
        assertEquals(listOf("Lama B"), entriesB.map { it.title })
        assertTrue(localStore.isMigrated("user_B"))
        assertTrue(localStore.remainingIds().isEmpty())
    }

    @Test
    fun migrator_doesNotMarkCompleteOrDeleteLocalRowsWhenRemoteFails() = runBlocking {
        val localStore = FakeLocalJournalStore(
            mutableListOf(
                JournalEntryDto(
                    id = 10L,
                    userId = "user_A",
                    title = "Penting",
                    content = "Jangan hilang",
                    dateMillis = 5_000L,
                    updatedAt = 5_000L
                )
            )
        )
        val remote = FakeRemoteDataSource(failUpsert = true)
        val migrator = RoomJournalLocalMigrator(localStore, remote)

        migrator.migrateIfNeeded("user_A")
        assertFalse(localStore.isMigrated("user_A"))
        assertEquals(listOf(10L), localStore.remainingIds())

        // Subsequent retry succeeds once remote recovers
        remote.failUpsert = false
        migrator.migrateIfNeeded("user_A")
        assertTrue(localStore.isMigrated("user_A"))
        assertTrue(localStore.remainingIds().isEmpty())
        assertEquals(listOf("Penting"), remote.observeAll("user_A").first().map { it.title })
    }

    private class FakeLocalJournalStore(
        private val rows: MutableList<JournalEntryDto>
    ) : LocalJournalStore {
        private val migratedUsers = mutableSetOf<String>()

        override fun isMigrated(userId: String): Boolean = userId in migratedUsers

        override fun markMigrated(userId: String) {
            migratedUsers += userId
        }

        override fun readEntries(userId: String): List<JournalEntryDto> =
            rows.filter { it.userId == userId }

        override fun deleteEntries(userId: String, ids: List<Long>) {
            val idSet = ids.toSet()
            rows.removeAll { it.userId == userId && it.id in idSet }
        }

        fun remainingIds(): List<Long> = rows.map { it.id }
    }

    private class FakeRemoteDataSource(
        var failUpsert: Boolean = false
    ) : JournalRemoteDataSource {
        private val items = MutableStateFlow<List<JournalEntryDto>>(emptyList())
        var upsertCalls = 0
            private set

        override fun observeAll(userId: String): Flow<List<JournalEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override fun observeLatestTag(userId: String): Flow<String?> =
            items.map { list -> list.lastOrNull { it.userId == userId }?.tag }

        override suspend fun getById(id: Long, userId: String): JournalEntryDto? =
            items.value.firstOrNull { it.id == id && it.userId == userId }

        override suspend fun save(entry: JournalEntryDto, userId: String): Long? = entry.id

        override suspend fun upsertBatch(entries: List<JournalEntryDto>, userId: String) {
            upsertCalls += 1
            if (failUpsert) throw IllegalStateException("Remote unavailable")
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            items.value = items.value.filterNot { it.userId == userId && it.id in byId } + byId.values
        }

        override suspend fun delete(id: Long, userId: String): Boolean = false

        override suspend fun togglePin(id: Long, userId: String): Boolean = false
    }
}

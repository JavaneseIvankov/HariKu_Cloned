package com.hariku.feature.mood

import com.hariku.core.auth.LOCAL_GUEST_USER_ID
import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.local.LocalMoodStore
import com.hariku.feature.mood.data.local.RoomMoodLocalMigrator
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodLocalMigratorTest {

    @Test
    fun migrator_uploadsMatchingUsersLegacyEntriesAndCleansLocalRowsIdempotently() = runBlocking {
        val localStore = FakeLocalMoodStore(
            mutableListOf(
                MoodEntryDto(
                    id = 1L,
                    userId = "user_A",
                    moodType = MoodType.SENANG.label,
                    timestamp = 1_000L,
                    note = "Mood lama A",
                    updatedAt = 1_000L
                ),
                MoodEntryDto(
                    id = 2L,
                    userId = "user_B",
                    moodType = MoodType.LELAH.label,
                    timestamp = 2_000L,
                    note = "Mood lama B",
                    updatedAt = 2_000L
                )
            )
        )
        val remote = FakeRemoteDataSource()
        val migrator = RoomMoodLocalMigrator(localStore, remote)
        val userIdFlow = MutableStateFlow<String?>("user_A")
        val repository = MoodRepositoryImpl(
            remoteDataSource = remote,
            userIdFlow = userIdFlow,
            ioDispatcher = Dispatchers.Unconfined,
            localMigrator = migrator
        )

        val entriesA = repository.observeMoodEntries().first()
        assertEquals(listOf("Mood lama A"), entriesA.map { it.note })
        assertTrue(localStore.isMigrated("user_A"))
        assertFalse(localStore.isMigrated("user_B"))
        assertEquals(listOf(2L), localStore.remainingIds())

        // Idempotent second run should not upload again
        migrator.migrateIfNeeded("user_A")
        assertEquals(1, remote.upsertCalls)

        // Switching to user_B migrates only user_B's rows
        userIdFlow.value = "user_B"
        val entriesB = repository.observeMoodEntries().first()
        assertEquals(listOf("Mood lama B"), entriesB.map { it.note })
        assertTrue(localStore.isMigrated("user_B"))
        assertTrue(localStore.remainingIds().isEmpty())
    }

    @Test
    fun migrator_doesNotMarkCompleteOrDeleteLocalRowsWhenRemoteFails() = runBlocking {
        val localStore = FakeLocalMoodStore(
            mutableListOf(
                MoodEntryDto(
                    id = 10L,
                    userId = "user_A",
                    moodType = MoodType.SEMANGAT.label,
                    timestamp = 5_000L,
                    note = "Jangan hilang",
                    updatedAt = 5_000L
                )
            )
        )
        val remote = FakeRemoteDataSource(failUpsert = true)
        val errors = mutableListOf<Throwable>()
        val migrator = RoomMoodLocalMigrator(localStore, remote, onError = { errors += it })

        migrator.migrateIfNeeded("user_A")
        assertFalse(localStore.isMigrated("user_A"))
        assertEquals(listOf(10L), localStore.remainingIds())
        assertEquals(1, errors.size)

        // Subsequent retry succeeds once remote recovers
        remote.failUpsert = false
        migrator.migrateIfNeeded("user_A")
        assertTrue(localStore.isMigrated("user_A"))
        assertTrue(localStore.remainingIds().isEmpty())
        assertEquals(listOf("Jangan hilang"), remote.observeAll("user_A").first().map { it.note })
    }

    @Test
    fun migrator_ignoresLocalGuestUserId() = runBlocking {
        val localStore = FakeLocalMoodStore(
            mutableListOf(
                MoodEntryDto(
                    id = 1L,
                    userId = LOCAL_GUEST_USER_ID,
                    moodType = MoodType.BIASA.label,
                    timestamp = 1_000L,
                    note = "Guest"
                )
            )
        )
        val remote = FakeRemoteDataSource()
        val migrator = RoomMoodLocalMigrator(localStore, remote)

        migrator.migrateIfNeeded(LOCAL_GUEST_USER_ID)
        assertEquals(0, remote.upsertCalls)
        assertFalse(localStore.isMigrated(LOCAL_GUEST_USER_ID))
        assertEquals(listOf(1L), localStore.remainingIds())
    }

    private class FakeLocalMoodStore(
        private val rows: MutableList<MoodEntryDto>
    ) : LocalMoodStore {
        private val migratedUsers = mutableSetOf<String>()

        override fun isMigrated(userId: String): Boolean = userId in migratedUsers

        override fun markMigrated(userId: String) {
            migratedUsers += userId
        }

        override fun readEntries(userId: String): List<MoodEntryDto> =
            rows.filter { it.userId == userId }

        override fun deleteEntries(userId: String, ids: List<Long>) {
            val idSet = ids.toSet()
            rows.removeAll { it.userId == userId && it.id in idSet }
        }

        fun remainingIds(): List<Long> = rows.map { it.id }
    }

    private class FakeRemoteDataSource(
        var failUpsert: Boolean = false
    ) : MoodRemoteDataSource {
        private val items = MutableStateFlow<List<MoodEntryDto>>(emptyList())
        var upsertCalls = 0
            private set

        override fun observeAll(userId: String): Flow<List<MoodEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun save(entry: MoodEntryDto, userId: String): Long? = entry.id

        override suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String) {
            upsertCalls += 1
            if (failUpsert) throw IllegalStateException("Remote unavailable")
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            items.value = items.value.filterNot { it.userId == userId && it.id in byId } + byId.values
        }

        override suspend fun update(
            entryId: Long,
            userId: String,
            moodType: String,
            note: String,
            updatedAt: Long
        ): Boolean = false

        override suspend fun delete(entryId: Long, userId: String): Boolean = false
    }
}

package com.hariku.feature_statistic

import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature_statistic.presentation.StatisticViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticViewModelTest {

    @Test
    fun statisticViewModel_reflectsLiveUpdatesAndAccountSwitches() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val remote = FakeRemoteDataSource()
            val repo = MoodRepositoryImpl(remote, userIdFlow, Dispatchers.Unconfined)
            val vm = StatisticViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(repo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )

            assertTrue(vm.uiState.value.isLoading)

            val collector = scope.launch { vm.uiState.collect {} }

            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.entries.isEmpty())

            // Live insert for user_A
            val idA = repo.saveMood(MoodType.SENANG, "Lancar", 1_000L).getOrThrow()
            assertEquals(listOf(MoodType.SENANG), vm.uiState.value.entries.map { it.mood })

            // Live update for user_A
            repo.updateMood(idA, MoodType.SEMANGAT, "Makin baik")
            assertEquals(listOf(MoodType.SEMANGAT), vm.uiState.value.entries.map { it.mood })

            // Switch to user_B isolates entries
            userIdFlow.value = "user_B"
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.entries.isEmpty())

            repo.saveMood(MoodType.LELAH, "Capek", 2_000L)
            assertEquals(listOf(MoodType.LELAH), vm.uiState.value.entries.map { it.mood })

            // Sign out clears entries
            userIdFlow.value = null
            assertTrue(vm.uiState.value.entries.isEmpty())

            collector.cancel()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun statisticViewModel_surfacesLoadErrorAndRecoversOnRetry() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            var shouldFail = true
            val failingRepo = object : MoodRepository {
                override fun observeMoodEntries(): Flow<List<MoodEntry>> = flow {
                    if (shouldFail) throw IllegalStateException("Firestore error")
                    emit(listOf(MoodEntry(id = 1L, mood = MoodType.SENANG, timestamp = 1_000L)))
                }

                override suspend fun saveMood(mood: MoodType, note: String, timestamp: Long) =
                    Result.success(1L)

                override suspend fun updateMood(entryId: Long, mood: MoodType, note: String) =
                    Result.success(Unit)

                override suspend fun deleteMood(entryId: Long) = Result.success(Unit)
            }

            val vm = StatisticViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(failingRepo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )
            val collector = scope.launch { vm.uiState.collect {} }

            assertFalse(vm.uiState.value.isLoading)
            assertEquals("Statistik mood gagal dimuat.", vm.uiState.value.loadError)
            assertTrue(vm.uiState.value.entries.isEmpty())

            shouldFail = false
            vm.retryLoad()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.loadError)
            assertEquals(1, vm.uiState.value.entries.size)

            collector.cancel()
        } finally {
            scope.cancel()
        }
    }

    private class FakeRemoteDataSource : MoodRemoteDataSource {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryDto>>(emptyList())

        override fun observeAll(userId: String): Flow<List<MoodEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun save(entry: MoodEntryDto, userId: String): Long? {
            val id = if (entry.id == 0L) nextId++ else entry.id
            items.value = listOf(entry.copy(id = id, userId = userId)) + items.value
            return id
        }

        override suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String) = Unit

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
}

package com.hariku.feature.mood

import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
import com.hariku.feature.mood.presentation.QuickMoodViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class QuickMoodViewModelTest {

    private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val sampleDate: LocalDate = LocalDate.of(2026, 11, 28)
    private val sampleTimestamp: Long =
        sampleDate.atTime(10, 0).atZone(jakartaZone).toInstant().toEpochMilli()

    @Test
    fun loadErrorClearsStaleSummaryAndRetryLoadRecovers() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = ControllableMoodRepository(userIdFlow)
            repo.seed("user_A", MoodEntry(id = 1L, mood = MoodType.SENANG, timestamp = sampleTimestamp, note = "Awal"))

            val vm = QuickMoodViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(repo),
                saveQuickMoodUseCase = SaveQuickMoodUseCase(repo),
                deleteMoodEntryUseCase = DeleteMoodEntryUseCase(repo),
                updateMoodEntryUseCase = UpdateMoodEntryUseCase(repo),
                userIdFlow = userIdFlow,
                todayProvider = { sampleDate },
                nowProvider = { sampleTimestamp },
                zoneId = jakartaZone,
                coroutineScopeOverride = scope
            )

            assertEquals(1, vm.uiState.value.todayCount)
            vm.onStartEditEntry(vm.uiState.value.latestTodayEntry!!)
            assertNotNull(vm.uiState.value.editingEntry)

            repo.failObserveOnce = true
            vm.retryLoad()

            val errorState = vm.uiState.value
            assertFalse(errorState.isLoading)
            assertEquals("Catatan mood gagal dimuat.", errorState.loadError)
            assertNull(errorState.latestTodayEntry)
            assertTrue(errorState.todayEntries.isEmpty())
            assertEquals(0, errorState.streakDays)
            assertNull(errorState.editingEntry)

            vm.retryLoad()
            val recoveredState = vm.uiState.value
            assertFalse(recoveredState.isLoading)
            assertNull(recoveredState.loadError)
            assertEquals(1, recoveredState.todayCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun mutationFailuresExposeActionErrorAndPreserveEditingDialog() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = ControllableMoodRepository(userIdFlow)
            val entry = MoodEntry(id = 1L, mood = MoodType.SENANG, timestamp = sampleTimestamp, note = "Awal")
            repo.seed("user_A", entry)

            val vm = QuickMoodViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(repo),
                saveQuickMoodUseCase = SaveQuickMoodUseCase(repo),
                deleteMoodEntryUseCase = DeleteMoodEntryUseCase(repo),
                updateMoodEntryUseCase = UpdateMoodEntryUseCase(repo),
                userIdFlow = userIdFlow,
                todayProvider = { sampleDate },
                nowProvider = { sampleTimestamp },
                zoneId = jakartaZone,
                coroutineScopeOverride = scope
            )

            repo.failMutation = true
            vm.onSelectMood(MoodType.MARAH)
            assertFalse(vm.uiState.value.isSaving)
            assertEquals("Mood gagal disimpan. Coba lagi.", vm.uiState.value.actionError)

            vm.onStartEditEntry(entry)
            vm.onConfirmEditEntry()
            assertFalse(vm.uiState.value.isSaving)
            assertEquals(entry, vm.uiState.value.editingEntry)
            assertEquals("Catatan mood gagal diperbarui. Coba lagi.", vm.uiState.value.actionError)

            vm.onDismissEditEntry()
            vm.onDeleteEntry(entry.id)
            assertFalse(vm.uiState.value.isSaving)
            assertEquals("Gagal menghapus catatan mood.", vm.uiState.value.actionError)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun inFlightMutationFromPreviousSessionDoesNotPolluteNewAccount() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = ControllableMoodRepository(userIdFlow)
            val deferredSave = CompletableDeferred<Result<Long>>()
            repo.pendingSave = deferredSave

            val vm = QuickMoodViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(repo),
                saveQuickMoodUseCase = SaveQuickMoodUseCase(repo),
                deleteMoodEntryUseCase = DeleteMoodEntryUseCase(repo),
                updateMoodEntryUseCase = UpdateMoodEntryUseCase(repo),
                userIdFlow = userIdFlow,
                todayProvider = { sampleDate },
                nowProvider = { sampleTimestamp },
                zoneId = jakartaZone,
                coroutineScopeOverride = scope
            )

            vm.onSelectMood(MoodType.SENANG)
            assertTrue(vm.uiState.value.isSaving)

            // Account switches while user_A's save is still suspended
            userIdFlow.value = "user_B"
            assertFalse(vm.uiState.value.isSaving)
            assertTrue(vm.uiState.value.isAuthenticated)

            // Completing user_A's stale deferred must not affect user_B
            deferredSave.complete(Result.success(99L))
            assertNull(vm.uiState.value.feedbackMessage)
            assertNull(vm.uiState.value.lastSavedEntryId)
            assertNull(vm.uiState.value.actionError)

            // Sign out sets isAuthenticated = false
            userIdFlow.value = null
            assertFalse(vm.uiState.value.isAuthenticated)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun sessionSwitchRecoversAfterPreviousObserveError() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = ControllableMoodRepository(userIdFlow)
            repo.failObserveOnce = true
            repo.seed("user_B", MoodEntry(id = 7L, mood = MoodType.SEMANGAT, timestamp = sampleTimestamp))

            val vm = QuickMoodViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(repo),
                saveQuickMoodUseCase = SaveQuickMoodUseCase(repo),
                deleteMoodEntryUseCase = DeleteMoodEntryUseCase(repo),
                updateMoodEntryUseCase = UpdateMoodEntryUseCase(repo),
                userIdFlow = userIdFlow,
                todayProvider = { sampleDate },
                nowProvider = { sampleTimestamp },
                zoneId = jakartaZone,
                coroutineScopeOverride = scope
            )

            assertEquals("Catatan mood gagal dimuat.", vm.uiState.value.loadError)

            userIdFlow.value = "user_B"
            assertNull(vm.uiState.value.loadError)
            assertEquals(1, vm.uiState.value.todayCount)
            assertEquals(MoodType.SEMANGAT, vm.uiState.value.latestTodayEntry?.mood)
        } finally {
            scope.cancel()
        }
    }

    private class ControllableMoodRepository(
        private val userIdFlow: MutableStateFlow<String?>
    ) : MoodRepository {
        private val entriesByUser = MutableStateFlow<Map<String, List<MoodEntry>>>(emptyMap())
        var failObserveOnce = false
        var failMutation = false
        var pendingSave: CompletableDeferred<Result<Long>>? = null

        fun seed(userId: String, entry: MoodEntry) {
            val current = entriesByUser.value[userId].orEmpty()
            entriesByUser.value = entriesByUser.value + (userId to (current + entry))
        }

        override fun observeMoodEntries(): Flow<List<MoodEntry>> = flow {
            if (failObserveOnce) {
                failObserveOnce = false
                throw IllegalStateException("Firestore unavailable")
            }
            userIdFlow.map { userId ->
                if (userId == null) emptyList() else entriesByUser.value[userId].orEmpty()
            }.collect { emit(it) }
        }

        override suspend fun saveMood(mood: MoodType, note: String, timestamp: Long): Result<Long> {
            pendingSave?.let { return it.await() }
            if (failMutation) return Result.failure(IllegalStateException("Save failed"))
            val userId = userIdFlow.value ?: return Result.failure(IllegalStateException("Unauthenticated"))
            val newId = (entriesByUser.value.values.flatten().maxOfOrNull { it.id } ?: 0L) + 1L
            seed(userId, MoodEntry(id = newId, mood = mood, timestamp = timestamp, note = note))
            return Result.success(newId)
        }

        override suspend fun updateMood(entryId: Long, mood: MoodType, note: String): Result<Unit> {
            if (failMutation) return Result.failure(IllegalStateException("Update failed"))
            return Result.success(Unit)
        }

        override suspend fun deleteMood(entryId: Long): Result<Unit> {
            if (failMutation) return Result.failure(IllegalStateException("Delete failed"))
            return Result.success(Unit)
        }
    }
}

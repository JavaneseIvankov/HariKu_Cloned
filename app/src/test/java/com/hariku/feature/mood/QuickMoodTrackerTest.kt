package com.hariku.feature.mood

import com.hariku.feature.mood.data.dto.MoodEntryDto
import com.hariku.feature.mood.data.remote.MoodRemoteDataSource
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
import com.hariku.feature.mood.presentation.MoodBurstStyle
import com.hariku.feature.mood.presentation.MoodTapMotionCatalog
import com.hariku.feature.mood.presentation.QuickMoodUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class QuickMoodTrackerTest {

    private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")
    private val sampleDate: LocalDate = LocalDate.of(2026, 11, 24)

    @Test
    fun moodTypeFromLabelMatchesAllKnownLabelsCaseInsensitively() {
        for (mood in MoodType.entries) {
            assertEquals(mood, MoodType.fromLabel(mood.label))
            assertEquals(mood, MoodType.fromLabel("  ${mood.label.uppercase()}  "))
        }
        assertNull(MoodType.fromLabel("TidakDikenal"))
        assertNull(MoodType.fromLabel(""))
    }

    @Test
    fun summarizeTodayFiltersOnlyTodayAndOrdersLatestFirst() {
        val yesterdayMillis = sampleDate.minusDays(1).atTime(23, 55).atZone(jakartaZone).toInstant().toEpochMilli()
        val morningMillis = sampleDate.atTime(8, 0).atZone(jakartaZone).toInstant().toEpochMilli()
        val noonMillis = sampleDate.atTime(12, 30).atZone(jakartaZone).toInstant().toEpochMilli()
        val tomorrowMillis = sampleDate.plusDays(1).atTime(0, 5).atZone(jakartaZone).toInstant().toEpochMilli()

        val entries = listOf(
            MoodEntry(id = 1L, mood = MoodType.LELAH, timestamp = yesterdayMillis),
            MoodEntry(id = 2L, mood = MoodType.BIASA, timestamp = morningMillis),
            MoodEntry(id = 3L, mood = MoodType.SENANG, timestamp = noonMillis),
            MoodEntry(id = 4L, mood = MoodType.SEMANGAT, timestamp = noonMillis),
            MoodEntry(id = 5L, mood = MoodType.CEMAS, timestamp = tomorrowMillis)
        )

        val summary = MoodTrackerLogic.summarizeToday(
            entries = entries,
            today = sampleDate,
            zone = jakartaZone
        )

        assertEquals(3, summary.todayCount)
        assertEquals(4L, summary.latestEntry?.id)
        assertEquals(MoodType.SEMANGAT, summary.latestEntry?.mood)
        assertEquals(listOf(4L, 3L, 2L), summary.todayEntries.map { it.id })
    }

    @Test
    fun summarizeTodayHandlesEmptyList() {
        val summary = MoodTrackerLogic.summarizeToday(
            entries = emptyList(),
            today = sampleDate,
            zone = jakartaZone
        )

        assertNull(summary.latestEntry)
        assertEquals(0, summary.todayCount)
    }

    @Test
    fun sanitizeNoteTrimsAndCapsLength() {
        assertEquals("Tenang hari ini", MoodTrackerLogic.sanitizeNote("   Tenang hari ini   "))

        val longNote = "a".repeat(MoodTrackerLogic.MAX_NOTE_LENGTH + 25)
        val sanitized = MoodTrackerLogic.sanitizeNote(longNote)
        assertEquals(MoodTrackerLogic.MAX_NOTE_LENGTH, sanitized.length)
    }

    @Test
    fun saveObserveUpdateAndDeleteFlowWorksEndToEndWithFakeRemote() = runBlocking {
        val fakeRemote = FakeMoodRemoteDataSource()
        val repository = MoodRepositoryImpl(
            remoteDataSource = fakeRemote,
            userIdFlow = MutableStateFlow("user_A"),
            ioDispatcher = Dispatchers.Unconfined
        )
        val observeUseCase = ObserveMoodEntriesUseCase(repository)
        val saveUseCase = SaveQuickMoodUseCase(repository)
        val updateUseCase = UpdateMoodEntryUseCase(repository)
        val deleteUseCase = DeleteMoodEntryUseCase(repository)

        val timestamp = sampleDate.atTime(9, 15).atZone(jakartaZone).toInstant().toEpochMilli()
        val saveResult = saveUseCase(
            mood = MoodType.SENANG,
            note = "   Pagi yang produktif   ",
            timestamp = timestamp
        )

        assertTrue(saveResult.isSuccess)
        val savedId = saveResult.getOrThrow()

        // Inject an unknown label document directly in storage to ensure graceful filtering.
        fakeRemote.save(MoodEntryDto(moodType = "UnknownLegacy", timestamp = timestamp), "user_A")

        val observed = observeUseCase().first()
        assertEquals(1, observed.size)
        assertEquals(savedId, observed.first().id)
        assertEquals(MoodType.SENANG, observed.first().mood)
        assertEquals("Pagi yang produktif", observed.first().note)

        val updateResult = updateUseCase(
            entryId = savedId,
            mood = MoodType.SEMANGAT,
            note = "   Presentasi berjalan lancar   "
        )
        assertTrue(updateResult.isSuccess)

        val afterUpdate = observeUseCase().first()
        assertEquals(1, afterUpdate.size)
        assertEquals(MoodType.SEMANGAT, afterUpdate.first().mood)
        assertEquals("Presentasi berjalan lancar", afterUpdate.first().note)
        assertEquals(timestamp, afterUpdate.first().timestamp)

        val deleteResult = deleteUseCase(savedId)
        assertTrue(deleteResult.isSuccess)
        assertTrue(observeUseCase().first().isEmpty())
    }

    @Test
    fun repositoryRejectsMutationsWhenUnauthenticated() = runBlocking {
        val repository = MoodRepositoryImpl(
            remoteDataSource = FakeMoodRemoteDataSource(),
            userIdFlow = MutableStateFlow(null),
            ioDispatcher = Dispatchers.Unconfined
        )

        assertTrue(ObserveMoodEntriesUseCase(repository)().first().isEmpty())
        assertTrue(SaveQuickMoodUseCase(repository)(MoodType.SENANG, "catatan", 1_000L).isFailure)
        assertTrue(UpdateMoodEntryUseCase(repository)(1L, MoodType.BIASA, "edit").isFailure)
        assertTrue(DeleteMoodEntryUseCase(repository)(1L).isFailure)
    }

    @Test
    fun historyPaginationSlicesFiveItemsPerPageAndClampsOutOfRangePages() {
        val baseMillis = sampleDate.atTime(10, 0).atZone(jakartaZone).toInstant().toEpochMilli()
        val twelveEntries = (1L..12L).map { id ->
            MoodEntry(id = id, mood = MoodType.BIASA, timestamp = baseMillis + id * 1000L)
        }

        val pageZero = QuickMoodUiState(isLoading = false, todayEntries = twelveEntries, historyPage = 0)
        assertEquals(3, pageZero.totalHistoryPages)
        assertEquals(0, pageZero.currentHistoryPage)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), pageZero.pagedTodayEntries.map { it.id })
        assertFalse(pageZero.canGoToPreviousHistoryPage)
        assertTrue(pageZero.canGoToNextHistoryPage)

        val pageOne = pageZero.copy(historyPage = 1)
        assertEquals(listOf(6L, 7L, 8L, 9L, 10L), pageOne.pagedTodayEntries.map { it.id })
        assertTrue(pageOne.canGoToPreviousHistoryPage)
        assertTrue(pageOne.canGoToNextHistoryPage)

        val lastPage = pageZero.copy(historyPage = 2)
        assertEquals(listOf(11L, 12L), lastPage.pagedTodayEntries.map { it.id })
        assertTrue(lastPage.canGoToPreviousHistoryPage)
        assertFalse(lastPage.canGoToNextHistoryPage)

        // Simulates deleting entries while viewing page 2 so only 5 entries (1 page) remain.
        val shrunk = lastPage.copy(todayEntries = twelveEntries.take(5))
        assertEquals(1, shrunk.totalHistoryPages)
        assertEquals(0, shrunk.currentHistoryPage)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), shrunk.pagedTodayEntries.map { it.id })
        assertFalse(shrunk.canGoToPreviousHistoryPage)
        assertFalse(shrunk.canGoToNextHistoryPage)

        val empty = pageZero.copy(todayEntries = emptyList(), historyPage = 2)
        assertEquals(1, empty.totalHistoryPages)
        assertEquals(0, empty.currentHistoryPage)
        assertTrue(empty.pagedTodayEntries.isEmpty())
    }

    @Test
    fun moodTapMotionSpecsCoverEveryMoodAndReturnCleanlyToRest() {
        val sampleFractions = (0..20).map { it / 20f }

        for (mood in MoodType.entries) {
            val spec = MoodTapMotionCatalog.specFor(mood)
            assertTrue("Duration must be positive for $mood", spec.durationMillis in 350..800)

            val start = spec.sampleAt(0f)
            val end = spec.sampleAt(1f)
            for (boundary in listOf(start, end)) {
                assertEquals(1f, boundary.scaleX, 0.0001f)
                assertEquals(1f, boundary.scaleY, 0.0001f)
                assertEquals(0f, boundary.offsetXDp, 0.0001f)
                assertEquals(0f, boundary.offsetYDp, 0.0001f)
                assertEquals(0f, boundary.rotationDeg, 0.0001f)
                assertEquals(1f, boundary.iconAlpha, 0.0001f)
            }

            val frames = sampleFractions.map(spec::sampleAt)
            val maxScale = frames.maxOf { maxOf(it.scaleX, it.scaleY) }
            assertTrue("Expected noticeable pop scale for $mood", maxScale > 1.10f)
            assertTrue(frames.all { it.iconAlpha in 0.5f..1f })
        }

        // Expressive signatures by mood family
        val senangMinY = (0..20).minOf { MoodTapMotionCatalog.specFor(MoodType.SENANG).sampleAt(it / 20f).offsetYDp }
        val semangatMinY = (0..20).minOf { MoodTapMotionCatalog.specFor(MoodType.SEMANGAT).sampleAt(it / 20f).offsetYDp }
        assertTrue(senangMinY < -5f)
        assertTrue(semangatMinY < -6f)
        assertEquals(MoodBurstStyle.JOYFUL_SPARKS, MoodTapMotionCatalog.specFor(MoodType.SENANG).burstStyle)

        val marahMaxShake = (0..20).maxOf {
            kotlin.math.abs(MoodTapMotionCatalog.specFor(MoodType.MARAH).sampleAt(it / 20f).offsetXDp)
        }
        assertTrue(marahMaxShake > 3f)
        assertEquals(MoodBurstStyle.DOUBLE_PULSE, MoodTapMotionCatalog.specFor(MoodType.MARAH).burstStyle)

        val lelahMaxSlump = (0..20).maxOf { MoodTapMotionCatalog.specFor(MoodType.LELAH).sampleAt(it / 20f).offsetYDp }
        assertTrue(lelahMaxSlump > 4f)
        assertEquals(MoodBurstStyle.SOFT_RIPPLE, MoodTapMotionCatalog.specFor(MoodType.LELAH).burstStyle)
    }

    @Test
    fun computeStreakCountsConsecutiveDaysAndCarriesOverYesterday() {
        fun entryOn(daysOffset: Long, hour: Int = 10): MoodEntry = MoodEntry(
            id = daysOffset + 100L,
            mood = MoodType.SENANG,
            timestamp = sampleDate.plusDays(daysOffset).atTime(hour, 0).atZone(jakartaZone).toInstant().toEpochMilli()
        )

        val emptyStreak = MoodTrackerLogic.computeStreak(emptyList(), today = sampleDate, zone = jakartaZone)
        assertEquals(0, emptyStreak.currentDays)
        assertFalse(emptyStreak.checkedInToday)

        // Multiple entries on today + yesterday + two days ago = 3-day streak checked in today
        val threeDayActive = MoodTrackerLogic.computeStreak(
            entries = listOf(entryOn(0, 8), entryOn(0, 20), entryOn(-1), entryOn(-2), entryOn(-5)),
            today = sampleDate,
            zone = jakartaZone
        )
        assertEquals(3, threeDayActive.currentDays)
        assertTrue(threeDayActive.checkedInToday)

        // Yesterday + two days ago, nothing today yet = 2-day streak waiting for today's check-in
        val pendingToday = MoodTrackerLogic.computeStreak(
            entries = listOf(entryOn(-1), entryOn(-2)),
            today = sampleDate,
            zone = jakartaZone
        )
        assertEquals(2, pendingToday.currentDays)
        assertFalse(pendingToday.checkedInToday)

        // Missed yesterday breaks the streak
        val broken = MoodTrackerLogic.computeStreak(
            entries = listOf(entryOn(-2), entryOn(-3)),
            today = sampleDate,
            zone = jakartaZone
        )
        assertEquals(0, broken.currentDays)
        assertFalse(broken.checkedInToday)

        // Future timestamps should not count toward today's streak
        val futureOnly = MoodTrackerLogic.computeStreak(
            entries = listOf(entryOn(1)),
            today = sampleDate,
            zone = jakartaZone
        )
        assertEquals(0, futureOnly.currentDays)
        assertFalse(futureOnly.checkedInToday)
    }

    @Test
    fun toggleContextTagAddsRemovesAndRespectsMaxLength() {
        val step1 = MoodTrackerLogic.toggleContextTag("", "Kuliah")
        assertEquals("Kuliah", step1)
        assertTrue(MoodTrackerLogic.hasContextTag(step1, "kuliah"))

        val step2 = MoodTrackerLogic.toggleContextTag(step1, "Tugas")
        assertEquals("Kuliah, Tugas", step2)
        assertTrue(MoodTrackerLogic.hasContextTag(step2, "Tugas"))

        val step3 = MoodTrackerLogic.toggleContextTag(step2, "Kuliah")
        assertEquals("Tugas", step3)
        assertFalse(MoodTrackerLogic.hasContextTag(step3, "Kuliah"))

        val step4 = MoodTrackerLogic.toggleContextTag(step3, "Tugas")
        assertEquals("", step4)

        val mixed = MoodTrackerLogic.toggleContextTag("Capek setelah praktikum", "Kurang tidur")
        assertEquals("Capek setelah praktikum, Kurang tidur", mixed)

        val nearLimit = "a".repeat(MoodTrackerLogic.MAX_NOTE_LENGTH - 3)
        assertEquals(nearLimit, MoodTrackerLogic.toggleContextTag(nearLimit, "Istirahat"))
    }

    private class FakeMoodRemoteDataSource : MoodRemoteDataSource {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryDto>>(emptyList())

        override fun observeAll(userId: String): Flow<List<MoodEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun save(entry: MoodEntryDto, userId: String): Long? {
            val id = if (entry.id == 0L) nextId++ else entry.id
            val saved = entry.copy(id = id, userId = userId)
            items.value = (items.value.filterNot { it.id == id && it.userId == userId } + saved)
                .sortedWith(compareByDescending<MoodEntryDto> { it.timestamp }.thenByDescending { it.id })
            return id
        }

        override suspend fun upsertBatch(entries: List<MoodEntryDto>, userId: String) {
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            items.value = (items.value.filterNot { it.userId == userId && it.id in byId } + byId.values)
                .sortedWith(compareByDescending<MoodEntryDto> { it.timestamp }.thenByDescending { it.id })
        }

        override suspend fun update(
            entryId: Long,
            userId: String,
            moodType: String,
            note: String,
            updatedAt: Long
        ): Boolean {
            var updated = false
            items.value = items.value.map { entry ->
                if (entry.id == entryId && entry.userId == userId) {
                    updated = true
                    entry.copy(moodType = moodType, note = note, updatedAt = updatedAt)
                } else {
                    entry
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

package com.hariku.feature.mood

import com.hariku.feature.mood.data.local.MoodDao
import com.hariku.feature.mood.data.local.MoodEntryEntity
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.presentation.MoodBurstStyle
import com.hariku.feature.mood.presentation.MoodTapMotionCatalog
import com.hariku.feature.mood.presentation.QuickMoodUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    fun saveObserveAndDeleteFlowWorksEndToEndWithFakeDao() = runBlocking {
        val fakeDao = FakeMoodDao()
        val repository = MoodRepositoryImpl(fakeDao)
        val observeUseCase = ObserveMoodEntriesUseCase(repository)
        val saveUseCase = SaveQuickMoodUseCase(repository)
        val deleteUseCase = DeleteMoodEntryUseCase(repository)

        val timestamp = sampleDate.atTime(9, 15).atZone(jakartaZone).toInstant().toEpochMilli()
        val saveResult = saveUseCase(
            mood = MoodType.SENANG,
            note = "   Pagi yang produktif   ",
            timestamp = timestamp
        )

        assertTrue(saveResult.isSuccess)
        val savedId = saveResult.getOrThrow()

        // Inject an unknown label row directly in storage to ensure graceful filtering.
        fakeDao.insert(MoodEntryEntity(moodType = "UnknownLegacy", timestamp = timestamp))

        val observed = observeUseCase().first()
        assertEquals(1, observed.size)
        assertEquals(savedId, observed.first().id)
        assertEquals(MoodType.SENANG, observed.first().mood)
        assertEquals("Pagi yang produktif", observed.first().note)

        val deleteResult = deleteUseCase(savedId)
        assertTrue(deleteResult.isSuccess)
        assertTrue(observeUseCase().first().isEmpty())
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

    private class FakeMoodDao : MoodDao {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insert(entry: MoodEntryEntity): Long {
            val id = nextId++
            val saved = entry.copy(id = id)
            items.value = (items.value + saved)
                .sortedWith(compareByDescending<MoodEntryEntity> { it.timestamp }.thenByDescending { it.id })
            return id
        }

        override fun observeEntries(): Flow<List<MoodEntryEntity>> = items

        override suspend fun deleteById(entryId: Long) {
            items.value = items.value.filterNot { it.id == entryId }
        }
    }
}

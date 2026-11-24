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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

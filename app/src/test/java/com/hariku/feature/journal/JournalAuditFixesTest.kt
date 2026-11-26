package com.hariku.feature.journal

import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import com.hariku.feature.journal.data.provider.PsychologicalContextProviderImpl
import com.hariku.feature.journal.domain.engine.AdaptivePromptEngine
import com.hariku.feature.journal.domain.engine.AnxietyLexiconAnalyzer
import com.hariku.feature.journal.domain.engine.VolatilityCalculator
import com.hariku.feature.journal.domain.model.AdsLevel
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.EviLevel
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.model.PsychologicalMetrics
import com.hariku.feature.journal.domain.repository.JournalRepository
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.ObserveJournalEntriesUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import com.hariku.feature.journal.presentation.JournalEvent
import com.hariku.feature.journal.presentation.JournalViewModel
import com.hariku.feature.journal.presentation.create.GuidedJournalEditorViewModel
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.repository.MoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class JournalAuditFixesTest {

    @Test
    fun psychologicalContextProvider_updatesAdsWhenUserIdEmitsLater() = runBlocking {
        val userIdFlow = MutableStateFlow<String?>(null)
        val provider = PsychologicalContextProviderImpl(
            moodRepository = EmptyMoodRepository(),
            chatbotRepository = FakeAnxiousChatbotRepository(),
            userIdFlow = userIdFlow,
            defaultDispatcher = Dispatchers.Unconfined
        )

        val collected = mutableListOf<com.hariku.feature.journal.domain.model.PsychologicalMetrics>()
        val job = launch(Dispatchers.Unconfined) {
            provider.observeMetrics().take(2).toList(collected)
        }

        assertEquals(1, collected.size)
        assertTrue(collected[0].isDefaultFallback)
        assertEquals(AdsLevel.RENDAH, collected[0].adsLevel)

        userIdFlow.value = "u1"
        job.join()

        assertEquals(2, collected.size)
        assertFalse(collected[1].isDefaultFallback)
        assertEquals(AdsLevel.TINGGI, collected[1].adsLevel)
    }

    @Test
    fun journalViewModel_togglePin_emitsAccurateMessageEvenWhenFlowUpdatesFirst() = runBlocking {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repo = FakeRaceJournalRepository(
                initial = listOf(
                    JournalEntry(
                        id = 1L,
                        title = "Catatan Pertama",
                        content = "Isi catatan",
                        dateMillis = 1000L,
                        isPinned = false
                    )
                )
            )
            val viewModel = JournalViewModel(
                observeJournalEntriesUseCase = ObserveJournalEntriesUseCase(repo),
                getAdaptivePromptUseCase = GetAdaptivePromptUseCase(
                    contextProvider = PsychologicalContextProviderImpl(
                        moodRepository = EmptyMoodRepository(),
                        chatbotRepository = FakeAnxiousChatbotRepository(),
                        userIdFlow = flowOf(null),
                        defaultDispatcher = Dispatchers.Unconfined
                    ),
                    journalRepository = repo
                ),
                togglePinJournalUseCase = TogglePinJournalUseCase(repo),
                deleteJournalEntryUseCase = DeleteJournalEntryUseCase(repo),
                coroutineScopeOverride = testScope
            )

            val collectorJob = testScope.launch {
                viewModel.uiState.collect { }
            }
            val firstState = viewModel.uiState.first { !it.isLoading }
            assertEquals(1, firstState.allEntries.size)

            viewModel.onTogglePin(1L)
            val event = viewModel.events.first()
            assertEquals(JournalEvent.ShowMessage("Jurnal disematkan di atas"), event)

            collectorJob.cancel()
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun mergeUtcDateWithLocalTime_preservesCalendarDateAndLocalTimeAcrossZones() {
        val utcMidnight = LocalDate.of(2026, 11, 24).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val jakartaZone = ZoneId.of("Asia/Jakarta")
        val jakartaRef = LocalDate.of(2026, 11, 20).atTime(LocalTime.of(14, 30)).atZone(jakartaZone).toInstant().toEpochMilli()
        val jakartaMerged = Instant.ofEpochMilli(
            GuidedJournalEditorViewModel.mergeUtcDateWithLocalTime(utcMidnight, jakartaRef, jakartaZone)
        ).atZone(jakartaZone)
        assertEquals(LocalDate.of(2026, 11, 24), jakartaMerged.toLocalDate())
        assertEquals(LocalTime.of(14, 30), jakartaMerged.toLocalTime())

        val nyZone = ZoneId.of("America/New_York")
        val nyRef = LocalDate.of(2026, 11, 20).atTime(LocalTime.of(14, 30)).atZone(nyZone).toInstant().toEpochMilli()
        val nyMerged = Instant.ofEpochMilli(
            GuidedJournalEditorViewModel.mergeUtcDateWithLocalTime(utcMidnight, nyRef, nyZone)
        ).atZone(nyZone)
        assertEquals(LocalDate.of(2026, 11, 24), nyMerged.toLocalDate())
        assertEquals(LocalTime.of(14, 30), nyMerged.toLocalTime())
    }

    @Test
    fun volatilityCalculator_ignoresMoodOutsideTrailingSevenDays() {
        val now = 1_764_000_000_000L
        val oldEntry = MoodEntry(mood = MoodType.SEDIH, timestamp = now - 8L * 24 * 60 * 60 * 1000)

        assertEquals(null, VolatilityCalculator.calculateSma7(listOf(oldEntry), now))
        assertEquals(0.0, VolatilityCalculator.calculateEvi(listOf(oldEntry), now), 0.001)
    }

    @Test
    fun volatilityCalculator_singleMoodIsStableButOpposingRecentMoodsAreVolatile() {
        val now = 1_764_000_000_000L
        val day = 24L * 60 * 60 * 1000
        val oneMood = listOf(MoodEntry(mood = MoodType.CEMAS, timestamp = now - 1_000L))
        assertEquals(0.0, VolatilityCalculator.calculateEvi(oneMood, now), 0.001)
        assertEquals(EviLevel.STABIL, VolatilityCalculator.classify(VolatilityCalculator.calculateEvi(oneMood, now)))

        val contrast = listOf(
            MoodEntry(mood = MoodType.SENANG, timestamp = now - day),
            MoodEntry(mood = MoodType.CEMAS, timestamp = now - 1_000L)
        )
        val evi = VolatilityCalculator.calculateEvi(contrast, now)
        assertEquals(2.0, evi, 0.001)
        assertEquals(EviLevel.VOLATIL, VolatilityCalculator.classify(evi))
    }

    @Test
    fun anxietyLexiconAnalyzer_doesNotCountStandalonePutusOrAsaAsAnxiety() {
        assertEquals(0.0, AnxietyLexiconAnalyzer.analyze("putus hubungan"), 0.001)
        assertEquals(0.0, AnxietyLexiconAnalyzer.analyze("asa hidup"), 0.001)
        assertEquals(0.5, AnxietyLexiconAnalyzer.analyze("putus asa"), 0.001)
        assertEquals(0.25, AnxietyLexiconAnalyzer.analyze("aku merasa putus asa"), 0.001)
    }

    @Test
    fun guidedEditor_freeWriteKeepsGeneralTemplateAfterAdaptiveRecommendationEmits() {
        val recommendation = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = PsychologicalMetrics.NEUTRAL,
            latestTag = JournalCategoryTag.UMUM,
            selectedFeelings = emptySet(),
            templateOverride = CbtTemplateType.GENERAL_REFLECTION
        )
        assertEquals(CbtTemplateType.GENERAL_REFLECTION, recommendation.template)
        assertTrue(recommendation.prompts.first().isNotBlank())
    }

    @Test
    fun anxietyLexiconAnalyzer_appliesTimeDecayForOldMessages() {
        val now = 1_764_000_000_000L
        val text = "cemas panik takut gelisah" // 4/4 = 1.0 raw ADS

        val recent = AnxietyLexiconAnalyzer.analyzeLatestWithDecay(
            messages = listOf(AnxietyLexiconAnalyzer.TimedMessage(text, now - 60_000L)),
            nowMillis = now
        )
        assertEquals(1.0, recent, 0.001)

        val threeDaysOld = AnxietyLexiconAnalyzer.analyzeLatestWithDecay(
            messages = listOf(AnxietyLexiconAnalyzer.TimedMessage(text, now - 3L * 24 * 60 * 60 * 1000)),
            nowMillis = now
        )
        assertEquals(0.5, threeDaysOld, 0.001)

        val tenDaysOld = AnxietyLexiconAnalyzer.analyzeLatestWithDecay(
            messages = listOf(AnxietyLexiconAnalyzer.TimedMessage(text, now - 10L * 24 * 60 * 60 * 1000)),
            nowMillis = now
        )
        assertEquals(0.0, tenDaysOld, 0.001)
    }

    @Test
    fun adaptivePromptEngine_mapsNaturalContextPhrasesAndIgnoresUmum() {
        assertEquals(null, AdaptivePromptEngine.resolveContextTag(JournalCategoryTag.UMUM))
        assertEquals(
            "urusan kuliah atau tugasmu",
            AdaptivePromptEngine.resolveContextTag(JournalCategoryTag.AKADEMIK)
        )
        val rec = AdaptivePromptEngine.recommend(null, JournalCategoryTag.UMUM)
        assertEquals(AdaptivePromptEngine.CONTEXT_FALLBACK, rec.contextTag)
        assertFalse(rec.prompt.contains(" umum ", ignoreCase = true))
    }

    @Test
    fun adaptivePromptEngine_positiveTonePreservesManuallyChosenTemplatePrompts() {
        val recommendation = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = PsychologicalMetrics.NEUTRAL,
            latestTag = JournalCategoryTag.UMUM,
            selectedFeelings = setOf("Senang"),
            templateOverride = CbtTemplateType.THOUGHT_RECORD
        )

        assertEquals(CbtTemplateType.THOUGHT_RECORD, recommendation.template)
        assertTrue(recommendation.prompts.any { it.contains("Pikiran otomatis", ignoreCase = true) })
        assertFalse(recommendation.prompts.any { it.contains("Satu hal yang disyukuri", ignoreCase = true) })
    }

    @Test
    fun adaptivePromptEngine_weightedScoringAndValenceResolution() {
        // 1. Murni positif tidak memaksakan tag cemas lama
        val positiveResult = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = PsychologicalMetrics(
                eviScore = 0.15,
                eviLevel = EviLevel.STABIL,
                adsScore = 0.5,
                adsLevel = AdsLevel.TINGGI
            ),
            latestTag = JournalCategoryTag.KECEMASAN,
            selectedFeelings = setOf("Senang", "Termotivasi", "Damai")
        )
        assertEquals(AdaptivePromptEngine.PromptTone.POSITIVE, positiveResult.tone)
        assertEquals(CbtTemplateType.GENERAL_REFLECTION, positiveResult.template)
        assertFalse(positiveResult.prompts.first().contains("kecemasan", ignoreCase = true))

        // 2. Kombinasi berat (Lelah + Cemas + Sedih) eskalasi ke Behavioral Activation (6 vs 3)
        val heavyCombo = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = PsychologicalMetrics.NEUTRAL,
            latestTag = JournalCategoryTag.UMUM,
            selectedFeelings = setOf("Lelah", "Cemas", "Sedih")
        )
        assertEquals(AdaptivePromptEngine.PromptTone.SUPPORTIVE, heavyCombo.tone)
        assertEquals(CbtTemplateType.BEHAVIORAL_ACTIVATION, heavyCombo.template)

        // 3. Campuran (Cemas + Ingin Fokus) + catatan bebas menjadi prioritas konteks #1
        val mixedCombo = AdaptivePromptEngine.recommendWithCheckIn(
            metrics = PsychologicalMetrics.NEUTRAL,
            latestTag = JournalCategoryTag.AKADEMIK,
            selectedFeelings = setOf("Cemas", "Ingin Fokus"),
            customFeelingNote = "presentasi besok pagi"
        )
        assertEquals(AdaptivePromptEngine.PromptTone.MIXED, mixedCombo.tone)
        assertEquals(CbtTemplateType.COGNITIVE_REFRAMING, mixedCombo.template)
        assertTrue(mixedCombo.prompts.first().contains("presentasi besok pagi"))
    }

    private class FakeRaceJournalRepository(initial: List<JournalEntry>) : JournalRepository {
        private val entriesFlow = MutableStateFlow(initial)

        override fun observeEntries(): Flow<List<JournalEntry>> = entriesFlow
        override fun observeLatestTag(): Flow<JournalCategoryTag?> = flowOf(null)
        override suspend fun getById(id: Long): Result<JournalEntry> =
            entriesFlow.value.firstOrNull { it.id == id }?.let { Result.success(it) }
                ?: Result.failure(NoSuchElementException())

        override suspend fun save(entry: JournalEntry): Result<Long> = Result.success(entry.id)
        override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)
        override suspend fun togglePin(id: Long): Result<Unit> {
            // Simulasikan Room memancarkan data baru SEBELUM togglePin mengembalikan Result.
            entriesFlow.value = entriesFlow.value.map {
                if (it.id == id) it.copy(isPinned = !it.isPinned) else it
            }
            return Result.success(Unit)
        }
    }

    private class EmptyMoodRepository : MoodRepository {
        override fun observeMoodEntries(): Flow<List<MoodEntry>> = flowOf(emptyList())
        override suspend fun saveMood(mood: MoodType, note: String, timestamp: Long): Result<Long> =
            Result.success(1L)
        override suspend fun updateMood(entryId: Long, mood: MoodType, note: String): Result<Unit> =
            Result.success(Unit)
        override suspend fun deleteMood(entryId: Long): Result<Unit> = Result.success(Unit)
    }

    private class FakeAnxiousChatbotRepository : ChatbotRepository {
        override fun getChatbots(userId: String): Flow<Result<List<Chatbot>>> =
            flowOf(Result.success(emptyList()))

        override fun getChatbotsWithHistory(userId: String): Flow<Result<List<ChatbotWithHistory>>> =
            flowOf(
                Result.success(
                    listOf(
                        ChatbotWithHistory(
                            chatbot = Chatbot(id = "bot-1", userId = userId),
                            lastMessage = "cemas panik",
                            lastMessageTime = 1000L
                        )
                    )
                )
            )

        override suspend fun addChatbot(chatbot: Chatbot): Result<Unit> = Result.success(Unit)
        override suspend fun deleteChatbot(chatbotId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getChatbotById(chatbotId: String, userId: String): Result<Chatbot> =
            Result.success(Chatbot(id = chatbotId, userId = userId))

        override fun getChatMessages(chatbotId: String, userId: String): Flow<Result<List<ChatMessage>>> =
            flowOf(
                Result.success(
                    listOf(
                        ChatMessage(
                            id = "m1",
                            chatbotId = chatbotId,
                            userId = userId,
                            message = "aku cemas panik takut gelisah",
                            isFromUser = true,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                )
            )

        override suspend fun sendMessage(message: ChatMessage): Result<Unit> = Result.success(Unit)
        override suspend fun markMessagesAsRead(chatbotId: String, userId: String): Result<Unit> = Result.success(Unit)
    }
}

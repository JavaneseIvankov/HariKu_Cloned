package com.hariku.feature.journal.data.provider

import com.google.firebase.auth.FirebaseAuth
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import com.hariku.feature.journal.domain.engine.AnxietyLexiconAnalyzer
import com.hariku.feature.journal.domain.engine.VolatilityCalculator
import com.hariku.feature.journal.domain.model.PsychologicalMetrics
import com.hariku.feature.journal.domain.provider.PsychologicalContextProvider
import com.hariku.feature.mood.domain.repository.MoodRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Mengombinasikan data Mood Tracker (EVI) dan Chat Assistant (ADS) menjadi [PsychologicalMetrics].
 * Melakukan fallback ke [PsychologicalMetrics.NEUTRAL] (`isDefaultFallback = true`) apabila data
 * masih kosong atau terjadi error (mis. Firebase belum aktif/pengguna belum login).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PsychologicalContextProviderImpl(
    private val moodRepository: MoodRepository,
    private val chatbotRepository: ChatbotRepository,
    private val currentUserIdProvider: () -> String? = {
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
    },
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : PsychologicalContextProvider {

    override fun observeMetrics(): Flow<PsychologicalMetrics> {
        val moodFlow = runCatching {
            moodRepository.observeMoodEntries()
                .catch { emit(emptyList()) }
        }.getOrElse { flowOf(emptyList()) }

        val chatMessagesFlow = observeLatestUserMessages()

        return combine(moodFlow, chatMessagesFlow) { moodEntries, userMessages ->
            if (moodEntries.isEmpty() && userMessages.isEmpty()) {
                PsychologicalMetrics.NEUTRAL
            } else {
                val eviScore = VolatilityCalculator.calculateEvi(moodEntries)
                val eviLevel = VolatilityCalculator.classify(eviScore)
                val adsScore = AnxietyLexiconAnalyzer.analyzeLatest(userMessages)
                val adsLevel = AnxietyLexiconAnalyzer.classify(adsScore)
                PsychologicalMetrics(
                    eviScore = eviScore,
                    eviLevel = eviLevel,
                    adsScore = adsScore,
                    adsLevel = adsLevel,
                    isDefaultFallback = false
                )
            }
        }
            .catch { emit(PsychologicalMetrics.NEUTRAL) }
            .flowOn(defaultDispatcher)
    }

    private fun observeLatestUserMessages(): Flow<List<String>> {
        val userId = runCatching { currentUserIdProvider() }.getOrNull()
        if (userId.isNullOrBlank()) return flowOf(emptyList())

        return runCatching {
            chatbotRepository.getChatbotsWithHistory(userId)
                .flatMapLatest { result ->
                    val latestChatbotId = result.getOrNull()
                        ?.maxByOrNull { it.lastMessageTime }
                        ?.chatbot
                        ?.id
                    if (latestChatbotId.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        chatbotRepository.getChatMessages(latestChatbotId, userId)
                            .map { messagesResult ->
                                messagesResult.getOrDefault(emptyList())
                                    .filter { it.isFromUser }
                                    .sortedBy { it.timestamp }
                                    .map { it.message }
                            }
                            .catch { emit(emptyList()) }
                    }
                }
                .catch { emit(emptyList()) }
        }.getOrElse { flowOf(emptyList()) }
    }
}

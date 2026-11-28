package com.hariku.feature.session

import com.hariku.core.auth.CurrentUserSession
import com.hariku.feature.auth.domain.model.AuthUser
import com.hariku.feature.auth.domain.repository.AuthRepository
import com.hariku.feature.auth.domain.usecase.GetCurrentUserUseCase
import com.hariku.feature.auth.domain.usecase.LogoutUseCase
import com.hariku.feature.auth.domain.usecase.ObserveAuthStateUseCase
import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import com.hariku.feature.chatbot.domain.usecase.GetChatMessagesUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotByIdUseCase
import com.hariku.feature.chatbot.domain.usecase.GetChatbotsWithHistoryUseCase
import com.hariku.feature.chatbot.domain.usecase.MarkMessagesAsReadUseCase
import com.hariku.feature.chatbot.domain.usecase.SendMessageUseCase
import com.hariku.feature.chatbot.presentation.ChatbotViewModel
import com.hariku.feature.chatbot.presentation.detail.ChatbotDetailViewModel
import com.hariku.feature.home.presentation.HomeViewModel
import com.hariku.feature.journal.data.dto.JournalEntryDto
import com.hariku.feature.journal.data.provider.PsychologicalContextProviderImpl
import com.hariku.feature.journal.data.remote.JournalRemoteDataSource
import com.hariku.feature.journal.data.repository.JournalRepositoryImpl
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.domain.usecase.DeleteJournalEntryUseCase
import com.hariku.feature.journal.domain.usecase.GetAdaptivePromptUseCase
import com.hariku.feature.journal.domain.usecase.GetJournalByIdUseCase
import com.hariku.feature.journal.domain.usecase.ObserveJournalEntriesUseCase
import com.hariku.feature.journal.domain.usecase.TogglePinJournalUseCase
import com.hariku.feature.journal.presentation.JournalViewModel
import com.hariku.feature.journal.presentation.detail.JournalDetailViewModel
import com.hariku.feature.mood.data.local.MoodDao
import com.hariku.feature.mood.data.local.MoodEntryEntity
import com.hariku.feature.mood.data.repository.MoodRepositoryImpl
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.usecase.DeleteMoodEntryUseCase
import com.hariku.feature.mood.domain.usecase.ObserveMoodEntriesUseCase
import com.hariku.feature.mood.domain.usecase.SaveQuickMoodUseCase
import com.hariku.feature.mood.domain.usecase.UpdateMoodEntryUseCase
import com.hariku.feature.mood.presentation.QuickMoodViewModel
import com.hariku.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossSessionStateTest {

    @Test
    fun currentUserSession_sharesSingleReactiveStreamAcrossTransitions() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val source = MutableStateFlow<String?>("user_A")
            val session = CurrentUserSession(source = source, initialUserId = "user_A", scope = scope)

            assertEquals("user_A", session.currentUserIdOrNull())
            assertEquals("user_A", session.userIdFlow.value)

            source.value = null
            assertNull(session.currentUserIdOrNull())

            source.value = "user_B"
            assertEquals("user_B", session.currentUserIdOrNull())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun homeAndProfileViewModels_updateReactivelyOnSessionChange() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userA = AuthUser(uid = "uid_A", email = "a@test.com", name = "Akun A")
            val userB = AuthUser(uid = "uid_B", email = "b@test.com", name = "Akun B")
            val authRepo = FakeAuthRepository(initialUser = userA)

            val homeVm = HomeViewModel(
                observeAuthStateUseCase = ObserveAuthStateUseCase(authRepo),
                getCurrentUserUseCase = GetCurrentUserUseCase(authRepo),
                coroutineScopeOverride = scope
            )
            val profileVm = ProfileViewModel(
                observeAuthStateUseCase = ObserveAuthStateUseCase(authRepo),
                getCurrentUserUseCase = GetCurrentUserUseCase(authRepo),
                logoutUseCase = LogoutUseCase(authRepo),
                coroutineScopeOverride = scope
            )

            val homeCollector = scope.launch { homeVm.currentUser.collect {} }
            val profileCollector = scope.launch { profileVm.currentUser.collect {} }

            assertEquals(userA, homeVm.currentUser.value)
            assertEquals(userA, profileVm.currentUser.value)

            profileVm.logout()
            assertNull(homeVm.currentUser.value)
            assertNull(profileVm.currentUser.value)

            authRepo.emitUser(userB)
            assertEquals(userB, homeVm.currentUser.value)
            assertEquals(userB, profileVm.currentUser.value)

            homeCollector.cancel()
            profileCollector.cancel()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun chatbotViewModel_resetsAndSwitchesBotsAcrossAccounts() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = FakeMultiAccountChatbotRepository()
            val vm = ChatbotViewModel(
                getChatbotsWithHistoryUseCase = GetChatbotsWithHistoryUseCase(repo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )

            assertFalse(vm.uiState.isLoading)
            assertEquals(listOf("Bot A"), vm.uiState.chatbots.map { it.chatbot.name })

            userIdFlow.value = null
            assertFalse(vm.uiState.isLoading)
            assertTrue(vm.uiState.chatbots.isEmpty())

            userIdFlow.value = "user_B"
            assertFalse(vm.uiState.isLoading)
            assertEquals(listOf("Bot B"), vm.uiState.chatbots.map { it.chatbot.name })
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun chatbotDetailViewModel_clearsMessagesOnAccountSwitchAndRejectsSignedOutSend() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val repo = FakeMultiAccountChatbotRepository()
            val vm = ChatbotDetailViewModel(
                getChatMessagesUseCase = GetChatMessagesUseCase(repo),
                getChatbotByIdUseCase = GetChatbotByIdUseCase(repo),
                sendMessageUseCase = SendMessageUseCase(repo),
                markMessagesAsReadUseCase = MarkMessagesAsReadUseCase(repo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )

            vm.initialize("bot-shared")
            assertEquals("Bot A", vm.uiState.chatbot?.name)
            assertEquals(listOf("Halo dari A"), vm.uiState.messages.map { it.message })

            userIdFlow.value = "user_B"
            assertEquals("Bot B", vm.uiState.chatbot?.name)
            assertEquals(listOf("Halo dari B"), vm.uiState.messages.map { it.message })

            userIdFlow.value = null
            assertNull(vm.uiState.chatbot)
            assertTrue(vm.uiState.messages.isEmpty())
            assertEquals("User tidak terautentikasi", vm.uiState.error)

            vm.clearError()
            vm.sendMessage("Coba kirim saat logout")
            assertEquals("User tidak terautentikasi", vm.uiState.error)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun quickMoodViewModel_resetsTransientUiStateOnAccountSwitch() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val moodRepo = MoodRepositoryImpl(FakeMoodDao(), userIdFlow)
            val vm = QuickMoodViewModel(
                observeMoodEntriesUseCase = ObserveMoodEntriesUseCase(moodRepo),
                saveQuickMoodUseCase = SaveQuickMoodUseCase(moodRepo),
                deleteMoodEntryUseCase = DeleteMoodEntryUseCase(moodRepo),
                updateMoodEntryUseCase = UpdateMoodEntryUseCase(moodRepo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )

            vm.onNoteInputChanged("Catatan sementara A")
            vm.onToggleNoteInput()
            vm.onSelectMood(MoodType.SENANG)
            val savedA = vm.uiState.value.latestTodayEntry
            assertNotNull(savedA)
            vm.onStartEditEntry(savedA!!)
            vm.onEditNoteChanged("Sedang edit A")

            assertNotNull(vm.uiState.value.editingEntry)
            assertEquals("Sedang edit A", vm.uiState.value.editNote)

            // Switch to user_B while edit dialog / transient state was open
            userIdFlow.value = "user_B"

            val stateB = vm.uiState.value
            assertNull(stateB.editingEntry)
            assertNull(stateB.editMood)
            assertEquals("", stateB.editNote)
            assertEquals("", stateB.noteInput)
            assertFalse(stateB.isNoteExpanded)
            assertNull(stateB.lastSavedEntryId)
            assertNull(stateB.feedbackMessage)
            assertTrue(stateB.todayEntries.isEmpty())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun journalViewModels_resetFiltersAndReloadDetailOnAccountSwitch() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val userIdFlow = MutableStateFlow<String?>("user_A")
            val journalRepo = JournalRepositoryImpl(FakeJournalRemoteDataSource(), userIdFlow, Dispatchers.Unconfined)
            val moodRepo = MoodRepositoryImpl(FakeMoodDao(), userIdFlow)
            val chatbotRepo = FakeMultiAccountChatbotRepository()

            val entryIdA = journalRepo.save(
                JournalEntry(
                    title = "Jurnal A",
                    content = "Konten milik A",
                    dateMillis = 1_000L,
                    tag = JournalCategoryTag.AKADEMIK
                )
            ).getOrThrow()

            val journalVm = JournalViewModel(
                observeJournalEntriesUseCase = ObserveJournalEntriesUseCase(journalRepo),
                getAdaptivePromptUseCase = GetAdaptivePromptUseCase(
                    contextProvider = PsychologicalContextProviderImpl(
                        moodRepository = moodRepo,
                        chatbotRepository = chatbotRepo,
                        userIdFlow = userIdFlow,
                        defaultDispatcher = Dispatchers.Unconfined
                    ),
                    journalRepository = journalRepo
                ),
                togglePinJournalUseCase = TogglePinJournalUseCase(journalRepo),
                deleteJournalEntryUseCase = DeleteJournalEntryUseCase(journalRepo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )
            val detailVm = JournalDetailViewModel(
                getJournalByIdUseCase = GetJournalByIdUseCase(journalRepo),
                togglePinJournalUseCase = TogglePinJournalUseCase(journalRepo),
                deleteJournalEntryUseCase = DeleteJournalEntryUseCase(journalRepo),
                userIdFlow = userIdFlow,
                coroutineScopeOverride = scope
            )

            val collector = scope.launch { journalVm.uiState.collect {} }
            journalVm.onSearchQueryChange("Jurnal A")
            journalVm.onSelectCategory(JournalCategoryTag.AKADEMIK)
            assertEquals("Jurnal A", journalVm.uiState.value.searchQuery)
            assertEquals(JournalCategoryTag.AKADEMIK, journalVm.uiState.value.selectedCategory)
            assertEquals(1, journalVm.uiState.value.allEntries.size)

            detailVm.loadJournal(entryIdA)
            assertEquals("Jurnal A", detailVm.uiState.value.entry?.title)

            // Switch to user_B: filters reset, entries empty, detail clears A's entry
            userIdFlow.value = "user_B"

            assertEquals("", journalVm.uiState.value.searchQuery)
            assertNull(journalVm.uiState.value.selectedCategory)
            assertTrue(journalVm.uiState.value.allEntries.isEmpty())

            assertNull(detailVm.uiState.value.entry)
            assertNotNull(detailVm.uiState.value.errorMessage)

            // Switch back to user_A: detail reloads A's entry automatically
            userIdFlow.value = "user_A"
            assertEquals("Jurnal A", detailVm.uiState.value.entry?.title)
            assertNull(detailVm.uiState.value.errorMessage)

            collector.cancel()
        } finally {
            scope.cancel()
        }
    }

    private class FakeAuthRepository(initialUser: AuthUser?) : AuthRepository {
        private val state = MutableStateFlow(initialUser)

        fun emitUser(user: AuthUser?) {
            state.value = user
        }

        override suspend fun login(email: String, password: String): Result<AuthUser> =
            state.value?.let { Result.success(it) } ?: Result.failure(IllegalStateException("no user"))

        override suspend fun signUp(email: String, password: String, name: String): Result<AuthUser> =
            login(email, password)

        override fun logout() {
            state.value = null
        }

        override fun getCurrentUser(): AuthUser? = state.value

        override fun getAuthState(): Flow<AuthUser?> = state
    }

    private class FakeMultiAccountChatbotRepository : ChatbotRepository {
        private val botsByUser = mapOf(
            "user_A" to listOf(Chatbot(id = "bot-shared", name = "Bot A", userId = "user_A")),
            "user_B" to listOf(Chatbot(id = "bot-shared", name = "Bot B", userId = "user_B"))
        )

        private val messagesByUser = mapOf(
            "user_A" to listOf(
                ChatMessage(
                    id = "mA",
                    chatbotId = "bot-shared",
                    userId = "user_A",
                    message = "Halo dari A",
                    isFromUser = true,
                    timestamp = 1_000L
                )
            ),
            "user_B" to listOf(
                ChatMessage(
                    id = "mB",
                    chatbotId = "bot-shared",
                    userId = "user_B",
                    message = "Halo dari B",
                    isFromUser = true,
                    timestamp = 2_000L
                )
            )
        )

        override fun getChatbots(userId: String): Flow<Result<List<Chatbot>>> =
            MutableStateFlow(Result.success(botsByUser[userId].orEmpty()))

        override fun getChatbotsWithHistory(userId: String): Flow<Result<List<ChatbotWithHistory>>> =
            MutableStateFlow(
                Result.success(
                    botsByUser[userId].orEmpty().map { bot ->
                        ChatbotWithHistory(
                            chatbot = bot,
                            lastMessage = messagesByUser[userId]?.lastOrNull()?.message.orEmpty(),
                            lastMessageTime = 1_000L
                        )
                    }
                )
            )

        override suspend fun addChatbot(chatbot: Chatbot): Result<Unit> = Result.success(Unit)
        override suspend fun deleteChatbot(chatbotId: String, userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getChatbotById(chatbotId: String, userId: String): Result<Chatbot> =
            botsByUser[userId]?.firstOrNull { it.id == chatbotId }?.let { Result.success(it) }
                ?: Result.failure(NoSuchElementException("Not found"))

        override fun getChatMessages(chatbotId: String, userId: String): Flow<Result<List<ChatMessage>>> =
            MutableStateFlow(Result.success(messagesByUser[userId].orEmpty()))

        override suspend fun sendMessage(message: ChatMessage): Result<Unit> = Result.success(Unit)
        override suspend fun markMessagesAsRead(chatbotId: String, userId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeMoodDao : MoodDao {
        private var nextId = 1L
        private val items = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insert(entry: MoodEntryEntity): Long {
            val id = nextId++
            items.value = listOf(entry.copy(id = id)) + items.value
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

    private class FakeJournalRemoteDataSource : JournalRemoteDataSource {
        private var nextId = 1L
        private val items = MutableStateFlow<List<JournalEntryDto>>(emptyList())

        override fun observeAll(userId: String): Flow<List<JournalEntryDto>> =
            items.map { list -> list.filter { it.userId == userId } }

        override suspend fun getById(id: Long, userId: String): JournalEntryDto? =
            items.value.firstOrNull { it.id == id && it.userId == userId }

        override fun observeLatestTag(userId: String): Flow<String?> =
            items.map { list ->
                list.filter { it.userId == userId }
                    .maxWithOrNull(compareBy<JournalEntryDto> { it.updatedAt }.thenBy { it.id })
                    ?.tag
            }

        override suspend fun save(entry: JournalEntryDto, userId: String): Long? {
            return if (entry.id == 0L) {
                val id = nextId++
                items.value = items.value + entry.copy(id = id, userId = userId)
                id
            } else {
                val existingIndex = items.value.indexOfFirst { it.id == entry.id && it.userId == userId }
                if (existingIndex < 0) return null
                items.value = items.value.map {
                    if (it.id == entry.id && it.userId == userId) entry.copy(userId = userId) else it
                }
                entry.id
            }
        }

        override suspend fun upsertBatch(entries: List<JournalEntryDto>, userId: String) {
            val byId = entries.associate { it.id to it.copy(userId = userId) }
            val remaining = items.value.filterNot { it.userId == userId && it.id in byId }
            items.value = remaining + byId.values
        }

        override suspend fun delete(id: Long, userId: String): Boolean {
            val before = items.value.size
            items.value = items.value.filterNot { it.id == id && it.userId == userId }
            return before > items.value.size
        }

        override suspend fun togglePin(id: Long, userId: String): Boolean {
            var updated = false
            items.value = items.value.map {
                if (it.id == id && it.userId == userId) {
                    updated = true
                    it.copy(isPinned = !it.isPinned)
                } else {
                    it
                }
            }
            return updated
        }
    }
}

package com.hariku.feature.chatbot.data.repository

import com.hariku.feature.chatbot.data.dto.ChatMessageDto
import com.hariku.feature.chatbot.data.mapper.ChatMessageMapper
import com.hariku.feature.chatbot.data.mapper.ChatbotMapper
import com.hariku.feature.chatbot.data.remote.ChatbotFirebaseService
import com.hariku.feature.chatbot.data.remote.GeminiApiService
import com.hariku.feature.chatbot.domain.model.Chatbot
import com.hariku.feature.chatbot.domain.model.ChatMessage
import com.hariku.feature.chatbot.domain.model.ChatbotWithHistory
import com.hariku.feature.chatbot.domain.repository.ChatbotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatbotRepositoryImpl(
    private val firebaseService: ChatbotFirebaseService,
    private val geminiApiService: GeminiApiService,
    private val mapper: ChatbotMapper,
    private val messageMapper: ChatMessageMapper
) : ChatbotRepository {
    
    override fun getChatbots(userId: String): Flow<Result<List<Chatbot>>> {
        return firebaseService.getChatbots(userId)
            .map { dtoList ->
                val chatbots = dtoList.map { mapper.toDomain(it, userId) }
                Result.success(chatbots)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
    
    override suspend fun addChatbot(chatbot: Chatbot): Result<Unit> {
        return try {
            val dto = mapper.toDto(chatbot)
            firebaseService.addChatbot(dto, chatbot.userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteChatbot(chatbotId: String, userId: String): Result<Unit> {
        return try {
            firebaseService.deleteChatbot(chatbotId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getChatbotById(chatbotId: String, userId: String): Result<Chatbot> {
        return try {
            val dto = firebaseService.getChatbotById(chatbotId, userId)
            if (dto != null) {
                Result.success(mapper.toDomain(dto, userId))
            } else {
                Result.failure(Exception("Chatbot not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getChatbotsWithHistory(userId: String): Flow<Result<List<ChatbotWithHistory>>> {
        return firebaseService.getChatbots(userId)
            .flatMapLatest { dtoList ->
                val historyFlows = dtoList.map { dto ->
                    val chatbot = mapper.toDomain(dto, userId)
                    combine(
                        firebaseService.observeLastMessage(chatbot.id, userId).catch { emit(null) },
                        firebaseService.observeUnreadCount(chatbot.id, userId).catch { emit(0) }
                    ) { lastMessage, unreadCount ->
                        ChatbotWithHistory(
                            chatbot = chatbot,
                            lastMessage = lastMessage?.message ?: "Belum ada percakapan",
                            lastMessageTime = lastMessage?.timestamp ?: chatbot.createdAt,
                            unreadCount = unreadCount
                        )
                    }
                }
                if (historyFlows.isEmpty()) {
                    flowOf(Result.success(emptyList<ChatbotWithHistory>()))
                } else {
                    combine(historyFlows) { items -> Result.success(items.toList()) }
                }
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
    
    override fun getChatMessages(chatbotId: String, userId: String): Flow<Result<List<ChatMessage>>> {
        return firebaseService.getChatMessages(chatbotId, userId)
            .map { dtoList ->
                val messages = dtoList.map { messageMapper.toDomain(it, userId, chatbotId) }
                Result.success(messages)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
    
    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val userMessageDto = messageMapper.toDto(message)
            firebaseService.sendMessage(userMessageDto, message.userId, message.chatbotId)
            
            val chatbotDto = firebaseService.getChatbotById(message.chatbotId, message.userId)
            val chatbot = chatbotDto?.let { mapper.toDomain(it, message.userId) }
                ?: return Result.failure(Exception("Chatbot tidak ditemukan"))
            
            val recentMessagesDto: List<ChatMessageDto> = firebaseService.getRecentMessages(
                chatbotId = message.chatbotId,
                userId = message.userId,
                limit = 10
            )
            val history: List<Pair<String, Boolean>> = recentMessagesDto.map { dto: ChatMessageDto ->
                Pair(dto.message, dto.fromUser)
            }
            
            val botResponse = geminiApiService.generateResponse(
                chatbot = chatbot,
                userMessage = message.message,
                conversationHistory = history
            )
            
            val botMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatbotId = message.chatbotId,
                userId = message.userId,
                message = botResponse,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            val botMessageDto = messageMapper.toDto(botMessage)
            firebaseService.sendMessage(botMessageDto, message.userId, message.chatbotId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markMessagesAsRead(chatbotId: String, userId: String): Result<Unit> {
        return try {
            firebaseService.markMessagesAsRead(chatbotId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
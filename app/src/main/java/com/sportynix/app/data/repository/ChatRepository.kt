package com.sportynix.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sportynix.app.data.local.dao.ChatDao
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.data.local.entity.HiddenChatEntity
import com.sportynix.app.data.local.entity.OutboxMessageEntity
import com.sportynix.app.data.remote.api.ChatApiService
import com.sportynix.app.domain.model.chat.Chat
import com.sportynix.app.domain.model.chat.SendMessageResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatApiService: ChatApiService,
    private val chatDao: ChatDao,
    private val syncManager: SyncManager,
    private val gson: Gson
) {

    suspend fun getMyChatsCachedFirst(onFresh: suspend (List<Chat>) -> Unit): List<Chat> {
        // Return from cache (DB or SharedPreferences) if available
        val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        val cachedJson = sharedPrefs.getString("chat_list_cache", null)
        val cachedChats = if (cachedJson != null) {
            try {
                val array = gson.fromJson(cachedJson, Array<Chat>::class.java)
                filterVisibleChats(array.toList())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Fetch fresh in background
        try {
            val response = chatApiService.getMyChats()
            if (response.isSuccessful) {
                // Simplified for this scope: in reality parse merged response
                val body = response.body()
                val chatsList = mutableListOf<Chat>()
                
                if (body != null && body.isJsonArray) {
                    chatsList.addAll(gson.fromJson(body, Array<Chat>::class.java))
                } else if (body != null && body.isJsonObject) {
                    val obj = body.asJsonObject
                    if (obj.has("team_groups")) chatsList.addAll(gson.fromJson(obj.get("team_groups"), Array<Chat>::class.java))
                    if (obj.has("channels")) chatsList.addAll(gson.fromJson(obj.get("channels"), Array<Chat>::class.java))
                    if (obj.has("direct_messages")) chatsList.addAll(gson.fromJson(obj.get("direct_messages"), Array<Chat>::class.java))
                    if (obj.has("rivalry_chats")) chatsList.addAll(gson.fromJson(obj.get("rivalry_chats"), Array<Chat>::class.java))
                    if (obj.has("challenge_chats")) chatsList.addAll(gson.fromJson(obj.get("challenge_chats"), Array<Chat>::class.java))
                }
                
                sharedPrefs.edit().putString("chat_list_cache", gson.toJson(chatsList)).apply()
                onFresh(filterVisibleChats(chatsList))
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch fresh chats")
        }

        return cachedChats
    }

    suspend fun getMessages(chatId: Long, limit: Int = 200): List<ChatMessageEntity> {
        return withContext(Dispatchers.IO) {
            chatDao.getRecentMessages(chatId, limit).filter {
                // Filter out negative temp messages older than 2 minutes unless queued
                if (it.id < 0 && !it.queued) {
                    val ageMs = System.currentTimeMillis() - parseDateToMs(it.createdAt)
                    ageMs <= 2 * 60 * 1000
                } else true
            }.reversed()
        }
    }

    suspend fun sendMessage(
        chatId: Long,
        messageText: String,
        messageType: String = "text",
        bookingId: Long? = null,
        metadata: Map<String, Any>? = null
    ): Result<SendMessageResponse> {
        val clientMsgId = "chat-$chatId-${System.currentTimeMillis()}-${(Math.random() * 1000000).toInt()}"
        val tempId = -System.currentTimeMillis()
        val now = java.time.Instant.now().toString()

        val outbox = OutboxMessageEntity(
            clientMsgId = clientMsgId,
            chatId = chatId,
            message = messageText,
            messageType = messageType,
            bookingId = bookingId,
            metadataJson = metadata?.let { gson.toJson(it) },
            tempMessageId = tempId,
            createdAt = now,
            retryCount = 0,
            lastError = null
        )

        val localMsg = ChatMessageEntity(
            id = tempId,
            chatId = chatId,
            senderId = 0, // Should be current user
            senderName = "You",
            senderAvatar = null,
            message = messageText,
            messageType = messageType,
            timestamp = now,
            createdAt = now,
            isRead = false,
            isDeleted = false,
            delivered = false,
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            duration = null,
            metadataJson = outbox.metadataJson,
            bookingId = bookingId,
            fileUrl = null,
            mediaExpiresAt = null,
            localMediaPath = null,
            queued = true,
            clientMsgId = clientMsgId
        )

        withContext(Dispatchers.IO) {
            chatDao.insertOutbox(outbox)
            chatDao.upsertMessage(localMsg)
        }

        return try {
            val payload = mutableMapOf<String, Any>("message" to messageText, "message_type" to messageType)
            if (metadata != null) payload["metadata"] = metadata
            if (bookingId != null && messageType == "event") {
                payload["booking"] = bookingId
                payload["booking_id"] = bookingId
            }

            val response = chatApiService.sendMessage(chatId, payload)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                withContext(Dispatchers.IO) {
                    chatDao.deleteOutboxByClientMsgId(clientMsgId)
                    chatDao.deleteMessage(chatId, tempId) // Remove temp
                    chatDao.upsertMessage(
                        localMsg.copy(
                            id = result.id,
                            senderId = result.sender,
                            senderName = result.senderName,
                            timestamp = result.timestamp,
                            createdAt = result.createdAt,
                            delivered = result.delivered ?: true,
                            queued = false,
                            clientMsgId = null
                        )
                    )
                }
                Result.success(result)
            } else {
                chatDao.bumpOutboxRetry(clientMsgId, response.errorBody()?.string() ?: "HTTP error")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            chatDao.bumpOutboxRetry(clientMsgId, e.message ?: "Network error")
            Result.failure(e)
        }
    }

    suspend fun hideChatForMe(chatId: Long) {
        withContext(Dispatchers.IO) {
            chatDao.hideChat(HiddenChatEntity(chatId))
        }
    }

    private suspend fun filterVisibleChats(chats: List<Chat>): List<Chat> {
        return withContext(Dispatchers.IO) {
            val hiddenIds = chatDao.getHiddenChatIds().toSet()
            chats.filter { !hiddenIds.contains(it.id) }
        }
    }

    private fun parseDateToMs(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

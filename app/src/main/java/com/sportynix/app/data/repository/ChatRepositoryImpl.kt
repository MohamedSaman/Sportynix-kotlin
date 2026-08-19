package com.sportynix.app.data.repository

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.sportynix.app.data.local.ChatDatabase
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.data.local.entity.ClearedCutoffEntity
import com.sportynix.app.data.local.entity.HiddenChatEntity
import com.sportynix.app.data.local.entity.OutboxMessageEntity
import com.sportynix.app.data.remote.api.ChatApiService
import com.sportynix.app.data.remote.websocket.WebSocketManager
import com.sportynix.app.domain.model.*
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatApi: ChatApiService,
    private val db: ChatDatabase,
    private val webSocketManager: WebSocketManager,
    private val gson: Gson
) : ChatRepository {

    private val dao = db.chatDao()
    private val mediaInFlightAt = ConcurrentHashMap<Long, Long>()

    override fun getMyChatsCachedFirst(): Flow<List<Chat>> = flow {
        // Emit from memory/server fetch
        val fresh = fetchMyChats().getOrDefault(emptyList())
        emit(fresh)
    }.flowOn(Dispatchers.IO)

    override suspend fun fetchMyChats(): Result<List<Chat>> = withContext(Dispatchers.IO) {
        try {
            val response = chatApi.getMyChats()
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Failed to fetch my chats: HTTP ${response.code()}"))
            }

            val element = response.body()!!
            val allChats = mutableListOf<Chat>()

            val hiddenIds = dao.getHiddenChatIds().toSet()

            if (element.isJsonArray) {
                val listType = object : TypeToken<List<Chat>>() {}.type
                val list = gson.fromJson<List<Chat>>(element.asJsonArray, listType)
                allChats.addAll(list)
            } else if (element.isJsonObject) {
                val obj = element.asJsonObject
                listOf("team_groups", "channels", "direct_messages", "rivalry_chats", "challenge_chats", "rivalries", "challenges").forEach { key ->
                    if (obj.has(key) && obj.get(key).isJsonArray) {
                        val listType = object : TypeToken<List<Chat>>() {}.type
                        val list = gson.fromJson<List<Chat>>(obj.getAsJsonArray(key), listType)
                        allChats.addAll(list)
                    }
                }
            }

            val visible = allChats.filter { !hiddenIds.contains(it.id) }
                .map { chat ->
                    val cutoff = dao.getCutoff(chat.id)
                    if (cutoff != null) {
                        val lastTime = chat.lastMessage?.createdAt ?: chat.lastMessageTime
                        if (lastTime == null || isBeforeCutoff(lastTime, cutoff)) {
                            chat.copy(lastMessage = null, lastMessageTime = null, unreadCount = 0)
                        } else chat
                    } else chat
                }

            Result.success(visible)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching my chats")
            Result.failure(e)
        }
    }

    override suspend fun discoverChannels(search: String?): Result<List<Chat>> = withContext(Dispatchers.IO) {
        try {
            val response = chatApi.discoverChannels(search)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.success(emptyList())
            }
            val element = response.body()!!
            val list = mutableListOf<Chat>()
            val listType = object : TypeToken<List<Chat>>() {}.type

            if (element.isJsonArray) {
                list.addAll(gson.fromJson(element.asJsonArray, listType))
            } else if (element.isJsonObject) {
                val obj = element.asJsonObject
                val array = obj.getAsJsonArray("channels") ?: obj.getAsJsonArray("results")
                if (array != null) {
                    list.addAll(gson.fromJson(array, listType))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Timber.e(e, "discoverChannels failed")
            Result.success(emptyList())
        }
    }

    override suspend fun followChannel(chatId: Long): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.followChannel(chatId)
            if (res.isSuccessful && res.body() != null) {
                val obj = res.body()!!.asJsonObject
                val msg = obj.get("message")?.asString ?: "Followed"
                val count = obj.get("follower_count")?.asInt ?: 0
                Result.success(msg to count)
            } else Result.failure(Exception("Failed to follow channel"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowChannel(chatId: Long): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.unfollowChannel(chatId)
            if (res.isSuccessful && res.body() != null) {
                val obj = res.body()!!.asJsonObject
                val msg = obj.get("message")?.asString ?: "Unfollowed"
                val count = obj.get("follower_count")?.asInt ?: 0
                Result.success(msg to count)
            } else Result.failure(Exception("Failed to unfollow channel"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowers(chatId: Long): Result<List<Follower>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getFollowers(chatId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to load followers"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChatForEveryone(chatId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.deleteChatForEveryone(chatId)
            hideChatForMe(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hideChatForMe(chatId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.hideChat(HiddenChatEntity(chatId))
            dao.clearMessagesForChat(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearChatForMe(chatId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            dao.setCutoff(ClearedCutoffEntity(chatId, nowIso))
            dao.unhideChat(chatId)
            dao.clearMessagesForChat(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessagesWithSync(chatId: Long, limit: Int): Flow<List<ChatMessage>> {
        return dao.getMessagesForChatFlow(chatId).map { entities ->
            entities.map { it.toDomain(gson) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun fetchMessagesFromServer(chatId: Long, limit: Int, before: String?): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val response = chatApi.getMessages(chatId, limit, before)
            if (!response.isSuccessful || response.body() == null) {
                val cached = dao.getMessagesForChat(chatId).map { it.toDomain(gson) }
                return@withContext Result.success(cached)
            }

            val element = response.body()!!
            val rawMessages = mutableListOf<ChatMessage>()
            val listType = object : TypeToken<List<ChatMessage>>() {}.type

            if (element.isJsonArray) {
                rawMessages.addAll(gson.fromJson(element.asJsonArray, listType))
            } else if (element.isJsonObject) {
                val obj = element.asJsonObject
                val array = obj.getAsJsonArray("messages") ?: obj.getAsJsonArray("results")
                if (array != null) {
                    rawMessages.addAll(gson.fromJson(array, listType))
                }
            }

            val cutoff = dao.getCutoff(chatId)
            val filtered = if (cutoff != null) {
                rawMessages.filter { !isBeforeCutoff(it.createdAt.ifEmpty { it.timestamp }, cutoff) }
            } else rawMessages

            val entities = filtered.map { it.toEntity(gson) }
            dao.upsertMessages(entities)

            Result.success(filtered)
        } catch (e: Exception) {
            Timber.e(e, "fetchMessagesFromServer error")
            val cached = dao.getMessagesForChat(chatId).map { it.toDomain(gson) }
            Result.success(cached)
        }
    }

    override suspend fun syncMessagesForChat(chatId: Long, lastSyncTimestamp: String?): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val deviceId = "android_" + context.packageName
            val response = chatApi.syncMessages(deviceId, lastSyncTimestamp, 200)
            if (!response.isSuccessful || response.body() == null) return@withContext Result.success(emptyList())

            val element = response.body()!!
            val rawMessages = mutableListOf<ChatMessage>()
            val listType = object : TypeToken<List<ChatMessage>>() {}.type

            if (element.isJsonObject) {
                val array = element.asJsonObject.getAsJsonArray("messages")
                if (array != null) {
                    rawMessages.addAll(gson.fromJson(array, listType))
                }
            }
            val chatMessages = rawMessages.filter { it.chat == chatId }
            dao.upsertMessages(chatMessages.map { it.toEntity(gson) })

            Result.success(chatMessages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatId: Long,
        message: String,
        messageType: String,
        bookingId: Long?,
        metadata: Map<String, Any>?,
        tempMessageId: Long?
    ): Result<SendMessageResponse> = withContext(Dispatchers.IO) {
        val clientMsgId = "chat-$chatId-${System.currentTimeMillis()}-${(100000..999999).random()}"
        val localTempId = tempMessageId ?: -System.currentTimeMillis()

        val queuedEntity = OutboxMessageEntity(
            clientMsgId = clientMsgId,
            chatId = chatId,
            tempMessageId = localTempId,
            message = message,
            messageType = messageType,
            bookingId = bookingId,
            metadataJson = metadata?.let { gson.toJson(it) },
            localMediaPath = null,
            senderId = null,
            senderName = "You",
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        )
        dao.insertOutbox(queuedEntity)

        // Insert local temporary ChatMessageEntity into Room
        val tempChatMessage = ChatMessageEntity(
            id = localTempId,
            chatId = chatId,
            senderId = 0,
            senderName = "You",
            senderAvatar = null,
            message = message,
            messageType = messageType,
            timestamp = queuedEntity.createdAt,
            createdAt = queuedEntity.createdAt,
            isRead = false,
            isDeleted = false,
            delivered = false,
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            duration = null,
            metadataJson = queuedEntity.metadataJson,
            bookingId = bookingId,
            fileUrl = null,
            mediaExpiresAt = null,
            localMediaPath = null,
            queued = true,
            clientMsgId = clientMsgId
        )
        dao.upsertMessage(tempChatMessage)

        try {
            val req = SendMessageRequest(message, messageType, bookingId, bookingId, metadata)
            val res = chatApi.sendMessage(chatId, req)

            if (res.isSuccessful && res.body() != null) {
                val sent = res.body()!!
                dao.deleteOutboxByClientMsgId(clientMsgId)
                dao.deleteMessage(chatId, localTempId)
                dao.upsertMessage(sent.toChatMessage(chatId).toEntity(gson))
                Result.success(sent)
            } else {
                Result.success(
                    SendMessageResponse(
                        id = localTempId,
                        chat = chatId,
                        message = message,
                        messageType = messageType,
                        sender = 0,
                        senderName = "You",
                        timestamp = queuedEntity.createdAt,
                        createdAt = queuedEntity.createdAt,
                        queued = true,
                        clientMsgId = clientMsgId
                    )
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "sendMessage network failure, queued in Room outbox")
            dao.bumpOutboxRetry(clientMsgId, e.message ?: "Network error")
            Result.success(
                SendMessageResponse(
                    id = localTempId,
                    chat = chatId,
                    message = message,
                    messageType = messageType,
                    sender = 0,
                    senderName = "You",
                    timestamp = queuedEntity.createdAt,
                    createdAt = queuedEntity.createdAt,
                    queued = true,
                    clientMsgId = clientMsgId
                )
            )
        }
    }

    override suspend fun queueMediaMessage(
        chatId: Long,
        mediaType: String,
        localMediaPath: String,
        caption: String,
        duration: Int?,
        metadata: Map<String, Any>?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val clientMsgId = "media-$chatId-${System.currentTimeMillis()}-${(100000..999999).random()}"
        val tempId = -(System.currentTimeMillis() * 1_000L + (0..999).random())
        val nowIso = java.time.Instant.now().toString()
        val sourceFile = File(localMediaPath.removePrefix("file://"))
        if (!sourceFile.exists()) return@withContext Result.failure(IllegalArgumentException("Selected media file is unavailable"))
        val extension = when (mediaType) { "voice" -> "m4a"; "video" -> "mp4"; else -> sourceFile.extension.ifBlank { "jpg" } }
        val outboxDirectory = File(context.filesDir, "chat_media/outbox").apply { mkdirs() }
        val durableFile = File(outboxDirectory, "${clientMsgId.replace(':', '_')}.$extension")
        sourceFile.copyTo(durableFile, overwrite = true)
        val durablePath = durableFile.absolutePath

        val metaMap = mutableMapOf<String, Any>("mediaType" to mediaType, "localMediaPath" to durablePath)
        if (duration != null) metaMap["duration"] = duration
        if (metadata != null) metaMap.putAll(metadata)

        val outbox = OutboxMessageEntity(
            clientMsgId = clientMsgId,
            chatId = chatId,
            tempMessageId = tempId,
            message = caption,
            messageType = mediaType,
            bookingId = null,
            metadataJson = gson.toJson(metaMap),
            localMediaPath = durablePath,
            senderId = null,
            senderName = "You",
            createdAt = nowIso
        )
        dao.insertOutbox(outbox)

        val tempMessage = ChatMessageEntity(
            id = tempId,
            chatId = chatId,
            senderId = 0,
            senderName = "You",
            senderAvatar = null,
            message = caption,
            messageType = mediaType,
            timestamp = nowIso,
            createdAt = nowIso,
            isRead = false,
            isDeleted = false,
            delivered = false,
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            duration = duration,
            metadataJson = gson.toJson(metaMap),
            bookingId = null,
            fileUrl = null,
            mediaExpiresAt = null,
            localMediaPath = durablePath,
            queued = true,
            clientMsgId = clientMsgId
        )
        dao.upsertMessage(tempMessage)

        Result.success(Unit)
    }

    override suspend fun editMessage(chatId: Long, messageId: Long, newMessage: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.editMessage(chatId, messageId, mapOf("message" to newMessage))
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                dao.upsertMessage(updated.toEntity(gson))
                Result.success(updated)
            } else Result.failure(Exception("Failed to edit message"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(chatId: Long, messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.deleteMessage(chatId, messageId)
            if (res.isSuccessful || res.code() == 204) {
                dao.deleteMessage(chatId, messageId)
                context.cacheDir.listFiles()?.filter { it.name.startsWith("chat_media_${messageId}.") }?.forEach { it.delete() }
                Result.success(Unit)
            } else Result.failure(Exception("Failed to delete message"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessageForMe(chatId: Long, messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteMessage(chatId, messageId)
            context.cacheDir.listFiles()?.filter { it.name.startsWith("chat_media_${messageId}.") }?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pinMessage(chatId: Long, messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        dao.updateMessagePinned(chatId, messageId, true)
        try {
            val response = chatApi.pinMessage(chatId, mapOf("message_id" to messageId))
            if (response.isSuccessful) Result.success(Unit)
            else {
                dao.updateMessagePinned(chatId, messageId, false)
                Result.failure(Exception("Failed to pin message"))
            }
        } catch (e: Exception) {
            dao.updateMessagePinned(chatId, messageId, false)
            Result.failure(e)
        }
    }

    override suspend fun unpinMessage(chatId: Long, messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        dao.updateMessagePinned(chatId, messageId, false)
        try {
            val response = chatApi.unpinMessage(chatId, mapOf("message_id" to messageId))
            if (response.isSuccessful) Result.success(Unit)
            else {
                dao.updateMessagePinned(chatId, messageId, true)
                Result.failure(Exception("Failed to unpin message"))
            }
        } catch (e: Exception) {
            dao.updateMessagePinned(chatId, messageId, true)
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(chatId: Long, messageIds: List<Long>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (messageIds.isEmpty()) return@withContext Result.success(Unit)
            chatApi.markAsRead(chatId, mapOf("message_ids" to messageIds))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markDelivered(chatId: Long, messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.markDelivered(chatId, mapOf("message_id" to messageId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTeamChat(teamId: Long): Result<Chat> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getTeamChat(teamId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to get team chat"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDirectChat(userId: Long): Result<Chat> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getDirectChat(userId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to get direct chat"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBlockedUsers(): Result<List<BlockedUser>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getBlockedUsers()
            if (res.isSuccessful && res.body() != null) {
                val elem = res.body()!!
                val listType = object : TypeToken<List<BlockedUser>>() {}.type
                if (elem.isJsonArray) {
                    Result.success(gson.fromJson(elem.asJsonArray, listType))
                } else {
                    Result.success(emptyList())
                }
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun blockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.blockUser(mapOf("user_id" to userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unblockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.unblockUser(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserReports(): Result<List<UserReportItem>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getUserReports()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun reportUser(reportedUserId: Long, reason: String, notes: String?, chatId: Long?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = mutableMapOf<String, Any?>("reported_user_id" to reportedUserId, "reason" to reason, "notes" to (notes ?: ""))
            if (chatId != null) body["chat_id"] = chatId
            chatApi.reportUser(body)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelUserReport(reportId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.cancelUserReport(reportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChatRequests(): Result<ChatRequestListResponse> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getChatRequests()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.success(ChatRequestListResponse())
        } catch (e: Exception) {
            Result.success(ChatRequestListResponse())
        }
    }

    override suspend fun sendChatRequest(userId: Long): Result<ChatRequestItem> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.sendChatRequest(mapOf("user_id" to userId))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to send chat request"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptChatRequest(requestId: Long): Result<Pair<ChatRequestItem, Chat>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.acceptChatRequest(requestId)
            if (res.isSuccessful && res.body() != null) {
                val obj = res.body()!!.asJsonObject
                val req = gson.fromJson(obj.getAsJsonObject("request"), ChatRequestItem::class.java)
                val chat = gson.fromJson(obj.getAsJsonObject("chat"), Chat::class.java)
                Result.success(req to chat)
            } else Result.failure(Exception("Failed to accept chat request"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineChatRequest(requestId: Long): Result<ChatRequestItem> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.declineChatRequest(requestId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to decline chat request"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelChatRequest(requestId: Long): Result<ChatRequestItem> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.cancelChatRequest(requestId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.failure(Exception("Failed to cancel chat request"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMutualUsers(query: String?): Result<List<MutualUser>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getMutualUsers(query)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun searchUsers(query: String): Result<List<UserSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.searchUsers(query)
            if (res.isSuccessful && res.body() != null) {
                val elem = res.body()!!
                val listType = object : TypeToken<List<UserSearchResult>>() {}.type
                if (elem.isJsonArray) {
                    Result.success(gson.fromJson(elem.asJsonArray, listType))
                } else if (elem.isJsonObject) {
                    val arr = elem.asJsonObject.getAsJsonArray("results")
                    if (arr != null) Result.success(gson.fromJson(arr, listType))
                    else Result.success(emptyList())
                } else Result.success(emptyList())
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun registerDevice(deviceToken: String, onesignalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.registerDevice(RegisterDeviceRequest(deviceToken, onesignalId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getUnreadCount()
            if (res.isSuccessful && res.body() != null) {
                val count = res.body()!!.asJsonObject.get("total_unread")?.asInt ?: 0
                Result.success(count)
            } else Result.success(0)
        } catch (e: Exception) {
            Result.success(0)
        }
    }

    override suspend fun getChatMembers(chatId: Long): Result<List<ChatMember>> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getChatMembers(chatId)
            if (res.isSuccessful && res.body() != null) {
                val obj = res.body()!!.asJsonObject
                val arr = obj.getAsJsonArray("members")
                val listType = object : TypeToken<List<ChatMember>>() {}.type
                if (arr != null) Result.success(gson.fromJson(arr, listType))
                else Result.success(emptyList())
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun addAdmin(chatId: Long, userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.addAdmin(chatId, mapOf("user_id" to userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAdmin(chatId: Long, userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            chatApi.removeAdmin(chatId, mapOf("user_id" to userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleAdminOnly(chatId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.toggleAdminOnly(chatId)
            if (res.isSuccessful && res.body() != null) {
                val adminOnly = res.body()!!.asJsonObject.get("admin_only")?.asBoolean ?: false
                Result.success(adminOnly)
            } else Result.failure(Exception("Failed to toggle admin only"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChatDetails(chatId: Long, historyPage: Int): Result<Chat> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.getChatDetails(chatId, historyPage, 10)
            if (res.isSuccessful && res.body() != null) {
                val chat = gson.fromJson(res.body()!!, Chat::class.java)
                Result.success(chat)
            } else Result.failure(Exception("Failed to load chat details"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadMedia(chatId: Long, messageId: Long, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val res = chatApi.downloadMedia(chatId, messageId)
            if (res.isSuccessful && res.body() != null) {
                res.body()!!.byteStream().use { input ->
                    destinationFile.parentFile?.mkdirs()
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                dao.getMessage(chatId, messageId)?.let { existing ->
                    dao.upsertMessage(existing.copy(localMediaPath = destinationFile.absolutePath))
                }
                Result.success(destinationFile)
            } else Result.failure(Exception("Download failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPhotoMessages(chatId: Long, limit: Int): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val messages = dao.getMessagesForChat(chatId).map { it.toDomain(gson) }
            .filter { it.messageType == "photo" && !it.isDeleted && !it.fileUrl.isNullOrEmpty() }
            .sortedByDescending { it.createdAt.ifEmpty { it.timestamp } }
        val final = if (limit > 0) messages.take(limit) else messages
        Result.success(final)
    }

    override suspend fun getEventMessages(chatId: Long, limit: Int): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val messages = dao.getMessagesForChat(chatId).map { it.toDomain(gson) }
            .filter { it.messageType == "event" && !it.isDeleted }
            .sortedByDescending { it.createdAt.ifEmpty { it.timestamp } }
        val final = if (limit > 0) messages.take(limit) else messages
        Result.success(final)
    }

    override suspend fun getAllMediaMessages(chatId: Long, limit: Int): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val messages = dao.getMessagesForChat(chatId).map { it.toDomain(gson) }
            .filter { (it.messageType == "photo" || it.messageType == "video") && !it.isDeleted && !it.fileUrl.isNullOrEmpty() }
            .sortedByDescending { it.createdAt.ifEmpty { it.timestamp } }
        val final = if (limit > 0) messages.take(limit) else messages
        Result.success(final)
    }

    override suspend fun syncQueuedOutbox(): Result<Unit> = withContext(Dispatchers.IO) {
        val outboxList = dao.getOutboxMessages(50)
        for (item in outboxList) {
            try {
                val meta = item.metadataJson?.let { gson.fromJson<Map<String, Any>>(it, object : TypeToken<Map<String, Any>>() {}.type) }
                if (item.messageType in setOf("photo", "video", "voice")) {
                    val localPath = item.localMediaPath ?: meta?.get("localMediaPath")?.toString()
                    if (localPath.isNullOrBlank()) {
                        dao.bumpOutboxRetry(item.clientMsgId, "Media outbox item is missing localMediaPath")
                        continue
                    }
                    val now = System.currentTimeMillis()
                    val lastAttempt = mediaInFlightAt[item.tempMessageId]
                    if (lastAttempt != null && now - lastAttempt < 15_000L) continue
                    val file = File(localPath.removePrefix("file://"))
                    if (!file.exists()) {
                        dao.bumpOutboxRetry(item.clientMsgId, "Local media file no longer exists")
                        continue
                    }
                    if (!webSocketManager.isChatWsConnected.value) {
                        dao.bumpOutboxRetry(item.clientMsgId, "WebSocket is not connected for media sync")
                        continue
                    }
                    mediaInFlightAt[item.tempMessageId] = now
                    val mime = when (item.messageType) {
                        "voice" -> "audio/mpeg"
                        "video" -> "video/mp4"
                        else -> "image/jpeg"
                    }
                    val dataUrl = "data:$mime;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                    val outboundMetadata = (meta?.get("reply_to") as? Map<*, *>)?.let { mapOf("reply_to" to it) }
                    val sent = webSocketManager.sendMessage(
                        mapOf(
                            "type" to "message",
                            "message_type" to item.messageType,
                            "media_data" to dataUrl,
                            "local_id" to item.tempMessageId,
                            "duration" to (meta?.get("duration") as? Number)?.toInt(),
                            "message" to item.message,
                            "metadata" to outboundMetadata
                        )
                    )
                    if (!sent) {
                        mediaInFlightAt.remove(item.tempMessageId)
                        dao.bumpOutboxRetry(item.clientMsgId, "WebSocket rejected media send")
                    }
                    continue
                }
                val req = SendMessageRequest(
                    message = item.message,
                    messageType = item.messageType,
                    booking = item.bookingId,
                    bookingId = item.bookingId,
                    metadata = meta
                )
                val res = chatApi.sendMessage(item.chatId, req)
                if (res.isSuccessful && res.body() != null) {
                    dao.deleteOutboxByClientMsgId(item.clientMsgId)
                    dao.deleteMessage(item.chatId, item.tempMessageId)
                    dao.upsertMessage(res.body()!!.toChatMessage(item.chatId).toEntity(gson))
                }
            } catch (e: Exception) {
                dao.bumpOutboxRetry(item.clientMsgId, e.message ?: "Retry error")
            }
        }
        Result.success(Unit)
    }

    override suspend fun reconcileQueuedMediaAck(chatId: Long, tempMessageId: Long, serverMessage: ChatMessage): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val pending = dao.getMessage(chatId, tempMessageId)
            dao.deleteOutboxByTempId(tempMessageId)
            dao.deleteMessage(chatId, tempMessageId)
            dao.upsertMessage(
                serverMessage.copy(
                    localMediaPath = pending?.localMediaPath ?: serverMessage.localMediaPath,
                    clientMsgId = pending?.clientMsgId ?: serverMessage.clientMsgId,
                    queued = false
                ).toEntity(gson)
            )
            mediaInFlightAt.remove(tempMessageId)
            Unit
        }
    }

    private fun isBeforeCutoff(timestampIso: String, cutoffIso: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val ts = sdf.parse(timestampIso)?.time ?: 0L
            val cut = sdf.parse(cutoffIso)?.time ?: 0L
            ts <= cut
        } catch (e: Exception) {
            false
        }
    }

    private fun ChatMessageEntity.toDomain(gson: Gson): ChatMessage {
        val metaMap: Map<String, Any>? = metadataJson?.let {
            try {
                gson.fromJson(it, object : TypeToken<Map<String, Any>>() {}.type)
            } catch (e: Exception) {
                null
            }
        }
        return ChatMessage(
            id = id,
            chat = chatId,
            sender = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            message = message,
            messageType = messageType,
            timestamp = timestamp,
            createdAt = createdAt,
            isRead = isRead,
            isDeleted = isDeleted,
            delivered = delivered,
            isPinned = isPinned,
            pinnedBy = pinnedBy,
            pinnedAt = pinnedAt,
            duration = duration,
            metadata = metaMap,
            bookingId = bookingId,
            fileUrl = fileUrl,
            mediaExpiresAt = mediaExpiresAt,
            localMediaPath = localMediaPath,
            queued = queued,
            clientMsgId = clientMsgId
        )
    }

    private fun ChatMessage.toEntity(gson: Gson): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            chatId = chat,
            senderId = sender,
            senderName = senderName,
            senderAvatar = senderAvatar,
            message = message,
            messageType = messageType,
            timestamp = timestamp,
            createdAt = createdAt,
            isRead = isRead,
            isDeleted = isDeleted,
            delivered = delivered,
            isPinned = isPinned,
            pinnedBy = pinnedBy,
            pinnedAt = pinnedAt,
            duration = duration,
            metadataJson = metadata?.let { gson.toJson(it) },
            bookingId = bookingId,
            fileUrl = fileUrl,
            mediaExpiresAt = mediaExpiresAt,
            localMediaPath = localMediaPath,
            queued = queued,
            clientMsgId = clientMsgId
        )
    }

    private fun SendMessageResponse.toChatMessage(chatId: Long): ChatMessage {
        return ChatMessage(
            id = id,
            chat = chatId,
            sender = sender,
            senderName = senderName,
            message = message,
            messageType = messageType,
            timestamp = timestamp,
            createdAt = createdAt,
            isPinned = isPinned ?: false,
            duration = duration,
            metadata = metadata,
            bookingId = bookingId,
            delivered = delivered ?: false,
            queued = queued ?: false,
            clientMsgId = clientMsgId
        )
    }
}

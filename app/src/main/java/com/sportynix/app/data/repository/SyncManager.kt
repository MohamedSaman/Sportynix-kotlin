package com.sportynix.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sportynix.app.data.local.dao.ChatDao
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.data.remote.api.ChatApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val chatApiService: ChatApiService,
    private val outboxQueueManager: OutboxQueueManager,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isSyncing = false
    private var deviceId: String? = null
    private val sharedPrefs by lazy { context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE) }

    init {
        initialize()
    }

    private fun initialize() {
        deviceId = sharedPrefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("device_id", deviceId).apply()
        }
        Timber.d("SyncManager initialized with device: $deviceId")
    }

    fun runSync() {
        if (deviceId == null) {
            Timber.w("SyncManager not initialized, skipping sync")
            return
        }

        if (isSyncing) {
            Timber.d("Sync already in progress, skipping")
            return
        }

        isSyncing = true
        Timber.d("Starting chat sync...")

        scope.launch {
            try {
                val lastSyncedAt = sharedPrefs.getString("last_synced_at", null)
                syncMessages(lastSyncedAt)
                reportSyncState()
                syncPendingOperations()
                Timber.d("Chat sync completed")
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
            } finally {
                isSyncing = false
            }
        }
    }

    private suspend fun syncMessages(since: String?) {
        Timber.d("Syncing messages...")
        var hasMore = true
        var emptyResponseCount = 0
        var totalMessages = 0
        // Kotlin implementation of keyset pagination
        try {
            while (hasMore) {
                val response = chatApiService.syncMessages(
                    deviceId = deviceId!!,
                    since = since,
                    limit = 200
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!.asJsonObject
                    if (body.has("messages")) {
                        val messagesJson = body.getAsJsonArray("messages")
                        if (messagesJson.isEmpty) {
                            break
                        }

                        val messagesList = mutableListOf<ChatMessageEntity>()
                        for (i in 0 until messagesJson.size()) {
                            val msg = messagesJson.get(i).asJsonObject
                            messagesList.add(
                                ChatMessageEntity(
                                    id = msg.get("id").asLong,
                                    chatId = msg.get("room_id").asLong,
                                    senderId = msg.get("sender_id").asLong,
                                    senderName = msg.get("sender_name").asString,
                                    senderAvatar = if (msg.has("sender_avatar") && !msg.get("sender_avatar").isJsonNull) msg.get("sender_avatar").asString else null,
                                    message = msg.get("content").asString,
                                    messageType = msg.get("message_type").asString,
                                    timestamp = msg.get("created_at").asString,
                                    createdAt = msg.get("created_at").asString,
                                    isRead = true,
                                    isDeleted = if (msg.has("is_deleted")) msg.get("is_deleted").asBoolean else false,
                                    delivered = true,
                                    isPinned = if (msg.has("is_pinned")) msg.get("is_pinned").asBoolean else false,
                                    pinnedBy = null,
                                    pinnedAt = null,
                                    duration = null,
                                    metadataJson = null,
                                    bookingId = null,
                                    fileUrl = if (msg.has("file_url") && !msg.get("file_url").isJsonNull) msg.get("file_url").asString else null,
                                    mediaExpiresAt = null,
                                    localMediaPath = null,
                                    queued = false,
                                    clientMsgId = if (msg.has("local_id") && !msg.get("local_id").isJsonNull) msg.get("local_id").asString else null
                                )
                            )
                        }

                        totalMessages += messagesList.size
                        withContext(Dispatchers.IO) {
                            chatDao.upsertMessages(messagesList)
                            // Reconcile outbox if local_id (clientMsgId) matches
                            messagesList.forEach { m ->
                                if (m.clientMsgId != null) {
                                    chatDao.deleteOutboxByClientMsgId(m.clientMsgId)
                                }
                            }
                        }

                        hasMore = if (body.has("has_more")) body.get("has_more").asBoolean else false
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            Timber.d("Synced \$totalMessages messages total")
        } catch (e: Exception) {
            Timber.e(e, "Message sync failed")
            throw e
        }
    }

    private fun reportSyncState() {
        val now = java.time.Instant.now().toString()
        sharedPrefs.edit().putString("last_synced_at", now).apply()
        Timber.d("Reported sync state to local")
    }

    fun syncPendingOperations() {
        Timber.d("Triggering outbox flush...")
        outboxQueueManager.flush()
    }
}

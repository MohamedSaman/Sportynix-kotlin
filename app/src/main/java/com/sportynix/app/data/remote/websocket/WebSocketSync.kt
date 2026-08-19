package com.sportynix.app.data.remote.websocket

import com.sportynix.app.data.local.dao.ChatDao
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.domain.model.WebSocketMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketSync @Inject constructor(
    private val chatDao: ChatDao
) {
    suspend fun handleWebSocketChatMessage(event: WebSocketMessage) = withContext(Dispatchers.IO) {
        try {
            val entity = ChatMessageEntity(
                id = event.messageId?.toLong() ?: System.currentTimeMillis(),
                chatId = event.chatId?.toLong() ?: event.conversationId?.toLong() ?: 0L,
                senderId = event.senderId?.toLong() ?: 0L,
                senderName = event.senderName ?: "Unknown",
                senderAvatar = event.senderAvatar,
                message = event.message ?: "",
                messageType = event.messageType ?: "text",
                timestamp = event.timestamp ?: "",
                createdAt = event.timestamp ?: "",
                isRead = false,
                isDeleted = false,
                delivered = true,
                isPinned = event.isPinned ?: false,
                pinnedBy = event.pinnedBy?.toLong(),
                pinnedAt = event.pinnedAt,
                duration = event.duration,
                metadataJson = null, // event.metadata is Record<String, Any>, would need Gson to serialize
                bookingId = event.bookingId ?: when (val booking = event.booking) {
                    is Number -> booking.toLong()
                    is String -> booking.toLongOrNull()
                    else -> null
                },
                fileUrl = event.fileUrl,
                mediaExpiresAt = event.mediaExpiresAt,
                localMediaPath = null,
                queued = false,
                clientMsgId = event.localId?.toString()
            )
            
            chatDao.upsertMessage(entity)
            Timber.d("Upserted message from WebSocket: \${entity.id}")

            // Reconcile Outbox if the clientMsgId (local_id) matches
            if (entity.clientMsgId != null) {
                chatDao.deleteOutboxByClientMsgId(entity.clientMsgId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle WebSocket message")
        }
    }
}

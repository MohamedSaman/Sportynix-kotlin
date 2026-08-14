package com.sportynix.app.data.remote.websocket

import com.sportynix.app.data.local.dao.ChatDao
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.domain.model.websocket.WebSocketMessage
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
                id = event.message_id?.toLong() ?: System.currentTimeMillis(),
                chatId = event.chat_id?.toLong() ?: event.conversation_id?.toLong() ?: 0L,
                senderId = event.sender_id?.toLong() ?: 0L,
                senderName = event.sender_name ?: "Unknown",
                senderAvatar = event.sender_avatar,
                message = event.message ?: "",
                messageType = event.message_type ?: "text",
                timestamp = event.timestamp ?: "",
                createdAt = event.timestamp ?: "",
                isRead = false,
                isDeleted = false,
                delivered = true,
                isPinned = event.is_pinned ?: false,
                pinnedBy = event.pinned_by?.toLong(),
                pinnedAt = event.pinned_at,
                duration = event.duration,
                metadataJson = null, // event.metadata is Record<String, Any>, would need Gson to serialize
                bookingId = event.booking_id?.toLong() ?: event.booking?.toLong(),
                fileUrl = event.file_url,
                mediaExpiresAt = event.media_expires_at,
                localMediaPath = null,
                queued = false,
                clientMsgId = event.local_id?.toString()
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

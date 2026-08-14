package com.sportynix.app.domain.model.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebSocketMessage(
    val type: String = "message",
    val message: String? = null,
    @SerialName("message_type")
    val messageType: String? = null,
    @SerialName("media_data")
    val mediaData: String? = null,
    val duration: Int? = null,
    val metadata: JsonElement? = null,
    @SerialName("booking_id")
    val bookingId: Int? = null,
    val booking: Int? = null,
    @SerialName("booking_details")
    val bookingDetails: JsonElement? = null,
    val sender: String? = null,
    @SerialName("sender_id")
    val senderId: Int? = null,
    @SerialName("sender_name")
    val senderName: String? = null,
    @SerialName("sender_avatar")
    val senderAvatar: String? = null,
    val timestamp: String? = null,
    @SerialName("message_id")
    val messageId: Int? = null,
    @SerialName("local_id")
    val localId: Int? = null,
    @SerialName("is_typing")
    val isTyping: Boolean? = null,
    @SerialName("is_pinned")
    val isPinned: Boolean? = null,
    @SerialName("pinned_by")
    val pinnedBy: Int? = null,
    @SerialName("pinned_at")
    val pinnedAt: String? = null,
    val user: String? = null,
    @SerialName("user_id")
    val userId: Int? = null,
    @SerialName("follower_count")
    val followerCount: Int? = null,
    val error: String? = null,
    @SerialName("conversation_id")
    val conversationId: Int? = null,
    @SerialName("chat_id")
    val chatId: Int? = null,
    @SerialName("file_url")
    val fileUrl: String? = null,
    @SerialName("media_expires_at")
    val mediaExpiresAt: String? = null,
    @SerialName("user_name")
    val userName: String? = null,
    val online: Boolean? = null,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    val delivered: Boolean? = null
)

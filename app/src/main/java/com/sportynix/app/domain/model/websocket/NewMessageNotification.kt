package com.sportynix.app.domain.model.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewMessageNotification(
    @SerialName("chat_id")
    val chatId: Int,
    @SerialName("message_id")
    val messageId: Int? = null,
    val message: String,
    @SerialName("message_type")
    val messageType: String,
    @SerialName("sender_id")
    val senderId: Int,
    @SerialName("sender_name")
    val senderName: String,
    @SerialName("sender_avatar")
    val senderAvatar: String? = null,
    val timestamp: String,
    @SerialName("unread_count")
    val unreadCount: Int? = null
)

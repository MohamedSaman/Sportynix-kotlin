package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val messageType: String,
    val timestamp: String,
    val createdAt: String,
    val isRead: Boolean,
    val isDeleted: Boolean,
    val delivered: Boolean,
    val isPinned: Boolean,
    val pinnedBy: Long?,
    val pinnedAt: String?,
    val duration: Int?,
    val metadataJson: String?,
    val bookingId: Long?,
    val fileUrl: String?,
    val mediaExpiresAt: String?,
    val localMediaPath: String?,
    val queued: Boolean,
    val clientMsgId: String?
)

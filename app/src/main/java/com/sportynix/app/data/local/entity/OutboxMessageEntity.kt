package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox_messages")
data class OutboxMessageEntity(
    @PrimaryKey val clientMsgId: String,
    val chatId: Long,
    val tempMessageId: Long,
    val message: String,
    val messageType: String,
    val bookingId: Long?,
    val metadataJson: String?,
    val localMediaPath: String?,
    val senderId: Long?,
    val senderName: String?,
    val createdAt: String,
    val retryCount: Int = 0,
    val lastError: String? = null
)

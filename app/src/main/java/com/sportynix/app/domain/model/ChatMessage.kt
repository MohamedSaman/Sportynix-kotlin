package com.sportynix.app.domain.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val messageText: String,
    val timestamp: String,
    val isFromCurrentUser: Boolean
)

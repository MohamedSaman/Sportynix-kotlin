package com.sportynix.app.domain.model

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val type: String = "GENERAL",
    val deepLink: String? = null
)

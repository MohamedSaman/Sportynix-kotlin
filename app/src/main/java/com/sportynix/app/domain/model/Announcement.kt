package com.sportynix.app.domain.model

data class Announcement(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isPinned: Boolean = false,
    val bgColor: String = "#1a8553",
    val imageUrl: String? = null,
    val actionUrl: String? = null
)

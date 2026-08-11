package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_chats")
data class HiddenChatEntity(
    @PrimaryKey val chatId: Long
)

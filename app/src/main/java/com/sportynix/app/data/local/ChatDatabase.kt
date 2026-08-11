package com.sportynix.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sportynix.app.data.local.dao.ChatDao
import com.sportynix.app.data.local.entity.ChatMessageEntity
import com.sportynix.app.data.local.entity.ClearedCutoffEntity
import com.sportynix.app.data.local.entity.HiddenChatEntity
import com.sportynix.app.data.local.entity.OutboxMessageEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        OutboxMessageEntity::class,
        HiddenChatEntity::class,
        ClearedCutoffEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

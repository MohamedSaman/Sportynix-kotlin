package com.sportynix.app.data.local.dao

import androidx.room.*
import com.sportynix.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY id ASC")
    fun getMessagesForChatFlow(chatId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY id ASC")
    suspend fun getMessagesForChat(chatId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentMessages(chatId: Long, limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId AND id = :messageId")
    suspend fun deleteMessage(chatId: Long, messageId: Long)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: Long)

    // Outbox Operations
    @Query("SELECT * FROM outbox_messages ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getOutboxMessages(limit: Int = 50): List<OutboxMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(message: OutboxMessageEntity)

    @Query("DELETE FROM outbox_messages WHERE clientMsgId = :clientMsgId")
    suspend fun deleteOutboxByClientMsgId(clientMsgId: String)

    @Query("DELETE FROM outbox_messages WHERE tempMessageId = :tempId")
    suspend fun deleteOutboxByTempId(tempId: Long)

    @Query("UPDATE outbox_messages SET retryCount = retryCount + 1, lastError = :error WHERE clientMsgId = :clientMsgId")
    suspend fun bumpOutboxRetry(clientMsgId: String, error: String)

    // Hidden Chats
    @Query("SELECT chatId FROM hidden_chats")
    suspend fun getHiddenChatIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideChat(hidden: HiddenChatEntity)

    @Query("DELETE FROM hidden_chats WHERE chatId = :chatId")
    suspend fun unhideChat(chatId: Long)

    // Cleared Cutoffs
    @Query("SELECT cutoffIso FROM cleared_cutoffs WHERE chatId = :chatId")
    suspend fun getCutoff(chatId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCutoff(cutoff: ClearedCutoffEntity)

    @Query("DELETE FROM cleared_cutoffs WHERE chatId = :chatId")
    suspend fun clearCutoff(chatId: Long)
}

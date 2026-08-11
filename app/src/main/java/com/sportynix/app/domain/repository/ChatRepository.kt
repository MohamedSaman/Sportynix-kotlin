package com.sportynix.app.domain.repository

import com.sportynix.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    fun getMyChatsCachedFirst(): Flow<List<Chat>>
    suspend fun fetchMyChats(): Result<List<Chat>>
    suspend fun discoverChannels(search: String? = null): Result<List<Chat>>
    suspend fun followChannel(chatId: Long): Result<Pair<String, Int>>
    suspend fun unfollowChannel(chatId: Long): Result<Pair<String, Int>>
    suspend fun getFollowers(chatId: Long): Result<List<Follower>>
    suspend fun deleteChatForEveryone(chatId: Long): Result<Unit>
    suspend fun hideChatForMe(chatId: Long): Result<Unit>
    suspend fun clearChatForMe(chatId: Long): Result<Unit>

    suspend fun getMessagesWithSync(chatId: Long, limit: Int = 50): Flow<List<ChatMessage>>
    suspend fun fetchMessagesFromServer(chatId: Long, limit: Int = 50, before: String? = null): Result<List<ChatMessage>>
    suspend fun syncMessagesForChat(chatId: Long, lastSyncTimestamp: String? = null): Result<List<ChatMessage>>

    suspend fun sendMessage(
        chatId: Long,
        message: String,
        messageType: String = "text",
        bookingId: Long? = null,
        metadata: Map<String, Any>? = null,
        tempMessageId: Long? = null
    ): Result<SendMessageResponse>

    suspend fun queueMediaMessage(
        chatId: Long,
        mediaType: String,
        localMediaPath: String,
        caption: String = "",
        duration: Int? = null,
        metadata: Map<String, Any>? = null
    ): Result<Unit>

    suspend fun editMessage(chatId: Long, messageId: Long, newMessage: String): Result<ChatMessage>
    suspend fun deleteMessage(chatId: Long, messageId: Long): Result<Unit>
    suspend fun deleteMessageForMe(chatId: Long, messageId: Long): Result<Unit>
    suspend fun pinMessage(chatId: Long, messageId: Long): Result<Unit>
    suspend fun unpinMessage(chatId: Long, messageId: Long): Result<Unit>
    suspend fun markAsRead(chatId: Long, messageIds: List<Long>): Result<Unit>
    suspend fun markDelivered(chatId: Long, messageId: Long): Result<Unit>

    suspend fun getTeamChat(teamId: Long): Result<Chat>
    suspend fun getDirectChat(userId: Long): Result<Chat>

    suspend fun getBlockedUsers(): Result<List<BlockedUser>>
    suspend fun blockUser(userId: Long): Result<Unit>
    suspend fun unblockUser(userId: Long): Result<Unit>

    suspend fun getUserReports(): Result<List<UserReportItem>>
    suspend fun reportUser(reportedUserId: Long, reason: String, notes: String? = null, chatId: Long? = null): Result<Unit>
    suspend fun cancelUserReport(reportId: Long): Result<Unit>

    suspend fun getChatRequests(): Result<ChatRequestListResponse>
    suspend fun sendChatRequest(userId: Long): Result<ChatRequestItem>
    suspend fun acceptChatRequest(requestId: Long): Result<Pair<ChatRequestItem, Chat>>
    suspend fun declineChatRequest(requestId: Long): Result<ChatRequestItem>
    suspend fun cancelChatRequest(requestId: Long): Result<ChatRequestItem>

    suspend fun getMutualUsers(query: String? = null): Result<List<MutualUser>>
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>>

    suspend fun registerDevice(deviceToken: String, onesignalId: String): Result<Unit>
    suspend fun getUnreadCount(): Result<Int>

    suspend fun getChatMembers(chatId: Long): Result<List<ChatMember>>
    suspend fun addAdmin(chatId: Long, userId: Long): Result<Unit>
    suspend fun removeAdmin(chatId: Long, userId: Long): Result<Unit>
    suspend fun toggleAdminOnly(chatId: Long): Result<Boolean>

    suspend fun getChatDetails(chatId: Long, historyPage: Int = 1): Result<Chat>
    suspend fun downloadMedia(chatId: Long, messageId: Long, destinationFile: File): Result<File>

    suspend fun getPhotoMessages(chatId: Long, limit: Int = 0): Result<List<ChatMessage>>
    suspend fun getEventMessages(chatId: Long, limit: Int = 0): Result<List<ChatMessage>>
    suspend fun getAllMediaMessages(chatId: Long, limit: Int = 0): Result<List<ChatMessage>>

    suspend fun syncQueuedOutbox(): Result<Unit>
}

package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.domain.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApiService {

    @GET("api/chats/my_chats/")
    suspend fun getMyChats(): Response<JsonElement>

    @GET("api/chats/discover_channels/")
    suspend fun discoverChannels(
        @Query("search") search: String? = null
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/follow/")
    suspend fun followChannel(
        @Path("chatId") chatId: Long
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/unfollow/")
    suspend fun unfollowChannel(
        @Path("chatId") chatId: Long
    ): Response<JsonElement>

    @GET("api/chats/{chatId}/followers/")
    suspend fun getFollowers(
        @Path("chatId") chatId: Long
    ): Response<List<Follower>>

    @DELETE("api/chats/{chatId}/")
    suspend fun deleteChatForEveryone(
        @Path("chatId") chatId: Long
    ): Response<Unit>

    @GET("api/chats/{chatId}/messages/")
    suspend fun getMessages(
        @Path("chatId") chatId: Long,
        @Query("page_size") pageSize: Int = 50,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null
    ): Response<JsonElement>

    @GET("api/sync/messages/")
    suspend fun syncMessages(
        @Query("device_id") deviceId: String,
        @Query("since") since: String? = null,
        @Query("limit") limit: Int = 200
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/send_message/")
    suspend fun sendMessage(
        @Path("chatId") chatId: Long,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @PATCH("api/chats/{chatId}/edit_message/{messageId}/")
    suspend fun editMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long,
        @Body body: Map<String, String>
    ): Response<ChatMessage>

    @DELETE("api/chats/{chatId}/messages/{messageId}/")
    suspend fun deleteMessage(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long
    ): Response<Unit>

    @POST("api/chats/{chatId}/pin_message/")
    suspend fun pinMessage(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/unpin_message/")
    suspend fun unpinMessage(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/mark_as_read/")
    suspend fun markAsRead(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, List<Long>>
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/mark_delivered/")
    suspend fun markDelivered(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @POST("api/teams/{teamId}/get_team_chat/")
    suspend fun getTeamChat(
        @Path("teamId") teamId: Long
    ): Response<Chat>

    @POST("api/chats/direct/{userId}/")
    suspend fun getDirectChat(
        @Path("userId") userId: Long
    ): Response<Chat>

    @GET("api/chats/blocked-users/")
    suspend fun getBlockedUsers(): Response<JsonElement>

    @POST("api/chats/blocked-users/")
    suspend fun blockUser(
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @DELETE("api/chats/blocked-users/{userId}/")
    suspend fun unblockUser(
        @Path("userId") userId: Long
    ): Response<JsonElement>

    @GET("api/chats/reports/")
    suspend fun getUserReports(): Response<List<UserReportItem>>

    @POST("api/chats/reports/")
    suspend fun reportUser(
        @Body body: Map<String, Any?>
    ): Response<JsonElement>

    @DELETE("api/chats/reports/{reportId}/")
    suspend fun cancelUserReport(
        @Path("reportId") reportId: Long
    ): Response<JsonElement>

    @GET("api/chats/chat_requests/")
    suspend fun getChatRequests(): Response<ChatRequestListResponse>

    @POST("api/chats/chat_requests/send/")
    suspend fun sendChatRequest(
        @Body body: Map<String, Long>
    ): Response<ChatRequestItem>

    @POST("api/chats/chat_requests/{requestId}/accept/")
    suspend fun acceptChatRequest(
        @Path("requestId") requestId: Long
    ): Response<JsonElement>

    @POST("api/chats/chat_requests/{requestId}/decline/")
    suspend fun declineChatRequest(
        @Path("requestId") requestId: Long
    ): Response<ChatRequestItem>

    @POST("api/chats/chat_requests/{requestId}/cancel/")
    suspend fun cancelChatRequest(
        @Path("requestId") requestId: Long
    ): Response<ChatRequestItem>

    @GET("api/chats/mutual_users/")
    suspend fun getMutualUsers(
        @Query("q") query: String? = null
    ): Response<List<MutualUser>>

    @GET("api/users/search/")
    suspend fun searchUsers(
        @Query("q") query: String
    ): Response<JsonElement>

    @POST("api/auth/register-device/")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<JsonElement>

    @GET("api/chats/unread-count/")
    suspend fun getUnreadCount(): Response<JsonElement>

    @GET("api/chats/{chatId}/members/")
    suspend fun getChatMembers(
        @Path("chatId") chatId: Long
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/add_admin/")
    suspend fun addAdmin(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/remove_admin/")
    suspend fun removeAdmin(
        @Path("chatId") chatId: Long,
        @Body body: Map<String, Long>
    ): Response<JsonElement>

    @POST("api/chats/{chatId}/toggle_admin_only/")
    suspend fun toggleAdminOnly(
        @Path("chatId") chatId: Long
    ): Response<JsonElement>

    @GET("api/chats/{chatId}/")
    suspend fun getChatDetails(
        @Path("chatId") chatId: Long,
        @Query("history_page") historyPage: Int = 1,
        @Query("history_page_size") historyPageSize: Int = 10
    ): Response<JsonElement>

    @GET("api/chats/{chatId}/messages/{messageId}/download/")
    suspend fun downloadMedia(
        @Path("chatId") chatId: Long,
        @Path("messageId") messageId: Long
    ): Response<ResponseBody>
}

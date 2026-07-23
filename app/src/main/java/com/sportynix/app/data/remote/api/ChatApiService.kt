package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.ChatConversationDto
import com.sportynix.app.data.remote.dto.ChatMessageDto
import com.sportynix.app.data.remote.dto.PaginatedMessagesDto
import com.sportynix.app.data.remote.dto.SendMessageRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {

    @GET("api/chats/")
    suspend fun getConversations(): Response<List<ChatConversationDto>>

    @GET("api/chats/{roomId}/messages/")
    suspend fun getMessages(
        @Path("roomId") roomId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 30
    ): Response<PaginatedMessagesDto>

    @POST("api/chats/{roomId}/messages/")
    suspend fun sendMessage(
        @Path("roomId") roomId: String,
        @Body request: SendMessageRequestDto
    ): Response<ChatMessageDto>

    @GET("api/chats/unread_count/")
    suspend fun getUnreadMessageCount(): Response<JsonElement>
}

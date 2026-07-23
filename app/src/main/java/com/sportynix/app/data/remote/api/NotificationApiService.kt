package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.MarkReadResponseDto
import com.sportynix.app.data.remote.dto.PaginatedNotificationsDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {
    @GET("api/notifications/")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<PaginatedNotificationsDto>

    @POST("api/notifications/{id}/mark_read/")
    suspend fun markAsRead(@Path("id") id: String): Response<MarkReadResponseDto>

    @POST("api/notifications/mark_all_read/")
    suspend fun markAllAsRead(): Response<JsonElement>

    @DELETE("api/notifications/{id}/")
    suspend fun deleteNotification(@Path("id") id: String): Response<Unit>

    @POST("api/notifications/clear_all/")
    suspend fun clearAllNotifications(): Response<JsonElement>
}

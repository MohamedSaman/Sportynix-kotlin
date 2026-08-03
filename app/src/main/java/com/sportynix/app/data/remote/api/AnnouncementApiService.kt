package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AnnouncementApiService {

    @GET("api/announcements/")
    suspend fun getAnnouncements(): Response<JsonElement>

    @GET("api/announcements/{id}/")
    suspend fun getAnnouncement(@Path("id") id: String): Response<JsonElement>
}

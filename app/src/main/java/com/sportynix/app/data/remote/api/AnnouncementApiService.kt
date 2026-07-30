package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET

interface AnnouncementApiService {

    @GET("api/announcements/")
    suspend fun getAnnouncements(): Response<JsonElement>
}

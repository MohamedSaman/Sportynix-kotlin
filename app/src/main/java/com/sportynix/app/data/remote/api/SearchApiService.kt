package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {
    @GET("api/sports/")
    suspend fun getSports(
        @Query("search") search: String? = null
    ): Response<JsonElement>

    @GET("api/teams/")
    suspend fun getTeams(
        @Query("search") search: String? = null
    ): Response<JsonElement>
}

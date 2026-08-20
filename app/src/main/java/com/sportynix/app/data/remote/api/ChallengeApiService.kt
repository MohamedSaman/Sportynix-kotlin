package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChallengeApiService {
    @GET("api/challenges/teams_for_challenge/") suspend fun myTeams(@Query("page") page: Int = 1): Response<JsonElement>
    @GET("api/challenges/available_opponents/") suspend fun opponents(@Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 10, @Query("search") search: String? = null): Response<JsonElement>
    @GET("api/challenges/sports/") suspend fun sports(): Response<JsonElement>
    @GET("api/challenges/venues_for_sport/") suspend fun venues(@Query("sport_id") sportId: Int): Response<JsonElement>
    @GET("api/challenges/sent/") suspend fun sent(@Query("page") page: Int = 1, @Query("page_size") pageSize: Int = 50): Response<JsonElement>
    @GET("api/challenges/incoming/") suspend fun incoming(@Query("status") status: String? = "pending"): Response<JsonElement>
    @GET("api/challenges/history/") suspend fun history(): Response<JsonElement>
    @GET("api/challenges/{id}/") suspend fun details(@Path("id") id: Int): Response<JsonElement>
    @GET("api/challenges/relationships/") suspend fun relationships(): Response<JsonElement>
    @POST("api/challenges/") suspend fun create(@Body body: JsonObject): Response<JsonElement>
    @POST("api/challenges/{id}/accept/") suspend fun accept(@Path("id") id: Int): Response<JsonElement>
    @POST("api/challenges/{id}/decline/") suspend fun decline(@Path("id") id: Int): Response<JsonElement>
    @POST("api/challenges/{id}/cancel/") suspend fun cancel(@Path("id") id: Int): Response<JsonElement>
}

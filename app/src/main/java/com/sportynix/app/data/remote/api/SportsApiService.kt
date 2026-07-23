package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SportsApiService {

    @GET("api/teams/")
    suspend fun getTeams(
        @Query("page") page: Int? = null,
        @Query("search") search: String? = null
    ): Response<JsonElement>

    @GET("api/teams/{id}/")
    suspend fun getTeamById(@Path("id") id: String): Response<JsonElement>

    @GET("api/league/matches/")
    suspend fun getLeagueMatches(
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null
    ): Response<JsonElement>

    @GET("api/tournaments/tournaments/")
    suspend fun getTournaments(
        @Query("page") page: Int? = null,
        @Query("status") status: String? = null
    ): Response<JsonElement>
}

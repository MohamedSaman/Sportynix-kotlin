package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.FixtureDto
import com.sportynix.app.data.remote.dto.LeagueDto
import com.sportynix.app.data.remote.dto.LeagueTeamDto
import com.sportynix.app.data.remote.dto.PlayerStatDto
import com.sportynix.app.data.remote.dto.StandingDto
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LeagueApiService {

    @GET("api/league/leagues/")
    suspend fun getLeagues(
        @Query("search") search: String? = null,
        @Query("sport") sport: String? = null,
        @Query("venue_id") venueId: String? = null
    ): List<LeagueDto>

    @GET("api/league/leagues/{id}/")
    suspend fun getLeagueDetail(
        @Path("id") leagueId: String
    ): LeagueDto

    @GET("api/league/leagues/{id}/fixtures/")
    suspend fun getLeagueFixtures(
        @Path("id") leagueId: String
    ): List<FixtureDto>

    @GET("api/league/leagues/{id}/standings/")
    suspend fun getLeagueStandings(
        @Path("id") leagueId: String
    ): List<StandingDto>

    @GET("api/league/leagues/{id}/stats/")
    suspend fun getLeagueStats(
        @Path("id") leagueId: String
    ): List<PlayerStatDto>

    @GET("api/league/league-teams/")
    suspend fun getLeagueTeams(
        @Query("league") leagueId: String? = null
    ): List<LeagueTeamDto>

    @POST("api/league/leagues/{id}/register-team/")
    suspend fun registerTeam(@Path("id") leagueId: String, @Body body: JsonObject): Response<JsonElement>
}

package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LeagueApiService {

    @GET("api/league/leagues/")
    suspend fun getLeagues(
        @Query("search") search: String? = null,
        @Query("sport_type") sportType: String? = null,
        @Query("status") status: String? = null,
        @Query("format") format: String? = null,
        @Query("season_year") seasonYear: Int? = null,
        @Query("is_public") isPublic: Boolean? = null,
        @Query("is_featured") isFeatured: Boolean? = null,
        @Query("venue_id") venueId: String? = null
    ): List<FullLeagueDto>

    @GET("api/league/leagues/{id}/")
    suspend fun getLeagueDetail(
        @Path("id") leagueId: String
    ): FullLeagueDto

    @Multipart
    @POST("api/league/leagues/")
    suspend fun createLeague(
        @Part("data") data: RequestBody,
        @Part logo: MultipartBody.Part? = null,
        @Part banner: MultipartBody.Part? = null
    ): FullLeagueDto

    @Multipart
    @PATCH("api/league/leagues/{id}/")
    suspend fun updateLeague(
        @Path("id") leagueId: String,
        @Part("data") data: RequestBody,
        @Part logo: MultipartBody.Part? = null,
        @Part banner: MultipartBody.Part? = null
    ): FullLeagueDto

    @POST("api/league/leagues/{id}/publish/")
    suspend fun publishLeague(@Path("id") leagueId: String): Response<JsonElement>

    @POST("api/league/leagues/{id}/start/")
    suspend fun startLeague(@Path("id") leagueId: String): Response<JsonElement>

    @POST("api/league/leagues/{id}/suspend/")
    suspend fun suspendLeague(@Path("id") leagueId: String): Response<JsonElement>

    @POST("api/league/leagues/{id}/complete/")
    suspend fun completeLeague(@Path("id") leagueId: String): Response<JsonElement>

    @POST("api/league/leagues/{id}/cancel/")
    suspend fun cancelLeague(@Path("id") leagueId: String): Response<JsonElement>

    @GET("api/league/leagues/{id}/teams/")
    suspend fun getLeagueTeams(@Path("id") leagueId: String): List<FullLeagueTeamDto>

    @GET("api/league/league-teams/{team_id}/")
    suspend fun getLeagueTeamDetail(@Path("team_id") teamId: String): FullLeagueTeamDto

    @POST("api/league/leagues/{id}/register-team/")
    suspend fun registerTeam(@Path("id") leagueId: String, @Body body: JsonObject): Response<JsonElement>

    @POST("api/league/league-teams/{team_id}/squad/")
    suspend fun addSquadMember(
        @Path("team_id") teamId: String,
        @Body body: JsonObject
    ): SquadMemberDto

    @POST("api/league/league-teams/{team_id}/squad/bulk/")
    suspend fun bulkAddSquadMembers(
        @Path("team_id") teamId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @DELETE("api/league/league-teams/{team_id}/squad/{member_id}/")
    suspend fun removeSquadMember(
        @Path("team_id") teamId: String,
        @Path("member_id") memberId: String
    ): Response<Unit>

    @PATCH("api/league/league-teams/{team_id}/squad/{member_id}/")
    suspend fun updateSquadMember(
        @Path("team_id") teamId: String,
        @Path("member_id") memberId: String,
        @Body body: JsonObject
    ): SquadMemberDto

    @POST("api/league/league-teams/{team_id}/co-admins/")
    suspend fun addCoAdmin(
        @Path("team_id") teamId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @DELETE("api/league/league-teams/{team_id}/co-admins/{user_id}/")
    suspend fun removeCoAdmin(
        @Path("team_id") teamId: String,
        @Path("user_id") userId: String
    ): Response<Unit>

    @GET("api/league/leagues/{id}/applications/")
    suspend fun getLeaguePlayerApplications(@Path("id") leagueId: String): List<LeaguePlayerApplicationDto>

    @POST("api/league/leagues/{id}/applications/")
    suspend fun applyAsPlayer(
        @Path("id") leagueId: String,
        @Body body: JsonObject
    ): LeaguePlayerApplicationDto

    @POST("api/league/player-applications/{app_id}/review/")
    suspend fun reviewApplication(
        @Path("app_id") appId: String,
        @Body body: JsonObject
    ): LeaguePlayerApplicationDto

    @POST("api/league/player-applications/bulk-review/")
    suspend fun bulkReviewApplications(@Body body: BulkReviewRequestDto): Response<JsonElement>

    @POST("api/league/player-applications/{app_id}/withdraw/")
    suspend fun withdrawApplication(@Path("app_id") appId: String): Response<Unit>

    @GET("api/league/leagues/{id}/fixtures/")
    suspend fun getLeagueFixtures(@Path("id") leagueId: String): List<FixtureDto>

    @POST("api/league/leagues/{id}/generate-schedule/")
    suspend fun generateSchedule(
        @Path("id") leagueId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @GET("api/league/leagues/{id}/standings/")
    suspend fun getLeagueStandings(@Path("id") leagueId: String): List<FullStandingDto>

    @GET("api/league/leagues/{id}/stats/")
    suspend fun getLeagueStats(@Path("id") leagueId: String): List<PlayerStatDto>

    @POST("api/league/matches/{id}/pause/")
    suspend fun pauseMatch(@Path("id") matchId: String): Response<JsonElement>

    @POST("api/league/matches/{id}/resume/")
    suspend fun resumeMatch(@Path("id") matchId: String): Response<JsonElement>
}

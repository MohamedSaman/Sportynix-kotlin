package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface TournamentApiService {

    @GET("api/tournaments/tournaments/")
    suspend fun getTournaments(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("sport_type") sportType: String? = null,
        @Query("venue_id") venueId: String? = null
    ): List<FullTournamentDto>

    @GET("api/tournaments/tournaments/{id}/")
    suspend fun getTournamentDetail(
        @Path("id") tournamentId: String
    ): FullTournamentDto

    @Multipart
    @POST("api/tournaments/tournaments/")
    suspend fun createTournamentMultipart(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part banner: MultipartBody.Part? = null
    ): FullTournamentDto

    @POST("api/tournaments/tournaments/")
    suspend fun createTournament(
        @Body body: JsonObject
    ): FullTournamentDto

    @DELETE("api/tournaments/tournaments/{id}/")
    suspend fun deleteTournament(
        @Path("id") tournamentId: String
    ): Response<Unit>

    @POST("api/tournaments/tournaments/{id}/publish/")
    suspend fun publishTournament(
        @Path("id") tournamentId: String
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/open-applications/")
    suspend fun openApplications(
        @Path("id") tournamentId: String
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/close-applications/")
    suspend fun closeApplications(
        @Path("id") tournamentId: String
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/finalize-teams/")
    suspend fun finalizeTeams(
        @Path("id") tournamentId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/generate-matches/")
    suspend fun generateMatches(
        @Path("id") tournamentId: String
    ): Response<JsonElement>

    @GET("api/tournaments/tournaments/{id}/eligible-teams/")
    suspend fun getEligibleTeams(
        @Path("id") tournamentId: String
    ): EligibleTeamsResponseDto

    @POST("api/tournaments/tournaments/{id}/apply-team/")
    suspend fun applyTeam(
        @Path("id") tournamentId: String,
        @Body body: TournamentApplyPayloadDto
    ): TournamentApplicationDto

    @GET("api/tournaments/tournaments/{id}/applications/")
    suspend fun getApplications(
        @Path("id") tournamentId: String,
        @Query("status") status: String? = null
    ): List<TournamentApplicationDto>

    @PATCH("api/tournaments/applications/{id}/review/")
    suspend fun reviewApplication(
        @Path("id") applicationId: String,
        @Body body: TournamentReviewPayloadDto
    ): TournamentApplicationDto

    @GET("api/tournaments/matches/")
    suspend fun getMatches(
        @Query("tournament") tournamentId: String
    ): List<FullTournamentMatchDto>

    @POST("api/tournaments/matches/{id}/attach-league-match/")
    suspend fun attachScoringLink(
        @Path("id") matchId: String,
        @Body body: JsonObject = JsonObject()
    ): Response<JsonElement>

    @GET("api/tournaments/participations/")
    suspend fun getParticipations(
        @Query("tournament") tournamentId: String
    ): List<TournamentParticipationDto>

    @POST("api/tournaments/tournaments/{id}/approve/")
    suspend fun approveTournament(
        @Path("id") tournamentId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/reject/")
    suspend fun rejectTournament(
        @Path("id") tournamentId: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    @POST("api/tournaments/tournaments/{id}/request-edit/")
    suspend fun requestTournamentEdit(
        @Path("id") tournamentId: String,
        @Body body: JsonObject
    ): TournamentEditRequestDto

    @GET("api/tournaments/tournaments/{id}/edit-requests/")
    suspend fun getTournamentEditRequests(
        @Path("id") tournamentId: String
    ): List<TournamentEditRequestDto>

    @POST("api/tournaments/tournaments/edit-requests/{id}/review/")
    suspend fun reviewTournamentEditRequest(
        @Path("id") requestId: String,
        @Body body: JsonObject
    ): TournamentEditRequestDto

    @POST("api/tournaments/applications/{id}/request-edit/")
    suspend fun requestApplicationEdit(
        @Path("id") applicationId: String,
        @Body body: JsonObject
    ): TournamentApplicationDto

    @POST("api/tournaments/applications/{id}/review-edit-request/")
    suspend fun reviewApplicationEditRequest(
        @Path("id") applicationId: String,
        @Body body: JsonObject
    ): TournamentApplicationDto

    @PATCH("api/tournaments/tournaments/{id}/limited-update/")
    suspend fun limitedUpdateTournament(
        @Path("id") tournamentId: String,
        @Body body: JsonObject
    ): FullTournamentDto

    @GET("api/teams/{id}/")
    suspend fun getTeamDetails(
        @Path("id") teamId: String
    ): JsonObject

    @GET("api/booking/teams/{id}/")
    suspend fun getBookingTeamDetails(
        @Path("id") teamId: String
    ): JsonObject
}

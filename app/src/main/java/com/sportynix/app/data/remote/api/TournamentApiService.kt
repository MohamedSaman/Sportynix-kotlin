package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.BracketDto
import com.sportynix.app.data.remote.dto.TournamentDto
import com.sportynix.app.data.remote.dto.TournamentMatchDto
import com.sportynix.app.data.remote.dto.TournamentRegistrationRequestDto
import com.sportynix.app.data.remote.dto.TournamentRegistrationResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TournamentApiService {

    @GET("api/tournaments/")
    suspend fun getTournaments(
        @Query("search") search: String? = null,
        @Query("sport") sport: String? = null,
        @Query("venue_id") venueId: String? = null
    ): List<TournamentDto>

    @GET("api/tournaments/{id}/")
    suspend fun getTournamentDetail(
        @Path("id") tournamentId: String
    ): TournamentDto

    @GET("api/tournaments/{id}/matches/")
    suspend fun getTournamentMatches(
        @Path("id") tournamentId: String
    ): List<TournamentMatchDto>

    @GET("api/tournaments/{id}/brackets/")
    suspend fun getTournamentBrackets(
        @Path("id") tournamentId: String
    ): List<BracketDto>

    @POST("api/tournaments/{id}/register/")
    suspend fun registerForTournament(
        @Path("id") tournamentId: String,
        @Body request: TournamentRegistrationRequestDto
    ): TournamentRegistrationResponseDto
}

package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.*
import retrofit2.http.*

interface CricketScoringApiService {

    @POST("api/league/cricket-scoring/{matchId}/start/")
    suspend fun startMatch(
        @Path("matchId") matchId: String,
        @Body request: StartMatchRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/end/")
    suspend fun endMatch(
        @Path("matchId") matchId: String,
        @Body request: EndMatchRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/start-innings/")
    suspend fun startInnings(
        @Path("matchId") matchId: String,
        @Body request: StartInningsRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/end-innings/")
    suspend fun endInnings(
        @Path("matchId") matchId: String
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/set-batsmen/")
    suspend fun setBatsmen(
        @Path("matchId") matchId: String,
        @Body request: SetBatsmenRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/set-bowler/")
    suspend fun setBowler(
        @Path("matchId") matchId: String,
        @Body request: SetBowlerRequestDto
    ): LiveStateDto

    @GET("api/league/cricket-scoring/{matchId}/playing-xi/")
    suspend fun getPlayingXI(
        @Path("matchId") matchId: String
    ): PlayingXIResponseDto

    @POST("api/league/cricket-scoring/{matchId}/playing-xi/")
    suspend fun setPlayingXI(
        @Path("matchId") matchId: String,
        @Body request: PlayingXIRequestDto
    ): PlayingXIResponseDto

    @POST("api/league/cricket-scoring/{matchId}/record-ball/")
    suspend fun recordBall(
        @Path("matchId") matchId: String,
        @Body request: RecordBallRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/record-penalty/")
    suspend fun recordPenalty(
        @Path("matchId") matchId: String,
        @Body request: RecordPenaltyRequestDto
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/swap-batsmen/")
    suspend fun swapBatsmen(
        @Path("matchId") matchId: String
    ): LiveStateDto

    @POST("api/league/cricket-scoring/{matchId}/undo-last-ball/")
    suspend fun undoLastBall(
        @Path("matchId") matchId: String
    ): LiveStateDto

    @GET("api/league/cricket-scoring/{matchId}/live-state/")
    suspend fun getLiveState(
        @Path("matchId") matchId: String
    ): LiveStateDto

    @GET("api/league/cricket-scoring/{matchId}/scorecard/")
    suspend fun getScorecard(
        @Path("matchId") matchId: String
    ): ScorecardDto

    @GET("api/league/cricket-scoring/{matchId}/eligible-batsmen/")
    suspend fun getEligibleBatsmen(
        @Path("matchId") matchId: String
    ): EligibleBatsmenResponseDto

    @GET("api/league/cricket-scoring/{matchId}/ball-by-ball/")
    suspend fun getBallByBall(
        @Path("matchId") matchId: String,
        @Query("innings") innings: Int? = null
    ): BallByBallResponseDto

    @GET("api/league/cricket-scoring/{matchId}/summary/")
    suspend fun getMatchSummary(
        @Path("matchId") matchId: String
    ): MatchSummaryDto

    @GET("api/league/cricket-scoring/{matchId}/mom-suggestion/")
    suspend fun getMOMSuggestion(
        @Path("matchId") matchId: String
    ): MOMSuggestionResponseDto

    @POST("api/league/cricket-scoring/{matchId}/finalize-mom/")
    suspend fun finalizeMOM(
        @Path("matchId") matchId: String,
        @Body request: FinalizeMOMRequestDto
    ): LiveStateDto
}

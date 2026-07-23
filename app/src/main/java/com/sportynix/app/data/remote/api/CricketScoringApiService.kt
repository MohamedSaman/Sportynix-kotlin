package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.BallRecordRequestDto
import com.sportynix.app.data.remote.dto.BallRecordResponseDto
import com.sportynix.app.data.remote.dto.CricketMatchDto
import com.sportynix.app.data.remote.dto.LiveScorecardDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CricketScoringApiService {

    @GET("api/cricket-scoring/matches/")
    suspend fun getCricketMatches(): List<CricketMatchDto>

    @GET("api/cricket-scoring/matches/{id}/live/")
    suspend fun getLiveMatchDetails(
        @Path("id") matchId: String
    ): LiveScorecardDto

    @GET("api/cricket-scoring/matches/{id}/scorecard/")
    suspend fun getFullScorecard(
        @Path("id") matchId: String
    ): LiveScorecardDto

    @POST("api/cricket-scoring/matches/{id}/record-ball/")
    suspend fun recordBall(
        @Path("id") matchId: String,
        @Body request: BallRecordRequestDto
    ): BallRecordResponseDto

    @POST("api/cricket-scoring/matches/{id}/undo-ball/")
    suspend fun undoLastBall(
        @Path("id") matchId: String
    ): BallRecordResponseDto
}

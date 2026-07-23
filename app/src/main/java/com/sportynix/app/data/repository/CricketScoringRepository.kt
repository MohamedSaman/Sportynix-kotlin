package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.core.network.WebSocketManager
import com.sportynix.app.data.remote.api.CricketScoringApiService
import com.sportynix.app.data.remote.dto.BallRecordRequestDto
import com.sportynix.app.data.remote.dto.BallRecordResponseDto
import com.sportynix.app.data.remote.dto.CricketMatchDto
import com.sportynix.app.data.remote.dto.LiveScorecardDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CricketScoringRepository @Inject constructor(
    private val apiService: CricketScoringApiService,
    private val webSocketManager: WebSocketManager
) {
    suspend fun getCricketMatches(): ApiResult<List<CricketMatchDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val matches = apiService.getCricketMatches()
                ApiResult.Success(matches)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load cricket matches")
            }
        }
    }

    suspend fun getLiveMatchDetails(matchId: String): ApiResult<LiveScorecardDto> {
        return withContext(Dispatchers.IO) {
            try {
                val scorecard = apiService.getLiveMatchDetails(matchId)
                ApiResult.Success(scorecard)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load live scorecard")
            }
        }
    }

    suspend fun recordBall(matchId: String, runs: Int, isWicket: Boolean, isWide: Boolean, isNoBall: Boolean, isBoundary: Boolean, isSix: Boolean): ApiResult<BallRecordResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val req = BallRecordRequestDto(runs, isWicket, isWide, isNoBall, isBoundary, isSix)
                val response = apiService.recordBall(matchId, req)
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to record ball")
            }
        }
    }

    suspend fun undoLastBall(matchId: String): ApiResult<BallRecordResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.undoLastBall(matchId)
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to undo ball")
            }
        }
    }

    fun connectLiveMatchWebSocket(matchId: String) {
        webSocketManager.connect("ws/live-match/$matchId/")
    }

    fun disconnectLiveMatchWebSocket() {
        webSocketManager.disconnect()
    }

    val liveWebSocketMessages = webSocketManager.incomingMessages
}

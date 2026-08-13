package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.CricketScoringApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.websocket.CricketMatchWebSocketManager
import com.sportynix.app.data.remote.websocket.CricketSocketEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CricketScoringRepository @Inject constructor(
    private val apiService: CricketScoringApiService,
    private val webSocketManager: CricketMatchWebSocketManager
) {
    val liveStateSocket: StateFlow<LiveStateDto?> = webSocketManager.liveState
    val isSocketConnected: StateFlow<Boolean> = webSocketManager.isConnected
    val socketEvents: SharedFlow<CricketSocketEvent> = webSocketManager.socketEvents

    fun connectWebSocket(matchId: String) {
        webSocketManager.connectToMatch(matchId)
    }

    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    suspend fun startMatch(matchId: String, req: StartMatchRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.startMatch(matchId, req) }

    suspend fun endMatch(matchId: String, req: EndMatchRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.endMatch(matchId, req) }

    suspend fun startInnings(matchId: String, req: StartInningsRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.startInnings(matchId, req) }

    suspend fun endInnings(matchId: String): ApiResult<LiveStateDto> =
        safeApiCall { apiService.endInnings(matchId) }

    suspend fun setBatsmen(matchId: String, req: SetBatsmenRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.setBatsmen(matchId, req) }

    suspend fun setBowler(matchId: String, req: SetBowlerRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.setBowler(matchId, req) }

    suspend fun getPlayingXI(matchId: String): ApiResult<PlayingXIResponseDto> =
        safeApiCall { apiService.getPlayingXI(matchId) }

    suspend fun setPlayingXI(matchId: String, req: PlayingXIRequestDto): ApiResult<PlayingXIResponseDto> =
        safeApiCall { apiService.setPlayingXI(matchId, req) }

    suspend fun recordBall(matchId: String, req: RecordBallRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.recordBall(matchId, req) }

    suspend fun recordPenalty(matchId: String, req: RecordPenaltyRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.recordPenalty(matchId, req) }

    suspend fun swapBatsmen(matchId: String): ApiResult<LiveStateDto> =
        safeApiCall { apiService.swapBatsmen(matchId) }

    suspend fun undoLastBall(matchId: String): ApiResult<LiveStateDto> =
        safeApiCall { apiService.undoLastBall(matchId) }

    suspend fun getLiveState(matchId: String): ApiResult<LiveStateDto> =
        safeApiCall { apiService.getLiveState(matchId) }

    suspend fun getScorecard(matchId: String): ApiResult<ScorecardDto> =
        safeApiCall { apiService.getScorecard(matchId) }

    suspend fun getEligibleBatsmen(matchId: String): ApiResult<EligibleBatsmenResponseDto> =
        safeApiCall { apiService.getEligibleBatsmen(matchId) }

    suspend fun getBallByBall(matchId: String, innings: Int? = null): ApiResult<BallByBallResponseDto> =
        safeApiCall { apiService.getBallByBall(matchId, innings) }

    suspend fun getMatchSummary(matchId: String): ApiResult<MatchSummaryDto> =
        safeApiCall { apiService.getMatchSummary(matchId) }

    suspend fun getMOMSuggestion(matchId: String): ApiResult<MOMSuggestionResponseDto> =
        safeApiCall { apiService.getMOMSuggestion(matchId) }

    suspend fun finalizeMOM(matchId: String, req: FinalizeMOMRequestDto): ApiResult<LiveStateDto> =
        safeApiCall { apiService.finalizeMOM(matchId, req) }

    private suspend inline fun <T> safeApiCall(crossinline call: suspend () -> T): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                ApiResult.Success(call())
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Network error occurred")
            }
        }
    }
}

package com.sportynix.app.data.repository

import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.AuctionApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.websocket.AuctionWSEvent
import com.sportynix.app.data.remote.websocket.AuctionWebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuctionRepository @Inject constructor(
    private val apiService: AuctionApiService,
    private val webSocketManager: AuctionWebSocketManager
) {
    val auctionSnapshot: StateFlow<AuctionSessionSnapshotDto?> = webSocketManager.auctionSnapshot
    val wsEvents: SharedFlow<AuctionWSEvent> = webSocketManager.events
    val isWsConnected: StateFlow<Boolean> = webSocketManager.isConnected

    fun connectWebSocket(auctionId: String) {
        webSocketManager.connect(auctionId)
    }

    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    suspend fun getAuctionByLeague(leagueId: String): ApiResult<AuctionSessionSnapshotDto?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAuctionByLeague(leagueId)
                if (response.code() == 404) {
                    ApiResult.Success(null)
                } else if (response.isSuccessful) {
                    ApiResult.Success(response.body())
                } else {
                    ApiResult.Error(message = "Failed to load auction: ${response.message()}")
                }
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load auction")
            }
        }
    }

    suspend fun getAuctionControlAccess(leagueId: String): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAuctionControlAccess(leagueId)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()?.canControl == true)
                } else {
                    ApiResult.Success(false)
                }
            } catch (e: Exception) {
                ApiResult.Success(false)
            }
        }
    }

    suspend fun upsertAuctionSession(payload: AuctionUpsertPayloadDto): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.upsertAuctionSession(payload)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to save auction setup")
            }
        }
    }

    suspend fun syncAuctionPool(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.syncAuctionPool(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to sync player pool")
            }
        }
    }

    suspend fun startAuction(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.startAuction(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to start auction")
            }
        }
    }

    suspend fun pauseAuction(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.pauseAuction(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to pause auction")
            }
        }
    }

    suspend fun resumeAuction(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.resumeAuction(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to resume auction")
            }
        }
    }

    suspend fun closeAuction(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.closeAuction(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to close auction")
            }
        }
    }

    suspend fun nominatePlayer(sessionId: String, playerId: String, openingBid: Double? = null): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    addProperty("player_id", playerId)
                    if (openingBid != null) addProperty("opening_bid", openingBid)
                }
                val snapshot = apiService.nominatePlayer(sessionId, body)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to nominate player")
            }
        }
    }

    suspend fun recordBid(sessionId: String, teamWalletId: String, amount: Double): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply {
                    addProperty("team_wallet_id", teamWalletId)
                    addProperty("amount", amount)
                }
                val snapshot = apiService.recordBid(sessionId, body)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to record bid")
            }
        }
    }

    suspend fun markPlayerSold(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.markPlayerSold(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to mark player as sold")
            }
        }
    }

    suspend fun markPlayerUnsold(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.markPlayerUnsold(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to mark player as unsold")
            }
        }
    }

    suspend fun addCommentary(sessionId: String, message: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JsonObject().apply { addProperty("message", message) }
                val snapshot = apiService.addCommentary(sessionId, body)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to post commentary")
            }
        }
    }

    suspend fun undoAction(sessionId: String): ApiResult<AuctionSessionSnapshotDto> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = apiService.undoAction(sessionId)
                ApiResult.Success(snapshot)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to undo action")
            }
        }
    }
}

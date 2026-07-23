package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.core.network.WebSocketManager
import com.sportynix.app.data.remote.api.AuctionApiService
import com.sportynix.app.data.remote.dto.AuctionBidRequestDto
import com.sportynix.app.data.remote.dto.AuctionBidResponseDto
import com.sportynix.app.data.remote.dto.AuctionDto
import com.sportynix.app.data.remote.dto.AuctionTeamDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuctionRepository @Inject constructor(
    private val apiService: AuctionApiService,
    private val webSocketManager: WebSocketManager
) {
    suspend fun getAuctions(): ApiResult<List<AuctionDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getAuctions()
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load auctions")
            }
        }
    }

    suspend fun getAuctionDetail(auctionId: String): ApiResult<AuctionDto> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getAuctionDetail(auctionId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load auction detail")
            }
        }
    }

    suspend fun getAuctionTeams(auctionId: String): ApiResult<List<AuctionTeamDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val data = apiService.getAuctionTeams(auctionId)
                ApiResult.Success(data)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to load auction teams")
            }
        }
    }

    suspend fun placeBid(auctionId: String, teamId: String, bidAmount: Double): ApiResult<AuctionBidResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val req = AuctionBidRequestDto(bidAmount, teamId)
                val response = apiService.placeBid(auctionId, req)
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Failed to place bid")
            }
        }
    }

    fun connectAuctionWebSocket(auctionId: String) {
        webSocketManager.connect("ws/auction/$auctionId/")
    }

    fun disconnectAuctionWebSocket() {
        webSocketManager.disconnect()
    }
}

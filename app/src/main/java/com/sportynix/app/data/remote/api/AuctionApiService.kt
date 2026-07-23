package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.AuctionBidRequestDto
import com.sportynix.app.data.remote.dto.AuctionBidResponseDto
import com.sportynix.app.data.remote.dto.AuctionDto
import com.sportynix.app.data.remote.dto.AuctionTeamDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuctionApiService {

    @GET("api/auction/auctions/")
    suspend fun getAuctions(): List<AuctionDto>

    @GET("api/auction/auctions/{id}/")
    suspend fun getAuctionDetail(
        @Path("id") auctionId: String
    ): AuctionDto

    @GET("api/auction/auctions/{id}/teams/")
    suspend fun getAuctionTeams(
        @Path("id") auctionId: String
    ): List<AuctionTeamDto>

    @POST("api/auction/auctions/{id}/place-bid/")
    suspend fun placeBid(
        @Path("id") auctionId: String,
        @Body request: AuctionBidRequestDto
    ): AuctionBidResponseDto
}

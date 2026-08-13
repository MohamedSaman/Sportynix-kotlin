package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuctionApiService {

    @GET("api/auction/sessions/league/{league_id}/")
    suspend fun getAuctionByLeague(@Path("league_id") leagueId: String): Response<AuctionSessionSnapshotDto>

    @GET("api/auction/sessions/league/{league_id}/control-access/")
    suspend fun getAuctionControlAccess(@Path("league_id") leagueId: String): Response<AuctionControlAccessDto>

    @POST("api/auction/sessions/")
    suspend fun upsertAuctionSession(@Body body: AuctionUpsertPayloadDto): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/sync-pool/")
    suspend fun syncAuctionPool(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/start/")
    suspend fun startAuction(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/pause/")
    suspend fun pauseAuction(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/resume/")
    suspend fun resumeAuction(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/close/")
    suspend fun closeAuction(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/nominate/")
    suspend fun nominatePlayer(@Path("id") sessionId: String, @Body body: JsonObject): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/record-bid/")
    suspend fun recordBid(@Path("id") sessionId: String, @Body body: JsonObject): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/mark-sold/")
    suspend fun markPlayerSold(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/mark-unsold/")
    suspend fun markPlayerUnsold(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/commentary/")
    suspend fun addCommentary(@Path("id") sessionId: String, @Body body: JsonObject): AuctionSessionSnapshotDto

    @POST("api/auction/sessions/{id}/undo/")
    suspend fun undoAction(@Path("id") sessionId: String, @Body body: JsonObject = JsonObject()): AuctionSessionSnapshotDto

    @PATCH("api/auction/sessions/{id}/")
    suspend fun updateAuctionSession(@Path("id") sessionId: String, @Body body: JsonObject): AuctionSessionSnapshotDto
}

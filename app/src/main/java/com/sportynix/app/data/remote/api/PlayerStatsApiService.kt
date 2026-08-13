package com.sportynix.app.data.remote.api

import com.sportynix.app.data.remote.dto.PlayerMatchStatsPageDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlayerStatsApiService {

    @GET("api/league/player-stats/")
    suspend fun getPlayerMatchStats(
        @Query("player") playerId: String,
        @Query("cricket_variant") cricketVariant: String? = null,
        @Query("context") context: String? = null,
        @Query("venue_category") venueCategory: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): PlayerMatchStatsPageDto
}

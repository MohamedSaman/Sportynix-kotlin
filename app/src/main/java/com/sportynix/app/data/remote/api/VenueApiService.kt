package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.ReviewRequestDto
import com.sportynix.app.data.remote.dto.TimeSlotDto
import com.sportynix.app.data.remote.dto.VenueDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VenueApiService {
    @GET("api/venues/discover/")
    suspend fun getVenues(
        @Query("search") search: String? = null,
        @Query("venue_category") venueCategory: String? = null,
        @Query("featured") featured: Int? = null,
        @Query("analytics_enabled") analyticsEnabled: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null
    ): Response<JsonElement>

    @GET("api/venues/{id}/")
    suspend fun getVenueById(@Path("id") id: String): Response<VenueDto>

    @POST("api/venues/{id}/reviews/")
    suspend fun submitVenueReview(
        @Path("id") id: String,
        @Body reviewRequest: ReviewRequestDto
    ): Response<JsonElement>

    @GET("api/available_slots/{sportId}/")
    suspend fun getAvailableSlots(
        @Path("sportId") sportId: String,
        @Query("date") date: String,
        @Query("venue") venueId: String
    ): Response<JsonElement>
}

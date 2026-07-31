package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.sportynix.app.data.remote.dto.DiscoverVenueResponseDto
import com.sportynix.app.data.remote.dto.FavoriteDto
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.data.remote.dto.VenueReviewDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface VenueApiService {
    @GET("api/venues/discover/")
    suspend fun getVenuesDiscover(
        @Query("page") page: Int? = 1,
        @Query("per_page") perPage: Int? = 10,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("search") search: String? = null,
        @Query("sport") sport: String? = null,
        @Query("venue_category") venueCategory: String? = null,
        @Query("featured") featured: Int? = null,
        @Query("radius_km") radiusKm: Int? = null
    ): Response<DiscoverVenueResponseDto>

    @GET("api/venues/discover/")
    suspend fun getVenues(
        @Query("page") page: Int? = 1,
        @Query("per_page") perPage: Int? = 10,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("search") search: String? = null,
        @Query("sport") sport: String? = null,
        @Query("venue_category") venueCategory: String? = null,
        @Query("featured") featured: Int? = null,
        @Query("radius_km") radiusKm: Int? = null
    ): Response<JsonElement>

    @GET("api/venues/{id}/")
    suspend fun getVenueById(@Path("id") id: String): Response<VenueDto>

    // ── Venue Reviews APIs ──
    @GET("api/venues/{id}/reviews/")
    suspend fun getVenueReviews(@Path("id") venueId: Int): Response<JsonElement>

    @Multipart
    @POST("api/venues/{id}/reviews/")
    suspend fun submitVenueReviewMultipart(
        @Path("id") venueId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part uploadedImages: List<MultipartBody.Part>
    ): Response<JsonElement>

    @Multipart
    @PUT("api/venues/{id}/reviews/{reviewId}/")
    suspend fun updateVenueReviewMultipart(
        @Path("id") venueId: Int,
        @Path("reviewId") reviewId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part uploadedImages: List<MultipartBody.Part>
    ): Response<JsonElement>

    @DELETE("api/venues/{id}/reviews/{reviewId}/")
    suspend fun deleteVenueReview(
        @Path("id") venueId: Int,
        @Path("reviewId") reviewId: Int
    ): Response<JsonElement>

    // ── Sport Reviews APIs ──
    @GET("api/venues/{id}/sports/{sportId}/reviews/")
    suspend fun getSportReviews(
        @Path("id") venueId: Int,
        @Path("sportId") sportId: Int
    ): Response<JsonElement>

    @Multipart
    @POST("api/venues/{id}/sports/{sportId}/reviews/")
    suspend fun submitSportReviewMultipart(
        @Path("id") venueId: Int,
        @Path("sportId") sportId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part uploadedImages: List<MultipartBody.Part>
    ): Response<JsonElement>

    @Multipart
    @PUT("api/venues/{id}/sports/{sportId}/reviews/{reviewId}/")
    suspend fun updateSportReviewMultipart(
        @Path("id") venueId: Int,
        @Path("sportId") sportId: Int,
        @Path("reviewId") reviewId: Int,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part uploadedImages: List<MultipartBody.Part>
    ): Response<JsonElement>

    @DELETE("api/venues/{id}/sports/{sportId}/reviews/{reviewId}/")
    suspend fun deleteSportReview(
        @Path("id") venueId: Int,
        @Path("sportId") sportId: Int,
        @Path("reviewId") reviewId: Int
    ): Response<JsonElement>

    // ── Favorites APIs ──
    @GET("api/favorites/")
    suspend fun getFavorites(): Response<List<FavoriteDto>>

    @POST("api/favorites/")
    suspend fun addFavorite(@Body body: Map<String, Any>): Response<FavoriteDto>

    @DELETE("api/favorites/{id}/")
    suspend fun removeFavorite(@Path("id") favoriteId: Int): Response<JsonElement>

    @GET("api/available_slots/{sportId}/")
    suspend fun getAvailableSlots(
        @Path("sportId") sportId: String,
        @Query("date") date: String,
        @Query("venue") venueId: String
    ): Response<JsonElement>
}

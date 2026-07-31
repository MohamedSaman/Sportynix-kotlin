package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FavoriteDto
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.data.remote.dto.VenueReviewDto
import com.sportynix.app.domain.model.Venue
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VenueRepository {
    fun getVenuesStream(): Flow<List<Venue>>
    suspend fun fetchVenues(sportType: String? = null, query: String? = null): ApiResult<List<Venue>>
    suspend fun fetchFeaturedVenues(): ApiResult<List<Venue>>
    suspend fun fetchNearbyVenues(latitude: Double, longitude: Double): ApiResult<List<Venue>>
    suspend fun getVenueById(id: String): ApiResult<Venue>
    
    // Discover Venues (matches Swift fetchDiscoverVenues)
    suspend fun fetchDiscoverVenues(
        page: Int = 1,
        perPage: Int = 10,
        latitude: Double? = null,
        longitude: Double? = null,
        search: String? = null,
        sport: String? = null,
        venueCategory: String? = null,
        featured: Int? = null,
        radiusKm: Int? = null
    ): ApiResult<Pair<List<VenueDto>, Boolean>>

    suspend fun fetchVenueDetailsDto(id: Int): ApiResult<VenueDto>

    // Venue Reviews
    suspend fun fetchVenueReviews(venueId: Int): ApiResult<List<VenueReviewDto>>
    
    suspend fun submitVenueReview(
        venueId: Int,
        rating: Double,
        comment: String,
        wouldRecommend: Boolean = true,
        categories: List<String> = emptyList(),
        imageFiles: List<File> = emptyList(),
        reviewId: Int? = null,
        keepPhotoIds: List<Int>? = null
    ): ApiResult<Unit>

    suspend fun deleteVenueReview(venueId: Int, reviewId: Int): ApiResult<Unit>

    // Sport Reviews
    suspend fun fetchSportReviews(venueId: Int, sportId: Int): ApiResult<List<VenueReviewDto>>

    suspend fun submitSportReview(
        venueId: Int,
        sportId: Int,
        rating: Double,
        comment: String,
        wouldRecommend: Boolean = true,
        categories: List<String> = emptyList(),
        imageFiles: List<File> = emptyList(),
        reviewId: Int? = null,
        keepPhotoIds: List<Int>? = null,
        bookingId: Int? = null
    ): ApiResult<Unit>

    suspend fun deleteSportReview(venueId: Int, sportId: Int, reviewId: Int): ApiResult<Unit>

    // Favorites
    suspend fun fetchFavorites(): ApiResult<List<FavoriteDto>>
    suspend fun addFavorite(type: String, venueId: Int? = null, sportId: Int? = null): ApiResult<FavoriteDto>
    suspend fun removeFavorite(id: Int): ApiResult<Unit>
    suspend fun checkVenueFavorite(venueId: Int): ApiResult<Int?>
    suspend fun checkSportFavorite(sportId: Int): ApiResult<Int?>
}

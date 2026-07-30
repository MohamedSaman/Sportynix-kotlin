package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Venue
import kotlinx.coroutines.flow.Flow

interface VenueRepository {
    fun getVenuesStream(): Flow<List<Venue>>
    suspend fun fetchVenues(sportType: String? = null, query: String? = null): ApiResult<List<Venue>>
    suspend fun fetchFeaturedVenues(): ApiResult<List<Venue>>
    suspend fun fetchNearbyVenues(latitude: Double, longitude: Double): ApiResult<List<Venue>>
    suspend fun getVenueById(id: String): ApiResult<Venue>
    suspend fun submitVenueReview(venueId: String, rating: Int, comment: String, recommends: Boolean = true): ApiResult<Unit>
}

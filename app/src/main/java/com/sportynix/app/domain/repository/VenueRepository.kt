package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Venue
import kotlinx.coroutines.flow.Flow

interface VenueRepository {
    fun getVenuesStream(): Flow<List<Venue>>
    suspend fun fetchVenues(sportType: String? = null, query: String? = null): ApiResult<List<Venue>>
    suspend fun getVenueById(id: String): ApiResult<Venue>
}

package com.sportynix.app.domain.usecase.venue

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVenuesUseCase @Inject constructor(
    private val repository: VenueRepository
) {
    fun getStream(): Flow<List<Venue>> = repository.getVenuesStream()

    suspend fun refresh(sportType: String? = null, query: String? = null): ApiResult<List<Venue>> {
        return repository.fetchVenues(sportType, query)
    }
}

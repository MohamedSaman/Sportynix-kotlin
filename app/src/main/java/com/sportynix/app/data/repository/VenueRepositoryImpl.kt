package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.VenueDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.mapper.toEntity
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepositoryImpl @Inject constructor(
    private val apiService: VenueApiService,
    private val venueDao: VenueDao
) : VenueRepository {

    override fun getVenuesStream(): Flow<List<Venue>> {
        return venueDao.getAllVenues().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchVenues(sportType: String?, query: String?): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenues(
                search = query,
                venueCategory = if (sportType != "ALL") sportType else null
            )
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                venueDao.insertVenues(dtos.map { it.toEntity() })
                ApiResult.Success(dtos.map { it.toDomain() })
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Failed to fetch venues")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to load venues")
        }
    }

    override suspend fun getVenueById(id: String): ApiResult<Venue> {
        return try {
            val response = apiService.getVenueById(id)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.toDomain())
            } else {
                val localEntity = venueDao.getVenueById(id)
                if (localEntity != null) {
                    ApiResult.Success(localEntity.toDomain())
                } else {
                    ApiResult.Error(message = "Venue not found")
                }
            }
        } catch (e: Exception) {
            val localEntity = venueDao.getVenueById(id)
            if (localEntity != null) {
                ApiResult.Success(localEntity.toDomain())
            } else {
                ApiResult.Error(message = e.localizedMessage ?: "Network error")
            }
        }
    }
}

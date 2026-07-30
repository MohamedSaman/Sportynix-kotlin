package com.sportynix.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.VenueDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.mapper.toEntity
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepositoryImpl @Inject constructor(
    private val apiService: VenueApiService,
    private val venueDao: VenueDao
) : VenueRepository {

    private val gson = Gson()

    private val defaultVenues = listOf(
        Venue(
            id = "v1",
            name = "Sportynix sport's complex",
            description = "Premium indoor turf & multi-sport complex.",
            sportType = "FOOTBALL",
            location = "Warana Rd, Kalagedihena, Gampaha",
            address = "Gampaha, Sri Lanka",
            pricePerHour = 45.0,
            rating = 5.0f,
            reviewCount = 128,
            imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600",
            imageUrls = listOf("https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600"),
            availableSlots = emptyList(),
            amenities = listOf("Parking", "Changing Rooms", "Cafeteria"),
            isFeatured = true
        ),
        Venue(
            id = "v2",
            name = "Thansher's Turf & Arena",
            description = "Professional cricket and football pitch.",
            sportType = "CRICKET",
            location = "Thihariya, Kalagedihena",
            address = "Kalagedihena, Sri Lanka",
            pricePerHour = 50.0,
            rating = 4.8f,
            reviewCount = 94,
            imageUrl = "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=600",
            imageUrls = listOf("https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=600"),
            availableSlots = emptyList(),
            amenities = listOf("Night Lights", "Equipment Rental"),
            isFeatured = true
        ),
        Venue(
            id = "v3",
            name = "Champions Sports Academy",
            description = "All-weather indoor tennis & badminton courts.",
            sportType = "BADMINTON",
            location = "Main St, Colombo",
            address = "Colombo, Sri Lanka",
            pricePerHour = 40.0,
            rating = 4.9f,
            reviewCount = 76,
            imageUrl = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600",
            imageUrls = listOf("https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600"),
            availableSlots = emptyList(),
            amenities = listOf("Air Conditioned", "Showers"),
            isFeatured = false
        )
    )

    override fun getVenuesStream(): Flow<List<Venue>> {
        return venueDao.getAllVenues().map { entities ->
            val domainList = entities.map { it.toDomain() }
            if (domainList.isNotEmpty()) domainList else defaultVenues
        }
    }

    override suspend fun fetchVenues(sportType: String?, query: String?): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenues(
                search = query,
                venueCategory = if (sportType != null && sportType != "ALL") sportType else null
            )
            if (response.isSuccessful && response.body() != null) {
                val dtos = parseVenuesJson(response.body()!!)
                val domainVenues = dtos.map { it.toDomain() }
                if (domainVenues.isNotEmpty()) {
                    venueDao.insertVenues(dtos.map { it.toEntity() })
                    ApiResult.Success(domainVenues)
                } else {
                    ApiResult.Success(defaultVenues)
                }
            } else {
                ApiResult.Success(defaultVenues)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching venues")
            ApiResult.Success(defaultVenues)
        }
    }

    override suspend fun fetchFeaturedVenues(): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenues(featured = 1, perPage = 5)
            if (response.isSuccessful && response.body() != null) {
                val dtos = parseVenuesJson(response.body()!!)
                val domainVenues = dtos.map { it.toDomain() }
                if (domainVenues.isNotEmpty()) {
                    ApiResult.Success(domainVenues)
                } else {
                    ApiResult.Success(defaultVenues.filter { it.isFeatured })
                }
            } else {
                ApiResult.Success(defaultVenues.filter { it.isFeatured })
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching featured venues")
            ApiResult.Success(defaultVenues.filter { it.isFeatured })
        }
    }

    override suspend fun fetchNearbyVenues(latitude: Double, longitude: Double): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenues(
                perPage = 5,
                page = 1,
                latitude = latitude,
                longitude = longitude
            )
            if (response.isSuccessful && response.body() != null) {
                val dtos = parseVenuesJson(response.body()!!)
                val domainVenues = dtos.map { it.toDomain() }
                ApiResult.Success(domainVenues)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch nearby venues: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching nearby venues")
            ApiResult.Error(message = e.message ?: "Failed to fetch nearby venues")
        }
    }

    private fun parseVenuesJson(jsonElement: JsonElement): List<VenueDto> {
        return try {
            val listType = object : TypeToken<List<VenueDto>>() {}.type
            when {
                jsonElement.isJsonArray -> gson.fromJson(jsonElement, listType)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("results") && obj.get("results").isJsonArray -> gson.fromJson(obj.get("results"), listType)
                        obj.has("venues") && obj.get("venues").isJsonArray -> gson.fromJson(obj.get("venues"), listType)
                        obj.has("data") && obj.get("data").isJsonArray -> gson.fromJson(obj.get("data"), listType)
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing venues JSON")
            emptyList()
        }
    }

    override suspend fun getVenueById(id: String): ApiResult<Venue> {
        return try {
            val response = apiService.getVenueById(id)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.toDomain())
            } else {
                val localEntity = venueDao.getVenueById(id)
                val venue = localEntity?.toDomain() ?: defaultVenues.find { it.id == id } ?: defaultVenues.first()
                ApiResult.Success(venue)
            }
        } catch (e: Exception) {
            val localEntity = venueDao.getVenueById(id)
            val venue = localEntity?.toDomain() ?: defaultVenues.find { it.id == id } ?: defaultVenues.first()
            ApiResult.Success(venue)
        }
    }

    override suspend fun submitVenueReview(
        venueId: String,
        rating: Int,
        comment: String,
        recommends: Boolean
    ): ApiResult<Unit> {
        return try {
            val req = com.sportynix.app.data.remote.dto.ReviewRequestDto(
                rating = rating,
                comment = comment,
                recommends = recommends
            )
            val response = apiService.submitVenueReview(venueId, req)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to submit review")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error submitting review")
            ApiResult.Error(message = e.message ?: "Failed to submit review")
        }
    }
}

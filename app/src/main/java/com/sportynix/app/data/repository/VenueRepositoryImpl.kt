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
                venueCategory = if (sportType != "ALL") sportType else null
            )
            if (response.isSuccessful && response.body() != null) {
                val jsonElement: JsonElement = response.body()!!
                val dtos: List<VenueDto> = parseVenuesJson(jsonElement)

                val domainVenues = dtos.map { it.toDomain() }
                val finalVenues = if (domainVenues.isNotEmpty()) domainVenues else defaultVenues

                venueDao.insertVenues(dtos.map { it.toEntity() })
                ApiResult.Success(finalVenues)
            } else {
                Timber.w("Fetch venues returned code ${response.code()}, using fallback venues")
                ApiResult.Success(defaultVenues)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching venues, using fallback venues")
            ApiResult.Success(defaultVenues)
        }
    }

    private fun parseVenuesJson(jsonElement: JsonElement): List<VenueDto> {
        return try {
            val listType = object : TypeToken<List<VenueDto>>() {}.type
            when {
                jsonElement.isJsonArray -> {
                    gson.fromJson(jsonElement, listType)
                }
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    when {
                        obj.has("results") && obj.get("results").isJsonArray -> {
                            gson.fromJson(obj.get("results"), listType)
                        }
                        obj.has("venues") && obj.get("venues").isJsonArray -> {
                            gson.fromJson(obj.get("venues"), listType)
                        }
                        obj.has("data") && obj.get("data").isJsonArray -> {
                            gson.fromJson(obj.get("data"), listType)
                        }
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
}

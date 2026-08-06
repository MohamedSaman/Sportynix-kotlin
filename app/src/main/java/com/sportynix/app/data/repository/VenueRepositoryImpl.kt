package com.sportynix.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.VenueDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.mapper.toEntity
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepositoryImpl @Inject constructor(
    private val apiService: VenueApiService,
    private val venueDao: VenueDao
) : VenueRepository {

    private val gson = Gson()

    override fun getVenuesStream(): Flow<List<Venue>> {
        return venueDao.getAllVenues().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchVenues(sportType: String?, query: String?): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenuesDiscover(
                search = query,
                venueCategory = if (sportType != null && sportType != "ALL") sportType else null
            )
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.results ?: emptyList()
                val domainVenues = dtos.map { it.toDomain() }
                ApiResult.Success(domainVenues)
            } else {
                ApiResult.Error(code = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching venues")
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchFeaturedVenues(): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenuesDiscover(featured = 1, perPage = 5)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.results ?: emptyList()
                ApiResult.Success(dtos.map { it.toDomain() })
            } else {
                ApiResult.Error(code = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching featured venues")
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchNearbyVenues(latitude: Double, longitude: Double): ApiResult<List<Venue>> {
        return try {
            val response = apiService.getVenuesDiscover(
                perPage = 10,
                page = 1,
                latitude = latitude,
                longitude = longitude
            )
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!.results ?: emptyList()
                ApiResult.Success(dtos.map { it.toDomain() })
            } else {
                ApiResult.Error(code = response.code(), message = response.message())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching nearby venues")
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun getVenueById(id: String): ApiResult<Venue> {
        return try {
            val response = apiService.getVenueById(id)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.toDomain())
            } else {
                ApiResult.Error(code = response.code(), message = "Venue not found")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchDiscoverVenues(
        page: Int,
        perPage: Int,
        latitude: Double?,
        longitude: Double?,
        search: String?,
        sport: String?,
        venueCategory: String?,
        featured: Int?,
        radiusKm: Int?
    ): ApiResult<Pair<List<VenueDto>, Boolean>> {
        return try {
            val response = apiService.getVenuesDiscover(
                page = page,
                perPage = perPage,
                latitude = latitude,
                longitude = longitude,
                search = search,
                sport = if (sport != "all") sport else null,
                venueCategory = if (venueCategory != "all") venueCategory else null,
                featured = featured,
                radiusKm = radiusKm
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val results = body.results ?: emptyList()
                val hasNext = !body.next.isNullOrEmpty()
                ApiResult.Success(Pair(results, hasNext))
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to discover venues")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error discovering venues")
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchVenueDetailsDto(id: Int): ApiResult<VenueDto> {
        return try {
            val response = apiService.getVenueById(id.toString())
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch venue details")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchVenueReviews(venueId: Int): ApiResult<List<VenueReviewDto>> {
        return try {
            val response = apiService.getVenueReviews(venueId)
            if (response.isSuccessful && response.body() != null) {
                val reviews = parseReviewsJson(response.body()!!)
                ApiResult.Success(reviews)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch reviews")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun submitVenueReview(
        venueId: Int,
        rating: Double,
        comment: String,
        wouldRecommend: Boolean,
        categories: List<String>,
        imageFiles: List<File>,
        reviewId: Int?,
        keepPhotoIds: List<Int>?
    ): ApiResult<Unit> {
        return try {
            val parts = mutableMapOf<String, RequestBody>()
            val textPlain = "text/plain".toMediaTypeOrNull()

            parts["rating"] = String.format(Locale.US, "%.1f", rating).toRequestBody(textPlain)
            parts["comment"] = comment.toRequestBody(textPlain)
            parts["venue"] = venueId.toString().toRequestBody(textPlain)
            parts["would_recommend"] = (if (wouldRecommend) "true" else "false").toRequestBody(textPlain)

            if (categories.isNotEmpty()) {
                parts["categories"] = gson.toJson(categories).toRequestBody(textPlain)
            }
            if (reviewId != null && keepPhotoIds != null) {
                parts["keep_photo_ids"] = gson.toJson(keepPhotoIds).toRequestBody(textPlain)
            }

            val imageParts = imageFiles.mapIndexed { index, file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("uploaded_images", "photo_$index.jpg", requestFile)
            }

            val response = if (reviewId != null) {
                apiService.updateVenueReviewMultipart(venueId, reviewId, parts, imageParts)
            } else {
                apiService.submitVenueReviewMultipart(venueId, parts, imageParts)
            }

            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to submit review")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun deleteVenueReview(venueId: Int, reviewId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.deleteVenueReview(venueId, reviewId)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error(code = response.code(), message = "Failed to delete review")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchSportReviews(venueId: Int, sportId: Int): ApiResult<List<VenueReviewDto>> {
        return try {
            val response = apiService.getSportReviews(venueId, sportId)
            if (response.isSuccessful && response.body() != null) {
                val reviews = parseReviewsJson(response.body()!!)
                ApiResult.Success(reviews)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch sport reviews")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun submitSportReview(
        venueId: Int,
        sportId: Int,
        rating: Double,
        comment: String,
        wouldRecommend: Boolean,
        categories: List<String>,
        imageFiles: List<File>,
        reviewId: Int?,
        keepPhotoIds: List<Int>?,
        bookingId: Int?
    ): ApiResult<Unit> {
        return try {
            val parts = mutableMapOf<String, RequestBody>()
            val textPlain = "text/plain".toMediaTypeOrNull()

            parts["rating"] = String.format(Locale.US, "%.1f", rating).toRequestBody(textPlain)
            parts["comment"] = comment.toRequestBody(textPlain)
            parts["would_recommend"] = (if (wouldRecommend) "true" else "false").toRequestBody(textPlain)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parts["visit_date"] = sdf.format(Date()).toRequestBody(textPlain)

            if (bookingId != null) {
                parts["booking"] = bookingId.toString().toRequestBody(textPlain)
            }
            if (categories.isNotEmpty()) {
                parts["categories"] = gson.toJson(categories).toRequestBody(textPlain)
            }
            if (reviewId != null && keepPhotoIds != null) {
                parts["keep_photo_ids"] = gson.toJson(keepPhotoIds).toRequestBody(textPlain)
            }

            val imageParts = imageFiles.mapIndexed { index, file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("uploaded_images", "photo_$index.jpg", requestFile)
            }

            val response = if (reviewId != null) {
                apiService.updateSportReviewMultipart(venueId, sportId, reviewId, parts, imageParts)
            } else {
                apiService.submitSportReviewMultipart(venueId, sportId, parts, imageParts)
            }

            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to submit sport review")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun deleteSportReview(venueId: Int, sportId: Int, reviewId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.deleteSportReview(venueId, sportId, reviewId)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error(code = response.code(), message = "Failed to delete sport review")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun fetchFavorites(): ApiResult<List<FavoriteDto>> {
        return try {
            val response = apiService.getFavorites()
            if (response.isSuccessful && response.body() != null) {
                val root = response.body()!!
                val elements = when {
                    root.isJsonArray -> root.asJsonArray.toList()
                    root.isJsonObject -> listOf("favorites", "results", "data", "items").firstNotNullOfOrNull { key ->
                        root.asJsonObject.get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList()
                    }.orEmpty()
                    else -> emptyList()
                }
                ApiResult.Success(elements.mapNotNull { element -> runCatching { gson.fromJson(element, FavoriteDto::class.java) }.getOrNull() })
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch favorites")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun addFavorite(type: String, venueId: Int?, sportId: Int?): ApiResult<FavoriteDto> {
        return try {
            val body = mutableMapOf<String, Any>("type" to type)
            venueId?.let { body["venue_id"] = it }
            sportId?.let { body["sport_id"] = it }
            val response = apiService.addFavorite(body)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to add favorite")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun removeFavorite(id: Int): ApiResult<Unit> {
        return try {
            val response = apiService.removeFavorite(id)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error(code = response.code(), message = "Failed to remove favorite")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun checkVenueFavorite(venueId: Int): ApiResult<Int?> {
        return when (val res = fetchFavorites()) {
            is ApiResult.Success -> {
                val favId = res.data.firstOrNull { it.type == "venue" && (it.venueId == venueId || it.venue?.id == venueId.toString()) }?.id
                ApiResult.Success(favId)
            }
            else -> ApiResult.Success(null)
        }
    }

    override suspend fun checkSportFavorite(sportId: Int): ApiResult<Int?> {
        return when (val res = fetchFavorites()) {
            is ApiResult.Success -> {
                val favId = res.data.firstOrNull { it.type == "sport" && (it.sportId == sportId || it.sport?.id == sportId) }?.id
                ApiResult.Success(favId)
            }
            else -> ApiResult.Success(null)
        }
    }

    private fun parseReviewsJson(jsonElement: JsonElement): List<VenueReviewDto> {
        return try {
            val listType = object : TypeToken<List<VenueReviewDto>>() {}.type
            when {
                jsonElement.isJsonArray -> gson.fromJson(jsonElement, listType)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    if (obj.has("results") && obj.get("results").isJsonArray) {
                        gson.fromJson(obj.get("results"), listType)
                    } else emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing reviews JSON")
            emptyList()
        }
    }
}

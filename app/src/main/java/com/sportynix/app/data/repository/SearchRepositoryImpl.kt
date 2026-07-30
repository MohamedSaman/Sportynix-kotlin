package com.sportynix.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.SearchApiService
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.domain.model.SearchResult
import com.sportynix.app.domain.model.SearchResultType
import com.sportynix.app.domain.model.VenueSport
import com.sportynix.app.domain.repository.SearchRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val venueApiService: VenueApiService,
    private val searchApiService: SearchApiService,
    private val gson: Gson
) : SearchRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("recent_searches_prefs", Context.MODE_PRIVATE)
    private val recentSearchesFlow = MutableStateFlow<List<String>>(emptyList())

    init {
        loadRecentSearchesFromPrefs()
    }

    private fun loadRecentSearchesFromPrefs() {
        val rawJson = prefs.getString("recent_searches_list", "[]") ?: "[]"
        try {
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(rawJson, type) ?: emptyList()
            recentSearchesFlow.value = list.filter { it.trim().length >= 3 }.take(3)
        } catch (e: Exception) {
            recentSearchesFlow.value = emptyList()
        }
    }

    private fun saveRecentSearchesToPrefs(list: List<String>) {
        val cleanList = list.filter { it.trim().length >= 3 }.take(3)
        recentSearchesFlow.value = cleanList
        prefs.edit().putString("recent_searches_list", gson.toJson(cleanList)).apply()
    }

    override fun getRecentSearches(): Flow<List<String>> = recentSearchesFlow.asStateFlow()

    override suspend fun addRecentSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.length < 3) return
        val current = recentSearchesFlow.value.filterNot { it.equals(cleaned, ignoreCase = true) }
        saveRecentSearchesToPrefs(listOf(cleaned) + current)
    }

    override suspend fun removeRecentSearch(query: String) {
        val current = recentSearchesFlow.value.filterNot { it.equals(query.trim(), ignoreCase = true) }
        saveRecentSearchesToPrefs(current)
    }

    override suspend fun clearRecentSearches() {
        saveRecentSearchesToPrefs(emptyList())
    }

    override suspend fun fetchPopularVenues(latitude: Double?, longitude: Double?): ApiResult<List<SearchResult>> {
        return try {
            val response = venueApiService.getVenues(
                perPage = 20,
                latitude = latitude,
                longitude = longitude
            )
            if (response.isSuccessful && response.body() != null) {
                val venuesDtoList = parseVenuesJson(response.body()!!)
                val popular = venuesDtoList
                    .filter { (it.rating ?: 0f) > 0 }
                    .sortedWith(compareByDescending<VenueDto> { it.rating ?: 0f }.thenByDescending { it.reviewCount ?: 0 })
                    .take(3)
                    .map { venue ->
                        SearchResult(
                            id = venue.id,
                            type = SearchResultType.VENUE,
                            title = venue.name,
                            subtitle = venue.address ?: venue.location ?: "No address",
                            rating = venue.rating ?: 5.0f,
                            reviews = venue.reviewCount ?: 2,
                            distance = venue.distance,
                            price = venue.sports?.firstOrNull()?.price?.toDoubleOrNull() ?: venue.pricePerHour,
                            imageUrl = normalizeUrl(venue.imageUrl ?: venue.imageUrlsList?.firstOrNull()),
                            sports = venue.sports?.map {
                                VenueSport(
                                    id = it.id ?: 0,
                                    name = it.name.orEmpty(),
                                    price = it.price.orEmpty()
                                )
                            } ?: emptyList()
                        )
                    }
                ApiResult.Success(popular)
            } else {
                ApiResult.Error(code = response.code(), message = "Failed to fetch popular venues")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching popular venues")
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    override suspend fun search(
        query: String,
        activeFilter: String,
        sportFilter: String,
        sortBy: String,
        latitude: Double?,
        longitude: Double?
    ): ApiResult<List<SearchResult>> = coroutineScope {
        try {
            val queryTrim = query.trim().lowercase()
            val venueResults = mutableListOf<SearchResult>()
            val sportResults = mutableListOf<SearchResult>()
            val teamResults = mutableListOf<SearchResult>()

            val fetchVenues = activeFilter == "all" || activeFilter == "venues"
            val fetchSports = activeFilter == "all" || activeFilter == "sports"
            val fetchTeams = activeFilter == "all" || activeFilter == "teams"

            val venueDtos: List<VenueDto> = if (fetchVenues) {
                try {
                    val resp = venueApiService.getVenues(search = query, perPage = 30, latitude = latitude, longitude = longitude)
                    if (resp.isSuccessful && resp.body() != null) parseVenuesJson(resp.body()!!) else emptyList()
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            val sportDtos: List<SearchResult> = if (fetchSports) {
                try {
                    val resp = searchApiService.getSports(search = query)
                    if (resp.isSuccessful && resp.body() != null) parseSportsJson(resp.body()!!) else emptyList()
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            val teamDtos: List<SearchResult> = if (fetchTeams) {
                try {
                    val resp = searchApiService.getTeams(search = query)
                    if (resp.isSuccessful && resp.body() != null) parseTeamsJson(resp.body()!!) else emptyList()
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            venueDtos.forEach { venue ->
                venueResults.add(
                    SearchResult(
                        id = venue.id,
                        type = SearchResultType.VENUE,
                        title = venue.name,
                        subtitle = venue.address ?: venue.location ?: "No address",
                        rating = venue.rating ?: 5.0f,
                        reviews = venue.reviewCount ?: 2,
                        distance = venue.distance,
                        price = venue.sports?.firstOrNull()?.price?.toDoubleOrNull() ?: venue.pricePerHour,
                        imageUrl = normalizeUrl(venue.imageUrl ?: venue.imageUrlsList?.firstOrNull()),
                        sports = venue.sports?.map {
                            VenueSport(
                                id = it.id ?: 0,
                                name = it.name.orEmpty(),
                                price = it.price.orEmpty()
                            )
                        } ?: emptyList()
                    )
                )
            }

            sportDtos.forEach { item ->
                sportResults.add(item)
            }

            teamDtos.forEach { item ->
                teamResults.add(item)
            }

            var merged = when (activeFilter) {
                "venues" -> venueResults
                "sports" -> sportResults
                "teams" -> teamResults
                else -> venueResults + sportResults + teamResults
            }

            if (sportFilter != "all") {
                merged = merged.filter { res ->
                    res.sportName?.equals(sportFilter, ignoreCase = true) == true ||
                            res.sports.any { it.name.equals(sportFilter, ignoreCase = true) }
                }
            }

            merged = when (sortBy) {
                "distance" -> merged.sortedBy { it.distance ?: Double.MAX_VALUE }
                "rating" -> merged.sortedByDescending { it.rating ?: 0f }
                "price" -> merged.sortedBy { it.price ?: Double.MAX_VALUE }
                else -> merged
            }

            ApiResult.Success(merged)
        } catch (e: Exception) {
            Timber.e(e, "Error performing search")
            ApiResult.Error(message = e.message ?: "Search failed")
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
            emptyList()
        }
    }

    private fun parseSportsJson(jsonElement: JsonElement): List<SearchResult> {
        val list = mutableListOf<SearchResult>()
        try {
            val array = when {
                jsonElement.isJsonArray -> jsonElement.asJsonArray
                jsonElement.isJsonObject && jsonElement.asJsonObject.has("results") -> jsonElement.asJsonObject.getAsJsonArray("results")
                else -> null
            }
            array?.forEach { element ->
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asString ?: ""
                    val name = obj.get("name")?.asString ?: obj.get("title")?.asString ?: "Sport"
                    val venueObj = if (obj.has("venue") && obj.get("venue").isJsonObject) obj.getAsJsonObject("venue") else null
                    val venueId = venueObj?.get("id")?.asString ?: obj.get("venue_id")?.asString
                    val venueName = venueObj?.get("name")?.asString ?: obj.get("venue_name")?.asString ?: "Venue #${id.take(3)}"
                    val venueAddress = venueObj?.get("address")?.asString ?: obj.get("venue_address")?.asString ?: ""
                    val price = obj.get("price")?.asDouble ?: 2000.0
                    val isAvailable = obj.get("available")?.asBoolean ?: true
                    val img = obj.get("image")?.asString ?: obj.get("image_url")?.asString

                    list.add(
                        SearchResult(
                            id = "sport-$id",
                            type = SearchResultType.SPORT,
                            title = name,
                            subtitle = venueName,
                            venueId = venueId,
                            venueName = venueName,
                            venueAddress = venueAddress,
                            price = price,
                            isAvailable = isAvailable,
                            imageUrl = normalizeUrl(img),
                            sportName = name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing sports JSON")
        }
        return list
    }

    private fun parseTeamsJson(jsonElement: JsonElement): List<SearchResult> {
        val list = mutableListOf<SearchResult>()
        try {
            val array = when {
                jsonElement.isJsonArray -> jsonElement.asJsonArray
                jsonElement.isJsonObject && jsonElement.asJsonObject.has("results") -> jsonElement.asJsonObject.getAsJsonArray("results")
                else -> null
            }
            array?.forEach { element ->
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asString ?: ""
                    val name = obj.get("name")?.asString ?: "Team"
                    val location = obj.get("location")?.asString ?: obj.get("address")?.asString ?: "Colombo, Western"
                    val sport = obj.get("sport")?.asString ?: "Cricket"
                    val img = obj.get("image")?.asString ?: obj.get("logo")?.asString

                    list.add(
                        SearchResult(
                            id = "team-$id",
                            type = SearchResultType.TEAM,
                            title = name,
                            subtitle = location,
                            sportName = sport,
                            imageUrl = normalizeUrl(img)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing teams JSON")
        }
        return list
    }

    private fun normalizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http")) url else "${BuildConfig.BASE_URL.trimEnd('/')}/$url".replace("//", "/")
    }
}

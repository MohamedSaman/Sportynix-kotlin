package com.sportynix.app.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonElement
import com.sportynix.app.data.remote.api.SportsApiService
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.domain.model.Venue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab { ALL, VENUES, TEAMS }

data class TeamSearchResult(
    val id: String,
    val name: String,
    val sport: String?,
    val location: String?,
    val logoUrl: String?,
    val memberCount: Int = 0
)

data class SearchUiState(
    val query: String = "",
    val venues: List<Venue> = emptyList(),
    val teams: List<TeamSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val activeTab: SearchTab = SearchTab.ALL,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val venueApiService: VenueApiService,
    private val sportsApiService: SportsApiService
) : ViewModel() {

    var state by mutableStateOf(SearchUiState())
        private set

    private var debounceJob: Job? = null

    fun onQueryChanged(query: String) {
        state = state.copy(query = query)
        debounceJob?.cancel()
        if (query.isBlank()) {
            state = state.copy(venues = emptyList(), teams = emptyList(), hasSearched = false, isLoading = false)
            return
        }
        debounceJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    fun setActiveTab(tab: SearchTab) {
        state = state.copy(activeTab = tab)
    }

    private suspend fun performSearch(query: String) {
        state = state.copy(isLoading = true, errorMessage = null)
        var fetchedVenues = emptyList<Venue>()
        var fetchedTeams = emptyList<TeamSearchResult>()

        // Search venues
        try {
            val venueResp = venueApiService.getVenues(search = query)
            if (venueResp.isSuccessful) {
                fetchedVenues = parseVenuesFromJson(venueResp.body())
            }
        } catch (_: Exception) {}

        // Search teams
        try {
            val teamResp = sportsApiService.getTeams(search = query)
            if (teamResp.isSuccessful) {
                fetchedTeams = parseTeamsFromJson(teamResp.body())
            }
        } catch (_: Exception) {}

        state = state.copy(
            venues = fetchedVenues,
            teams = fetchedTeams,
            isLoading = false,
            hasSearched = true
        )
    }

    private fun parseVenuesFromJson(element: JsonElement?): List<Venue> {
        if (element == null || element.isJsonNull) return emptyList()
        val gson = com.google.gson.Gson()
        return try {
            val dtoList = when {
                element.isJsonArray -> {
                    gson.fromJson(element.asJsonArray, Array<VenueDto>::class.java).toList()
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val arr = obj.get("results") ?: obj.get("data") ?: obj.get("venues")
                    if (arr != null && arr.isJsonArray) {
                        gson.fromJson(arr.asJsonArray, Array<VenueDto>::class.java).toList()
                    } else emptyList()
                }
                else -> emptyList()
            }
            dtoList.map { dto ->
                Venue(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description ?: "",
                    sportType = dto.sportType ?: "",
                    location = dto.location ?: "",
                    address = dto.address ?: "",
                    pricePerHour = dto.pricePerHour ?: 0.0,
                    rating = dto.rating ?: 0f,
                    reviewCount = dto.reviewCount ?: 0,
                    imageUrl = dto.imageUrl ?: "",
                    imageUrls = dto.imageUrlsList ?: emptyList(),
                    amenities = dto.amenities ?: emptyList(),
                    isFeatured = dto.isFeatured ?: false
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseTeamsFromJson(element: JsonElement?): List<TeamSearchResult> {
        if (element == null || element.isJsonNull) return emptyList()
        return try {
            val gson = com.google.gson.Gson()
            val arr = when {
                element.isJsonArray -> element.asJsonArray
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    (obj.get("results") ?: obj.get("data") ?: obj.get("teams"))?.asJsonArray
                }
                else -> null
            } ?: return emptyList()

            arr.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val obj = el.asJsonObject
                TeamSearchResult(
                    id = obj.get("id")?.asString ?: return@mapNotNull null,
                    name = obj.get("name")?.asString ?: "",
                    sport = obj.get("sport")?.asString ?: obj.get("sport_name")?.asString,
                    location = obj.get("location")?.asString ?: obj.get("city")?.asString,
                    logoUrl = obj.get("logo")?.asString ?: obj.get("logo_url")?.asString,
                    memberCount = obj.get("member_count")?.asInt ?: 0
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}

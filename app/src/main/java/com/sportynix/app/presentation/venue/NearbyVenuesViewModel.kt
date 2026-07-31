package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.SearchHistoryManager
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class NearbyTab { NEARBY, SEARCH }

data class NearbyVenuesUiState(
    val activeTab: NearbyTab = NearbyTab.NEARBY,
    val searchQuery: String = "",
    val selectedSport: String = "all",
    val selectedCategory: String = "all",
    val draftSport: String = "all",
    val draftCategory: String = "all",
    val showFiltersModal: Boolean = false,
    val userLocation: Pair<Double, Double>? = null,
    val venues: List<VenueDto> = emptyList(),
    val page: Int = 1,
    val hasNext: Boolean = true,
    val isLoading: Boolean = false,
    val isInlineLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NearbyVenuesViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    var state by mutableStateOf(NearbyVenuesUiState())
        private set

    private var searchJob: Job? = null

    init {
        loadSearchHistory()
    }

    fun setTab(tab: NearbyTab) {
        if (state.activeTab == tab) return
        state = state.copy(activeTab = tab)
        reloadDiscover()
    }

    fun onSearchQueryChanged(query: String) {
        state = state.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            reloadDiscover()
        }
    }

    fun submitSearch(query: String) {
        if (query.isNotBlank()) {
            searchHistoryManager.pushSearchHistory(query)
            loadSearchHistory()
        }
        reloadDiscover()
    }

    fun openFiltersModal() {
        state = state.copy(
            draftSport = state.selectedSport,
            draftCategory = state.selectedCategory,
            showFiltersModal = true
        )
    }

    fun dismissFiltersModal() {
        state = state.copy(showFiltersModal = false)
    }

    fun setDraftSport(sport: String) {
        state = state.copy(draftSport = sport)
    }

    fun setDraftCategory(category: String) {
        state = state.copy(draftCategory = category)
    }

    fun applyFilters() {
        state = state.copy(
            selectedSport = state.draftSport,
            selectedCategory = state.draftCategory,
            showFiltersModal = false
        )
        reloadDiscover()
    }

    fun clearFilters() {
        state = state.copy(
            selectedSport = "all",
            selectedCategory = "all",
            draftSport = "all",
            draftCategory = "all",
            searchQuery = "",
            showFiltersModal = false
        )
        reloadDiscover()
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        val currentLoc = state.userLocation
        if (currentLoc == null || calculateDistanceMeters(currentLoc.first, currentLoc.second, lat, lon) > 50) {
            state = state.copy(userLocation = Pair(lat, lon))
            reloadDiscover()
        }
    }

    fun discoverVenues(pageNum: Int, isRefreshing: Boolean = false) {
        if (state.isLoading || state.isInlineLoading) return

        viewModelScope.launch {
            if (pageNum == 1) {
                if (isRefreshing || state.venues.isNotEmpty()) {
                    state = state.copy(isInlineLoading = true, errorMessage = null)
                } else {
                    state = state.copy(isLoading = true, errorMessage = null)
                }
            } else {
                state = state.copy(isInlineLoading = true)
            }

            val lat = state.userLocation?.first
            val lng = state.userLocation?.second

            val searchStr = if (state.activeTab == NearbyTab.SEARCH) state.searchQuery.ifBlank { null } else null
            val sportStr = if (state.activeTab == NearbyTab.SEARCH) state.selectedSport else null
            val catStr = if (state.activeTab == NearbyTab.SEARCH) state.selectedCategory else null
            val perPageVal = if (state.activeTab == NearbyTab.SEARCH) 25 else 10
            val radiusVal = if (state.activeTab == NearbyTab.SEARCH) 1000 else null

            when (val res = venueRepository.fetchDiscoverVenues(
                page = pageNum,
                perPage = perPageVal,
                latitude = lat,
                longitude = lng,
                search = searchStr,
                sport = sportStr,
                venueCategory = catStr,
                radiusKm = radiusVal
            )) {
                is ApiResult.Success -> {
                    val (fetched, hasNextPage) = res.data
                    val updatedList = if (pageNum == 1) fetched else state.venues + fetched
                    state = state.copy(
                        venues = updatedList,
                        page = pageNum,
                        hasNext = hasNextPage,
                        isLoading = false,
                        isInlineLoading = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    state = state.copy(
                        isLoading = false,
                        isInlineLoading = false,
                        isRefreshing = false,
                        errorMessage = res.message
                    )
                }
                else -> {
                    state = state.copy(isLoading = false, isInlineLoading = false, isRefreshing = false)
                }
            }
        }
    }

    fun reloadDiscover() {
        discoverVenues(pageNum = 1, isRefreshing = true)
    }

    fun loadMore() {
        if (state.hasNext && !state.isLoading && !state.isInlineLoading) {
            discoverVenues(pageNum = state.page + 1)
        }
    }

    private fun loadSearchHistory() {
        state = state.copy(searchHistory = searchHistoryManager.getSearchHistory())
    }

    fun removeSearchHistoryItem(item: String) {
        searchHistoryManager.removeSearchHistoryItem(item)
        loadSearchHistory()
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearSearchHistory()
        loadSearchHistory()
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

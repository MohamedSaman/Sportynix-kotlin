package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Venue
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
    val userLocation: Pair<Double, Double>? = null,
    val venues: List<Venue> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = true,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NearbyVenuesViewModel @Inject constructor(
    private val venueRepository: VenueRepository
) : ViewModel() {

    var state by mutableStateOf(NearbyVenuesUiState())
        private set

    private var searchJob: Job? = null

    init {
        loadVenues(pageNum = 1)
    }

    fun setTab(tab: NearbyTab) {
        if (state.activeTab == tab) return
        state = state.copy(activeTab = tab, page = 1, venues = emptyList())
        loadVenues(pageNum = 1)
    }

    fun onSearchQueryChanged(query: String) {
        state = state.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadVenues(pageNum = 1)
        }
    }

    fun setSportFilter(sport: String) {
        state = state.copy(selectedSport = sport, page = 1)
        loadVenues(pageNum = 1)
    }

    fun setCategoryFilter(category: String) {
        state = state.copy(selectedCategory = category, page = 1)
        loadVenues(pageNum = 1)
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        state = state.copy(userLocation = Pair(lat, lon))
        loadVenues(pageNum = 1)
    }

    fun loadVenues(pageNum: Int = 1, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (pageNum == 1 && !isRefresh) {
                state = state.copy(isLoading = true, errorMessage = null)
            } else if (pageNum > 1) {
                state = state.copy(isLoadingMore = true)
            }

            val sportParam = if (state.selectedSport != "all") state.selectedSport else null
            val queryParam = if (state.searchQuery.isNotBlank()) state.searchQuery.trim() else null

            val result = if (state.activeTab == NearbyTab.NEARBY && state.userLocation != null) {
                venueRepository.fetchNearbyVenues(state.userLocation!!.first, state.userLocation!!.second)
            } else {
                venueRepository.fetchVenues(sportType = sportParam, query = queryParam)
            }

            when (result) {
                is ApiResult.Success -> {
                    val newVenues = result.data
                    val updatedList = if (pageNum == 1 || isRefresh) newVenues else state.venues + newVenues
                    state = state.copy(
                        venues = updatedList,
                        page = pageNum,
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> {
                    state = state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    state = state.copy(isLoading = false, isLoadingMore = false, isRefreshing = false)
                }
            }
        }
    }

    fun refresh() {
        state = state.copy(isRefreshing = true)
        loadVenues(pageNum = 1, isRefresh = true)
    }
}

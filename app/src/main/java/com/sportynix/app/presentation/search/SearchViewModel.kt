package com.sportynix.app.presentation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.SearchResult
import com.sportynix.app.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val activeFilter: String = "all", // "all", "venues", "sports", "teams"
    val activeSportFilter: String = "all",
    val sortBy: String = "relevance", // "relevance", "distance", "rating", "price"
    val recentSearches: List<String> = emptyList(),
    val popularVenues: List<SearchResult> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val userLocation: Pair<Double, Double>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    var state by mutableStateOf(SearchUiState())
        private set

    private var debounceJob: Job? = null
    private var requestJob: Job? = null
    private var requestVersion: Long = 0

    init {
        observeRecentSearches()
        loadPopularVenues()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collectLatest { list ->
                state = state.copy(recentSearches = list)
            }
        }
    }

    fun loadPopularVenues() {
        viewModelScope.launch {
            val loc = state.userLocation
            when (val result = searchRepository.fetchPopularVenues(loc?.first, loc?.second)) {
                is ApiResult.Success -> {
                    state = state.copy(popularVenues = result.data)
                }
                else -> {}
            }
        }
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        state = state.copy(userLocation = Pair(lat, lon))
        loadPopularVenues()
    }

    fun onSearchQueryChanged(query: String) {
        state = state.copy(searchQuery = query)
        debounceJob?.cancel()
        requestJob?.cancel()
        requestVersion++

        if (query.trim().isEmpty()) {
            state = state.copy(searchResults = emptyList(), isLoading = false, errorMessage = null)
            return
        }

        if (query.trim().length < 3) {
            state = state.copy(searchResults = emptyList(), isLoading = false, errorMessage = null)
            return
        }

        debounceJob = viewModelScope.launch {
            delay(300)
            executeSearch()
        }
    }

    fun setFilter(filter: String) {
        if (state.activeFilter == filter) return
        state = state.copy(activeFilter = filter)
        if (state.searchQuery.trim().length >= 3) {
            executeSearch()
        }
    }

    fun setSportFilter(sport: String) {
        state = state.copy(activeSportFilter = sport)
        if (state.searchQuery.trim().length >= 3) {
            executeSearch()
        }
    }

    fun setSortBy(sort: String) {
        state = state.copy(sortBy = sort)
        if (state.searchQuery.trim().length >= 3) {
            executeSearch()
        }
    }

    private fun executeSearch() {
        val q = state.searchQuery.trim()
        if (q.length < 3) return
        requestJob?.cancel()
        val version = ++requestVersion
        val filter = state.activeFilter
        val sportFilter = state.activeSportFilter
        val sort = state.sortBy
        val loc = state.userLocation
        requestJob = viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            val result = searchRepository.search(
                query = q,
                activeFilter = filter,
                sportFilter = sportFilter,
                sortBy = sort,
                latitude = loc?.first,
                longitude = loc?.second
            )

            if (version != requestVersion || q != state.searchQuery.trim()) return@launch
            when (result) {
                is ApiResult.Success -> {
                    state = state.copy(searchResults = result.data, isLoading = false, errorMessage = null)
                    searchRepository.addRecentSearch(q)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    state = state.copy(isLoading = false)
                }
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }
}

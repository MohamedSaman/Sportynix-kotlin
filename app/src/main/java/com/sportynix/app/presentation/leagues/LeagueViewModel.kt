package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.remote.websocket.LiveMatchWebSocketManager
import com.sportynix.app.data.repository.LeagueRepository
import com.sportynix.app.domain.model.LiveMatchSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeagueListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val leagues: List<FullLeagueDto> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedSport: String = "all", // all, cricket, football, volleyball, etc.
    val selectedStatus: String = "all", // all, upcoming, in_progress, completed, registration
    val selectedCricketVariant: String = "all", // all, softball, hardball
    val liveMatches: List<LiveMatchSnapshot> = emptyList()
)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository,
    private val liveMatchWebSocketManager: LiveMatchWebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueListUiState())
    val uiState: StateFlow<LeagueListUiState> = _uiState.asStateFlow()

    init {
        loadLeagues()
        observeLiveMatches()
    }

    private fun observeLiveMatches() {
        viewModelScope.launch {
            liveMatchWebSocketManager.connect()
            liveMatchWebSocketManager.matchesState.collect { matches ->
                _uiState.value = _uiState.value.copy(liveMatches = matches)
            }
        }
    }

    fun loadLeagues(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val sportFilter = if (_uiState.value.selectedSport == "all") null else _uiState.value.selectedSport
            val statusFilter = if (_uiState.value.selectedStatus == "all") null else _uiState.value.selectedStatus
            val queryFilter = if (_uiState.value.searchQuery.isBlank()) null else _uiState.value.searchQuery

            when (val result = leagueRepository.getLeagues(
                search = queryFilter,
                sportType = sportFilter,
                status = statusFilter
            )) {
                is ApiResult.Success -> {
                    var filtered = result.data
                    if (_uiState.value.selectedCricketVariant != "all") {
                        val variantKey = _uiState.value.selectedCricketVariant.lowercase()
                        filtered = filtered.filter { league ->
                            val v = (league.cricketVariant ?: "").lowercase()
                            v.contains(variantKey)
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        leagues = filtered,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message ?: "Failed to load leagues"
                    )
                }
                else -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadLeagues()
    }

    fun onSportSelected(sport: String) {
        _uiState.value = _uiState.value.copy(selectedSport = sport)
        loadLeagues()
    }

    fun onStatusSelected(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        loadLeagues()
    }

    fun onCricketVariantSelected(variant: String) {
        _uiState.value = _uiState.value.copy(selectedCricketVariant = variant)
        loadLeagues()
    }
}

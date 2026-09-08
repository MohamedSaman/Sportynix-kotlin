package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.remote.dto.TournamentDto
import com.sportynix.app.data.remote.websocket.AllLiveMatchesWebSocketService
import com.sportynix.app.data.remote.websocket.LiveMatchWSEvent
import com.sportynix.app.data.repository.LeagueRepository
import com.sportynix.app.data.repository.TournamentRepository
import com.sportynix.app.domain.model.LiveMatchSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeagueListUiState(
    // General
    val activeTab: Int = 0, // 0 = Matches, 1 = Leagues, 2 = Tournaments
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",

    // Leagues tab
    val leagues: List<FullLeagueDto> = emptyList(),
    val selectedLeagueSport: String = "All Sports",
    val selectedLeagueFormat: String = "All Formats",
    val selectedLeagueStatus: String = "All Statuses",
    val selectedLeagueFeatured: String = "All",
    val selectedLeagueSort: String = "Default",

    // Tournaments tab
    val tournaments: List<TournamentDto> = emptyList(),
    val tournamentStatusFilter: String = "All",
    val cricketVariantFilter: String = "All",
    val formatFilter: String = "All",
    val approvalStatusFilter: String = "All",

    // Matches (Live WS) tab
    val liveMatches: List<LiveMatchSnapshot> = emptyList(),
    val selectedSportFilter: String = "All",
    val selectedStatusFilter: String = "Live" // Live, Upcoming, Completed, League, Tournament, Friendly
)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository,
    private val tournamentRepository: TournamentRepository,
    private val liveMatchWebSocketService: AllLiveMatchesWebSocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueListUiState())
    val uiState: StateFlow<LeagueListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadLeagues()
        loadTournaments()
        observeLiveMatches()
    }

    private fun observeLiveMatches() {
        viewModelScope.launch {
            liveMatchWebSocketService.connect()
            val wsFilter = mapStatusFilterToWS(_uiState.value.selectedStatusFilter)
            liveMatchWebSocketService.setFilter(wsFilter)

            liveMatchWebSocketService.events.collect { event ->
                when (event) {
                    is LiveMatchWSEvent.MatchSnapshotUpdate -> {
                        _uiState.value = _uiState.value.copy(liveMatches = event.matches)
                    }
                    is LiveMatchWSEvent.SingleMatchScoreUpdate -> {
                        val currentList = _uiState.value.liveMatches.toMutableList()
                        val index = currentList.indexOfFirst { it.matchId == event.matchId }
                        if (index != -1) {
                            currentList[index] = event.liveData
                        } else {
                            currentList.add(0, event.liveData)
                        }
                        _uiState.value = _uiState.value.copy(liveMatches = currentList)
                    }
                }
            }
        }
    }

    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab, searchQuery = "")
        if (tab == 1) {
            loadLeagues()
        } else if (tab == 2) {
            loadTournaments()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            if (_uiState.value.activeTab == 1) {
                loadLeagues()
            } else if (_uiState.value.activeTab == 2) {
                loadTournaments()
            }
        }
    }

    fun onMatchStatusFilterChanged(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        viewModelScope.launch {
            val wsFilter = mapStatusFilterToWS(status)
            liveMatchWebSocketService.setFilter(wsFilter)
        }
    }

    fun onMatchSportFilterChanged(sport: String) {
        _uiState.value = _uiState.value.copy(selectedSportFilter = sport)
    }

    fun updateLeagueFilters(
        sport: String,
        format: String,
        status: String,
        featured: String,
        sort: String
    ) {
        _uiState.value = _uiState.value.copy(
            selectedLeagueSport = sport,
            selectedLeagueFormat = format,
            selectedLeagueStatus = status,
            selectedLeagueFeatured = featured,
            selectedLeagueSort = sort
        )
        loadLeagues()
    }

    fun updateTournamentFilters(
        status: String,
        variant: String,
        format: String,
        approval: String
    ) {
        _uiState.value = _uiState.value.copy(
            tournamentStatusFilter = status,
            cricketVariantFilter = variant,
            formatFilter = format,
            approvalStatusFilter = approval
        )
    }

    fun loadLeagues(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val sportFilter = if (_uiState.value.selectedLeagueSport == "All Sports") null else _uiState.value.selectedLeagueSport.lowercase()
            val statusFilter = if (_uiState.value.selectedLeagueStatus == "All Statuses") null else mapLeagueStatusToBackend(_uiState.value.selectedLeagueStatus)
            val formatFilter = if (_uiState.value.selectedLeagueFormat == "All Formats") null else mapFormatToBackend(_uiState.value.selectedLeagueFormat)
            val queryFilter = if (_uiState.value.searchQuery.isBlank() || _uiState.value.activeTab != 1) null else _uiState.value.searchQuery

            when (val result = leagueRepository.getLeagues(
                search = queryFilter,
                sportType = sportFilter,
                status = statusFilter,
                format = formatFilter
            )) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        leagues = result.data,
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
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    fun loadTournaments(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val queryFilter = if (_uiState.value.searchQuery.isBlank() || _uiState.value.activeTab != 2) null else _uiState.value.searchQuery

            when (val result = tournamentRepository.getTournaments(search = queryFilter)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        tournaments = result.data,
                        error = null
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message ?: "Failed to load tournaments"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    fun deleteLeague(leagueId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = leagueRepository.deleteLeague(leagueId)) {
                is ApiResult.Success -> {
                    val newList = _uiState.value.leagues.filter { it.id != leagueId }
                    _uiState.value = _uiState.value.copy(isLoading = false, leagues = newList)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onError(result.message ?: "Failed to delete league")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveMatchWebSocketService.disconnect()
    }

    private fun mapStatusFilterToWS(filter: String): String {
        return when (filter) {
            "Live" -> "live"
            "Upcoming" -> "upcoming"
            "Completed" -> "completed"
            else -> "all"
        }
    }

    private fun mapLeagueStatusToBackend(status: String): String? {
        return when (status) {
            "All Statuses" -> null
            "In Progress" -> "in_progress"
            "Registration" -> "registration"
            else -> status.lowercase()
        }
    }

    private fun mapFormatToBackend(format: String): String? {
        return when (format) {
            "All Formats" -> null
            "Round Robin" -> "round_robin"
            "Knockout" -> "knockout"
            "Group + Knockout" -> "group_knockout"
            "League + Playoff" -> "league_playoff"
            else -> format.replace(" ", "_").replace("+", "").lowercase()
        }
    }
}

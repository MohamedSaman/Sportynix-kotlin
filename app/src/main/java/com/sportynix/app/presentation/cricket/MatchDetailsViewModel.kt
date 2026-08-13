package com.sportynix.app.presentation.cricket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.LeagueApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.CricketScoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MatchDetailsTab {
    INFO, LIVE, SCORECARD, COMMENTARY, SQUADS
}

data class MatchDetailsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: MatchDetailsTab = MatchDetailsTab.INFO,
    val availableTabs: List<MatchDetailsTab> = listOf(MatchDetailsTab.INFO, MatchDetailsTab.SQUADS),
    val matchDetails: LiveMatchDto? = null,
    val liveState: LiveStateDto? = null,
    val scorecard: ScorecardDto? = null,
    val commentary: List<BallByBallBallDto> = emptyList(),
    val playingXI: PlayingXIResponseDto? = null,
    val team1Squad: List<PlayingXIPlayerDto> = emptyList(),
    val team2Squad: List<PlayingXIPlayerDto> = emptyList(),
    val isSocketConnected: Boolean = false,
    val isEditModalOpen: Boolean = false,
    val isPlayingXIModalOpen: Boolean = false,
    val updatedDate: String? = null,
    val updatedTime: String? = null,
    val updatedVenue: String? = null
)

@HiltViewModel
class MatchDetailsViewModel @Inject constructor(
    private val repository: CricketScoringRepository,
    private val leagueApiService: LeagueApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchDetailsUiState())
    val uiState: StateFlow<MatchDetailsUiState> = _uiState.asStateFlow()

    private var currentMatchId: String? = null

    fun initializeMatchDetails(matchId: String) {
        currentMatchId = matchId
        repository.connectWebSocket(matchId)

        viewModelScope.launch {
            repository.isSocketConnected.collectLatest { connected ->
                _uiState.value = _uiState.value.copy(isSocketConnected = connected)
            }
        }

        viewModelScope.launch {
            repository.liveStateSocket.collectLatest { socketState ->
                if (socketState != null) {
                    _uiState.value = _uiState.value.copy(liveState = socketState)
                }
            }
        }

        fetchData(matchId)
    }

    fun selectTab(tab: MatchDetailsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun fetchData(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Live state & scorecard
            when (val res = repository.getLiveState(targetMatchId)) {
                is ApiResult.Success -> {
                    val state = res.data
                    val tabs = when (state.status.lowercase()) {
                        "live" -> listOf(MatchDetailsTab.INFO, MatchDetailsTab.LIVE, MatchDetailsTab.SCORECARD, MatchDetailsTab.COMMENTARY, MatchDetailsTab.SQUADS)
                        "completed" -> listOf(MatchDetailsTab.INFO, MatchDetailsTab.SCORECARD, MatchDetailsTab.COMMENTARY, MatchDetailsTab.SQUADS)
                        else -> listOf(MatchDetailsTab.INFO, MatchDetailsTab.SQUADS)
                    }

                    val defaultTab = if (tabs.contains(_uiState.value.selectedTab)) _uiState.value.selectedTab else tabs.first()

                    _uiState.value = _uiState.value.copy(
                        liveState = state,
                        availableTabs = tabs,
                        selectedTab = defaultTab
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
                else -> {}
            }

            fetchScorecard(targetMatchId)
            fetchCommentary(targetMatchId)
            fetchPlayingXI(targetMatchId)

            _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
        }
    }

    fun fetchScorecard(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            when (val res = repository.getScorecard(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(scorecard = res.data)
                }
                else -> {}
            }
        }
    }

    fun fetchCommentary(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            when (val res = repository.getBallByBall(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(commentary = res.data.balls)
                }
                else -> {}
            }
        }
    }

    fun fetchPlayingXI(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            when (val res = repository.getPlayingXI(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        playingXI = res.data,
                        team1Squad = res.data.team1?.players ?: emptyList(),
                        team2Squad = res.data.team2?.players ?: emptyList()
                    )
                }
                else -> {}
            }
        }
    }

    fun openEditModal() {
        val details = _uiState.value.matchDetails
        _uiState.value = _uiState.value.copy(
            isEditModalOpen = true,
            updatedDate = details?.scheduledDate,
            updatedTime = details?.scheduledTime,
            updatedVenue = details?.venue
        )
    }

    fun closeEditModal() {
        _uiState.value = _uiState.value.copy(isEditModalOpen = false)
    }

    fun updateMatchInfo(date: String?, time: String?, venue: String?) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                closeEditModal()
                fetchData(matchId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectWebSocket()
    }
}

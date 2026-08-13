package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeagueDetailUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val league: FullLeagueDto? = null,
    val teams: List<FullLeagueTeamDto> = emptyList(),
    val fixtures: List<FixtureDto> = emptyList(),
    val standings: List<FullStandingDto> = emptyList(),
    val stats: List<PlayerStatDto> = emptyList(),
    val selectedTab: Int = 0, // 0: Teams, 1: Matches, 2: Points, 3: Profile
    val error: String? = null,
    val actionSuccessMessage: String? = null,

    // Permissions & User Application State
    val isCreator: Boolean = false,
    val isAdmin: Boolean = false,
    val isModerator: Boolean = false,
    val userApplicationStatus: String? = null,
    val userApplicationId: String? = null,

    // Apply Player Modal
    val showApplyModal: Boolean = false,
    val applyNote: String = "",
    val preferredVariant: String = "softball",
    val primaryRole: String = "batsman",
    val battingStyle: String = "right_hand_bat",
    val bowlingStyle: String = "right_arm_medium",
    val isSubmittingApplication: Boolean = false
)

@HiltViewModel
class LeagueDetailViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueDetailUiState())
    val uiState: StateFlow<LeagueDetailUiState> = _uiState.asStateFlow()

    private var currentLeagueId: String? = null

    fun loadLeagueDetail(leagueId: String, isRefresh: Boolean = false) {
        currentLeagueId = leagueId
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            when (val res = leagueRepository.getLeagueDetail(leagueId)) {
                is ApiResult.Success -> {
                    val l = res.data
                    _uiState.value = _uiState.value.copy(
                        league = l,
                        isCreator = l.isCreator ?: false,
                        isAdmin = l.isAdmin ?: false,
                        isModerator = l.isModerator ?: false,
                        userApplicationStatus = l.userApplicationStatus,
                        userApplicationId = l.userApplicationId
                    )
                    loadSubData(leagueId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = res.message ?: "Failed to load league details"
                    )
                }
                else -> {}
            }
        }
    }

    private suspend fun loadSubData(leagueId: String) {
        val teamsRes = leagueRepository.getLeagueTeams(leagueId)
        val fixturesRes = leagueRepository.getLeagueFixtures(leagueId)
        val standingsRes = leagueRepository.getLeagueStandings(leagueId)
        val statsRes = leagueRepository.getLeagueStats(leagueId)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            teams = (teamsRes as? ApiResult.Success)?.data ?: emptyList(),
            fixtures = (fixturesRes as? ApiResult.Success)?.data ?: emptyList(),
            standings = (standingsRes as? ApiResult.Success)?.data ?: emptyList(),
            stats = (statsRes as? ApiResult.Success)?.data ?: emptyList()
        )
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun toggleApplyModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showApplyModal = show)
    }

    fun updateApplicationForm(note: String, variant: String, role: String, bat: String, bowl: String) {
        _uiState.value = _uiState.value.copy(
            applyNote = note,
            preferredVariant = variant,
            primaryRole = role,
            battingStyle = bat,
            bowlingStyle = bowl
        )
    }

    fun submitPlayerApplication() {
        val leagueId = currentLeagueId ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmittingApplication = true, error = null)
            val body = JsonObject().apply {
                addProperty("application_note", state.applyNote)
                addProperty("cricket_preferred_variant", state.preferredVariant)
                addProperty("cricket_primary_role", state.primaryRole)
                addProperty("cricket_batting_style", state.battingStyle)
                addProperty("cricket_bowling_style", state.bowlingStyle)
            }

            when (val res = leagueRepository.applyAsPlayer(leagueId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingApplication = false,
                        showApplyModal = false,
                        userApplicationStatus = res.data.status,
                        userApplicationId = res.data.id,
                        actionSuccessMessage = "Application submitted successfully!"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingApplication = false,
                        error = res.message ?: "Failed to submit application"
                    )
                }
                else -> {}
            }
        }
    }

    fun withdrawApplication() {
        val appId = _uiState.value.userApplicationId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.withdrawApplication(appId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userApplicationStatus = "withdrawn",
                        userApplicationId = null,
                        actionSuccessMessage = "Application withdrawn"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun executeLifecycleAction(action: String) {
        val leagueId = currentLeagueId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = when (action.lowercase()) {
                "publish" -> leagueRepository.publishLeague(leagueId)
                "start" -> leagueRepository.startLeague(leagueId)
                "suspend" -> leagueRepository.suspendLeague(leagueId)
                "complete" -> leagueRepository.completeLeague(leagueId)
                "cancel" -> leagueRepository.cancelLeague(leagueId)
                else -> ApiResult.Error(message = "Invalid action")
            }

            when (res) {
                is ApiResult.Success -> {
                    loadLeagueDetail(leagueId)
                    _uiState.value = _uiState.value.copy(actionSuccessMessage = "League status updated to $action")
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccessMessage = null)
    }
}

package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FullLeagueTeamDto
import com.sportynix.app.data.remote.dto.SquadMemberDto
import com.sportynix.app.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamDetailUiState(
    val isLoading: Boolean = false,
    val team: FullLeagueTeamDto? = null,
    val squad: List<SquadMemberDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val canManage: Boolean = false,

    // Dialog & Add Player controls
    val showAddPlayerModal: Boolean = false,
    val targetUserId: String = "",
    val jerseyNumber: Int? = null,
    val role: String = "player", // captain, vice_captain, player, coach, manager

    // Co-Admin Dialog
    val showCoAdminModal: Boolean = false,
    val coAdminUserId: String = ""
)

@HiltViewModel
class LeagueTeamsViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamDetailUiState())
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    private var currentTeamId: String? = null

    fun loadTeamDetail(teamId: String) {
        currentTeamId = teamId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.getLeagueTeamDetail(teamId)) {
                is ApiResult.Success -> {
                    val t = res.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        team = t,
                        squad = t.squad ?: emptyList(),
                        canManage = t.canManageTeam ?: t.isCaptain ?: t.isCoAdmin ?: t.isLeagueCreator ?: false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun toggleAddPlayerModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddPlayerModal = show)
    }

    fun toggleCoAdminModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCoAdminModal = show)
    }

    fun addPlayerToSquad(userId: String, jersey: Int?, role: String) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val body = JsonObject().apply {
                addProperty("user_id", userId)
                if (jersey != null) addProperty("jersey_number", jersey)
                addProperty("role", role)
            }

            when (val res = leagueRepository.addSquadMember(teamId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        showAddPlayerModal = false,
                        successMessage = "Player added to squad!"
                    )
                    loadTeamDetail(teamId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun removePlayerFromSquad(memberId: String) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.removeSquadMember(teamId, memberId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Player removed from squad")
                    loadTeamDetail(teamId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun updatePlayerRole(memberId: String, newRole: String, jersey: Int?) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val body = JsonObject().apply {
                addProperty("role", newRole)
                if (jersey != null) addProperty("jersey_number", jersey)
            }

            when (val res = leagueRepository.updateSquadMember(teamId, memberId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Squad member role updated")
                    loadTeamDetail(teamId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun addCoAdmin(userId: String) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.addCoAdmin(teamId, userId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(showCoAdminModal = false, successMessage = "Co-admin added")
                    loadTeamDetail(teamId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun removeCoAdmin(userId: String) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.removeCoAdmin(teamId, userId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Co-admin removed")
                    loadTeamDetail(teamId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

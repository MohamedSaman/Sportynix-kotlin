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
    val isRefreshing: Boolean = false,
    val team: FullLeagueTeamDto? = null,
    val squad: List<SquadMemberDto> = emptyList(),
    val filteredSquad: List<SquadMemberDto> = emptyList(),
    val squadSearchQuery: String = "",
    val squadRoleFilter: String = "all", // all, captain, batsman, bowler, all-rounder, etc.
    val error: String? = null,
    val successMessage: String? = null,
    val canManage: Boolean = false,

    // Dialog & Add Player controls
    val showAddPlayerModal: Boolean = false,
    val targetUserId: String = "",
    val jerseyNumber: Int? = null,
    val role: String = "player", // captain, vice_captain, player, coach, manager
    val playingPosition: String = "Batsman",

    // Co-Admin Dialog
    val showCoAdminModal: Boolean = false,
    val coAdminUserId: String = "",

    // Role Edit Dialog
    val selectedMemberForEdit: SquadMemberDto? = null
)

@HiltViewModel
class LeagueTeamsViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamDetailUiState())
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    private var currentTeamId: String? = null

    fun loadTeamDetail(teamId: String, isRefresh: Boolean = false) {
        currentTeamId = teamId
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            when (val res = leagueRepository.getLeagueTeamDetail(teamId)) {
                is ApiResult.Success -> {
                    val t = res.data
                    val squadList = t.squad ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        team = t,
                        squad = squadList,
                        canManage = t.canManageTeam ?: t.isCaptain ?: t.isCoAdmin ?: t.isLeagueCreator ?: false
                    )
                    applySquadFilter()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = res.message ?: "Failed to load team details"
                    )
                }
                else -> {}
            }
        }
    }

    fun setSquadSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(squadSearchQuery = query)
        applySquadFilter()
    }

    fun setSquadRoleFilter(filter: String) {
        _uiState.value = _uiState.value.copy(squadRoleFilter = filter)
        applySquadFilter()
    }

    private fun applySquadFilter() {
        val state = _uiState.value
        val query = state.squadSearchQuery.trim().lowercase()

        var list = when (state.squadRoleFilter.lowercase()) {
            "all" -> state.squad
            "captains" -> state.squad.filter {
                it.role.equals("captain", ignoreCase = true) || it.role.equals("vice_captain", ignoreCase = true)
            }
            else -> state.squad.filter {
                it.role.equals(state.squadRoleFilter, ignoreCase = true) ||
                        (it.playingPosition ?: "").contains(state.squadRoleFilter, ignoreCase = true)
            }
        }

        if (query.isNotEmpty()) {
            list = list.filter { member ->
                val name = (member.user.fullName ?: member.user.name ?: "").lowercase()
                val username = (member.user.username ?: "").lowercase()
                val role = member.role.lowercase()
                val pos = (member.playingPosition ?: "").lowercase()
                val jersey = member.jerseyNumber?.toString() ?: ""

                name.contains(query) || username.contains(query) || role.contains(query) ||
                        pos.contains(query) || jersey.contains(query)
            }
        }

        _uiState.value = state.copy(filteredSquad = list)
    }

    fun toggleAddPlayerModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddPlayerModal = show)
    }

    fun toggleCoAdminModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCoAdminModal = show)
    }

    fun selectMemberForEdit(member: SquadMemberDto?) {
        _uiState.value = _uiState.value.copy(selectedMemberForEdit = member)
    }

    fun addPlayerToSquad(userId: String, jersey: Int?, role: String, playingPosition: String? = null) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val body = JsonObject().apply {
                addProperty("user_id", userId)
                if (jersey != null) addProperty("jersey_number", jersey)
                addProperty("role", role)
                if (!playingPosition.isNullOrBlank()) {
                    addProperty("playing_position", playingPosition)
                }
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

    fun updatePlayerRole(memberId: String, newRole: String, jersey: Int?, playingPosition: String? = null) {
        val teamId = currentTeamId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val body = JsonObject().apply {
                addProperty("role", newRole)
                if (jersey != null) addProperty("jersey_number", jersey)
                if (!playingPosition.isNullOrBlank()) {
                    addProperty("playing_position", playingPosition)
                }
            }

            when (val res = leagueRepository.updateSquadMember(teamId, memberId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        selectedMemberForEdit = null,
                        successMessage = "Squad member updated"
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

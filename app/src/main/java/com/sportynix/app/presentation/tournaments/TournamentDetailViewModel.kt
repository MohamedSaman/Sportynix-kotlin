package com.sportynix.app.presentation.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TournamentDetailUiState(
    val isLoading: Boolean = true,
    val tournament: FullTournamentDto? = null,
    val matches: List<FullTournamentMatchDto> = emptyList(),
    val applications: List<TournamentApplicationDto> = emptyList(),
    val participations: List<TournamentParticipationDto> = emptyList(),
    val eligibleTeams: List<EligibleTeamDto> = emptyList(),
    val teamMembers: List<TournamentTeamMemberDto> = emptyList(),
    val editRequests: List<TournamentEditRequestDto> = emptyList(),
    val selectedTab: Int = 0, // 0: Teams, 1: Matches, 2: Points Table, 3: Profile & Rules
    val selectedApplicationTab: String = "pending", // pending, approved, rejected, waitlisted, finalized
    val showApplyModal: Boolean = false,
    val selectedTeamId: String? = null,
    val selectedMemberIds: Set<String> = emptySet(),
    val selectedCaptainId: String? = null,
    val applicationTeamNote: String = "",
    val isSubmittingApplication: Boolean = false,
    val loadingTeamMembers: Boolean = false,
    val showApplicationsReviewModal: Boolean = false,
    val selectedApplication: TournamentApplicationDto? = null,
    val showRequestEditModal: Boolean = false,
    val requestEditReason: String = "",
    val showLimitedEditModal: Boolean = false,
    val isActionLoading: Boolean = false,
    val actionSuccessMessage: String? = null,
    val error: String? = null
) {
    val isHost: Boolean
        get() = tournament?.isCreator == true

    val isStaff: Boolean
        get() = tournament?.isStaffUser == true

    val isApproved: Boolean
        get() = tournament?.approvalStatus?.equals("approved", true) == true

    val isPendingApproval: Boolean
        get() = tournament?.approvalStatus?.equals("pending", true) == true || tournament?.approvalStatus == null

    val isApplicationsOpen: Boolean
        get() = tournament?.applicationsEffectivelyOpen == true || tournament?.status?.equals("applications_open", true) == true

    val canApply: Boolean
        get() = !isHost && !isStaff && isApproved && isApplicationsOpen
}

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    private var currentTournamentId: String? = null

    fun loadTournamentDetail(tournamentId: String) {
        currentTournamentId = tournamentId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val detailDeferred = async { repository.getTournamentDetail(tournamentId) }
            val matchesDeferred = async { repository.getMatches(tournamentId) }
            val appsDeferred = async { repository.getApplications(tournamentId) }
            val partDeferred = async { repository.getParticipations(tournamentId) }
            val eligibleDeferred = async { repository.getEligibleTeams(tournamentId) }
            val editReqDeferred = async { repository.getTournamentEditRequests(tournamentId) }

            val detailRes = detailDeferred.await()
            val matchesRes = matchesDeferred.await()
            val appsRes = appsDeferred.await()
            val partRes = partDeferred.await()
            val eligibleRes = eligibleDeferred.await()
            val editReqRes = editReqDeferred.await()

            var tournamentData: FullTournamentDto? = null
            var errorMsg: String? = null

            when (detailRes) {
                is ApiResult.Success -> tournamentData = detailRes.data
                is ApiResult.Error -> errorMsg = detailRes.message
                else -> {}
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                tournament = tournamentData,
                matches = (matchesRes as? ApiResult.Success)?.data ?: emptyList(),
                applications = (appsRes as? ApiResult.Success)?.data ?: emptyList(),
                participations = (partRes as? ApiResult.Success)?.data ?: emptyList(),
                eligibleTeams = (eligibleRes as? ApiResult.Success)?.data ?: emptyList(),
                editRequests = (editReqRes as? ApiResult.Success)?.data ?: emptyList(),
                error = errorMsg
            )
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun selectApplicationTab(tab: String) {
        _uiState.value = _uiState.value.copy(selectedApplicationTab = tab)
    }

    fun toggleApplyModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showApplyModal = show,
            selectedTeamId = null,
            selectedMemberIds = emptySet(),
            selectedCaptainId = null,
            applicationTeamNote = "",
            teamMembers = emptyList()
        )
    }

    fun toggleApplicationsReviewModal(show: Boolean, app: TournamentApplicationDto? = null) {
        _uiState.value = _uiState.value.copy(
            showApplicationsReviewModal = show,
            selectedApplication = app
        )
    }

    fun toggleRequestEditModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showRequestEditModal = show,
            requestEditReason = ""
        )
    }

    fun toggleLimitedEditModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLimitedEditModal = show)
    }

    fun updateRequestEditReason(reason: String) {
        _uiState.value = _uiState.value.copy(requestEditReason = reason)
    }

    fun updateApplicationNote(note: String) {
        _uiState.value = _uiState.value.copy(applicationTeamNote = note)
    }

    fun selectTeamForApplication(teamId: String) {
        _uiState.value = _uiState.value.copy(
            selectedTeamId = teamId,
            selectedMemberIds = emptySet(),
            selectedCaptainId = null,
            loadingTeamMembers = true
        )
        viewModelScope.launch {
            when (val res = repository.getTeamMembersForRoster(teamId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        teamMembers = res.data,
                        loadingTeamMembers = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loadingTeamMembers = false,
                        error = res.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(loadingTeamMembers = false)
                }
            }
        }
    }

    fun toggleRosterMember(memberId: String) {
        val t = _uiState.value.tournament ?: return
        val currentSet = _uiState.value.selectedMemberIds.toMutableSet()
        if (currentSet.contains(memberId)) {
            currentSet.remove(memberId)
            val newCap = if (_uiState.value.selectedCaptainId == memberId) null else _uiState.value.selectedCaptainId
            _uiState.value = _uiState.value.copy(selectedMemberIds = currentSet, selectedCaptainId = newCap)
        } else {
            if (currentSet.size >= t.rosterSizeLimit) {
                _uiState.value = _uiState.value.copy(error = "Maximum roster limit is ${t.rosterSizeLimit} players.")
                return
            }
            currentSet.add(memberId)
            _uiState.value = _uiState.value.copy(selectedMemberIds = currentSet)
        }
    }

    fun selectCaptain(memberId: String) {
        if (!_uiState.value.selectedMemberIds.contains(memberId)) {
            val currentSet = _uiState.value.selectedMemberIds.toMutableSet()
            currentSet.add(memberId)
            _uiState.value = _uiState.value.copy(selectedMemberIds = currentSet, selectedCaptainId = memberId)
        } else {
            _uiState.value = _uiState.value.copy(selectedCaptainId = memberId)
        }
    }

    fun submitApplication() {
        val t = _uiState.value.tournament ?: return
        val teamId = _uiState.value.selectedTeamId ?: run {
            _uiState.value = _uiState.value.copy(error = "Please select a team.")
            return
        }

        if (_uiState.value.selectedMemberIds.size < t.minRosterSize) {
            _uiState.value = _uiState.value.copy(error = "Minimum roster size is ${t.minRosterSize} players.")
            return
        }

        val captainId = _uiState.value.selectedCaptainId ?: run {
            _uiState.value = _uiState.value.copy(error = "Please select a captain from the roster.")
            return
        }

        val rosterPayload = _uiState.value.selectedMemberIds.map { memberId ->
            TournamentRosterMemberPayloadDto(
                userId = memberId,
                role = "player",
                isCaptain = memberId == captainId
            )
        }

        val payload = TournamentApplyPayloadDto(
            teamId = teamId,
            teamNote = _uiState.value.applicationTeamNote.ifBlank { null },
            roster = rosterPayload
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingApplication = true, error = null)
            when (val res = repository.applyTeam(t.id, payload)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingApplication = false,
                        showApplyModal = false,
                        actionSuccessMessage = "Team application submitted successfully!"
                    )
                    loadTournamentDetail(t.id)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingApplication = false,
                        error = res.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSubmittingApplication = false)
                }
            }
        }
    }

    fun openApplications() {
        executeLifecycleAction { repository.openApplications(it) }
    }

    fun closeApplications() {
        executeLifecycleAction { repository.closeApplications(it) }
    }

    fun finalizeTeams() {
        executeLifecycleAction { repository.finalizeTeams(it) }
    }

    fun generateMatches() {
        executeLifecycleAction { repository.generateMatches(it) }
    }

    fun publishTournament() {
        executeLifecycleAction { repository.publishTournament(it) }
    }

    fun deleteTournament(onSuccess: () -> Unit) {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.deleteTournament(tId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun reviewApplication(applicationId: String, status: String, reviewNote: String? = null) {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.reviewApplication(applicationId, status, reviewNote)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        actionSuccessMessage = "Application marked as $status."
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun attachScoringLink(matchId: String) {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.attachScoringLink(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        actionSuccessMessage = "Scoring link attached successfully!"
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun approveTournament(approvalNote: String = "") {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.approveTournament(tId, approvalNote)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        actionSuccessMessage = "Tournament approved and published!"
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun rejectTournament(rejectionNote: String = "") {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.rejectTournament(tId, rejectionNote)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        actionSuccessMessage = "Tournament rejected."
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun submitRequestEdit() {
        val tId = currentTournamentId ?: return
        val reason = _uiState.value.requestEditReason.trim()
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please describe the changes requested.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.requestTournamentEdit(tId, reason)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        showRequestEditModal = false,
                        actionSuccessMessage = "Edit request submitted to admin."
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun limitedUpdateTournament(body: JsonObject) {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = repository.limitedUpdateTournament(tId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        showLimitedEditModal = false,
                        actionSuccessMessage = "Tournament updated successfully!"
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, actionSuccessMessage = null)
    }

    private fun executeLifecycleAction(action: suspend (String) -> ApiResult<Unit>) {
        val tId = currentTournamentId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            when (val res = action(tId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        actionSuccessMessage = "Action completed successfully!"
                    )
                    loadTournamentDetail(tId)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
            }
        }
    }
}

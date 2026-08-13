package com.sportynix.app.presentation.cricket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.CricketScoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveScoringUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val liveState: LiveStateDto? = null,
    val scorecard: ScorecardDto? = null,
    val eligibleBatsmen: List<EligibleBatsmanDto> = emptyList(),
    val playingXI: PlayingXIResponseDto? = null,
    val momSuggestions: MOMSuggestionResponseDto? = null,
    val isSocketConnected: Boolean = false,
    val isFreeHit: Boolean = false,
    
    // Dialog / Sheet States
    val showTossDialog: Boolean = false,
    val showPlayingXISheet: Boolean = false,
    val showOpeningPlayersSheet: Boolean = false,
    val showBowlerSheet: Boolean = false,
    val showWicketSheet: Boolean = false,
    val showExtrasSheet: Boolean = false,
    val showOverSummaryModal: Boolean = false,
    val showInningsBreakModal: Boolean = false,
    val showMOMModal: Boolean = false,
    val showPenaltyDialog: Boolean = false,

    // Temp Selection State
    val selectedStrikerId: String? = null,
    val selectedNonStrikerId: String? = null,
    val selectedBowlerId: String? = null,
    val previousBowlerId: String? = null,
    val pendingWicketType: String? = null,
    val pendingDismissedId: String? = null,
    val pendingFielderId: String? = null,
    val lastOverSummary: String? = null
)

@HiltViewModel
class CricketScoringViewModel @Inject constructor(
    private val repository: CricketScoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveScoringUiState())
    val uiState: StateFlow<LiveScoringUiState> = _uiState.asStateFlow()

    private var currentMatchId: String? = null

    fun initializeScoring(matchId: String) {
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
                    _uiState.value = _uiState.value.copy(
                        liveState = socketState,
                        isFreeHit = socketState.isFreeHit
                    )
                }
            }
        }

        fetchFullState(matchId)
    }

    fun fetchFullState(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val res = repository.getLiveState(targetMatchId)) {
                is ApiResult.Success -> {
                    val state = res.data
                    _uiState.value = _uiState.value.copy(
                        liveState = state,
                        isFreeHit = state.isFreeHit
                    )
                    checkRequiredDialogs(state)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
                else -> {}
            }

            fetchEligibleBatsmen(targetMatchId)
            fetchPlayingXI(targetMatchId)
            fetchScorecard(targetMatchId)

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun checkRequiredDialogs(state: LiveStateDto) {
        if (state.status == "scheduled" || state.tossWinnerId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(showTossDialog = true)
            return
        }

        val activeInning = if (state.currentInnings == 2) state.inning2 else state.inning1
        if (activeInning != null) {
            if (activeInning.striker == null || activeInning.nonStriker == null) {
                _uiState.value = _uiState.value.copy(showOpeningPlayersSheet = true)
            } else if (activeInning.bowler == null) {
                _uiState.value = _uiState.value.copy(showBowlerSheet = true)
            }
        }
    }

    fun startMatch(tossWinnerId: String, tossDecision: String) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = StartMatchRequestDto(tossWinnerId, tossDecision)
            when (val res = repository.startMatch(matchId, req)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        liveState = res.data,
                        showTossDialog = false,
                        showOpeningPlayersSheet = true,
                        isSubmitting = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun setOpeningBatsmen(strikerId: String, nonStrikerId: String) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = SetBatsmenRequestDto(strikerId, nonStrikerId)
            when (val res = repository.setBatsmen(matchId, req)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        liveState = res.data,
                        showOpeningPlayersSheet = false,
                        showBowlerSheet = true,
                        isSubmitting = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun setBowler(bowlerId: String) {
        val matchId = currentMatchId ?: return
        if (bowlerId == _uiState.value.previousBowlerId) {
            _uiState.value = _uiState.value.copy(error = "Same bowler cannot bowl consecutive overs!")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = SetBowlerRequestDto(bowlerId)
            when (val res = repository.setBowler(matchId, req)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        liveState = res.data,
                        showBowlerSheet = false,
                        selectedBowlerId = bowlerId,
                        isSubmitting = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun recordBall(
        ballType: String,
        runs: Int,
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        wicketType: String? = null,
        dismissedBatsmanId: String? = null,
        fielderId: String? = null
    ) {
        val matchId = currentMatchId ?: return
        val currentBowler = _uiState.value.liveState?.let { if (it.currentInnings == 2) it.inning2?.bowler else it.inning1?.bowler }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = RecordBallRequestDto(
                ballType = ballType,
                runs = runs,
                extraRuns = extraRuns,
                isWicket = isWicket,
                wicketType = wicketType,
                dismissedBatsmanId = dismissedBatsmanId,
                fielderId = fielderId,
                bowlerId = currentBowler?.id
            )

            when (val res = repository.recordBall(matchId, req)) {
                is ApiResult.Success -> {
                    val updatedState = res.data
                    val activeInning = if (updatedState.currentInnings == 2) updatedState.inning2 else updatedState.inning1

                    val isOverCompleted = activeInning != null && activeInning.overs > 0 && activeInning.overs % 1.0 == 0.0

                    _uiState.value = _uiState.value.copy(
                        liveState = updatedState,
                        isFreeHit = updatedState.isFreeHit,
                        showWicketSheet = false,
                        showExtrasSheet = false,
                        isSubmitting = false
                    )

                    if (isWicket && (activeInning?.wickets ?: 0) < 10) {
                        fetchEligibleBatsmen(matchId)
                        _uiState.value = _uiState.value.copy(showOpeningPlayersSheet = true)
                    } else if (isOverCompleted) {
                        _uiState.value = _uiState.value.copy(
                            previousBowlerId = currentBowler?.id,
                            showOverSummaryModal = true,
                            showBowlerSheet = true
                        )
                    }

                    if (activeInning?.isCompleted == true) {
                        if (updatedState.currentInnings == 1) {
                            _uiState.value = _uiState.value.copy(showInningsBreakModal = true)
                        } else {
                            fetchMOMSuggestions(matchId)
                            _uiState.value = _uiState.value.copy(showMOMModal = true)
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun swapBatsmen() {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            when (val res = repository.swapBatsmen(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveState = res.data, isSubmitting = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun undoLastBall() {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            when (val res = repository.undoLastBall(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveState = res.data, isSubmitting = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun startSecondInnings(battingTeamId: String, bowlingTeamId: String) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = StartInningsRequestDto(battingTeamId, bowlingTeamId, 2)
            when (val res = repository.startInnings(matchId, req)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        liveState = res.data,
                        showInningsBreakModal = false,
                        showOpeningPlayersSheet = true,
                        isSubmitting = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    fun finalizeMOM(playerId: String, reason: String? = null) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = FinalizeMOMRequestDto(playerId, reason)
            when (val res = repository.finalizeMOM(matchId, req)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        liveState = res.data,
                        showMOMModal = false,
                        isSubmitting = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isSubmitting = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isSubmitting = false) }
            }
        }
    }

    private fun fetchEligibleBatsmen(matchId: String) {
        viewModelScope.launch {
            when (val res = repository.getEligibleBatsmen(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(eligibleBatsmen = res.data.eligibleBatsmen)
                }
                else -> {}
            }
        }
    }

    private fun fetchPlayingXI(matchId: String) {
        viewModelScope.launch {
            when (val res = repository.getPlayingXI(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(playingXI = res.data)
                }
                else -> {}
            }
        }
    }

    private fun fetchScorecard(matchId: String) {
        viewModelScope.launch {
            when (val res = repository.getScorecard(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(scorecard = res.data)
                }
                else -> {}
            }
        }
    }

    private fun fetchMOMSuggestions(matchId: String) {
        viewModelScope.launch {
            when (val res = repository.getMOMSuggestion(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(momSuggestions = res.data)
                }
                else -> {}
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleWicketSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showWicketSheet = show)
    }

    fun toggleExtrasSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExtrasSheet = show)
    }

    fun toggleOverSummaryModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOverSummaryModal = show)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectWebSocket()
    }
}

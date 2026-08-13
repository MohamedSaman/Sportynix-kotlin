package com.sportynix.app.presentation.cricket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.CricketScoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LiveViewTab {
    LIVE, SCORECARD, BALL_BY_BALL, STATS
}

sealed class LiveAnimationEvent {
    data class BoundaryFlash(val runs: Int, val isSix: Boolean) : LiveAnimationEvent()
    data class WicketAlert(val batsmanName: String, val wicketType: String) : LiveAnimationEvent()
}

data class CricketLiveViewUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: LiveViewTab = LiveViewTab.LIVE,
    val liveState: LiveStateDto? = null,
    val scorecard: ScorecardDto? = null,
    val ballByBall: List<BallByBallBallDto> = emptyList(),
    val matchSummary: MatchSummaryDto? = null,
    val isSocketConnected: Boolean = false,
    val activeBoundaryEvent: LiveAnimationEvent.BoundaryFlash? = null,
    val activeWicketEvent: LiveAnimationEvent.WicketAlert? = null
)

@HiltViewModel
class CricketLiveViewViewModel @Inject constructor(
    private val repository: CricketScoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CricketLiveViewUiState())
    val uiState: StateFlow<CricketLiveViewUiState> = _uiState.asStateFlow()

    private val _animationEvents = MutableSharedFlow<LiveAnimationEvent>(replay = 0)
    val animationEvents: SharedFlow<LiveAnimationEvent> = _animationEvents.asSharedFlow()

    private var currentMatchId: String? = null
    private var fallbackPollJob: Job? = null

    fun initializeLiveView(matchId: String) {
        currentMatchId = matchId
        repository.connectWebSocket(matchId)

        viewModelScope.launch {
            repository.isSocketConnected.collectLatest { connected ->
                _uiState.value = _uiState.value.copy(isSocketConnected = connected)
                if (!connected) {
                    startFallbackPolling(matchId)
                } else {
                    stopFallbackPolling()
                }
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

    fun selectTab(tab: LiveViewTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun fetchData(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val res = repository.getLiveState(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveState = res.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
                else -> {}
            }

            fetchScorecard(targetMatchId)
            fetchBallByBall(targetMatchId)

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

    fun fetchBallByBall(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            when (val res = repository.getBallByBall(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(ballByBall = res.data.balls)
                }
                else -> {}
            }
        }
    }

    private fun startFallbackPolling(matchId: String) {
        fallbackPollJob?.cancel()
        fallbackPollJob = viewModelScope.launch {
            while (true) {
                delay(30000) // 30-second fallback polling ONLY when WebSocket is disconnected
                if (!_uiState.value.isSocketConnected) {
                    fetchData(matchId)
                }
            }
        }
    }

    private fun stopFallbackPolling() {
        fallbackPollJob?.cancel()
        fallbackPollJob = null
    }

    fun clearAnimationEvents() {
        _uiState.value = _uiState.value.copy(
            activeBoundaryEvent = null,
            activeWicketEvent = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopFallbackPolling()
        repository.disconnectWebSocket()
    }
}

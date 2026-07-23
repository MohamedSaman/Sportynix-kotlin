package com.sportynix.app.presentation.cricket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.CricketMatchDto
import com.sportynix.app.data.remote.dto.LiveScorecardDto
import com.sportynix.app.data.repository.CricketScoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CricketUiState(
    val isLoading: Boolean = false,
    val matches: List<CricketMatchDto> = emptyList(),
    val liveScorecard: LiveScorecardDto? = null,
    val error: String? = null
)

@HiltViewModel
class CricketScoringViewModel @Inject constructor(
    private val repository: CricketScoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CricketUiState())
    val uiState: StateFlow<CricketUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = repository.getCricketMatches()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, matches = res.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun startLiveScoring(matchId: String) {
        viewModelScope.launch {
            repository.connectLiveMatchWebSocket(matchId)
            when (val res = repository.getLiveMatchDetails(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveScorecard = res.data)
                }
                else -> {}
            }
        }
    }

    fun recordBall(matchId: String, runs: Int, isWicket: Boolean = false, isWide: Boolean = false, isNoBall: Boolean = false) {
        viewModelScope.launch {
            val isBoundary = runs == 4
            val isSix = runs == 6
            when (val res = repository.recordBall(matchId, runs, isWicket, isWide, isNoBall, isBoundary, isSix)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveScorecard = res.data.updatedScore)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
                else -> {}
            }
        }
    }

    fun undoLastBall(matchId: String) {
        viewModelScope.launch {
            when (val res = repository.undoLastBall(matchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveScorecard = res.data.updatedScore)
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectLiveMatchWebSocket()
    }
}

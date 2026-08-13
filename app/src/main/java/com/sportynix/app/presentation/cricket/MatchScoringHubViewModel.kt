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

data class MatchScoringHubUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val liveState: LiveStateDto? = null,
    val isSocketConnected: Boolean = false
)

@HiltViewModel
class MatchScoringHubViewModel @Inject constructor(
    private val repository: CricketScoringRepository,
    private val leagueApiService: LeagueApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchScoringHubUiState())
    val uiState: StateFlow<MatchScoringHubUiState> = _uiState.asStateFlow()

    private var currentMatchId: String? = null

    fun initializeHub(matchId: String) {
        currentMatchId = matchId
        repository.connectWebSocket(matchId)

        viewModelScope.launch {
            repository.isSocketConnected.collectLatest { connected ->
                _uiState.value = _uiState.value.copy(isSocketConnected = connected)
            }
        }

        viewModelScope.launch {
            repository.liveStateSocket.collectLatest { state ->
                if (state != null) {
                    _uiState.value = _uiState.value.copy(liveState = state)
                }
            }
        }

        fetchData(matchId)
    }

    fun fetchData(matchId: String? = null) {
        val targetMatchId = matchId ?: currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = repository.getLiveState(targetMatchId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(liveState = res.data, isLoading = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message, isLoading = false)
                }
                else -> { _uiState.value = _uiState.value.copy(isLoading = false) }
            }
        }
    }

    fun pauseMatch() {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            try {
                leagueApiService.pauseMatch(matchId)
                fetchData(matchId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isSubmitting = false)
            }
        }
    }

    fun resumeMatch() {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            try {
                leagueApiService.resumeMatch(matchId)
                fetchData(matchId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isSubmitting = false)
            }
        }
    }

    fun endMatch(result: String? = null) {
        val matchId = currentMatchId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val req = EndMatchRequestDto(result = result)
            when (val res = repository.endMatch(matchId, req)) {
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

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectWebSocket()
    }
}

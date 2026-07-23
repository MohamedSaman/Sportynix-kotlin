package com.sportynix.app.presentation.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.TournamentDto
import com.sportynix.app.data.remote.dto.TournamentMatchDto
import com.sportynix.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TournamentUiState(
    val isLoading: Boolean = false,
    val tournaments: List<TournamentDto> = emptyList(),
    val selectedTournament: TournamentDto? = null,
    val matches: List<TournamentMatchDto> = emptyList(),
    val registrationSuccessMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val repository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentUiState())
    val uiState: StateFlow<TournamentUiState> = _uiState.asStateFlow()

    init {
        loadTournaments()
    }

    fun loadTournaments(search: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getTournaments(search)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tournaments = result.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun loadTournamentDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val detailRes = repository.getTournamentDetail(id)
            val matchesRes = repository.getTournamentMatches(id)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedTournament = (detailRes as? ApiResult.Success)?.data,
                matches = (matchesRes as? ApiResult.Success)?.data ?: emptyList()
            )
        }
    }

    fun registerTeam(tournamentId: String, teamName: String, captainPhone: String) {
        viewModelScope.launch {
            when (val result = repository.registerForTournament(tournamentId, teamName, captainPhone)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        registrationSuccessMessage = result.data.message
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }
}

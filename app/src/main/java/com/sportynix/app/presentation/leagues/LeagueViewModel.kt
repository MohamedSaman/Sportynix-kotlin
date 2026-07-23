package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FixtureDto
import com.sportynix.app.data.remote.dto.LeagueDto
import com.sportynix.app.data.remote.dto.StandingDto
import com.sportynix.app.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeagueUiState(
    val isLoading: Boolean = false,
    val leagues: List<LeagueDto> = emptyList(),
    val selectedLeague: LeagueDto? = null,
    val fixtures: List<FixtureDto> = emptyList(),
    val standings: List<StandingDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val repository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    init {
        loadLeagues()
    }

    fun loadLeagues(search: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getLeagues(search)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        leagues = result.data
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

    fun loadLeagueDetails(leagueId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val leagueResult = repository.getLeagueDetail(leagueId)
            val fixturesResult = repository.getLeagueFixtures(leagueId)
            val standingsResult = repository.getLeagueStandings(leagueId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedLeague = (leagueResult as? ApiResult.Success)?.data,
                fixtures = (fixturesResult as? ApiResult.Success)?.data ?: emptyList(),
                standings = (standingsResult as? ApiResult.Success)?.data ?: emptyList()
            )
        }
    }
}

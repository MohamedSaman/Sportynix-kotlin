package com.sportynix.app.presentation.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.FullTournamentDto
import com.sportynix.app.data.remote.dto.FullTournamentMatchDto
import com.sportynix.app.data.repository.TournamentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TournamentUiState(
    val isLoading: Boolean = false,
    val tournaments: List<FullTournamentDto> = emptyList(),
    val selectedTournament: FullTournamentDto? = null,
    val matches: List<FullTournamentMatchDto> = emptyList(),
    val searchQuery: String = "",
    val selectedSportFilter: String = "All",
    val selectedStatusFilter: String = "All",
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

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadTournaments(query)
    }

    fun onSportFilterChanged(sport: String) {
        _uiState.value = _uiState.value.copy(selectedSportFilter = sport)
        val sportParam = if (sport == "All") null else sport.lowercase()
        loadTournaments(_uiState.value.searchQuery, sportParam)
    }

    fun onStatusFilterChanged(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        val statusParam = if (status == "All") null else status.lowercase()
        loadTournaments(_uiState.value.searchQuery, status = statusParam)
    }

    fun loadTournaments(
        search: String? = null,
        sportType: String? = null,
        status: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getTournaments(
                search = search?.ifBlank { null },
                sportType = sportType?.ifBlank { null },
                status = status?.ifBlank { null }
            )) {
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
}

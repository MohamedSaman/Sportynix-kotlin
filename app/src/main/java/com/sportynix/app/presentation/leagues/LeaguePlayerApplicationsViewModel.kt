package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.LeaguePlayerApplicationDto
import com.sportynix.app.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApplicationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val applications: List<LeaguePlayerApplicationDto> = emptyList(),
    val filteredApplications: List<LeaguePlayerApplicationDto> = emptyList(),
    val selectedFilter: String = "pending", // all, pending, approved, rejected
    val selectedAppIds: Set<String> = emptySet(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class LeaguePlayerApplicationsViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationsUiState())
    val uiState: StateFlow<ApplicationsUiState> = _uiState.asStateFlow()

    private var currentLeagueId: String? = null

    fun loadApplications(leagueId: String, isRefresh: Boolean = false) {
        currentLeagueId = leagueId
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            when (val res = leagueRepository.getLeaguePlayerApplications(leagueId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        applications = res.data,
                        selectedAppIds = emptySet()
                    )
                    applyFilter()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = res.message ?: "Failed to load player applications"
                    )
                }
                else -> {}
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val list = if (state.selectedFilter == "all") {
            state.applications
        } else {
            state.applications.filter { it.status.lowercase() == state.selectedFilter.lowercase() }
        }
        _uiState.value = state.copy(filteredApplications = list)
    }

    fun toggleAppSelection(appId: String) {
        val current = _uiState.value.selectedAppIds.toMutableSet()
        if (current.contains(appId)) {
            current.remove(appId)
        } else {
            current.add(appId)
        }
        _uiState.value = _uiState.value.copy(selectedAppIds = current)
    }

    fun selectAllFiltered() {
        val allIds = _uiState.value.filteredApplications.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedAppIds = allIds)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedAppIds = emptySet())
    }

    fun reviewSingle(appId: String, status: String, note: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.reviewApplication(appId, status, note)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Application marked as $status")
                    currentLeagueId?.let { loadApplications(it) }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun bulkReview(status: String, note: String? = null) {
        val selected = _uiState.value.selectedAppIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.bulkReviewApplications(selected, status, note)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "${selected.size} applications marked as $status"
                    )
                    currentLeagueId?.let { loadApplications(it) }
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

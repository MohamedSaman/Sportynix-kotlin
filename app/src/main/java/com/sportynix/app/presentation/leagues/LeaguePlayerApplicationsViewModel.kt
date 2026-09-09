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
    val searchQuery: String = "",
    val selectedAppIds: Set<String> = emptySet(),
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
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
                    val apps = res.data
                    val total = apps.size
                    val pending = apps.count { it.status.equals("pending", ignoreCase = true) }
                    val approved = apps.count { it.status.equals("approved", ignoreCase = true) }
                    val rejected = apps.count { it.status.equals("rejected", ignoreCase = true) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        applications = apps,
                        totalCount = total,
                        pendingCount = pending,
                        approvedCount = approved,
                        rejectedCount = rejected,
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

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        var list = if (state.selectedFilter == "all") {
            state.applications
        } else {
            state.applications.filter { it.status.equals(state.selectedFilter, ignoreCase = true) }
        }

        if (query.isNotEmpty()) {
            list = list.filter { app ->
                val name = (app.user.fullName ?: app.user.name ?: "").lowercase()
                val username = (app.user.username ?: "").lowercase()
                val role = (app.cricketPrimaryRole ?: "").lowercase()
                val variant = (app.cricketPreferredVariant ?: "").lowercase()
                val pos = (app.cricketPlayingPosition ?: "").lowercase()
                val note = (app.applicationNote ?: "").lowercase()

                name.contains(query) || username.contains(query) || role.contains(query) ||
                        variant.contains(query) || pos.contains(query) || note.contains(query)
            }
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

    fun selectAllPending() {
        val pendingIds = _uiState.value.filteredApplications
            .filter { it.status.equals("pending", ignoreCase = true) }
            .map { it.id }
            .toSet()
        _uiState.value = _uiState.value.copy(selectedAppIds = pendingIds)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedAppIds = emptySet())
    }

    fun reviewSingle(appId: String, status: String, note: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val res = leagueRepository.reviewApplication(appId, status, note)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(successMessage = "Application ${status.lowercase()}")
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
                        successMessage = "${selected.size} applications ${status.lowercase()}"
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

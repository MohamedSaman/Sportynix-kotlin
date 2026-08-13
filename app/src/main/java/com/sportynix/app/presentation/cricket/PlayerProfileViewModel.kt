package com.sportynix.app.presentation.cricket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.PlayerMatchStatDto
import com.sportynix.app.data.repository.CareerStats
import com.sportynix.app.data.repository.PlayerStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerProfileUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val playerId: String = "",
    val playerName: String = "Player Profile",
    val playerRole: String? = null,
    val battingStyle: String? = null,
    val bowlingStyle: String? = null,
    val profileImage: String? = null,

    // Filters
    val cricketVariant: String = "all", // "all", "softball", "hardball"
    val contextFilter: String = "all",  // "all", "league", "tournament"
    val venueCategory: String = "all",  // "all", "indoor", "outdoor"

    // Pagination State
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val recentMatchStats: List<PlayerMatchStatDto> = emptyList(),
    val careerStats: CareerStats = CareerStats()
)

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val repository: PlayerStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerProfileUiState())
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    fun initializePlayer(
        playerId: String,
        name: String? = null,
        role: String? = null,
        batting: String? = null,
        bowling: String? = null,
        image: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            playerId = playerId,
            playerName = name ?: "Player Profile",
            playerRole = role,
            battingStyle = batting,
            bowlingStyle = bowling,
            profileImage = image
        )
        fetchPlayerStats(resetPage = true)
    }

    fun setCricketVariantFilter(variant: String) {
        if (_uiState.value.cricketVariant == variant) return
        _uiState.value = _uiState.value.copy(cricketVariant = variant)
        fetchPlayerStats(resetPage = true)
    }

    fun setContextFilter(context: String) {
        if (_uiState.value.contextFilter == context) return
        _uiState.value = _uiState.value.copy(contextFilter = context)
        fetchPlayerStats(resetPage = true)
    }

    fun setVenueCategoryFilter(venueCategory: String) {
        if (_uiState.value.venueCategory == venueCategory) return
        _uiState.value = _uiState.value.copy(venueCategory = venueCategory)
        fetchPlayerStats(resetPage = true)
    }

    fun fetchPlayerStats(resetPage: Boolean = false) {
        val playerId = _uiState.value.playerId
        if (playerId.isBlank()) return

        val page = if (resetPage) 1 else _uiState.value.currentPage + 1

        viewModelScope.launch {
            if (resetPage) {
                _uiState.value = _uiState.value.copy(isLoading = true, currentPage = 1)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            when (val res = repository.getPlayerMatchStatsPage(
                playerId = playerId,
                cricketVariant = _uiState.value.cricketVariant,
                context = _uiState.value.contextFilter,
                venueCategory = _uiState.value.venueCategory,
                page = page,
                pageSize = 10
            )) {
                is ApiResult.Success -> {
                    val pageData = res.data
                    val newResults = if (resetPage) pageData.results else _uiState.value.recentMatchStats + pageData.results
                    val hasMore = !pageData.next.isNullOrBlank()

                    val career = repository.calculateCareerStats(newResults)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        recentMatchStats = newResults,
                        careerStats = career,
                        currentPage = page,
                        hasMore = hasMore
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = res.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

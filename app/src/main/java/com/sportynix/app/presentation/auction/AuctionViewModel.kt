package com.sportynix.app.presentation.auction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.AuctionDto
import com.sportynix.app.data.remote.dto.AuctionTeamDto
import com.sportynix.app.data.repository.AuctionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionUiState(
    val isLoading: Boolean = false,
    val auctions: List<AuctionDto> = emptyList(),
    val activeAuction: AuctionDto? = null,
    val teams: List<AuctionTeamDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AuctionViewModel @Inject constructor(
    private val repository: AuctionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionUiState())
    val uiState: StateFlow<AuctionUiState> = _uiState.asStateFlow()

    init {
        loadAuctions()
    }

    fun loadAuctions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = repository.getAuctions()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, auctions = res.data)
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

    fun enterAuctionRoom(auctionId: String) {
        viewModelScope.launch {
            repository.connectAuctionWebSocket(auctionId)
            val detailRes = repository.getAuctionDetail(auctionId)
            val teamsRes = repository.getAuctionTeams(auctionId)

            _uiState.value = _uiState.value.copy(
                activeAuction = (detailRes as? ApiResult.Success)?.data,
                teams = (teamsRes as? ApiResult.Success)?.data ?: emptyList()
            )
        }
    }

    fun placeBid(auctionId: String, teamId: String, amount: Double) {
        viewModelScope.launch {
            when (val res = repository.placeBid(auctionId, teamId, amount)) {
                is ApiResult.Success -> {
                    _uiState.value.activeAuction?.let { current ->
                        _uiState.value = _uiState.value.copy(
                            activeAuction = current.copy(
                                currentBidAmount = res.data.newHighestBid,
                                currentHighestBidder = res.data.highestBidderName
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectAuctionWebSocket()
    }
}

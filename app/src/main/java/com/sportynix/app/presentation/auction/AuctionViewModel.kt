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

    fun enterAuctionRoom(auctionId: String) {
        viewModelScope.launch {
            repository.connectWebSocket(auctionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectWebSocket()
    }
}

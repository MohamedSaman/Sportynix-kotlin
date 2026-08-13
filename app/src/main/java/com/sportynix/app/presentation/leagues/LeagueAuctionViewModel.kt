package com.sportynix.app.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.AuctionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuctionUiState(
    val isLoading: Boolean = false,
    val canControl: Boolean = false,
    val snapshot: AuctionSessionSnapshotDto? = null,
    val isWsConnected: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Dialog & Action States
    val selectedTab: Int = 0, // 0: Live Room, 1: Team Wallets, 2: Players Pool, 3: Commentary
    val nominatedPlayer: AuctionPlayerDto? = null,
    val showNominateDialog: Boolean = false,
    val openingBidInput: String = "",

    val showBidDialog: Boolean = false,
    val selectedTeamWalletId: String = "",
    val customBidAmountInput: String = "",

    val showPlayerStatsModal: Boolean = false,
    val statsPlayer: AuctionPlayerDto? = null
)

@HiltViewModel
class LeagueAuctionViewModel @Inject constructor(
    private val auctionRepository: AuctionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuctionUiState())
    val uiState: StateFlow<AuctionUiState> = _uiState.asStateFlow()

    private var currentLeagueId: String? = null
    private var currentSessionId: String? = null

    init {
        observeAuctionSnapshot()
        observeWsConnection()
    }

    private fun observeAuctionSnapshot() {
        viewModelScope.launch {
            auctionRepository.auctionSnapshot.collect { snapshot ->
                if (snapshot != null) {
                    currentSessionId = snapshot.id
                    _uiState.value = _uiState.value.copy(snapshot = snapshot, isLoading = false)
                }
            }
        }
    }

    private fun observeWsConnection() {
        viewModelScope.launch {
            auctionRepository.isWsConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isWsConnected = connected)
            }
        }
    }

    fun initAuction(leagueId: String) {
        currentLeagueId = leagueId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Check permissions
            val accessRes = auctionRepository.getAuctionControlAccess(leagueId)
            val canControl = (accessRes as? ApiResult.Success)?.data ?: false
            _uiState.value = _uiState.value.copy(canControl = canControl)

            // Fetch session snapshot
            when (val res = auctionRepository.getAuctionByLeague(leagueId)) {
                is ApiResult.Success -> {
                    val snapshot = res.data
                    if (snapshot != null) {
                        currentSessionId = snapshot.id
                        _uiState.value = _uiState.value.copy(isLoading = false, snapshot = snapshot)
                        auctionRepository.connectWebSocket(snapshot.id)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, snapshot = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun createAuctionSetup(startingPoints: Double, minOpeningBid: Double, increment: Double) {
        val leagueId = currentLeagueId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val payload = AuctionUpsertPayloadDto(
                leagueId = leagueId,
                startingPoints = startingPoints,
                minimumOpeningBid = minOpeningBid,
                bidIncrement = increment
            )

            when (val res = auctionRepository.upsertAuctionSession(payload)) {
                is ApiResult.Success -> {
                    currentSessionId = res.data.id
                    _uiState.value = _uiState.value.copy(isLoading = false, snapshot = res.data)
                    auctionRepository.connectWebSocket(res.data.id)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                }
                else -> {}
            }
        }
    }

    fun startAuction() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.startAuction(sId)
        }
    }

    fun pauseAuction() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.pauseAuction(sId)
        }
    }

    fun resumeAuction() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.resumeAuction(sId)
        }
    }

    fun closeAuction() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.closeAuction(sId)
        }
    }

    fun openNominateDialog(player: AuctionPlayerDto) {
        _uiState.value = _uiState.value.copy(
            showNominateDialog = true,
            nominatedPlayer = player,
            openingBidInput = _uiState.value.snapshot?.minimumOpeningBid?.toString() ?: "100"
        )
    }

    fun closeNominateDialog() {
        _uiState.value = _uiState.value.copy(showNominateDialog = false, nominatedPlayer = null)
    }

    fun nominatePlayer(openingBid: Double) {
        val sId = currentSessionId ?: return
        val player = _uiState.value.nominatedPlayer ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showNominateDialog = false)
            auctionRepository.nominatePlayer(sId, player.id, openingBid)
        }
    }

    fun recordBid(walletId: String, amount: Double) {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.recordBid(sId, walletId, amount)
        }
    }

    fun markSold() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.markPlayerSold(sId)
        }
    }

    fun markUnsold() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.markPlayerUnsold(sId)
        }
    }

    fun postCommentary(message: String) {
        val sId = currentSessionId ?: return
        if (message.isBlank()) return
        viewModelScope.launch {
            auctionRepository.addCommentary(sId, message)
        }
    }

    fun undoAction() {
        val sId = currentSessionId ?: return
        viewModelScope.launch {
            auctionRepository.undoAction(sId)
        }
    }

    fun openPlayerStats(player: AuctionPlayerDto) {
        _uiState.value = _uiState.value.copy(showPlayerStatsModal = true, statsPlayer = player)
    }

    fun closePlayerStats() {
        _uiState.value = _uiState.value.copy(showPlayerStatsModal = false, statsPlayer = null)
    }

    override fun onCleared() {
        super.onCleared()
        auctionRepository.disconnectWebSocket()
    }
}

package com.sportynix.app.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.domain.model.*
import com.sportynix.app.domain.repository.ChatRepository
import com.sportynix.app.domain.repository.BookingRepository
import com.sportynix.app.data.remote.api.TeamApiService
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ChatInfoUiState(
    val chatId: Long = 0,
    val chat: Chat? = null,
    val members: List<ChatMember> = emptyList(),
    val photosPreview: List<ChatMessage> = emptyList(),
    val eventsPreview: List<ChatMessage> = emptyList(),
    val currentUserId: Long? = null,
    val historyPage: Int = 1,
    val historyTotal: Int = 0,
    val isLoadingMore: Boolean = false,
    val qrCode: String? = null,
    val qrGame: PastGame? = null,
    val isQrLoading: Boolean = false,
    val historyGamesTab: String = "upcoming", // "upcoming", "cancelled", "other"
    val showReportModal: Boolean = false,
    val reportReason: String = "",
    val reportNotes: String = "",
    val isActionBusy: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val bookingRepository: BookingRepository,
    private val teamApi: TeamApiService,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: Long = savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ChatInfoUiState(chatId = chatId))
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    init {
        if (chatId > 0) {
            loadInfo()
        }
    }

    fun loadInfo() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val details = chatRepository.getChatDetails(chatId, 1)
            if (details.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = details.exceptionOrNull()?.message ?: "Failed to load chat info") }
                return@launch
            }
            coroutineScope {
                val members = async { chatRepository.getChatMembers(chatId).getOrDefault(emptyList()) }
                val photos = async { chatRepository.getPhotoMessages(chatId, 6).getOrDefault(emptyList()) }
                val events = async { chatRepository.getEventMessages(chatId, 6).getOrDefault(emptyList()) }
                val userId = async { sessionManager.userId.first()?.toLongOrNull() }
                val chat = details.getOrNull()
                _uiState.update { it.copy(chat = chat, members = members.await(), photosPreview = photos.await(), eventsPreview = events.await(), currentUserId = userId.await(), historyPage = 1, historyTotal = chat?.challengeInfo?.pastGames?.total ?: 0, isLoading = false) }
            }
        }
    }

    fun loadMoreGames() {
        val state = _uiState.value
        val loaded = state.chat?.challengeInfo?.pastGames?.results.orEmpty()
        if (state.isLoadingMore || loaded.size >= state.historyTotal) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.historyPage + 1
            chatRepository.getChatDetails(chatId, nextPage).onSuccess { next ->
                val incoming = next.challengeInfo?.pastGames?.results.orEmpty()
                val merged = (loaded + incoming).distinctBy { it.id }
                val updatedChallenge = state.chat?.challengeInfo?.copy(
                    pastGames = state.chat.challengeInfo.pastGames?.copy(page = nextPage, total = next.challengeInfo?.pastGames?.total ?: state.historyTotal, results = merged)
                )
                _uiState.update { it.copy(chat = it.chat?.copy(challengeInfo = updatedChallenge), historyPage = nextPage, historyTotal = next.challengeInfo?.pastGames?.total ?: it.historyTotal) }
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Unable to load more games") } }
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    fun setHistoryTab(tab: String) {
        _uiState.update { it.copy(historyGamesTab = tab) }
    }

    fun toggleBlockUser(otherUserId: Long, isBlocked: Boolean) {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (isBlocked) {
                chatRepository.unblockUser(otherUserId)
            } else {
                chatRepository.blockUser(otherUserId)
            }
            result.onSuccess {
                _uiState.update { it.copy(successMessage = if (isBlocked) "User unblocked" else "User blocked") }
                loadInfo()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to update block status") }
            }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun toggleFollow() {
        val chat = _uiState.value.chat ?: return
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (chat.isFollowing == true) chatRepository.unfollowChannel(chatId) else chatRepository.followChannel(chatId)
            result.onSuccess { loadInfo() }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun toggleAdminOnly() {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.toggleAdminOnly(chatId).onSuccess { enabled ->
                _uiState.update { state -> state.copy(chat = state.chat?.copy(adminOnly = enabled)) }
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun setMemberAdmin(member: ChatMember, makeAdmin: Boolean) {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (makeAdmin) chatRepository.addAdmin(chatId, member.id) else chatRepository.removeAdmin(chatId, member.id)
            result.onSuccess {
                _uiState.update { it.copy(successMessage = if (makeAdmin) "${member.fullName} is now an admin" else "Admin access removed") }
                loadInfo()
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun openDirectChat(userId: Long, onOpened: (Long) -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isActionBusy || userId == _uiState.value.currentUserId) return@launch
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.getDirectChat(userId).onSuccess { onOpened(it.id) }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun leaveChat(onComplete: () -> Unit) {
        val chat = _uiState.value.chat ?: return
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = when (chat.chatType) {
                "team_channel" -> chatRepository.unfollowChannel(chatId).map { Unit }
                else -> runCatching {
                    val teamId = chat.team?.id ?: error("Team information is unavailable")
                    val response = teamApi.leave(teamId.toInt())
                    if (!response.isSuccessful) error("Unable to leave: HTTP ${response.code()}")
                }
            }
            result.onSuccess { onComplete() }.onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Unable to leave chat") } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun openQr(game: PastGame) {
        val bookingId = game.booking?.id ?: return
        if (_uiState.value.isQrLoading) return
        _uiState.update { it.copy(qrGame = game, qrCode = null, isQrLoading = true) }
        viewModelScope.launch {
            when (val result = bookingRepository.fetchBookingQRCode(bookingId.toInt())) {
                is ApiResult.Success -> _uiState.update { it.copy(qrCode = result.data, isQrLoading = false) }
                else -> _uiState.update { it.copy(qrGame = null, isQrLoading = false, errorMessage = "Unable to load booking QR code") }
            }
        }
    }

    fun closeQr() = _uiState.update { it.copy(qrGame = null, qrCode = null, isQrLoading = false) }

    fun showReportModal(show: Boolean) {
        _uiState.update { it.copy(showReportModal = show) }
    }

    fun submitReport(reportedUserId: Long, reason: String, notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.reportUser(reportedUserId, reason, notes, chatId).onSuccess {
                _uiState.update { it.copy(showReportModal = false, isActionBusy = false, successMessage = "Report submitted") }
            }.onFailure { error -> _uiState.update { it.copy(isActionBusy = false, errorMessage = error.message ?: "Unable to submit report") } }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
    fun dismissSuccess() = _uiState.update { it.copy(successMessage = null) }

    fun clearChatForMe(onComplete: () -> Unit) {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.clearChatForMe(chatId).onSuccess {
                onComplete()
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Unable to clear chat") } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun deleteChatForEveryone(onComplete: () -> Unit) {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.deleteChatForEveryone(chatId).onSuccess {
                onComplete()
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Unable to delete chat") } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun hideChatForMe(onComplete: () -> Unit) {
        if (_uiState.value.isActionBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.hideChatForMe(chatId).onSuccess { onComplete() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }
}

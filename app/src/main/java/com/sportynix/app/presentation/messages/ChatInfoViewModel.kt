package com.sportynix.app.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.domain.model.*
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatInfoUiState(
    val chatId: Long = 0,
    val chat: Chat? = null,
    val members: List<ChatMember> = emptyList(),
    val photosPreview: List<ChatMessage> = emptyList(),
    val eventsPreview: List<ChatMessage> = emptyList(),
    val historyGamesTab: String = "upcoming", // "upcoming", "cancelled", "other"
    val showReportModal: Boolean = false,
    val reportReason: String = "",
    val reportNotes: String = "",
    val isActionBusy: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
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
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.getChatDetails(chatId).onSuccess { chat ->
                _uiState.update { it.copy(chat = chat) }
            }
            chatRepository.getChatMembers(chatId).onSuccess { members ->
                _uiState.update { it.copy(members = members) }
            }
            chatRepository.getPhotoMessages(chatId, 6).onSuccess { photos ->
                _uiState.update { it.copy(photosPreview = photos) }
            }
            chatRepository.getEventMessages(chatId, 6).onSuccess { events ->
                _uiState.update { it.copy(eventsPreview = events, isLoading = false) }
            }
        }
    }

    fun setHistoryTab(tab: String) {
        _uiState.update { it.copy(historyGamesTab = tab) }
    }

    fun toggleBlockUser(otherUserId: Long, isBlocked: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (isBlocked) {
                chatRepository.unblockUser(otherUserId)
            } else {
                chatRepository.blockUser(otherUserId)
            }
            result.onSuccess { loadInfo() }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to update block status") }
            }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun toggleFollow() {
        val chat = _uiState.value.chat ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (chat.isFollowing == true) chatRepository.unfollowChannel(chatId) else chatRepository.followChannel(chatId)
            result.onSuccess { loadInfo() }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun toggleAdminOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.toggleAdminOnly(chatId).onSuccess { enabled ->
                _uiState.update { state -> state.copy(chat = state.chat?.copy(adminOnly = enabled)) }
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun setMemberAdmin(member: ChatMember, makeAdmin: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            val result = if (makeAdmin) chatRepository.addAdmin(chatId, member.id) else chatRepository.removeAdmin(chatId, member.id)
            result.onSuccess { loadInfo() }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun openDirectChat(userId: Long, onOpened: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.getDirectChat(userId).onSuccess { onOpened(it.id) }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isActionBusy = false) }
        }
    }

    fun showReportModal(show: Boolean) {
        _uiState.update { it.copy(showReportModal = show) }
    }

    fun submitReport(reportedUserId: Long, reason: String, notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true) }
            chatRepository.reportUser(reportedUserId, reason, notes, chatId).onSuccess {
                _uiState.update { it.copy(showReportModal = false, isActionBusy = false) }
            }
        }
    }

    fun clearChatForMe(onComplete: () -> Unit) {
        viewModelScope.launch {
            chatRepository.clearChatForMe(chatId).onSuccess {
                onComplete()
            }
        }
    }

    fun deleteChatForEveryone(onComplete: () -> Unit) {
        viewModelScope.launch {
            chatRepository.deleteChatForEveryone(chatId).onSuccess {
                onComplete()
            }
        }
    }

    fun hideChatForMe(onComplete: () -> Unit) {
        viewModelScope.launch {
            chatRepository.hideChatForMe(chatId).onSuccess { onComplete() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}

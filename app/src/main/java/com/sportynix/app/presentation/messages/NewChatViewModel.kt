package com.sportynix.app.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.domain.model.*
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

data class NewChatUiState(
    val query: String = "",
    val mutualUsers: List<MutualUser> = emptyList(),
    val recentUsers: List<MutualUser> = emptyList(),
    val searchResults: List<UserSearchResult> = emptyList(),
    val chatRequestsReceived: List<ChatRequestItem> = emptyList(),
    val chatRequestsSent: List<ChatRequestItem> = emptyList(),
    val busyUserId: Long? = null,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            if (_uiState.value.isInitialLoading && _uiState.value.mutualUsers.isNotEmpty()) return@launch
            _uiState.update { it.copy(isInitialLoading = true, errorMessage = null) }
            val mutualTask = async { chatRepository.getMutualUsers() }
            val chatsTask = async { chatRepository.fetchMyChats() }
            val requestsTask = async { chatRepository.getChatRequests() }
            val mutualRes = mutualTask.await()
            val chatsRes = chatsTask.await()
            val reqsRes = requestsTask.await()
            val recent = chatsRes.getOrDefault(emptyList()).filter { it.chatType in setOf("direct", "rivalry", "challenge") && it.otherUserId != null }.mapNotNull { chat ->
                val id = chat.otherUserId ?: return@mapNotNull null
                MutualUser(id, "", chat.otherUserName ?: chat.displayName ?: "User", chat.otherUserAvatar)
            }.distinctBy { it.id }

            _uiState.update {
                it.copy(
                    mutualUsers = mutualRes.getOrDefault(emptyList()),
                    recentUsers = recent,
                    chatRequestsReceived = reqsRes.getOrNull()?.received ?: emptyList(),
                    chatRequestsSent = reqsRes.getOrNull()?.sent ?: emptyList(),
                    isInitialLoading = false,
                    errorMessage = listOf(mutualRes, chatsRes, reqsRes).firstOrNull { it.isFailure }?.exceptionOrNull()?.message
                )
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        searchJob?.cancel()

        val trimmed = newQuery.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            if (_uiState.value.query.trim() != trimmed) return@launch
            if (trimmed.length < 2) { _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }; return@launch }
            _uiState.update { it.copy(isLoading = true) }

            val searchRes = chatRepository.searchUsers(trimmed)
            val mutualRes = chatRepository.getMutualUsers(trimmed)

            if (_uiState.value.query.trim() != trimmed) return@launch
            _uiState.update {
                it.copy(
                    searchResults = searchRes.getOrDefault(emptyList()),
                    mutualUsers = mutualRes.getOrDefault(it.mutualUsers),
                    isLoading = false
                )
            }
        }
    }

    fun openDirectChat(userId: Long, onChatOpened: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyUserId = userId) }
            chatRepository.getDirectChat(userId).onSuccess { chat ->
                _uiState.update { it.copy(busyUserId = null) }
                onChatOpened(chat.id)
            }.onFailure { err ->
                _uiState.update { it.copy(busyUserId = null, errorMessage = err.message) }
            }
        }
    }

    fun sendChatRequest(userId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyUserId = userId) }
            chatRepository.sendChatRequest(userId).onSuccess {
                loadInitialData()
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
            _uiState.update { it.copy(busyUserId = null) }
        }
    }

    fun acceptChatRequest(request: ChatRequestItem, onChatOpened: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyUserId = request.fromUser.id) }
            chatRepository.acceptChatRequest(request.id).onSuccess { (_, chat) ->
                loadInitialData()
                onChatOpened(chat.id)
            }
            _uiState.update { it.copy(busyUserId = null) }
        }
    }

    fun declineChatRequest(request: ChatRequestItem) {
        viewModelScope.launch {
            chatRepository.declineChatRequest(request.id).onSuccess {
                loadInitialData()
            }
        }
    }

    fun cancelChatRequest(request: ChatRequestItem) {
        viewModelScope.launch {
            chatRepository.cancelChatRequest(request.id).onSuccess {
                loadInitialData()
            }
        }
    }

    fun cancelSentRequestFor(userId: Long) {
        _uiState.value.chatRequestsSent.firstOrNull { it.toUser.id == userId && it.status == "pending" }?.let(::cancelChatRequest)
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}

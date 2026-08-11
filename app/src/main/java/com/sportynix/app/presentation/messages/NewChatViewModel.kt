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
import javax.inject.Inject

data class NewChatUiState(
    val query: String = "",
    val mutualUsers: List<MutualUser> = emptyList(),
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
            _uiState.update { it.copy(isInitialLoading = true) }
            val mutualRes = chatRepository.getMutualUsers()
            val reqsRes = chatRepository.getChatRequests()

            _uiState.update {
                it.copy(
                    mutualUsers = mutualRes.getOrDefault(emptyList()),
                    chatRequestsReceived = reqsRes.getOrNull()?.received ?: emptyList(),
                    chatRequestsSent = reqsRes.getOrNull()?.sent ?: emptyList(),
                    isInitialLoading = false
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
            delay(300) // 300ms debounce
            _uiState.update { it.copy(isLoading = true) }

            val searchRes = chatRepository.searchUsers(trimmed)
            val mutualRes = chatRepository.getMutualUsers(trimmed)

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
}

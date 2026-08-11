package com.sportynix.app.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.websocket.WebSocketManager
import com.sportynix.app.data.remote.api.TeamApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sportynix.app.domain.model.*
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MessagesListUiState(
    val activeTab: String = "my_chats", // "my_chats", "discover"
    val chatFilter: String = "all", // "all", "unread", "groups", "channels", "chat_requests"
    val discoverSubTab: String = "teams", // "teams", "channels"
    val chatRequestTab: String = "received", // "received", "sent"
    val conversations: List<Chat> = emptyList(),
    val discoverChannels: List<Chat> = emptyList(),
    val discoverTeams: List<DiscoverTeam> = emptyList(),
    val chatRequestsReceived: List<ChatRequestItem> = emptyList(),
    val chatRequestsSent: List<ChatRequestItem> = emptyList(),
    val unreadCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val busyTeamId: Long? = null
)

@HiltViewModel
class MessagesListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val teamApi: TeamApiService,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesListUiState())
    val uiState: StateFlow<MessagesListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        observeWebSocketCounts()
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == "discover") {
            loadDiscover()
        }
    }

    fun setChatFilter(filter: String) {
        _uiState.update { it.copy(chatFilter = filter) }
        if (filter == "chat_requests") {
            loadChatRequests()
        }
    }

    fun setDiscoverSubTab(subTab: String) {
        _uiState.update { it.copy(discoverSubTab = subTab) }
    }

    fun setChatRequestTab(tab: String) {
        _uiState.update { it.copy(chatRequestTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadConversations(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) _uiState.update { it.copy(isRefreshing = true) }
            else _uiState.update { it.copy(isLoading = true) }

            chatRepository.getMyChatsCachedFirst().collect { chats ->
                val sorted = chats.sortedByDescending { chat ->
                    chat.lastMessageTime ?: chat.updatedAt ?: chat.createdAt ?: ""
                }
                val totalUnread = sorted.sumOf { it.unreadCount }
                _uiState.update {
                    it.copy(
                        conversations = sorted,
                        unreadCount = totalUnread,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun loadDiscover() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val channelsRes = chatRepository.discoverChannels(_uiState.value.searchQuery)
            val teams = runCatching {
                val response = teamApi.discoverTeams()
                if (!response.isSuccessful || response.body() == null) emptyList() else {
                    val root = response.body()!!
                    val array = when {
                        root.isJsonArray -> root.asJsonArray
                        root.isJsonObject && root.asJsonObject.has("results") -> root.asJsonObject.getAsJsonArray("results")
                        root.isJsonObject && root.asJsonObject.has("teams") -> root.asJsonObject.getAsJsonArray("teams")
                        else -> com.google.gson.JsonArray()
                    }
                    val type = object : TypeToken<List<DiscoverTeam>>() {}.type
                    gson.fromJson<List<DiscoverTeam>>(array, type)
                }
            }.getOrElse { emptyList() }
            channelsRes.onSuccess { channels ->
                _uiState.update { it.copy(discoverChannels = channels, discoverTeams = teams, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(discoverTeams = teams, isLoading = false, errorMessage = it.errorMessage ?: "Unable to load channels") }
            }
        }
    }

    fun toggleTeamJoin(team: DiscoverTeam) {
        val id = team.id.toInt()
        viewModelScope.launch {
            _uiState.update { it.copy(busyTeamId = team.id) }
            runCatching {
                val requested = team.joinStatus.equals("requested", true) || team.joinStatus.equals("pending", true)
                val response = if (requested) teamApi.cancelJoin(id) else teamApi.requestJoin(id)
                if (!response.isSuccessful) error("Team request failed: HTTP ${response.code()}")
            }.onSuccess { loadDiscover() }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to update team request") }
            }
            _uiState.update { it.copy(busyTeamId = null) }
        }
    }

    fun loadChatRequests() {
        viewModelScope.launch {
            val res = chatRepository.getChatRequests()
            res.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        chatRequestsReceived = data.received,
                        chatRequestsSent = data.sent
                    )
                }
            }
        }
    }

    fun followChannel(chatId: Long) {
        viewModelScope.launch {
            chatRepository.followChannel(chatId).onSuccess {
                loadConversations(isRefreshing = true)
                loadDiscover()
            }
        }
    }

    fun unfollowChannel(chatId: Long) {
        viewModelScope.launch {
            chatRepository.unfollowChannel(chatId).onSuccess {
                loadConversations(isRefreshing = true)
                loadDiscover()
            }
        }
    }

    fun acceptChatRequest(request: ChatRequestItem, onChatOpened: (Long) -> Unit) {
        viewModelScope.launch {
            chatRepository.acceptChatRequest(request.id).onSuccess { (item, chat) ->
                loadChatRequests()
                loadConversations(isRefreshing = true)
                onChatOpened(chat.id)
            }
        }
    }

    fun declineChatRequest(request: ChatRequestItem) {
        viewModelScope.launch {
            chatRepository.declineChatRequest(request.id).onSuccess {
                loadChatRequests()
            }
        }
    }

    fun cancelChatRequest(request: ChatRequestItem) {
        viewModelScope.launch {
            chatRepository.cancelChatRequest(request.id).onSuccess {
                loadChatRequests()
            }
        }
    }

    private fun observeWebSocketCounts() {
        viewModelScope.launch {
            webSocketManager.unreadCountsState.collect { (messagesCount, _) ->
                _uiState.update { it.copy(unreadCount = messagesCount) }
            }
        }
        viewModelScope.launch {
            webSocketManager.newNotificationFlow.collect { notif ->
                loadConversations(isRefreshing = true)
            }
        }
    }
}

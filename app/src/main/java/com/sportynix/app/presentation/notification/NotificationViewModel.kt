package com.sportynix.app.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.api.AnnouncementApiService
import com.sportynix.app.data.remote.api.NotificationApiService
import com.sportynix.app.data.remote.dto.NotificationDto
import com.sportynix.app.domain.model.AnnouncementDetailPayload
import com.sportynix.app.domain.model.ContentPayloadBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationTab { ALL, UNREAD, BOOKING, POINTS }

sealed interface NotificationDestination {
    data class BookingDetail(val id: Int) : NotificationDestination
    data object BookingHistory : NotificationDestination
    data class Team(val teamId: Int? = null, val invitations: Boolean = false) : NotificationDestination
    data object Points : NotificationDestination
    data object Challenges : NotificationDestination
    data class Chat(val id: Int, val name: String) : NotificationDestination
    data object MessageRequests : NotificationDestination
    data object Profile : NotificationDestination
    data class PlayerProfile(val id: String) : NotificationDestination
    data class LeagueDetail(val id: String) : NotificationDestination
    data class Announcement(val payload: AnnouncementDetailPayload) : NotificationDestination
}

data class NotificationUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMarkingAll: Boolean = false,
    val isClearingAll: Boolean = false,
    val deletingIds: Set<String> = emptySet(),
    val activeTab: NotificationTab = NotificationTab.ALL,
    val errorMessage: String? = null
) {
    val filtered: List<NotificationDto> get() = when (activeTab) {
        NotificationTab.ALL -> notifications
        NotificationTab.UNREAD -> notifications.filterNot(NotificationDto::isRead)
        NotificationTab.BOOKING -> notifications.filter { it.type.orEmpty().contains("booking", true) }
        NotificationTab.POINTS -> notifications.filter { it.type.orEmpty().let { t -> t.contains("points", true) || t.contains("referral", true) } }
    }
    fun count(tab: NotificationTab) = copy(activeTab = tab).filtered.size
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationsApi: NotificationApiService,
    private val announcementsApi: AnnouncementApiService,
    private val countStore: NotificationCountStore
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()
    private val navigationChannel = Channel<NotificationDestination>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private val markingIds = mutableSetOf<String>()
    private var fetchRunning = false
    private var lastNavigationId: String? = null

    init { fetchNotifications() }

    fun fetchNotifications(refresh: Boolean = false) {
        if (fetchRunning) return
        fetchRunning = true
        _state.value = _state.value.copy(isLoading = !refresh && _state.value.notifications.isEmpty(), isRefreshing = refresh, errorMessage = null)
        viewModelScope.launch {
            try {
                val response = notificationsApi.getNotifications(pageSize = 100)
                if (!response.isSuccessful) error("Notifications request failed (${response.code()})")
                _state.value = _state.value.copy(notifications = response.body()?.results.orEmpty(), isLoading = false, isRefreshing = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false, errorMessage = e.message ?: "Failed to load notifications")
            } finally { fetchRunning = false }
        }
    }

    fun setActiveTab(tab: NotificationTab) { _state.value = _state.value.copy(activeTab = tab) }
    fun clearError() { _state.value = _state.value.copy(errorMessage = null) }

    fun markAsRead(id: String) {
        val item = _state.value.notifications.firstOrNull { it.id == id } ?: return
        if (item.isRead || !markingIds.add(id)) return
        _state.value = _state.value.copy(notifications = _state.value.notifications.map { if (it.id == id) it.copy(isRead = true) else it })
        countStore.requestRefresh()
        viewModelScope.launch {
            try {
                val response = notificationsApi.markAsRead(id)
                if (!response.isSuccessful) error("Mark read failed (${response.code()})")
                countStore.requestRefresh()
            } catch (_: Exception) { /* Swift keeps the optimistic single-item state. */ }
            finally { markingIds.remove(id) }
        }
    }

    fun markAllAsRead() {
        if (_state.value.isMarkingAll || _state.value.notifications.none { !it.isRead }) return
        _state.value = _state.value.copy(isMarkingAll = true, notifications = _state.value.notifications.map { it.copy(isRead = true) })
        countStore.requestRefresh()
        viewModelScope.launch {
            try {
                val response = notificationsApi.markAllAsRead()
                if (!response.isSuccessful) error("Mark all failed (${response.code()})")
                _state.value = _state.value.copy(isMarkingAll = false)
                countStore.requestRefresh()
            } catch (_: Exception) {
                _state.value = _state.value.copy(isMarkingAll = false)
                fetchNotifications(refresh = true)
            }
        }
    }

    fun deleteNotification(id: String) {
        if (id in _state.value.deletingIds) return
        _state.value = _state.value.copy(deletingIds = _state.value.deletingIds + id)
        viewModelScope.launch {
            try {
                val response = notificationsApi.deleteNotification(id)
                if (!response.isSuccessful) error("Delete failed (${response.code()})")
                _state.value = _state.value.copy(notifications = _state.value.notifications.filterNot { it.id == id }, deletingIds = _state.value.deletingIds - id)
                countStore.requestRefresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(deletingIds = _state.value.deletingIds - id, errorMessage = e.message ?: "Failed to delete notification")
            }
        }
    }

    fun clearAll() {
        if (_state.value.isClearingAll || _state.value.notifications.isEmpty()) return
        _state.value = _state.value.copy(isClearingAll = true)
        viewModelScope.launch {
            try {
                val response = notificationsApi.clearAllNotifications()
                if (!response.isSuccessful) error("Clear all failed (${response.code()})")
                _state.value = _state.value.copy(notifications = emptyList(), isClearingAll = false)
                countStore.requestRefresh()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isClearingAll = false, errorMessage = e.message ?: "Failed to clear notifications")
            }
        }
    }

    fun open(notification: NotificationDto) {
        if (lastNavigationId == notification.id) return
        lastNavigationId = notification.id
        markAsRead(notification.id)
        viewModelScope.launch {
            resolve(notification)?.let { navigationChannel.send(it) }
            lastNavigationId = null
        }
    }

    private suspend fun resolve(n: NotificationDto): NotificationDestination? {
        val type = n.type.orEmpty().lowercase()
        val bookingTypes = setOf("booking_confirmed", "permanent_booking_confirmed", "booking_completed", "booking_cancelled", "booking_no_show", "team_assigned_success", "team_booking_assigned", "booking_reminder")
        if (type in bookingTypes || type.contains("booking")) return ContentPayloadBuilder.intValue(n.data, "booking_id")?.let(NotificationDestination::BookingDetail) ?: NotificationDestination.BookingHistory
        if (type in setOf("team_join_request", "team_invitation") || type.contains("team") && (type.contains("join") || type.contains("invitation"))) return NotificationDestination.Team(invitations = true)
        if (type in setOf("team_approved", "team_joined")) return NotificationDestination.Team(ContentPayloadBuilder.intValue(n.data, "team_id", "teamId"))
        if (type in setOf("team_rejected", "team_declined", "team_removed", "team_deleted", "team_channel_followed")) return NotificationDestination.Team()
        if (type.contains("team")) {
            val content = "${n.title} ${n.message}".lowercase()
            if (content.contains("welcome to") || content.contains("new member")) return NotificationDestination.Team(ContentPayloadBuilder.intValue(n.data, "team_id", "teamId"))
        }
        if (type in setOf("points_earned", "points_deducted", "referral_earned")) return NotificationDestination.Points
        if (type in setOf("challenge_received", "challenge_accepted", "challenge_declined", "challenge_cancelled", "challenge_completed")) return NotificationDestination.Challenges
        if (type in setOf("chat_request_received", "chat_request") || (type.contains("chat_request") || type.contains("chat request")) && !type.contains("accepted")) return NotificationDestination.MessageRequests
        if (type in setOf("chat_request_accepted", "direct_chat_request_accepted") || type.contains("chat_request") && type.contains("accepted")) {
            val id = ContentPayloadBuilder.intValue(n.data, "chat_id") ?: return NotificationDestination.MessageRequests
            return NotificationDestination.Chat(id, ContentPayloadBuilder.value(n.data, "chat_name", "sender_name") ?: n.title)
        }
        if (type == "new_chat_message") {
            val id = ContentPayloadBuilder.intValue(n.data, "chat_id") ?: return null
            return NotificationDestination.Chat(id, ContentPayloadBuilder.value(n.data, "sender_name", "chat_name") ?: n.title)
        }
        val initial = ContentPayloadBuilder.fromNotification(n) ?: return null
        if (initial.imageUrl != null) return NotificationDestination.Announcement(initial)
        val linkedId = ContentPayloadBuilder.value(n.data, "announcement_id", "content_id") ?: return NotificationDestination.Announcement(initial)
        return try {
            val response = announcementsApi.getAnnouncement(linkedId)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isJsonObject) error("Announcement fetch failed")
            NotificationDestination.Announcement(ContentPayloadBuilder.fromAnnouncement(body.asJsonObject))
        } catch (_: Exception) { NotificationDestination.Announcement(initial) }
    }
}

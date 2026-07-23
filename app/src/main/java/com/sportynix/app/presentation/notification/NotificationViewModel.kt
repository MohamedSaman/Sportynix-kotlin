package com.sportynix.app.presentation.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.api.NotificationApiService
import com.sportynix.app.data.remote.dto.NotificationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationTab { ALL, UNREAD, BOOKING, POINTS }

data class NotificationUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val activeTab: NotificationTab = NotificationTab.ALL,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationApiService: NotificationApiService
) : ViewModel() {

    var state by mutableStateOf(NotificationUiState())
        private set

    init {
        fetchNotifications()
    }

    fun fetchNotifications(page: Int = 1, append: Boolean = false) {
        if (state.isLoading || (append && state.isLoadingMore)) return
        viewModelScope.launch {
            if (!append) {
                state = state.copy(isLoading = !state.isRefreshing, errorMessage = null)
            } else {
                state = state.copy(isLoadingMore = true)
            }
            try {
                val response = notificationApiService.getNotifications(page = page)
                if (response.isSuccessful) {
                    val body = response.body()
                    val fetched = body?.results ?: emptyList()
                    // Filter chat notifications (they belong in Messages)
                    val filtered = fetched.filter { it.type != "new_chat_message" }
                    val updated = if (append) {
                        val existing = state.notifications.toMutableList()
                        filtered.forEach { n -> if (existing.none { it.id == n.id }) existing.add(n) }
                        existing
                    } else {
                        filtered
                    }
                    state = state.copy(
                        notifications = updated.sortedByDescending { it.createdAt ?: "" },
                        hasNextPage = body?.next != null,
                        currentPage = page,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                } else {
                    state = state.copy(isLoading = false, isRefreshing = false, isLoadingMore = false,
                        errorMessage = "Failed to load notifications")
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, isRefreshing = false, isLoadingMore = false,
                    errorMessage = e.message)
            }
        }
    }

    fun refresh() {
        state = state.copy(isRefreshing = true)
        fetchNotifications(page = 1, append = false)
    }

    fun loadMore() {
        if (!state.hasNextPage || state.isLoadingMore || state.isLoading) return
        fetchNotifications(page = state.currentPage + 1, append = true)
    }

    fun setActiveTab(tab: NotificationTab) {
        state = state.copy(activeTab = tab)
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                notificationApiService.markAsRead(id)
                state = state.copy(
                    notifications = state.notifications.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    }
                )
            } catch (_: Exception) {}
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                notificationApiService.markAllAsRead()
                state = state.copy(
                    notifications = state.notifications.map { it.copy(isRead = true) }
                )
            } catch (_: Exception) {}
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            try {
                notificationApiService.deleteNotification(id)
                state = state.copy(
                    notifications = state.notifications.filter { it.id != id }
                )
            } catch (_: Exception) {}
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                notificationApiService.clearAllNotifications()
                state = state.copy(notifications = emptyList())
            } catch (_: Exception) {}
        }
    }

    fun toggleSelectionMode() {
        state = state.copy(
            isSelectionMode = !state.isSelectionMode,
            selectedIds = emptySet()
        )
    }

    fun toggleSelection(id: String) {
        val current = state.selectedIds.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        state = state.copy(selectedIds = current)
    }

    fun deleteSelected() {
        val ids = state.selectedIds.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                try { notificationApiService.deleteNotification(id) } catch (_: Exception) {}
            }
            state = state.copy(
                notifications = state.notifications.filter { it.id !in ids },
                isSelectionMode = false,
                selectedIds = emptySet()
            )
        }
    }

    fun filteredNotifications(): List<NotificationDto> {
        return when (state.activeTab) {
            NotificationTab.ALL -> state.notifications
            NotificationTab.UNREAD -> state.notifications.filter { !it.isRead }
            NotificationTab.BOOKING -> state.notifications.filter {
                it.type?.contains("booking", ignoreCase = true) == true
            }
            NotificationTab.POINTS -> state.notifications.filter {
                it.type?.contains("point", ignoreCase = true) == true ||
                it.type?.contains("reward", ignoreCase = true) == true
            }
        }
    }
}

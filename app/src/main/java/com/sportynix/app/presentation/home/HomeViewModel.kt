package com.sportynix.app.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.AuthApiService
import com.sportynix.app.data.remote.websocket.LiveMatchWebSocketManager
import com.sportynix.app.presentation.notification.NotificationCountStore
import com.sportynix.app.domain.model.Announcement
import com.sportynix.app.domain.model.LiveMatchSnapshot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.AnnouncementRepository
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PromoBannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val gradientColors: List<Long>,
    val iconName: String? = null,
    val navigateTo: String? = null
)

val defaultPromoBanners = listOf(
    PromoBannerItem(
        id = "1",
        title = "New Features",
        subtitle = "Faster bookings, live scores & exclusive offers.",
        badge = "New",
        gradientColors = listOf(0xFF1A8553, 0x631A8553)
    ),
    PromoBannerItem(
        id = "2",
        title = "Challenge Teams",
        subtitle = "Compete with other teams and prove your skills!",
        badge = "New",
        gradientColors = listOf(0xFF2563EB, 0x632563EB),
        iconName = "trophy",
        navigateTo = "Challenge"
    ),
    PromoBannerItem(
        id = "3",
        title = "Feeds & Updates",
        subtitle = "Stay on top of sports news and updates.",
        badge = "Coming Soon",
        gradientColors = listOf(0xFF1E3A8A, 0x593B82F6),
        iconName = "newspaper"
    )
)

data class HomeUiState(
    val featuredVenues: List<Venue> = emptyList(),
    val nearbyVenues: List<Venue> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val dismissedAnnouncementIds: Set<String> = emptySet(),
    val recentMatches: List<LiveMatchSnapshot> = emptyList(),
    val promoBanners: List<PromoBannerItem> = defaultPromoBanners,
    val selectedCategory: String = "Popular",
    val userLocation: Pair<Double, Double>? = null,
    val locationPermissionDenied: Boolean = false,
    val unreadNotificationsCount: Int = 0,
    val unreadMessagesCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingNearby: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLiveWsConnected: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    private val announcementRepository: AnnouncementRepository,
    private val authApiService: AuthApiService,
    private val liveMatchWebSocketManager: LiveMatchWebSocketManager,
    private val notificationCountStore: NotificationCountStore
) : ViewModel() {

    var state by mutableStateOf(HomeUiState())
        private set

    private var retryJob: Job? = null
    private var locationJob: Job? = null
    private var initialized = false
    private var lastDashboardRefreshMs = 0L
    private var lastNearbyRequest: Pair<Double, Double>? = null

    init {
        initializeDashboard()
        observeWebSocketMatches()
        viewModelScope.launch { notificationCountStore.refreshRequests.collect { fetchUnreadCounts() } }
    }

    fun initializeDashboard() {
        val now = System.currentTimeMillis()
        if (initialized && now - lastDashboardRefreshMs < 5 * 60 * 1000L) return
        initialized = true
        lastDashboardRefreshMs = now
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            fetchFeaturedVenues()
            fetchAnnouncements()
            fetchUnreadCounts()
            liveMatchWebSocketManager.connect()
            state = state.copy(isLoading = false)

            if (state.featuredVenues.isEmpty()) {
                startRetryLoop()
            }
        }
    }

    private fun observeWebSocketMatches() {
        liveMatchWebSocketManager.matchesState
            .onEach { matches ->
                state = state.copy(recentMatches = matches)
            }
            .launchIn(viewModelScope)

        liveMatchWebSocketManager.isConnected
            .onEach { connected ->
                state = state.copy(isLiveWsConnected = connected)
            }
            .launchIn(viewModelScope)
    }

    fun refreshAllData() {
        lastDashboardRefreshMs = System.currentTimeMillis()
        viewModelScope.launch {
            state = state.copy(isRefreshing = true, errorMessage = null)
            fetchFeaturedVenues()
            fetchAnnouncements()
            fetchUnreadCounts()
            state.userLocation?.let { (lat, lon) ->
                fetchNearbyVenues(lat, lon)
            }
            state = state.copy(isRefreshing = false)
        }
    }

    private suspend fun fetchFeaturedVenues() {
        when (val result = venueRepository.fetchFeaturedVenues()) {
            is ApiResult.Success -> {
                state = state.copy(featuredVenues = result.data, errorMessage = null)
                cancelRetryLoop()
            }
            is ApiResult.Error -> {
                if (state.featuredVenues.isEmpty()) {
                    state = state.copy(errorMessage = "Unable to load venues. Check network connection.")
                }
            }
            else -> {}
        }
    }

    private suspend fun fetchAnnouncements() {
        when (val result = announcementRepository.getAnnouncements()) {
            is ApiResult.Success -> {
                state = state.copy(announcements = result.data)
            }
            else -> {}
        }
    }

    fun fetchNearbyVenues(latitude: Double, longitude: Double) {
        val coordinates = latitude to longitude
        if (lastNearbyRequest == coordinates && locationJob?.isActive == true) return
        lastNearbyRequest = coordinates
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            delay(300)
            state = state.copy(isLoadingNearby = true, userLocation = Pair(latitude, longitude), locationPermissionDenied = false)
            when (val result = venueRepository.fetchNearbyVenues(latitude, longitude)) {
                is ApiResult.Success -> {
                    state = state.copy(nearbyVenues = result.data, isLoadingNearby = false)
                }
                is ApiResult.Error -> {
                    state = state.copy(nearbyVenues = emptyList(), isLoadingNearby = false)
                }
                else -> {
                    state = state.copy(isLoadingNearby = false)
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        state = state.copy(locationPermissionDenied = true, isLoadingNearby = false)
    }

    fun dismissAnnouncement(announcementId: String) {
        val updated = state.dismissedAnnouncementIds.toMutableSet().apply { add(announcementId) }
        state = state.copy(dismissedAnnouncementIds = updated)
    }

    fun selectCategory(category: String) {
        state = state.copy(selectedCategory = category)
    }

    fun fetchUnreadCounts() {
        viewModelScope.launch {
            try {
                val response = authApiService.getUnreadCounts()
                if (response.isSuccessful && response.body() != null) {
                    val counts = response.body()!!
                    state = state.copy(
                        unreadNotificationsCount = counts.notifications,
                        unreadMessagesCount = counts.messages
                    )
                }
            } catch (e: Exception) {
                Timber.w(e, "Error fetching unread counts")
            }
        }
    }

    private fun startRetryLoop() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            while (state.featuredVenues.isEmpty()) {
                delay(5000)
                fetchFeaturedVenues()
                fetchAnnouncements()
            }
        }
    }

    private fun cancelRetryLoop() {
        retryJob?.cancel()
        retryJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelRetryLoop()
        locationJob?.cancel()
    }
}

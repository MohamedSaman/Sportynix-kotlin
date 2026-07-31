package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class BookingSortOption(val label: String, val description: String) {
    PLAY_DATE_NEWEST("Play Date: Newest First", "Sort by when you will play"),
    PLAY_DATE_OLDEST("Play Date: Oldest First", "Sort by when you will play"),
    BOOKED_DATE_LATEST("Booked Date: Latest First", "Sort by when you booked"),
    BOOKED_DATE_OLDEST("Booked Date: Oldest First", "Sort by when you booked")
}

data class BookingHistoryUiState(
    val bookings: List<Booking> = emptyList(),
    val countsMap: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedBookingType: Int = 0, // 0 = Normal, 1 = Permanent
    val selectedFilter: String = "All",
    val sortOption: BookingSortOption = BookingSortOption.PLAY_DATE_NEWEST,
    val showSortSheet: Boolean = false,

    // QR State
    val selectedBookingForQR: Booking? = null,
    val qrCodeUrl: String? = null,
    val isLoadingQR: Boolean = false,
    val showQRModal: Boolean = false,

    // Cancel State
    val bookingToCancel: Booking? = null,
    val showCancelAlert: Boolean = false,
    val isCancelling: Boolean = false,

    // Team State
    val bookingForTeam: Booking? = null,
    val userTeams: List<Pair<Int, String>> = emptyList(),
    val isLoadingTeams: Boolean = false,
    val showTeamSheet: Boolean = false,
    val isAssigningTeam: Boolean = false
)

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val bookingApiService: BookingApiService,
    private val userApiService: UserApiService
) : ViewModel() {

    var uiState by mutableStateOf(BookingHistoryUiState())
        private set

    private val cacheMap = mutableMapOf<String, Pair<Long, List<Booking>>>()
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    init {
        loadBookings(forceRefresh = false)
    }

    fun loadBookings(forceRefresh: Boolean = false) {
        val typeStr = if (uiState.selectedBookingType == 1) "permanent" else "normal"
        val filterStr = uiState.selectedFilter.lowercase().replace(" ", "_")
        val cacheKey = "$typeStr:$filterStr"

        val cached = cacheMap[cacheKey]
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && (now - cached.first) < CACHE_DURATION_MS) {
            uiState = uiState.copy(bookings = sortBookings(cached.second, uiState.sortOption), isLoading = false)
            return
        }

        uiState = uiState.copy(isLoading = !forceRefresh, isRefreshing = forceRefresh, errorMessage = null)
        viewModelScope.launch {
            when (val res = bookingRepository.fetchUserBookings(typeStr, if (filterStr == "all") null else filterStr)) {
                is ApiResult.Success -> {
                    val sorted = sortBookings(res.data, uiState.sortOption)
                    cacheMap[cacheKey] = Pair(now, res.data)
                    uiState = uiState.copy(bookings = sorted, isLoading = false, isRefreshing = false)
                }
                is ApiResult.ServerError -> {
                    uiState = uiState.copy(errorMessage = res.message, isLoading = false, isRefreshing = false)
                }
                is ApiResult.Error -> {
                    uiState = uiState.copy(errorMessage = res.message, isLoading = false, isRefreshing = false)
                }
                else -> {
                    uiState = uiState.copy(isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    fun invalidateCache() {
        cacheMap.clear()
    }

    fun setBookingType(type: Int) {
        if (uiState.selectedBookingType == type) return
        uiState = uiState.copy(selectedBookingType = type, selectedFilter = "All")
        loadBookings(forceRefresh = false)
    }

    fun setFilter(filter: String) {
        if (uiState.selectedFilter == filter) return
        uiState = uiState.copy(selectedFilter = filter)
        loadBookings(forceRefresh = false)
    }

    fun setSortOption(option: BookingSortOption) {
        val sorted = sortBookings(uiState.bookings, option)
        uiState = uiState.copy(sortOption = option, bookings = sorted, showSortSheet = false)
    }

    fun setShowSortSheet(show: Boolean) {
        uiState = uiState.copy(showSortSheet = show)
    }

    fun openQRModal(booking: Booking) {
        uiState = uiState.copy(
            selectedBookingForQR = booking,
            qrCodeUrl = booking.qrCodeURL,
            isLoadingQR = booking.qrCodeURL.isNullOrEmpty(),
            showQRModal = true
        )
        if (booking.qrCodeURL.isNullOrEmpty()) {
            viewModelScope.launch {
                when (val res = bookingRepository.fetchBookingQRCode(booking.bookingId)) {
                    is ApiResult.Success -> {
                        uiState = uiState.copy(qrCodeUrl = res.data, isLoadingQR = false)
                    }
                    else -> {
                        uiState = uiState.copy(isLoadingQR = false)
                    }
                }
            }
        }
    }

    fun dismissQRModal() {
        uiState = uiState.copy(showQRModal = false, selectedBookingForQR = null, qrCodeUrl = null)
    }

    fun openTeamSheet(booking: Booking) {
        uiState = uiState.copy(bookingForTeam = booking, showTeamSheet = true, isLoadingTeams = true)
        viewModelScope.launch {
            try {
                val res = bookingApiService.getMyTeams()
                if (res.isSuccessful && res.body() != null) {
                    val json = res.body()!!
                    val teams = parseTeamsJson(json)
                    uiState = uiState.copy(isLoadingTeams = false, userTeams = teams)
                } else {
                    uiState = uiState.copy(isLoadingTeams = false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching teams")
                uiState = uiState.copy(isLoadingTeams = false)
            }
        }
    }

    fun dismissTeamSheet() {
        uiState = uiState.copy(showTeamSheet = false, bookingForTeam = null)
    }

    fun assignTeam(teamId: Int) {
        val booking = uiState.bookingForTeam ?: return
        uiState = uiState.copy(isAssigningTeam = true)
        viewModelScope.launch {
            bookingRepository.assignTeamInt(booking.bookingId, teamId)
            invalidateCache()
            uiState = uiState.copy(isAssigningTeam = false, showTeamSheet = false, bookingForTeam = null)
            loadBookings(forceRefresh = true)
        }
    }

    fun removeTeam(booking: Booking) {
        viewModelScope.launch {
            bookingRepository.removeTeamInt(booking.bookingId)
            invalidateCache()
            loadBookings(forceRefresh = true)
        }
    }

    private fun sortBookings(list: List<Booking>, option: BookingSortOption): List<Booking> {
        return when (option) {
            BookingSortOption.PLAY_DATE_NEWEST -> list.sortedByDescending { parseDate(it.playDateStart) }
            BookingSortOption.PLAY_DATE_OLDEST -> list.sortedBy { parseDate(it.playDateStart) }
            BookingSortOption.BOOKED_DATE_LATEST -> list.sortedByDescending { parseDate(it.bookedDate ?: it.createdAt) }
            BookingSortOption.BOOKED_DATE_OLDEST -> list.sortedBy { parseDate(it.bookedDate ?: it.createdAt) }
        }
    }

    private fun parseDate(dateStr: String?): Date {
        if (dateStr.isNullOrEmpty() || dateStr == "N/A") return Date(0)
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "MMM d, yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) return d
            } catch (_: Exception) {}
        }
        return Date(0)
    }

    private fun parseTeamsJson(jsonElement: com.google.gson.JsonElement): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        try {
            if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray.forEach { elem ->
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val id = if (obj.has("id")) obj.get("id").asInt else 0
                        val name = if (obj.has("name")) obj.get("name").asString else "Team"
                        result.add(Pair(id, name))
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }
}

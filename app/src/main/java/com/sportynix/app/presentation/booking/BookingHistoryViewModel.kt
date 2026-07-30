package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.dto.BlockedUserDto
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
    val isLoading: Boolean = false,
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
    val userTeams: List<Pair<Int, String>> = emptyList(), // ID, Name
    val isLoadingTeams: Boolean = false,
    val showTeamSheet: Boolean = false,
    val isAssigningTeam: Boolean = false
)

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val userApiService: UserApiService
) : ViewModel() {

    var uiState by mutableStateOf(BookingHistoryUiState())
        private set

    init {
        loadBookings()
    }

    fun loadBookings() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = bookingRepository.fetchBookings()) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(bookings = res.data, isLoading = false)
                }
                is ApiResult.ServerError -> {
                    uiState = uiState.copy(errorMessage = res.message, isLoading = false)
                }
                is ApiResult.Error -> {
                    uiState = uiState.copy(errorMessage = res.message, isLoading = false)
                }
                else -> {
                    uiState = uiState.copy(isLoading = false)
                }
            }
        }
    }

    fun setBookingType(type: Int) {
        uiState = uiState.copy(selectedBookingType = type)
    }

    fun setFilter(filter: String) {
        uiState = uiState.copy(selectedFilter = filter)
    }

    fun setSortOption(option: BookingSortOption) {
        uiState = uiState.copy(sortOption = option, showSortSheet = false)
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

    fun promptCancelBooking(booking: Booking) {
        uiState = uiState.copy(bookingToCancel = booking, showCancelAlert = true)
    }

    fun dismissCancelAlert() {
        uiState = uiState.copy(bookingToCancel = null, showCancelAlert = false)
    }

    fun confirmCancelBooking() {
        val booking = uiState.bookingToCancel ?: return
        uiState = uiState.copy(isCancelling = true, showCancelAlert = false)
        viewModelScope.launch {
            val res = if (booking.isPermanent) {
                bookingRepository.cancelSeriesInt(booking.bookingId)
            } else {
                bookingRepository.cancelBookingInt(booking.bookingId)
            }
            uiState = uiState.copy(isCancelling = false, bookingToCancel = null)
            loadBookings()
        }
    }

    fun openTeamSheet(booking: Booking) {
        uiState = uiState.copy(bookingForTeam = booking, showTeamSheet = true, isLoadingTeams = true)
        viewModelScope.launch {
            // Fetch user teams
            uiState = uiState.copy(isLoadingTeams = false, userTeams = listOf(1 to "My Team Alpha", 2 to "Sportynix Strikers"))
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
            uiState = uiState.copy(isAssigningTeam = false, showTeamSheet = false, bookingForTeam = null)
            loadBookings()
        }
    }

    fun removeTeam(booking: Booking) {
        viewModelScope.launch {
            bookingRepository.removeTeamInt(booking.bookingId)
            loadBookings()
        }
    }

    fun parseDate(dateStr: String): Date {
        if (dateStr.isEmpty() || dateStr == "N/A") return Date(0)
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
            } catch (e: Exception) {
                // continue
            }
        }
        return Date(0)
    }
}

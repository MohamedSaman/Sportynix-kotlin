package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.BookingTeamData
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class BookingSortOption(val label: String, val description: String) {
    PLAY_DATE_NEWEST("Play Date: Newest First", "Sort by when you will play"),
    PLAY_DATE_OLDEST("Play Date: Oldest First", "Sort by when you will play"),
    BOOKED_DATE_LATEST("Booked Date: Latest First", "Sort by when you booked"),
    BOOKED_DATE_OLDEST("Booked Date: Oldest First", "Sort by when you booked")
}

data class BookingHistoryUiState(
    val allBookings: List<Booking> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val normalCount: Int = 0,
    val permanentCount: Int = 0,
    val filterCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val currentUserId: Int? = null,
    val selectedBookingType: Int = 0,
    val selectedFilter: String = "All",
    val sortOption: BookingSortOption = BookingSortOption.PLAY_DATE_NEWEST,
    val showSortSheet: Boolean = false,
    val selectedBookingForQR: Booking? = null,
    val qrCodeUrl: String? = null,
    val isLoadingQR: Boolean = false,
    val qrError: String? = null,
    val showQRModal: Boolean = false,
    val bookingToCancel: Booking? = null,
    val showCancelAlert: Boolean = false,
    val cancellingBookingId: Int? = null,
    val bookingForTeam: Booking? = null,
    val userTeams: List<BookingTeamData> = emptyList(),
    val isLoadingTeams: Boolean = false,
    val showTeamSheet: Boolean = false,
    val assigningBookingId: Int? = null
)

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    var uiState by mutableStateOf(BookingHistoryUiState(isLoading = true))
        private set

    private var fetchJob: Job? = null
    private val qrRequests = mutableSetOf<Int>()

    init {
        viewModelScope.launch {
            val user = profileRepository.currentUser.value ?: profileRepository.fetchProfile().getOrNull()
            uiState = uiState.copy(currentUserId = user?.id?.toIntOrNull())
        }
        loadBookings()
    }

    fun loadBookings(forceRefresh: Boolean = false) {
        if (fetchJob?.isActive == true) return
        uiState = uiState.copy(
            isLoading = !forceRefresh && uiState.allBookings.isEmpty(),
            isRefreshing = forceRefresh,
            errorMessage = null
        )
        fetchJob = viewModelScope.launch {
            when (val result = bookingRepository.fetchBookings()) {
                is ApiResult.Success -> {
                    val grouped = applyPermanentCounts(result.data)
                    uiState = uiState.copy(allBookings = grouped, isLoading = false, isRefreshing = false)
                    deriveVisibleBookings()
                }
                is ApiResult.ServerError -> uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
                is ApiResult.Error -> uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
                else -> uiState = uiState.copy(isLoading = false, isRefreshing = false, errorMessage = "Unable to load bookings")
            }
        }
    }

    fun refresh() = loadBookings(forceRefresh = true)

    fun setBookingType(type: Int) {
        if (uiState.selectedBookingType == type) return
        uiState = uiState.copy(selectedBookingType = type, selectedFilter = "All")
        deriveVisibleBookings()
    }

    fun setFilter(filter: String) {
        if (uiState.selectedFilter == filter) return
        uiState = uiState.copy(selectedFilter = filter)
        deriveVisibleBookings()
    }

    fun setSortOption(option: BookingSortOption) {
        uiState = uiState.copy(sortOption = option, showSortSheet = false)
        deriveVisibleBookings()
    }

    fun setShowSortSheet(show: Boolean) { uiState = uiState.copy(showSortSheet = show) }
    fun clearMessage() { uiState = uiState.copy(message = null) }

    fun openQRModal(booking: Booking) {
        if (!qrRequests.add(booking.bookingId)) return
        uiState = uiState.copy(selectedBookingForQR = booking, qrCodeUrl = booking.qrCodeURL,
            qrError = null, isLoadingQR = booking.qrCodeURL.isNullOrBlank(), showQRModal = true)
        if (!booking.qrCodeURL.isNullOrBlank()) {
            qrRequests.remove(booking.bookingId)
            return
        }
        viewModelScope.launch {
            when (val result = bookingRepository.fetchBookingQRCode(booking.bookingId)) {
                is ApiResult.Success -> uiState = uiState.copy(qrCodeUrl = result.data, isLoadingQR = false)
                is ApiResult.ServerError -> uiState = uiState.copy(isLoadingQR = false, qrError = result.message)
                is ApiResult.Error -> uiState = uiState.copy(isLoadingQR = false, qrError = result.message)
                else -> uiState = uiState.copy(isLoadingQR = false, qrError = "QR code is unavailable")
            }
            qrRequests.remove(booking.bookingId)
        }
    }

    fun dismissQRModal() {
        uiState.selectedBookingForQR?.bookingId?.let(qrRequests::remove)
        uiState = uiState.copy(showQRModal = false, selectedBookingForQR = null, qrCodeUrl = null, qrError = null)
    }

    fun requestCancellation(booking: Booking) {
        if (!booking.canCancel || uiState.cancellingBookingId != null) return
        uiState = uiState.copy(bookingToCancel = booking, showCancelAlert = true)
    }

    fun dismissCancellation() { uiState = uiState.copy(bookingToCancel = null, showCancelAlert = false) }

    fun confirmCancellation() {
        val booking = uiState.bookingToCancel ?: return
        if (uiState.cancellingBookingId != null) return
        uiState = uiState.copy(showCancelAlert = false, cancellingBookingId = booking.bookingId)
        viewModelScope.launch {
            val result = if (booking.isPermanent) bookingRepository.cancelSeriesInt(booking.bookingId)
            else bookingRepository.cancelBookingInt(booking.bookingId)
            val success = result is ApiResult.Success || result.messageOrNull().contains("already cancelled", ignoreCase = true)
            if (success) {
                val updated = uiState.allBookings.map {
                    if (it.bookingId == booking.bookingId || (booking.isPermanent && sameSeries(it, booking))) it.copy(status = "Cancelled", canCancel = false) else it
                }
                uiState = uiState.copy(allBookings = updated, bookingToCancel = null, cancellingBookingId = null,
                    message = if (result is ApiResult.Success) "Booking cancelled successfully" else "Booking was already cancelled")
                deriveVisibleBookings()
                loadBookings(forceRefresh = true)
            } else {
                uiState = uiState.copy(bookingToCancel = null, cancellingBookingId = null,
                    errorMessage = result.messageOrNull().ifBlank { "Failed to cancel booking" })
            }
        }
    }

    fun openTeamSheet(booking: Booking) {
        if (uiState.isLoadingTeams || uiState.assigningBookingId != null) return
        uiState = uiState.copy(bookingForTeam = booking, isLoadingTeams = true)
        viewModelScope.launch {
            when (val result = bookingRepository.fetchMyTeams()) {
                is ApiResult.Success -> uiState = if (result.data.isEmpty()) {
                    uiState.copy(isLoadingTeams = false, bookingForTeam = null,
                        message = "You are not a member of any teams. Create a team first.")
                } else uiState.copy(isLoadingTeams = false, userTeams = result.data, showTeamSheet = true)
                else -> uiState = uiState.copy(isLoadingTeams = false, bookingForTeam = null,
                    errorMessage = result.messageOrNull().ifBlank { "Failed to load teams" })
            }
        }
    }

    fun dismissTeamSheet() { uiState = uiState.copy(showTeamSheet = false, bookingForTeam = null) }

    fun assignTeam(teamId: Int) {
        val booking = uiState.bookingForTeam ?: return
        if (uiState.assigningBookingId != null) return
        val team = uiState.userTeams.firstOrNull { it.id == teamId }
        uiState = uiState.copy(assigningBookingId = booking.bookingId, showTeamSheet = false)
        viewModelScope.launch {
            when (val result = bookingRepository.assignTeamInt(booking.bookingId, teamId)) {
                is ApiResult.Success -> {
                    updateBooking(booking.bookingId) { it.copy(teamId = teamId, teamName = team?.name ?: "Team", memberCount = team?.memberCount ?: 0) }
                    uiState = uiState.copy(assigningBookingId = null, bookingForTeam = null, message = "Team assigned successfully")
                    loadBookings(forceRefresh = true)
                }
                else -> uiState = uiState.copy(assigningBookingId = null, errorMessage = result.messageOrNull().ifBlank { "Failed to assign team" })
            }
        }
    }

    fun removeTeam(booking: Booking) {
        if (uiState.assigningBookingId != null) return
        uiState = uiState.copy(assigningBookingId = booking.bookingId)
        viewModelScope.launch {
            when (val result = bookingRepository.removeTeamInt(booking.bookingId)) {
                is ApiResult.Success -> {
                    updateBooking(booking.bookingId) { it.copy(teamId = null, teamName = "Personal", memberCount = 0) }
                    uiState = uiState.copy(assigningBookingId = null, message = "Team removed from booking")
                    loadBookings(forceRefresh = true)
                }
                else -> uiState = uiState.copy(assigningBookingId = null, errorMessage = result.messageOrNull().ifBlank { "Failed to remove team" })
            }
        }
    }

    fun updateReview(bookingId: Int, reviewId: Int?, rating: Double?) {
        updateBooking(bookingId) { it.copy(reviewId = reviewId, reviewRating = rating) }
    }

    private fun updateBooking(id: Int, transform: (Booking) -> Booking) {
        uiState = uiState.copy(allBookings = uiState.allBookings.map { if (it.bookingId == id) transform(it) else it })
        deriveVisibleBookings()
    }

    private fun deriveVisibleBookings() {
        val typed = uiState.allBookings.filter { it.isPermanent == (uiState.selectedBookingType == 1) }
        val counts = listOf("All", "Upcoming", "Completed", "Cancelled", "No-Show").associateWith { filter ->
            if (filter == "All") typed.size else typed.count { normalizedStatus(it.status) == filter }
        }
        val filtered = if (uiState.selectedFilter == "All") typed else typed.filter { normalizedStatus(it.status) == uiState.selectedFilter }
        uiState = uiState.copy(
            bookings = sortBookings(filtered, uiState.sortOption),
            normalCount = uiState.allBookings.count { !it.isPermanent },
            permanentCount = uiState.allBookings.count { it.isPermanent },
            filterCounts = counts
        )
    }

    private fun applyPermanentCounts(bookings: List<Booking>): List<Booking> {
        val counts = bookings.filter(Booking::isPermanent).groupingBy(::seriesKey).eachCount()
        return bookings.map { if (it.isPermanent) it.copy(slotCount = counts[seriesKey(it)] ?: 1) else it.copy(slotCount = 1) }
    }

    private fun seriesKey(booking: Booking): String = booking.permanentSourceId?.let { "series_$it" }
        ?: "${booking.complexName}|${booking.sport}|${booking.bookedDate}|${booking.timeSlot}"

    private fun sameSeries(first: Booking, second: Booking): Boolean {
        if (!first.isPermanent || !second.isPermanent) return false
        if (first.permanentSourceId != null && second.permanentSourceId != null) return first.permanentSourceId == second.permanentSourceId
        if (first.permanentSourceId == second.bookingId || second.permanentSourceId == first.bookingId) return true
        return seriesKey(first) == seriesKey(second)
    }

    private fun normalizedStatus(value: String): String = when (value.lowercase(Locale.US).trim()) {
        "playing", "ongoing" -> "Ongoing"
        "confirmed", "upcoming", "pending" -> "Upcoming"
        "completed" -> "Completed"
        "no-show", "no_show", "noshow" -> "No-Show"
        else -> "Cancelled"
    }

    private fun sortBookings(list: List<Booking>, option: BookingSortOption): List<Booking> = when (option) {
        BookingSortOption.PLAY_DATE_NEWEST -> list.sortedByDescending { parseDate(it.playDateStart) }
        BookingSortOption.PLAY_DATE_OLDEST -> list.sortedBy { parseDate(it.playDateStart) }
        BookingSortOption.BOOKED_DATE_LATEST -> list.sortedByDescending { parseDate(it.bookedDate.takeIf(String::isNotBlank) ?: it.createdAt) }
        BookingSortOption.BOOKED_DATE_OLDEST -> list.sortedBy { parseDate(it.bookedDate.takeIf(String::isNotBlank) ?: it.createdAt) }
    }

    private fun parseDate(value: String?): Date {
        if (value.isNullOrBlank() || value == "N/A") return Date(0)
        val formats = listOf("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "MMM d, yyyy")
        formats.forEach { format ->
            runCatching { SimpleDateFormat(format, Locale.US).apply { isLenient = false }.parse(value) }.getOrNull()?.let { return it }
        }
        return Date(0)
    }

    private fun ApiResult<*>.messageOrNull(): String = when (this) {
        is ApiResult.ServerError -> message
        is ApiResult.Error -> message
        else -> ""
    }
}

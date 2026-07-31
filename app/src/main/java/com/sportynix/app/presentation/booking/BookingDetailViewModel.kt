package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BookingDetailUiState(
    val booking: Booking? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Permanent Series Slots
    val permanentSlots: List<Booking> = emptyList(),
    val activePermanentTab: String = "All",
    val showAllPermanentSlots: Boolean = false,

    // Payments History
    val payments: List<PaymentOrderInfoDto> = emptyList(),

    // User Teams
    val userTeams: List<Pair<Int, String>> = emptyList(),
    val showTeamSheet: Boolean = false,
    val isAssigningTeam: Boolean = false,

    // QR State
    val qrCodeUrl: String? = null,
    val showQRModal: Boolean = false,

    // Balance Payment Checkout
    val isInitiatingBalancePayment: Boolean = false,
    val balanceCheckout: PaymentCheckoutResponseDto? = null
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val bookingApiService: BookingApiService,
    private val gson: Gson
) : ViewModel() {

    var uiState by mutableStateOf(BookingDetailUiState())
        private set

    fun initBooking(initialBooking: Booking?, bookingIdStr: String?) {
        if (initialBooking != null) {
            uiState = uiState.copy(booking = initialBooking)
            loadExtraDetails(initialBooking.bookingId)
        } else if (!bookingIdStr.isNullOrEmpty()) {
            val idInt = bookingIdStr.toIntOrNull()
            if (idInt != null) {
                fetchBookingById(idInt)
            }
        }
    }

    fun fetchBookingById(bId: Int) {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = bookingRepository.getBookingDetail(bId.toString())) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(booking = res.data, isLoading = false)
                    loadExtraDetails(bId)
                }
                else -> {
                    // Try fallback endpoint
                    try {
                        val fallbackRes = bookingApiService.getBookingDetailFallback(bId)
                        if (fallbackRes.isSuccessful && fallbackRes.body() != null) {
                            val apiBooking = fallbackRes.body()!!
                            val mapped = mapApiBookingToDomain(apiBooking)
                            uiState = uiState.copy(booking = mapped, isLoading = false)
                            loadExtraDetails(bId)
                        } else {
                            uiState = uiState.copy(isLoading = false, errorMessage = "Unable to load booking details.")
                        }
                    } catch (e: Exception) {
                        uiState = uiState.copy(isLoading = false, errorMessage = e.message)
                    }
                }
            }
        }
    }

    private fun loadExtraDetails(bId: Int) {
        val current = uiState.booking
        if (current?.isPermanent == true || current?.permanentSourceId != null) {
            fetchPermanentSeriesSlots(current.permanentSourceId ?: bId)
        }
        fetchUserTeams()
    }

    private fun fetchPermanentSeriesSlots(sourceId: Int) {
        viewModelScope.launch {
            try {
                when (val res = bookingRepository.fetchUserBookings("permanent", null)) {
                    is ApiResult.Success -> {
                        val group = res.data.filter { it.permanentSourceId == sourceId || it.bookingId == sourceId }
                        uiState = uiState.copy(permanentSlots = group)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching permanent series slots")
            }
        }
    }

    fun setPermanentTab(tab: String) {
        uiState = uiState.copy(activePermanentTab = tab)
    }

    fun setShowAllPermanentSlots(show: Boolean) {
        uiState = uiState.copy(showAllPermanentSlots = show)
    }

    fun fetchUserTeams() {
        viewModelScope.launch {
            try {
                val res = bookingApiService.getMyTeams()
                if (res.isSuccessful && res.body() != null) {
                    val teams = parseTeamsJson(res.body()!!)
                    uiState = uiState.copy(userTeams = teams)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching user teams")
            }
        }
    }

    fun openTeamSheet() {
        uiState = uiState.copy(showTeamSheet = true)
    }

    fun dismissTeamSheet() {
        uiState = uiState.copy(showTeamSheet = false)
    }

    fun assignTeam(teamId: Int) {
        val b = uiState.booking ?: return
        uiState = uiState.copy(isAssigningTeam = true)
        viewModelScope.launch {
            bookingRepository.assignTeamInt(b.bookingId, teamId)
            fetchBookingById(b.bookingId)
            uiState = uiState.copy(isAssigningTeam = false, showTeamSheet = false)
        }
    }

    fun removeTeam() {
        val b = uiState.booking ?: return
        viewModelScope.launch {
            bookingRepository.removeTeamInt(b.bookingId)
            fetchBookingById(b.bookingId)
        }
    }

    fun initiateBalancePayment(onSuccess: (PaymentCheckoutResponseDto) -> Unit) {
        val b = uiState.booking ?: return
        uiState = uiState.copy(isInitiatingBalancePayment = true)
        viewModelScope.launch {
            try {
                val req = PaymentCheckoutRequestDto(
                    venueId = b.venueId?.toString() ?: "1",
                    sportId = b.sportId.toString(),
                    bookingType = if (b.isPermanent) "Permanent" else "Normal",
                    date = b.playDateStart,
                    slots = listOf(QuoteSlotDto(startTime = "08:00", endTime = "09:00", duration = 60, price = 500.0)),
                    paymentOption = "full"
                )
                val res = bookingApiService.createPaymentCheckout(req)
                if (res.isSuccessful && res.body() != null) {
                    val checkoutResp = res.body()!!
                    uiState = uiState.copy(isInitiatingBalancePayment = false, balanceCheckout = checkoutResp)
                    onSuccess(checkoutResp)
                } else {
                    uiState = uiState.copy(isInitiatingBalancePayment = false, errorMessage = "Failed to start balance payment")
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isInitiatingBalancePayment = false, errorMessage = e.message)
            }
        }
    }

    fun openQRModal() {
        val b = uiState.booking ?: return
        uiState = uiState.copy(showQRModal = true, qrCodeUrl = b.qrCodeURL)
        if (b.qrCodeURL.isNullOrEmpty()) {
            viewModelScope.launch {
                when (val res = bookingRepository.fetchBookingQRCode(b.bookingId)) {
                    is ApiResult.Success -> {
                        uiState = uiState.copy(qrCodeUrl = res.data)
                    }
                    else -> {}
                }
            }
        }
    }

    fun dismissQRModal() {
        uiState = uiState.copy(showQRModal = false)
    }

    private fun mapApiBookingToDomain(api: APIBooking): Booking {
        return Booking(
            id = api.id,
            complexName = api.venueName ?: api.complexName ?: api.venue ?: "Venue",
            sport = api.sportName ?: api.sport ?: "Sport",
            courtName = api.court ?: "Court 1",
            teamName = api.teamInfo?.name ?: "Personal",
            memberCount = api.teamInfo?.membersCount ?: 0,
            teamId = api.teamInfo?.id,
            playDateStart = api.date ?: api.bookingDate ?: "",
            playDateEnd = api.date ?: api.bookingDate ?: "",
            timeSlot = api.time ?: "${api.startTime ?: ""} - ${api.endTime ?: ""}",
            duration = api.duration ?: "60 mins",
            location = api.location ?: "",
            price = "LKR ${api.price ?: "0.00"}",
            slotCount = 1,
            bookingId = api.id,
            bookedDate = api.bookedDate ?: api.bookedAt ?: "",
            status = api.status ?: "Confirmed",
            isPermanent = api.isPermanent == true,
            permanentSourceId = api.permanentSourceId,
            imageURL = api.image ?: "",
            qrCode = api.qrCode != null,
            qrCodeURL = null,
            venueId = api.venueId,
            sportId = api.sportId ?: 1,
            reviewId = api.reviewId,
            reviewRating = api.reviewRating,
            isChallengeBooking = api.isChallengeBooking == true,
            opponentTeamName = api.opponentTeamInfo?.name,
            opponentMemberCount = api.opponentTeamInfo?.membersCount,
            userId = api.userId,
            canCancel = api.canCancel == true,
            createdAt = api.createdAt ?: api.bookedAt ?: ""
        )
    }

    private fun parseTeamsJson(jsonElement: JsonElement): List<Pair<Int, String>> {
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

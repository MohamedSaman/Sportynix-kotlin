package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingDetailDisplayModel(
    val id: String,
    val venueName: String = "Sportynix Complex",
    val venueImage: String = "",
    val sportName: String = "Football",
    val status: String = "Confirmed",
    val bookingReference: String = "SPN-2026-88412",
    val date: String = "2026-07-29",
    val timeSlot: String = "07:00 AM - 08:00 AM",
    val totalPrice: Double = 800.0,
    val amountPaid: Double = 400.0,
    val balanceDue: Double = 400.0,
    val isPermanent: Boolean = false
)

data class BookingState(
    val quoteResponse: QuoteResponseDto? = null,
    val selectedBookingDetail: BookingDetailDisplayModel? = null,
    val userBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    var state by mutableStateOf(BookingState())
        private set

    fun loadVenueAndSlots(venueId: String, sportId: String, date: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = false)
        }
    }

    fun fetchQuote(
        venueId: String,
        sportId: String,
        date: String,
        slotIds: String,
        bookingType: String,
        selectedDays: String,
        paymentOption: String
    ) {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val slots = slotIds.split(",").map {
                QuoteSlotDto(startTime = "07:00", endTime = "08:00", duration = 60, price = 400.0)
            }
            val req = QuoteRequestDto(
                venueId = venueId,
                sportId = sportId,
                bookingType = bookingType,
                date = date,
                selectedDays = selectedDays.split(",").filter { it.isNotEmpty() },
                slots = slots,
                paymentOption = paymentOption
            )
            when (val res = bookingRepository.getQuote(req)) {
                is ApiResult.Success -> {
                    state = state.copy(quoteResponse = res.data, isLoading = false)
                }
                else -> {
                    state = state.copy(isLoading = false)
                }
            }
        }
    }

    fun confirmBookingOrCheckout(
        venueId: String,
        sportId: String,
        date: String,
        slotIds: String,
        bookingType: String,
        selectedDays: String,
        paymentOption: String,
        onPaymentCheckoutReady: (checkoutUrl: String, orderId: String, amount: Double) -> Unit,
        onDirectConfirmationReady: () -> Unit
    ) {
        viewModelScope.launch {
            val slots = slotIds.split(",").map {
                CreateBookingSlotDto(startTime = "07:00", endTime = "08:00", duration = 60, price = 400.0)
            }
            val daysList = selectedDays.split(",").filter { it.isNotEmpty() }

            val checkoutReq = PaymentCheckoutRequestDto(
                venueId = venueId,
                sportId = sportId,
                bookingType = bookingType,
                date = date,
                selectedDays = daysList,
                slots = slots,
                paymentOption = paymentOption
            )

            when (val res = bookingRepository.createPaymentCheckout(checkoutReq)) {
                is ApiResult.Success -> {
                    val url = res.data.checkout?.url ?: "https://api.sportynix.com"
                    val orderId = res.data.payment?.orderId ?: "ORD-88412"
                    val amt = res.data.payment?.amount ?: 400.0
                    onPaymentCheckoutReady(url, orderId, amt)
                }
                else -> {
                    val createReq = CreateBookingRequestDto(
                        venueId = venueId,
                        sportId = sportId,
                        bookingType = bookingType,
                        date = date,
                        selectedDays = daysList,
                        slots = slots,
                        paymentOption = paymentOption
                    )
                    bookingRepository.createBooking(createReq)
                    onDirectConfirmationReady()
                }
            }
        }
    }

    fun pollPaymentStatus(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = bookingRepository.getPaymentStatus(orderId)) {
                is ApiResult.Success -> {
                    val st = (res.data.paymentStatus ?: res.data.gatewayState ?: "").lowercase()
                    if (st in listOf("succeeded", "confirmed", "success", "completed")) {
                        bookingRepository.clearPendingPaymentSession()
                        onSuccess()
                    } else if (st in listOf("failed", "cancelled", "canceled", "declined")) {
                        onFailure("Payment was declined or cancelled. Please try again.")
                    } else {
                        onFailure("Verifying payment status with bank gateway...")
                    }
                }
                else -> {
                    onFailure("Verifying payment status...")
                }
            }
        }
    }

    fun loadBookingDetail(bookingId: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            when (val res = bookingRepository.getBookingDetail(bookingId)) {
                is ApiResult.Success -> {
                    val b = res.data
                    state = state.copy(
                        selectedBookingDetail = BookingDetailDisplayModel(
                            id = b.id,
                            venueName = b.venueName,
                            venueImage = b.venueImage ?: "",
                            sportName = b.sportName ?: "Football",
                            status = b.status.name,
                            bookingReference = b.bookingReference ?: "SPN-${b.id}",
                            date = b.date,
                            timeSlot = b.time,
                            totalPrice = b.price,
                            amountPaid = b.price * 0.5,
                            balanceDue = b.price * 0.5,
                            isPermanent = b.isPermanent
                        ),
                        isLoading = false
                    )
                }
                else -> {
                    state = state.copy(
                        selectedBookingDetail = BookingDetailDisplayModel(id = bookingId),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cancelBooking(
        bookingId: String,
        isSeries: Boolean,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            when (bookingRepository.cancelBooking(bookingId, reason = "User cancelled", isSeries = isSeries)) {
                is ApiResult.Success -> onSuccess()
                else -> onSuccess()
            }
        }
    }

    fun loadUserBookings() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            when (val res = bookingRepository.fetchUserBookings()) {
                is ApiResult.Success -> state = state.copy(userBookings = res.data, isLoading = false)
                else -> state = state.copy(isLoading = false)
            }
        }
    }
}

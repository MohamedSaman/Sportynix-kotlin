package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.PaymentSessionManager
import com.sportynix.app.data.local.PendingBookingPaymentSession
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BookingSummaryUiState(
    val payload: BookingPayload? = null,
    val userName: String = "N/A",
    val userEmail: String = "N/A",
    val userPhone: String = "N/A",
    val quote: QuoteResponseDto? = null,
    val savedCards: List<SavedCardDto> = emptyList(),
    val selectedSavedCard: SavedCardDto? = null,
    val useNewCard: Boolean = true,
    val saveCardForFuture: Boolean = false,
    val selectedPaymentOption: String = "advance", // "advance" or "full"
    val redeemPoints: Boolean = false,
    val pointsToRedeem: Int = 0,
    val isLoadingQuote: Boolean = false,
    val isSubmittingBooking: Boolean = false,
    val checkoutResult: PaymentCheckoutResponseDto? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class BookingSummaryViewModel @Inject constructor(
    private val bookingApiService: BookingApiService,
    private val paymentSessionManager: PaymentSessionManager,
    private val gson: Gson,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var state by mutableStateOf(BookingSummaryUiState())
        private set

    fun initSummary(payload: BookingPayload) {
        if (state.payload == payload) return
        state = state.copy(payload = payload)
        loadUserDetails()
        fetchPaymentQuote()
        fetchSavedCards()
    }

    private fun loadUserDetails() {
        fun applyUser(user: com.sportynix.app.data.remote.dto.UserDataDto?) {
            val name = user?.fullName?.takeIf { it.isNotBlank() }
                ?: listOfNotNull(user?.firstName, user?.lastName).joinToString(" ").trim().takeIf { it.isNotBlank() }
                ?: "N/A"
            state = state.copy(
                userName = name,
                userEmail = user?.email?.takeIf { it.isNotBlank() } ?: "N/A",
                userPhone = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: "N/A"
            )
        }
        val cached = profileRepository.currentUser.value
        if (cached != null) {
            applyUser(cached)
        } else {
            viewModelScope.launch {
                profileRepository.fetchProfile().onSuccess(::applyUser)
            }
        }
    }

    fun setPaymentOption(option: String) {
        if (state.selectedPaymentOption == option) return
        state = state.copy(selectedPaymentOption = option)
        fetchPaymentQuote()
    }

    fun setRedeemPoints(redeem: Boolean, points: Int = 0) {
        state = state.copy(redeemPoints = redeem, pointsToRedeem = if (redeem) points else 0)
        fetchPaymentQuote()
    }

    fun setSelectedSavedCard(card: SavedCardDto?) {
        state = state.copy(selectedSavedCard = card, useNewCard = card == null)
    }

    fun setSaveCardForFuture(save: Boolean) {
        state = state.copy(saveCardForFuture = save)
    }

    fun fetchPaymentQuote() {
        val payload = state.payload ?: return
        viewModelScope.launch {
            state = state.copy(isLoadingQuote = true, errorMessage = null)
            try {
                val slotDtos = payload.slots.map { slot ->
                    QuoteSlotDto(
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        duration = slot.duration,
                        price = slot.price
                    )
                }
                val req = QuoteRequestDto(
                    venueId = payload.venueId.toString(),
                    sportId = payload.sportId.toString(),
                    bookingType = payload.bookingType,
                    date = payload.bookingDate,
                    selectedDays = payload.selectedDays,
                    slots = slotDtos,
                    paymentOption = state.selectedPaymentOption,
                    pointsRedeemed = if (state.redeemPoints) state.pointsToRedeem else 0
                )
                val res = bookingApiService.getPaymentQuote(req)
                if (res.isSuccessful && res.body() != null) {
                    val quote = res.body()!!
                    val serverOption = quote.paymentOption
                        ?.takeIf { it == "advance" || it == "full" }
                    state = state.copy(
                        quote = quote,
                        selectedPaymentOption = serverOption ?: state.selectedPaymentOption,
                        isLoadingQuote = false
                    )
                } else {
                    state = state.copy(isLoadingQuote = false, errorMessage = "Failed to fetch payment quote")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching payment quote")
                state = state.copy(isLoadingQuote = false)
            }
        }
    }

    private fun fetchSavedCards() {
        viewModelScope.launch {
            try {
                val res = bookingApiService.getSavedCards()
                if (res.isSuccessful && res.body() != null) {
                    state = state.copy(savedCards = res.body()!!)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching saved cards")
            }
        }
    }

    fun confirmBooking(onSuccess: (PaymentCheckoutResponseDto) -> Unit) {
        val payload = state.payload ?: return
        if (state.isSubmittingBooking || state.checkoutResult != null) return
        viewModelScope.launch {
            state = state.copy(isSubmittingBooking = true, errorMessage = null)
            try {
                val slotDtos = payload.slots.map { slot ->
                    QuoteSlotDto(
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        duration = slot.duration,
                        price = slot.price
                    )
                }
                val req = PaymentCheckoutRequestDto(
                    venueId = payload.venueId.toString(),
                    sportId = payload.sportId.toString(),
                    bookingType = payload.bookingType,
                    date = payload.bookingDate,
                    selectedDays = payload.selectedDays,
                    slots = slotDtos,
                    userName = state.userName.takeIf { it != "N/A" },
                    userEmail = state.userEmail.takeIf { it != "N/A" },
                    userNumber = state.userPhone.takeIf { it != "N/A" },
                    paymentOption = state.selectedPaymentOption,
                    savedCardId = state.selectedSavedCard?.id,
                    saveCard = state.saveCardForFuture,
                    pointsToRedeem = if (state.redeemPoints) state.pointsToRedeem else 0
                )

                val res = bookingApiService.createPaymentCheckout(req)
                if (res.isSuccessful && res.body() != null) {
                    val checkoutResp = res.body()!!
                    state = state.copy(isSubmittingBooking = false, checkoutResult = checkoutResp)

                    // Persist pending payment session for recovery
                    val session = PendingBookingPaymentSession(
                        bookingData = payload,
                        bookingType = payload.bookingType,
                        payment = checkoutResp.payment,
                        checkout = checkoutResp.checkout,
                        bookings = checkoutResp.bookings,
                        reservationExpiresAt = checkoutResp.reservationExpiresAt,
                        checkoutOpenedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                    )
                    paymentSessionManager.savePendingSession(session)

                    onSuccess(checkoutResp)
                } else {
                    state = state.copy(isSubmittingBooking = false, errorMessage = "Checkout creation failed. Please try again.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating payment checkout")
                state = state.copy(isSubmittingBooking = false, errorMessage = e.message ?: "Network error during checkout")
            }
        }
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }
}

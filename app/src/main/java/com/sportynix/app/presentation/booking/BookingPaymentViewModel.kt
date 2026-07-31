package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.local.PaymentSessionManager
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.data.remote.dto.PaymentStatusResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class PaymentStep { READY, WAITING, VERIFYING, SUCCESS, FAILED, EXPIRED }

@HiltViewModel
class BookingPaymentViewModel @Inject constructor(
    private val bookingApiService: BookingApiService,
    private val paymentSessionManager: PaymentSessionManager
) : ViewModel() {

    var step by mutableStateOf(PaymentStep.READY)
        private set

    var statusMessage by mutableStateOf("You will continue to secure card payment.")
        private set

    var verifiedBookings by mutableStateOf<List<ConfirmedBookingDto>>(emptyList())
        private set

    fun pollStatus(orderId: String, onSuccess: (List<ConfirmedBookingDto>) -> Unit) {
        if (orderId.isEmpty()) return
        viewModelScope.launch {
            step = PaymentStep.VERIFYING
            statusMessage = "Verifying online card payment..."
            var attempts = 0
            while (attempts < 15) {
                try {
                    val res = bookingApiService.getPaymentStatus(orderId)
                    if (res.isSuccessful && res.body() != null) {
                        val body = res.body()!!
                        val pStatus = (body.paymentStatus ?: body.gatewayState ?: "").lowercase()
                        if (listOf("succeeded", "confirmed", "success", "completed").contains(pStatus)) {
                            step = PaymentStep.SUCCESS
                            statusMessage = "Your payment was verified and booking is confirmed!"
                            val list = body.confirmationBookings ?: emptyList()
                            verifiedBookings = list
                            paymentSessionManager.clearPendingSession()
                            onSuccess(list)
                            return@launch
                        } else if (listOf("failed", "cancelled", "canceled", "declined").contains(pStatus)) {
                            step = PaymentStep.FAILED
                            statusMessage = "Payment was cancelled or declined. Please try again."
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error polling payment status")
                }
                attempts++
                delay(3000)
            }
            step = PaymentStep.FAILED
            statusMessage = "Payment verification timed out. Please check your booking history."
        }
    }
}

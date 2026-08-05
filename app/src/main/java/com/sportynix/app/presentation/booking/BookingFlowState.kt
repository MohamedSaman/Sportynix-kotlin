package com.sportynix.app.presentation.booking

import com.sportynix.app.data.remote.dto.BookingPayload
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto

/**
 * Process-local hand-off for the booking graph. The API payloads are intentionally not flattened
 * into route strings: doing so loses prices, display times, venue metadata and confirmed QR values.
 * PaymentSessionManager remains the durable recovery mechanism when Android recreates the process.
 */
object BookingFlowState {
    var payload: BookingPayload? = null
    var checkout: PaymentCheckoutResponseDto? = null
    var confirmedBookings: List<ConfirmedBookingDto> = emptyList()
    var bookingType: String = "Normal"

    fun clear() {
        payload = null
        checkout = null
        confirmedBookings = emptyList()
        bookingType = "Normal"
    }
}

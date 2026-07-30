package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.Booking
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getBookingsStream(): Flow<List<Booking>>
    suspend fun fetchUserBookings(bookingType: String? = null, status: String? = null): ApiResult<List<Booking>>
    suspend fun getBookingDetail(bookingId: String): ApiResult<Booking>
    suspend fun getQuote(request: QuoteRequestDto): ApiResult<QuoteResponseDto>
    suspend fun createBooking(request: CreateBookingRequestDto): ApiResult<List<ConfirmedBookingDto>>
    suspend fun createSimpleBooking(venueId: String, slotId: String, date: String): ApiResult<Booking>
    suspend fun createPaymentCheckout(request: PaymentCheckoutRequestDto): ApiResult<PaymentCheckoutResponseDto>
    suspend fun getPaymentStatus(orderId: String): ApiResult<PaymentStatusResponseDto>
    suspend fun cancelBooking(bookingId: String, reason: String = "User cancelled", isSeries: Boolean = false): ApiResult<Unit>
    suspend fun assignTeam(bookingId: String, teamId: Int): ApiResult<Unit>
    suspend fun savePendingPaymentSession(sessionJson: String)
    suspend fun getPendingPaymentSession(): String?
    suspend fun clearPendingPaymentSession()
}

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

    // ── Live Swift Equivalent Methods ──
    suspend fun fetchAvailableSlots(sportId: Int, venueId: Int, date: String): ApiResult<List<SlotData>>
    suspend fun fetchPermanentAvailability(sportId: Int, selectedDays: List<Int>): ApiResult<Map<String, PermanentSlotAvailability>>
    suspend fun holdSlot(sportId: Int, date: String, startTime: String, endTime: String, isPermanent: Boolean = false, selectedDays: List<Int> = emptyList()): ApiResult<Unit>
    suspend fun releaseSlot(sportId: Int, date: String, startTime: String, endTime: String, isPermanent: Boolean = false, selectedDays: List<Int> = emptyList()): ApiResult<Unit>
    suspend fun convertHoldsToBookings(sportId: Int, bookingType: String, slots: List<Map<String, Any>>, selectedDays: List<String>): ApiResult<Unit>
    suspend fun createBooking(payload: BookingPayload, userName: String, userEmail: String, userPhone: String): ApiResult<List<ConfirmedBookingData>>
    suspend fun fetchBookings(): ApiResult<List<Booking>>
    suspend fun fetchBookingDetails(id: Int): ApiResult<Booking>
    suspend fun fetchBookingQRCode(id: Int): ApiResult<String>
    suspend fun cancelBookingInt(id: Int): ApiResult<Unit>
    suspend fun cancelSeriesInt(id: Int): ApiResult<Unit>
    suspend fun assignTeamInt(id: Int, teamId: Int): ApiResult<Unit>
    suspend fun removeTeamInt(id: Int): ApiResult<Unit>
    suspend fun fetchSportsForVenue(venueId: Int): ApiResult<List<VenueSportDto>>
    suspend fun fetchMyTeams(): ApiResult<List<BookingTeamData>>
}

package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Booking
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getBookingsStream(): Flow<List<Booking>>
    suspend fun fetchUserBookings(): ApiResult<List<Booking>>
    suspend fun createBooking(venueId: String, slotId: String, bookingDate: String): ApiResult<Booking>
    suspend fun cancelBooking(bookingId: String): ApiResult<Unit>
}

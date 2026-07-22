package com.sportynix.app.data.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.local.dao.BookingDao
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.mapper.toEntity
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.CreateBookingRequestDto
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val apiService: BookingApiService,
    private val bookingDao: BookingDao
) : BookingRepository {

    override fun getBookingsStream(): Flow<List<Booking>> {
        return bookingDao.getAllBookings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchUserBookings(): ApiResult<List<Booking>> {
        return try {
            val response = apiService.getUserBookings()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                bookingDao.insertBookings(dtos.map { it.toEntity() })
                ApiResult.Success(dtos.map { it.toDomain() })
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Failed to fetch bookings")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Network error fetching bookings")
        }
    }

    override suspend fun createBooking(
        venueId: String,
        slotId: String,
        bookingDate: String
    ): ApiResult<Booking> {
        return try {
            val response = apiService.createBooking(CreateBookingRequestDto(venueId, slotId, bookingDate))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                bookingDao.insertBooking(dto.toEntity())
                ApiResult.Success(dto.toDomain())
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Failed to create booking")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Booking creation error")
        }
    }

    override suspend fun cancelBooking(bookingId: String): ApiResult<Unit> {
        return try {
            val response = apiService.cancelBooking(bookingId)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.ServerError(response.code(), response.message() ?: "Cancellation failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.localizedMessage ?: "Failed to cancel booking")
        }
    }
}

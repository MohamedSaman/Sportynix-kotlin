package com.sportynix.app.domain.usecase.booking

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserBookingsUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    fun getStream(): Flow<List<Booking>> = repository.getBookingsStream()

    suspend fun refresh(): ApiResult<List<Booking>> = repository.fetchUserBookings()
}

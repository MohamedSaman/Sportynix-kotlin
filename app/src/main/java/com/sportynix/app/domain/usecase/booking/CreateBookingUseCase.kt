package com.sportynix.app.domain.usecase.booking

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.repository.BookingRepository
import javax.inject.Inject

class CreateBookingUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(venueId: String, slotId: String, date: String): ApiResult<Booking> {
        if (venueId.isBlank() || slotId.isBlank() || date.isBlank()) {
            return ApiResult.Error(message = "Invalid booking details selected")
        }
        return repository.createBooking(venueId, slotId, date)
    }
}

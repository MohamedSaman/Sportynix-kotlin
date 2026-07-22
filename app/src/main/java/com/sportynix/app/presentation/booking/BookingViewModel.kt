package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.usecase.booking.CreateBookingUseCase
import com.sportynix.app.domain.usecase.booking.GetUserBookingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val userBookings: List<Booking> = emptyList(),
    val currentBooking: Booking? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class BookingUiEffect {
    data class NavigateToPayment(val bookingId: String, val amount: Double) : BookingUiEffect()
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase
) : ViewModel() {

    var state by mutableStateOf(BookingUiState())
        private set

    private val _effect = MutableSharedFlow<BookingUiEffect>()
    val effect = _effect.asSharedFlow()

    fun confirmBooking(venueId: String, slotId: String, date: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = createBookingUseCase(venueId, slotId, date)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false, currentBooking = result.data)
                    _effect.emit(BookingUiEffect.NavigateToPayment(result.data.id, result.data.totalPrice))
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "Failed to create booking")
                }
            }
        }
    }
}

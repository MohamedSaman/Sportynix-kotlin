package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.dto.APIBooking
import com.sportynix.app.data.remote.dto.CancelBookingRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookingCancellationViewModel @Inject constructor(
    private val bookingApiService: BookingApiService
) : ViewModel() {

    var booking by mutableStateOf<APIBooking?>(null)
        private set
    var isLoadingPolicy by mutableStateOf(true)
        private set
    var isCancelling by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadBooking(bookingId: Int) {
        viewModelScope.launch {
            isLoadingPolicy = true
            try {
                val res = bookingApiService.getBookingDetail(bookingId)
                if (res.isSuccessful && res.body() != null) {
                    booking = res.body()!!
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching booking policy")
            } finally {
                isLoadingPolicy = false
            }
        }
    }

    fun cancelBooking(bookingId: Int, isSeries: Boolean, onSuccess: () -> Unit) {
        if (isCancelling) return
        viewModelScope.launch {
            isCancelling = true
            errorMessage = null
            try {
                val req = CancelBookingRequestDto(reason = "User cancelled from cancellation review screen.")
                val res = if (isSeries && booking?.permanentSourceId != null) {
                    bookingApiService.cancelSeries(bookingId, req)
                } else {
                    bookingApiService.cancelBooking(bookingId, req)
                }

                if (res.isSuccessful) {
                    isCancelling = false
                    onSuccess()
                } else {
                    isCancelling = false
                    errorMessage = "Failed to cancel booking. Please try again."
                }
            } catch (e: Exception) {
                isCancelling = false
                errorMessage = e.message ?: "Network error during cancellation"
            }
        }
    }
}

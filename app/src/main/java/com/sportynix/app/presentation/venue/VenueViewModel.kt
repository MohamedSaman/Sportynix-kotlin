package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VenueUiState(
    val venue: Venue? = null,
    val selectedSlot: TimeSlot? = null,
    val selectedDate: String = "2026-07-23",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VenueViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state by mutableStateOf(VenueUiState())
        private set

    val venueId: String? = savedStateHandle.get<String>("venueId")

    init {
        venueId?.let { loadVenueDetails(it) }
    }

    fun loadVenueDetails(id: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = venueRepository.getVenueById(id)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false, venue = result.data)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    state = state.copy(isLoading = false)
                }
            }
        }
    }

    fun selectSlot(slot: TimeSlot) {
        state = state.copy(selectedSlot = slot)
    }

    fun selectDate(date: String) {
        state = state.copy(selectedDate = date)
    }
}

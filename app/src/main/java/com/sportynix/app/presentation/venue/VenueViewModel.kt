package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VenueTab { SPORTS, GALLERY, INFO, EVENTS }

data class VenueSportItem(
    val id: String,
    val name: String,
    val price: Double,
    val rating: Float = 0.0f,
    val reviewCount: Int = 0,
    val imageUrl: String = ""
)

data class VenueEventItem(
    val id: String,
    val name: String,
    val type: String, // League or Tournament
    val status: String,
    val startDate: String? = null
)

data class VenueUiState(
    val venue: Venue? = null,
    val activeTab: VenueTab = VenueTab.SPORTS,
    val isFavorited: Boolean = false,
    val sportsList: List<VenueSportItem> = emptyList(),
    val eventsList: List<VenueEventItem> = emptyList(),
    val selectedSlot: TimeSlot? = null,
    val selectedDate: String = "2026-07-23",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VenueViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    private val userApiService: UserApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state by mutableStateOf(VenueUiState())
        private set

    val venueId: String? = savedStateHandle.get<String>("venueId")

    init {
        venueId?.let { loadVenueDetails(it) }
    }

    fun setTab(tab: VenueTab) {
        state = state.copy(activeTab = tab)
    }

    fun loadVenueDetails(id: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = venueRepository.getVenueById(id)) {
                is ApiResult.Success -> {
                    val venueData = result.data
                    // Generate sports breakdown matching React Native / backend contract
                    val mockSports = listOf(
                        VenueSportItem(
                            id = "sport_badminton",
                            name = "Badminton",
                            price = 400.0,
                            rating = 5.0f,
                            reviewCount = 2,
                            imageUrl = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=600&q=80"
                        ),
                        VenueSportItem(
                            id = "sport_football",
                            name = "Football",
                            price = 2500.0,
                            rating = 4.8f,
                            reviewCount = 5,
                            imageUrl = "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?auto=format&fit=crop&w=600&q=80"
                        ),
                        VenueSportItem(
                            id = "sport_cricket_football",
                            name = "Cricket & Football",
                            price = 2000.0,
                            rating = 4.5f,
                            reviewCount = 1,
                            imageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=600&q=80"
                        )
                    )

                    val mockEvents = listOf(
                        VenueEventItem(
                            id = "league_1",
                            name = "WebXKey Masters League",
                            type = "League",
                            status = "LIVE",
                            startDate = "Jul 23, 2026"
                        ),
                        VenueEventItem(
                            id = "tourney_1",
                            name = "Gampaha Indoor Cup",
                            type = "Tournament",
                            status = "Upcoming",
                            startDate = "Aug 10, 2026"
                        )
                    )

                    state = state.copy(
                        isLoading = false,
                        venue = venueData,
                        sportsList = mockSports,
                        eventsList = mockEvents
                    )
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

    fun toggleFavorite() {
        val id = venueId ?: return
        val current = state.isFavorited
        state = state.copy(isFavorited = !current)
        viewModelScope.launch {
            try {
                if (current) {
                    userApiService.removeFavorite(id)
                } else {
                    userApiService.addFavorite(id)
                }
            } catch (_: Exception) {
                // Revert on failure
                state = state.copy(isFavorited = current)
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

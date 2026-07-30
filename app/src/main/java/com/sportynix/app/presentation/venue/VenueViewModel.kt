package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.domain.model.RatingBreakdown
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.model.VenueReview
import com.sportynix.app.domain.model.VenueSport
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class VenueTab { SPORTS, GALLERY, INFO, EVENTS }

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
    val eventFilter: String = "All",
    val isFavorited: Boolean = false,
    val sportsList: List<VenueSport> = emptyList(),
    val eventsList: List<VenueEventItem> = emptyList(),
    val reviewsList: List<VenueReview> = emptyList(),
    val sportReviews: List<VenueReview> = emptyList(),
    val ratingBreakdown: RatingBreakdown = RatingBreakdown(star5 = 2),
    val showAddReviewDialog: Boolean = false,
    val newReviewRating: Int = 5,
    val newReviewComment: String = "",
    val newReviewRecommends: Boolean = true,
    val isSubmittingReview: Boolean = false,
    val selectedSlot: TimeSlot? = null,
    val selectedDate: String = "2026-07-28",
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

    fun setEventFilter(filter: String) {
        state = state.copy(eventFilter = filter)
    }

    fun loadVenueDetails(id: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = venueRepository.getVenueById(id)) {
                is ApiResult.Success -> {
                    val venueData = result.data
                    val defaultSports = if (venueData.sports.isNotEmpty()) {
                        venueData.sports
                    } else {
                        listOf(
                            VenueSport(
                                id = 1,
                                name = "Badminton",
                                price = "400.00",
                                rating = 0.0f,
                                reviewsCount = 0,
                                imageUrl = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=600"
                            ),
                            VenueSport(
                                id = 2,
                                name = "Football",
                                price = "2500.00",
                                rating = 0.0f,
                                reviewsCount = 0,
                                imageUrl = "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=600"
                            ),
                            VenueSport(
                                id = 3,
                                name = "Cricket & Football",
                                price = "2000.00",
                                rating = 0.0f,
                                reviewsCount = 0,
                                imageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=600"
                            )
                        )
                    }

                    val defaultEvents = listOf(
                        VenueEventItem("e1", "123", "League", "Cricket", "31 Jul 2026"),
                        VenueEventItem("e2", "WML", "League", "Cricket", "24 Jul 2026"),
                        VenueEventItem("e3", "WebXKey Masters League", "League", "Cricket", "02 Jul 2026"),
                        VenueEventItem("e4", "Sportynix Premier League", "League", "Cricket", "09 Jun 2026"),
                        VenueEventItem("e5", "Webxkey Premier League", "League", "Cricket", "31 May 2026"),
                        VenueEventItem("e6", "ABC", "League", "Cricket", null)
                    )

                    val defaultReviews = if (venueData.reviewsList.isNotEmpty()) {
                        venueData.reviewsList
                    } else {
                        listOf(
                            VenueReview(
                                id = 101,
                                userName = "Mohamed Saman",
                                createdAt = "Jun 10, 2026",
                                rating = 5.0f,
                                comment = "Good for play",
                                recommends = true
                            ),
                            VenueReview(
                                id = 102,
                                userName = "Mohammed Akmal",
                                createdAt = "Jun 8, 2026",
                                rating = 5.0f,
                                comment = "Good",
                                recommends = true
                            )
                        )
                    }

                    state = state.copy(
                        isLoading = false,
                        venue = venueData,
                        sportsList = defaultSports,
                        eventsList = defaultEvents,
                        reviewsList = defaultReviews,
                        ratingBreakdown = if (venueData.reviewsList.isNotEmpty()) venueData.ratingBreakdown else RatingBreakdown(star5 = 2)
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
                state = state.copy(isFavorited = current)
            }
        }
    }

    fun openAddReviewDialog() {
        state = state.copy(showAddReviewDialog = true)
    }

    fun closeAddReviewDialog() {
        state = state.copy(showAddReviewDialog = false)
    }

    fun updateNewReviewRating(rating: Int) {
        state = state.copy(newReviewRating = rating)
    }

    fun updateNewReviewComment(comment: String) {
        state = state.copy(newReviewComment = comment)
    }

    fun updateNewReviewRecommends(recommends: Boolean) {
        state = state.copy(newReviewRecommends = recommends)
    }

    fun submitReview() {
        val id = venueId ?: return
        if (state.newReviewComment.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isSubmittingReview = true)
            when (venueRepository.submitVenueReview(
                venueId = id,
                rating = state.newReviewRating,
                comment = state.newReviewComment.trim(),
                recommends = state.newReviewRecommends
            )) {
                is ApiResult.Success -> {
                    val added = VenueReview(
                        id = (System.currentTimeMillis() % 10000).toInt(),
                        userName = "You",
                        rating = state.newReviewRating.toFloat(),
                        createdAt = "Just now",
                        comment = state.newReviewComment.trim(),
                        recommends = state.newReviewRecommends
                    )
                    val updated = listOf(added) + state.reviewsList
                    state = state.copy(
                        reviewsList = updated,
                        showAddReviewDialog = false,
                        newReviewComment = "",
                        isSubmittingReview = false
                    )
                }
                else -> {
                    state = state.copy(isSubmittingReview = false)
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

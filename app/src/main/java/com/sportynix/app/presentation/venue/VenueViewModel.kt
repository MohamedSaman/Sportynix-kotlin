package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.repository.ProfileRepository
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
    val type: String,
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
    private val profileRepository: ProfileRepository,
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
                    val defaultSports = if (venueData.sports.isNotEmpty()) venueData.sports else listOf(
                        VenueSport(id = 1, name = "Badminton", price = "Rs 1500/hr", rating = 5.0f, reviewsCount = 2),
                        VenueSport(id = 2, name = "Futsal", price = "Rs 3000/hr", rating = 4.8f, reviewsCount = 1)
                    )
                    val defaultEvents = listOf(
                        VenueEventItem(id = "1", name = "Internal League 2026", type = "League", status = "Upcoming", startDate = "15 Aug 2026"),
                        VenueEventItem(id = "2", name = "Super Smash Tournament", type = "Tournament", status = "Open Registration", startDate = "20 Aug 2026")
                    )
                    val defaultReviews = if (venueData.reviewsList.isNotEmpty()) venueData.reviewsList else listOf(
                        VenueReview(id = 1, userName = "Naveed", userAvatar = null, rating = 5.0f, createdAt = "2 days ago", comment = "Excellent court surface!", recommends = true)
                    )

                    val isFav = checkIsFavorited(id)

                    state = state.copy(
                        venue = venueData,
                        isFavorited = isFav,
                        isLoading = false,
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

    private suspend fun checkIsFavorited(id: String): Boolean {
        val favs = profileRepository.getFavorites().getOrNull()
        return favs?.any { it.venue?.id == id } == true
    }

    fun toggleFavorite() {
        val id = venueId ?: return
        val current = state.isFavorited
        state = state.copy(isFavorited = !current)
        viewModelScope.launch {
            try {
                if (current) {
                    val favs = profileRepository.getFavorites().getOrNull()
                    val favItem = favs?.find { it.venue?.id == id }
                    if (favItem != null) {
                        profileRepository.removeFavorite(favItem.id)
                    }
                } else {
                    profileRepository.addFavoriteVenue(id)
                }
            } catch (_: Exception) {
                state = state.copy(isFavorited = current)
            }
        }
    }

    fun openAddReviewDialog() {
        state = state.copy(showAddReviewDialog = true)
    }

    fun dismissAddReviewDialog() {
        state = state.copy(
            showAddReviewDialog = false,
            newReviewRating = 5,
            newReviewComment = "",
            newReviewRecommends = true
        )
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
        val comment = state.newReviewComment.trim()
        if (comment.isEmpty()) return

        viewModelScope.launch {
            state = state.copy(isSubmittingReview = true)
            val newReview = VenueReview(
                id = (System.currentTimeMillis() % 100000).toInt(),
                userName = "You",
                userAvatar = null,
                rating = state.newReviewRating.toFloat(),
                createdAt = "Just now",
                comment = comment,
                recommends = state.newReviewRecommends
            )
            val updatedList = listOf(newReview) + state.reviewsList
            state = state.copy(
                reviewsList = updatedList,
                isSubmittingReview = false,
                showAddReviewDialog = false,
                newReviewComment = "",
                newReviewRating = 5
            )
        }
    }

    fun selectSlot(slot: TimeSlot) {
        state = state.copy(selectedSlot = slot)
    }

    fun selectDate(date: String) {
        state = state.copy(selectedDate = date)
    }
}

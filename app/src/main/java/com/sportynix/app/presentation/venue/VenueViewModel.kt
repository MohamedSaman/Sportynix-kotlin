package com.sportynix.app.presentation.venue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.remote.api.LeagueApiService
import com.sportynix.app.data.remote.api.TournamentApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

enum class VenueTab { SPORTS, GALLERY, INFO, EVENTS }
enum class VenueEventFilter(val label: String) { ALL("All"), UPCOMING("Upcoming"), ONGOING("Ongoing"), PAST("Past"), VENUE_HOSTED("Venue Hosted") }
enum class VenueEventType { LEAGUE, TOURNAMENT }

data class VenueEventItem(
    val id: String,
    val type: VenueEventType,
    val name: String,
    val sportType: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String,
    val logo: String? = null,
    val banner: String? = null,
    val isVenueHosted: Boolean = false,
    val leaguePayload: LeagueDto? = null,
    val tournamentPayload: TournamentDto? = null
)

data class VenueUiState(
    val venueId: Int = 0,
    val venueData: VenueDto? = null,
    val venue: Venue? = null,
    val selectedTab: VenueTab = VenueTab.SPORTS,
    val isFavorite: Boolean = false,
    val favoriteId: Int? = null,
    val isFavoriteLoading: Boolean = false,
    val isSportFavorite: Boolean = false,
    val sportFavoriteId: Int? = null,
    val isSportFavoriteLoading: Boolean = false,
    val reviews: List<VenueReviewDto> = emptyList(),
    val venueEvents: List<VenueEventItem> = emptyList(),
    val eventsLoading: Boolean = false,
    val eventFilter: VenueEventFilter = VenueEventFilter.ALL,
    val showWriteReviewSheet: Boolean = false,
    val editingReview: VenueReviewDto? = null,
    val showImagePreview: Boolean = false,
    val previewImageUrls: List<String> = emptyList(),
    val previewImageIndex: Int = 0,
    val selectedSlot: TimeSlot? = null,
    val selectedDate: String = "2026-07-31",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VenueViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    private val leagueApiService: LeagueApiService,
    private val tournamentApiService: TournamentApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state by mutableStateOf(VenueUiState())
        private set

    val navVenueId: String? = savedStateHandle.get<String>("venueId")
    val venueId: String? get() = navVenueId ?: state.venueId.takeIf { it != 0 }?.toString()

    init {
        navVenueId?.toIntOrNull()?.let { id ->
            initVenue(id)
        }
    }

    fun initVenue(id: Int) {
        state = state.copy(venueId = id)
        fetchDetails(id)
        checkFavoriteStatus(id)
        fetchVenueEvents(id)
    }

    fun loadVenueDetails(id: String) {
        val intId = id.toIntOrNull() ?: 1
        initVenue(intId)
    }

    fun setTab(tab: VenueTab) {
        state = state.copy(selectedTab = tab)
    }

    fun setEventFilter(filter: VenueEventFilter) {
        state = state.copy(eventFilter = filter)
    }

    fun selectSlot(slot: TimeSlot) {
        state = state.copy(selectedSlot = slot)
    }

    fun selectDate(date: String) {
        state = state.copy(selectedDate = date)
    }

    fun openWriteReviewSheet(review: VenueReviewDto? = null) {
        state = state.copy(showWriteReviewSheet = true, editingReview = review)
    }

    fun dismissWriteReviewSheet() {
        state = state.copy(showWriteReviewSheet = false, editingReview = null)
    }

    fun openImagePreview(urls: List<String>, index: Int) {
        state = state.copy(showImagePreview = true, previewImageUrls = urls, previewImageIndex = index)
    }

    fun dismissImagePreview() {
        state = state.copy(showImagePreview = false)
    }

    fun fetchDetails(id: Int = state.venueId) {
        if (id == 0) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = venueRepository.fetchVenueDetailsDto(id)) {
                is ApiResult.Success -> {
                    val dto = result.data
                    state = state.copy(
                        venueData = dto,
                        venue = dto.toDomain(),
                        isLoading = false
                    )
                    fetchReviews(id)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> state = state.copy(isLoading = false)
            }
        }
    }

    fun fetchReviews(id: Int = state.venueId) {
        if (id == 0) return
        viewModelScope.launch {
            when (val res = venueRepository.fetchVenueReviews(id)) {
                is ApiResult.Success -> {
                    state = state.copy(reviews = res.data)
                }
                else -> {}
            }
        }
    }

    fun toggleFavorite() {
        val id = state.venueId
        if (id == 0 || state.isFavoriteLoading) return

        val wasOn = state.isFavorite
        state = state.copy(isFavorite = !wasOn, isFavoriteLoading = true)

        viewModelScope.launch {
            if (wasOn && state.favoriteId != null) {
                when (venueRepository.removeFavorite(state.favoriteId!!)) {
                    is ApiResult.Success -> {
                        state = state.copy(favoriteId = null, isFavoriteLoading = false)
                    }
                    else -> {
                        state = state.copy(isFavorite = wasOn, isFavoriteLoading = false)
                    }
                }
            } else {
                when (val res = venueRepository.addFavorite("venue", venueId = id)) {
                    is ApiResult.Success -> {
                        state = state.copy(favoriteId = res.data.id, isFavoriteLoading = false)
                    }
                    else -> {
                        state = state.copy(isFavorite = wasOn, isFavoriteLoading = false)
                    }
                }
            }
        }
    }

    fun initSportFavorite(id: Int) {
        if (id == 0) return
        viewModelScope.launch {
            when (val result = venueRepository.checkSportFavorite(id)) {
                is ApiResult.Success -> state = state.copy(isSportFavorite = result.data != null, sportFavoriteId = result.data)
                else -> Unit
            }
        }
    }

    fun toggleSportFavorite(id: Int) {
        if (id == 0 || state.isSportFavoriteLoading) return
        val wasOn = state.isSportFavorite
        val oldId = state.sportFavoriteId
        state = state.copy(isSportFavorite = !wasOn, isSportFavoriteLoading = true)
        viewModelScope.launch {
            val result = if (wasOn && oldId != null) venueRepository.removeFavorite(oldId) else venueRepository.addFavorite("sport", sportId = id)
            when (result) {
                is ApiResult.Success -> state = state.copy(isSportFavoriteLoading = false, sportFavoriteId = if (wasOn) null else (result.data as? FavoriteDto)?.id)
                else -> state = state.copy(isSportFavorite = wasOn, isSportFavoriteLoading = false)
            }
        }
    }

    private fun checkFavoriteStatus(id: Int) {
        viewModelScope.launch {
            when (val res = venueRepository.checkVenueFavorite(id)) {
                is ApiResult.Success -> {
                    res.data?.let { favId ->
                        state = state.copy(isFavorite = true, favoriteId = favId)
                    }
                }
                else -> {}
            }
        }
    }

    fun submitReview(
        rating: Double,
        comment: String,
        wouldRecommend: Boolean,
        categories: List<String>,
        imageFiles: List<File>,
        keepPhotoIds: List<Int>
    ) {
        val id = state.venueId
        if (id == 0) return
        val editingId = state.editingReview?.id

        viewModelScope.launch {
            dismissWriteReviewSheet()
            when (venueRepository.submitVenueReview(
                venueId = id,
                rating = rating,
                comment = comment,
                wouldRecommend = wouldRecommend,
                categories = categories,
                imageFiles = imageFiles,
                reviewId = editingId,
                keepPhotoIds = if (editingId != null) keepPhotoIds else null
            )) {
                is ApiResult.Success -> {
                    fetchDetails(id)
                }
                else -> {}
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        val id = state.venueId
        if (id == 0) return
        viewModelScope.launch {
            when (venueRepository.deleteVenueReview(id, reviewId)) {
                is ApiResult.Success -> {
                    fetchDetails(id)
                }
                else -> {}
            }
        }
    }

    fun fetchVenueEvents(id: Int = state.venueId) {
        if (id == 0) return
        viewModelScope.launch {
            state = state.copy(eventsLoading = true)
            val itemsList = mutableListOf<VenueEventItem>()
            val venueIdStr = id.toString()

            try {
                val leagues = leagueApiService.getLeagues(venueId = venueIdStr)
                leagues.forEach { l ->
                    itemsList.add(
                        VenueEventItem(
                            id = l.id,
                            type = VenueEventType.LEAGUE,
                            name = l.name,
                            sportType = l.sport,
                            startDate = l.startDate,
                            endDate = l.endDate,
                            status = l.status,
                            logo = l.logoUrl,
                            banner = l.bannerUrl,
                            isVenueHosted = false,
                            leaguePayload = l,
                            tournamentPayload = null
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching venue leagues")
            }

            try {
                val tournaments = tournamentApiService.getTournaments(venueId = venueIdStr)
                tournaments.forEach { t ->
                    itemsList.add(
                        VenueEventItem(
                            id = t.id,
                            type = VenueEventType.TOURNAMENT,
                            name = t.title,
                            sportType = t.sport,
                            startDate = t.startDate,
                            endDate = null,
                            status = "Open",
                            logo = null,
                            banner = null,
                            isVenueHosted = false,
                            leaguePayload = null,
                            tournamentPayload = t
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching venue tournaments")
            }

            itemsList.sortByDescending { it.startDate ?: "" }
            state = state.copy(venueEvents = itemsList, eventsLoading = false)
        }
    }
}

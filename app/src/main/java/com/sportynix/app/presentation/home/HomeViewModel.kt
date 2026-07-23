package com.sportynix.app.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.api.AuthApiService
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.domain.usecase.venue.GetVenuesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredVenues: List<Venue> = emptyList(),
    val nearbyVenues: List<Venue> = emptyList(),
    val selectedCategory: String = "ALL",
    val unreadNotificationsCount: Int = 0,
    val unreadMessagesCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getVenuesUseCase: GetVenuesUseCase,
    private val authApiService: AuthApiService
) : ViewModel() {

    var state by mutableStateOf(HomeUiState())
        private set

    init {
        observeVenuesStream()
        refreshVenues()
        fetchUnreadCounts()
    }

    private fun observeVenuesStream() {
        getVenuesUseCase.getStream()
            .onEach { venues ->
                if (venues.isNotEmpty()) {
                    state = state.copy(
                        featuredVenues = venues.filter { it.isFeatured },
                        nearbyVenues = venues
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun fetchUnreadCounts() {
        viewModelScope.launch {
            try {
                val response = authApiService.getUnreadCounts()
                if (response.isSuccessful && response.body() != null) {
                    val counts = response.body()!!
                    state = state.copy(
                        unreadNotificationsCount = counts.notifications,
                        unreadMessagesCount = counts.messages
                    )
                }
            } catch (e: Exception) {
                // Silently handle error for badge counts
            }
        }
    }

    fun refreshVenues(category: String = state.selectedCategory) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, selectedCategory = category)
            val sportTypeParam = if (category == "ALL") null else category
            when (val result = getVenuesUseCase.refresh(sportType = sportTypeParam)) {
                is ApiResult.Success -> {
                    val venues = result.data
                    state = state.copy(
                        isLoading = false,
                        nearbyVenues = venues,
                        featuredVenues = venues.filter { it.isFeatured }
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
}

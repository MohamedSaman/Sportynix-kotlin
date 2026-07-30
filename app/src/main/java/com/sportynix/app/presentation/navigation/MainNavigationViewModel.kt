package com.sportynix.app.presentation.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainNavigationViewModel @Inject constructor() : ViewModel() {

    private val _selectedTab = MutableStateFlow(TabItem.HOME)
    val selectedTab: StateFlow<TabItem> = _selectedTab.asStateFlow()

    private val _shouldFocusSearch = MutableStateFlow(false)
    val shouldFocusSearch: StateFlow<Boolean> = _shouldFocusSearch.asStateFlow()

    private val _pendingBookingDetailId = MutableStateFlow<Int?>(null)
    val pendingBookingDetailId: StateFlow<Int?> = _pendingBookingDetailId.asStateFlow()

    private val _pendingPlayerProfileId = MutableStateFlow<String?>(null)
    val pendingPlayerProfileId: StateFlow<String?> = _pendingPlayerProfileId.asStateFlow()

    private val _pendingLeagueDetailId = MutableStateFlow<String?>(null)
    val pendingLeagueDetailId: StateFlow<String?> = _pendingLeagueDetailId.asStateFlow()

    private val _pendingTeamInviteToken = MutableStateFlow<String?>(null)
    val pendingTeamInviteToken: StateFlow<String?> = _pendingTeamInviteToken.asStateFlow()

    fun selectTab(tab: TabItem, focusSearch: Boolean = false) {
        _selectedTab.value = tab
        _shouldFocusSearch.value = focusSearch
    }

    fun clearFocusSearch() {
        _shouldFocusSearch.value = false
    }

    fun setPendingBookingDetail(bookingId: Int?) {
        _pendingBookingDetailId.value = bookingId
        if (bookingId != null) {
            _selectedTab.value = TabItem.BOOKING
        }
    }

    fun clearPendingBookingDetail() {
        _pendingBookingDetailId.value = null
    }

    fun setPendingPlayerProfile(playerId: String?) {
        _pendingPlayerProfileId.value = playerId
    }

    fun clearPendingPlayerProfile() {
        _pendingPlayerProfileId.value = null
    }

    fun setPendingLeagueDetail(leagueId: String?) {
        _pendingLeagueDetailId.value = leagueId
        if (leagueId != null) {
            _selectedTab.value = TabItem.LEAGUE
        }
    }

    fun clearPendingLeagueDetail() {
        _pendingLeagueDetailId.value = null
    }

    fun setPendingTeamInviteToken(token: String?) {
        _pendingTeamInviteToken.value = token
        if (token != null) {
            _selectedTab.value = TabItem.PROFILE
        }
    }

    fun clearPendingTeamInviteToken() {
        _pendingTeamInviteToken.value = null
    }
}

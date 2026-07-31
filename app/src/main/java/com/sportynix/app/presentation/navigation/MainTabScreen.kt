package com.sportynix.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.booking.BookingHistoryScreen
import com.sportynix.app.presentation.home.HomeScreen
import com.sportynix.app.presentation.leagues.LeagueListScreen
import com.sportynix.app.presentation.profile.ProfileScreen
import com.sportynix.app.presentation.search.SearchScreen

@Composable
fun MainTabScreen(
    onNavigateToVenueDetail: (String) -> Unit,
    onNavigateToBookingDetail: (Int) -> Unit,
    onNavigateToNewBooking: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onNavigateToLiveCricket: (String) -> Unit,
    onNavigateToAuction: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPoints: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onNavigateToPaymentMethods: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: MainNavigationViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val shouldFocusSearch by viewModel.shouldFocusSearch.collectAsState()
    val pendingBookingId by viewModel.pendingBookingDetailId.collectAsState()

    val isDark = isSystemInDarkTheme()
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    LaunchedEffect(pendingBookingId) {
        pendingBookingId?.let { bId ->
            onNavigateToBookingDetail(bId)
            viewModel.clearPendingBookingDetail()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgClr)
    ) {
        // Screen Content — fills entire screen, renders behind floating glass bar
        Crossfade(
            targetState = selectedTab,
            label = "MainTabCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                TabItem.LEAGUE -> {
                    LeagueListScreen(
                        onNavigateBack = { viewModel.selectTab(TabItem.HOME) },
                        onNavigateToLeagueDetail = { }
                    )
                }
                TabItem.BOOKING -> {
                    BookingHistoryScreen(
                        onNavigateBack = { viewModel.selectTab(TabItem.HOME) },
                        onNavigateToDetail = { booking -> onNavigateToBookingDetail(booking.bookingId.toString()) },
                        onNavigateToCancel = { booking -> }
                    )
                }
                TabItem.HOME -> {
                    HomeScreen(
                        onNavigateToVenueDetail = onNavigateToVenueDetail,
                        onNavigateToSearch = {
                            viewModel.selectTab(TabItem.SEARCH, focusSearch = true)
                        },
                        onNavigateToProfile = {
                            viewModel.selectTab(TabItem.PROFILE)
                        },
                        onNavigateToNotification = onNavigateToNotification,
                        onNavigateToBookingHistory = {
                            viewModel.selectTab(TabItem.BOOKING)
                        },
                        onNavigateToLeagues = {
                            viewModel.selectTab(TabItem.LEAGUE)
                        },
                        onNavigateToTournaments = onNavigateToTournaments,
                        onNavigateToLiveCricket = onNavigateToLiveCricket,
                        onNavigateToAuction = onNavigateToAuction
                    )
                }
                TabItem.SEARCH -> {
                    SearchScreen(
                        onNavigateBack = { viewModel.selectTab(TabItem.HOME) },
                        onNavigateToVenueDetail = onNavigateToVenueDetail,
                        autoFocusSearch = shouldFocusSearch,
                        onSearchFocused = { viewModel.clearFocusSearch() }
                    )
                }
                TabItem.PROFILE -> {
                    ProfileScreen(
                        onNavigateBack = { viewModel.selectTab(TabItem.HOME) },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToBookingHistory = { viewModel.selectTab(TabItem.BOOKING) },
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToTeam = onNavigateToTeam,
                        onNavigateToPayments = onNavigateToPaymentMethods,
                        onNavigateToPoints = onNavigateToPoints,
                        onNavigateToReferrals = onNavigateToReferral,
                        onNavigateToAboutUs = onNavigateToAbout,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onLogout = onNavigateToSignIn
                    )
                }
            }
        }

        // Floating Liquid-Glass Tab Bar — aligned to bottom, NO solid background box under it
        LiquidTabBar(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                viewModel.selectTab(tab)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 6.dp)
        )
    }
}

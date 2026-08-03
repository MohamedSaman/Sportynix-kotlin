package com.sportynix.app.presentation.home

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Announcement
import com.sportynix.app.domain.model.LiveMatchSnapshot
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.presentation.components.CustomGlassHeader
import com.sportynix.app.presentation.components.GlassBottomNavigation
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.ShimmerSkeleton
import com.sportynix.app.presentation.components.VenueCard
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToVenueDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToBookingHistory: () -> Unit = {},
    onNavigateToLeagues: () -> Unit = {},
    onNavigateToTournaments: () -> Unit = {},
    onNavigateToLiveCricket: (String) -> Unit = {},
    onNavigateToAuction: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val context = LocalContext.current
    var selectedBottomNav by remember { mutableStateOf("home") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            requestDeviceLocation(context, viewModel)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        requestDeviceLocation(context, viewModel)
    }

    val sportsCategories = remember {
        listOf(
            HomeSportCategory("Popular", Icons.Default.LocalFireDepartment),
            HomeSportCategory("Football", Icons.Default.SportsSoccer),
            HomeSportCategory("Cricket", Icons.Default.SportsCricket),
            HomeSportCategory("Basketball", Icons.Default.SportsBasketball),
            HomeSportCategory("Badminton", Icons.Default.SportsTennis)
        )
    }

    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    val filteredFeaturedVenues = remember(state.featuredVenues, state.selectedCategory) {
        if (state.selectedCategory == "Popular") {
            state.featuredVenues
        } else {
            state.featuredVenues.filter {
                it.sportType.equals(state.selectedCategory, ignoreCase = true) ||
                it.sports.any { s -> s.name.equals(state.selectedCategory, ignoreCase = true) }
            }
        }
    }

    val filteredNearbyVenues = remember(state.nearbyVenues, state.selectedCategory) {
        if (state.selectedCategory == "Popular") {
            state.nearbyVenues
        } else {
            state.nearbyVenues.filter {
                it.sportType.equals(state.selectedCategory, ignoreCase = true) ||
                it.sports.any { s -> s.name.equals(state.selectedCategory, ignoreCase = true) }
            }
        }
    }

    val visibleAnnouncements = remember(state.announcements, state.dismissedAnnouncementIds) {
        state.announcements.filterNot { it.id in state.dismissedAnnouncementIds }
    }

    Scaffold(
        topBar = {
            CustomGlassHeader(
                locationText = if (state.userLocation != null) "Nearby You" else "Finding location...",
                onNotificationsPress = onNavigateToNotification,
                onMessagesPress = { },
                unreadNotificationsCount = state.unreadNotificationsCount,
                unreadMessagesCount = state.unreadMessagesCount
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // ── 1. SEARCH BAR & FILTER BUTTON ──
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable(onClick = onNavigateToSearch),
                            shape = RoundedCornerShape(25.dp),
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Search venues, sports...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF1E262C) else Color(0xFFE2E8F0))
                                .clickable(onClick = onNavigateToSearch),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = primaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── 2. SPORTS FILTER CHIPS ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sportsCategories.forEach { category ->
                            val catName = category.name
                            val isSelected = state.selectedCategory == catName
                            val chipBg = if (isSelected) {
                                primaryGreen
                            } else {
                                if (isDark) Color(0xFF1E242B) else Color(0xFFE2E8F0)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(chipBg)
                                    .clickable { viewModel.selectCategory(catName) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = catName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── 3. ANNOUNCEMENTS CAROUSEL ──
                item {
                    if (visibleAnnouncements.isNotEmpty()) Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcements",
                                tint = primaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Announcements",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val pagerState = rememberPagerState(pageCount = { visibleAnnouncements.size })

                        LaunchedEffect(visibleAnnouncements.size) {
                            while (visibleAnnouncements.size > 1) {
                                delay(4000)
                                val nextPage = (pagerState.currentPage + 1) % visibleAnnouncements.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp
                        ) { page ->
                            val announcement = visibleAnnouncements[page]
                            AnnouncementBannerCard(
                                announcement = announcement,
                                onDismiss = { viewModel.dismissAnnouncement(announcement.id) }
                            )
                        }
                    }
                }

                // ── 4. GENERAL VENUES SECTION ──
                item {
                    Column(modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "General Venues",
                                    tint = primaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "General Venues",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "See All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGreen,
                                modifier = Modifier.clickable { onNavigateToSearch() }
                            )
                        }

                        if (state.isLoading && filteredFeaturedVenues.isEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(3) {
                                    ShimmerSkeleton(
                                        modifier = Modifier
                                            .width(270.dp)
                                            .height(230.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        } else if (filteredFeaturedVenues.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No venues available",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredFeaturedVenues, key = { it.id }) { venue ->
                                    VenueCard(
                                        venue = venue,
                                        onClick = { onNavigateToVenueDetail(venue.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 5. NEARBY VENUES SECTION ──
                item {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Nearby Venues",
                                    tint = primaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nearby Venues",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "See All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGreen,
                                modifier = Modifier.clickable { onNavigateToSearch() }
                            )
                        }

                        if (state.isLoadingNearby) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(3) {
                                    ShimmerSkeleton(
                                        modifier = Modifier
                                            .width(270.dp)
                                            .height(230.dp),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        } else if (state.locationPermissionDenied || state.userLocation == null) {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location Required",
                                        tint = primaryGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Discover Nearby Venues",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Enable location permission to find sports venues around you",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enable Location", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (filteredNearbyVenues.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No venues found near your location",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredNearbyVenues, key = { it.id }) { venue ->
                                    VenueCard(
                                        venue = venue,
                                        distanceKm = venue.distance ?: 0.0,
                                        showViewMapButton = true,
                                        onClick = { onNavigateToVenueDetail(venue.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 6. FEATURED OFFERS CAROUSEL ──
                item {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Featured",
                                tint = primaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Featured",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val promoPagerState = rememberPagerState(pageCount = { state.promoBanners.size })

                        LaunchedEffect(state.promoBanners.size) {
                            while (state.promoBanners.size > 1) {
                                delay(4000)
                                val nextPage = (promoPagerState.currentPage + 1) % state.promoBanners.size
                                promoPagerState.animateScrollToPage(nextPage)
                            }
                        }

                        HorizontalPager(
                            state = promoPagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp
                        ) { page ->
                            val banner = state.promoBanners[page]
                            PromoBannerCard(
                                banner = banner,
                                onClick = {
                                    if (banner.navigateTo == "Challenge") {
                                        onNavigateToLeagues()
                                    }
                                }
                            )
                        }

                        // Promo Dots Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(state.promoBanners.size) { index ->
                                val isActive = promoPagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (isActive) 16.dp else 6.dp, 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) primaryGreen
                                            else Color.Gray.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }

                // ── 7. RECENT MATCHES SECTION ──
                item {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sports,
                                    contentDescription = "Recent Matches",
                                    tint = primaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recent Matches",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "See All",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGreen,
                                modifier = Modifier.clickable { onNavigateToLeagues() }
                            )
                        }

                        val matchesList = state.recentMatches

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(matchesList, key = { it.matchId }) { match ->
                                LiveMatchCardItem(
                                    match = match,
                                    onClick = { onNavigateToLiveCricket(match.matchId.toString()) }
                                )
                            }
                        }
                    }
                }

                // ── 8. QUICK ACTIONS SECTION ──
                item {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Quick Actions",
                                tint = primaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Actions",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Action 1: Create Team
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(115.dp)
                                    .clickable { onNavigateToLeagues() },
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = if (isDark) Color(0xFF0F291E) else Color(0xFFE6F4ED),
                                borderColor = primaryGreen.copy(alpha = 0.4f),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = "Create Team",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Create Team",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Build your squad",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Action 2: Challenge
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(115.dp)
                                    .clickable { onNavigateToLeagues() },
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = if (isDark) Color(0xFF0F1E38) else Color(0xFFEBF2FF),
                                borderColor = Color(0xFF2563EB).copy(alpha = 0.4f),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Challenge",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Challenge",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Compete & win",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeSportCategory(val name: String, val icon: ImageVector)

@Composable
private fun AnnouncementBannerCard(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1A8553), Color(0x731A8553))
                    )
                )
        ) {
            if (!announcement.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = announcement.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }

            // Top right Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = announcement.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
                        Text(
                            text = "Sportynix",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Column {
                    Text(
                        text = announcement.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (announcement.subtitle.isNotEmpty()) {
                        Text(
                            text = announcement.subtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Learn More",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    banner: PromoBannerItem,
    onClick: () -> Unit
) {
    val colors = banner.gradientColors.map { Color(it) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors = colors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = banner.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = banner.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = banner.subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Learn More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (banner.iconName) {
                        "trophy" -> Icons.Default.EmojiEvents
                        "newspaper" -> Icons.Default.Newspaper
                        else -> Icons.Default.Stars
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveMatchCardItem(
    match: LiveMatchSnapshot,
    onClick: () -> Unit
) {
    val isLive = match.status.equals("live", ignoreCase = true) || match.status.equals("in_progress", ignoreCase = true)
    val isCompleted = match.status.equals("completed", ignoreCase = true)

    GlassCard(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.leagueName.ifEmpty { match.sourceLabel ?: "League" }}${if (match.matchNumber != null) " • Match ${match.matchNumber}" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isLive -> Color(0x33EF4444)
                                isCompleted -> Color(0x296B7280)
                                else -> Color(0x293B82F6)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when {
                                isLive -> "LIVE"
                                isCompleted -> "COMPLETED"
                                else -> "UPCOMING"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isLive -> Color(0xFFEF4444)
                                isCompleted -> Color(0xFF9CA3AF)
                                else -> Color(0xFF3B82F6)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tags row (League / Cricket Variant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F22C55E))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "League",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E)
                    )
                }

                if (!match.cricketVariant.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1F9CA3AF))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = match.cricketVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Team 1 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!match.team1.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = match.team1.logo,
                            contentDescription = match.team1.name,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D8A4F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.team1.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.team1.name,
                        fontSize = 13.sp,
                        fontWeight = if (match.battingTeamId == match.team1.id) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (match.team1.score.isNotEmpty()) {
                    Text(
                        text = match.team1.score,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Team 2 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!match.team2.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = match.team2.logo,
                            contentDescription = match.team2.name,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = match.team2.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.team2.name,
                        fontSize = 13.sp,
                        fontWeight = if (match.battingTeamId == match.team2.id) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (match.team2.score.isNotEmpty()) {
                    Text(
                        text = match.team2.score,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Message / Result
            val footerText = match.displayMessage ?: match.result ?: match.scheduledDate ?: ""
            if (footerText.isNotEmpty()) {
                Text(
                    text = footerText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF22C55E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun requestDeviceLocation(context: Context, viewModel: HomeViewModel) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            viewModel.onLocationPermissionDenied()
            return
        }
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            viewModel.onLocationPermissionDenied()
            return
        }

        var loc: Location? = null
        if (isGpsEnabled) {
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        if (loc == null && isNetworkEnabled) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (loc != null) {
            viewModel.fetchNearbyVenues(loc.latitude, loc.longitude)
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    viewModel.fetchNearbyVenues(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            if (isNetworkEnabled) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
            } else if (isGpsEnabled) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }
    } catch (e: Exception) {
        viewModel.onLocationPermissionDenied()
    }
}

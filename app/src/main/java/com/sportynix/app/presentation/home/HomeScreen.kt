package com.sportynix.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.presentation.components.CustomGlassHeader
import com.sportynix.app.presentation.components.GlassBottomNavigation
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.ShimmerSkeleton
import com.sportynix.app.presentation.components.VenueCard
import com.sportynix.app.presentation.theme.*

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
    val isDark = isSystemInDarkTheme()
    var showAnnouncement by remember { mutableStateOf(true) }
    var selectedBottomNav by remember { mutableStateOf("home") }

    val sportsCategories = listOf(
        "Popular" to "🔥",
        "Football" to "⚽",
        "Cricket" to "🏏",
        "Basketball" to "🏀",
        "Badminton" to "🏸",
        "Tennis" to "🎾"
    )

    val borderCol = if (isDark) GlassBorderDark else GlassBorderLight

    Scaffold(
        topBar = {
            CustomGlassHeader(
                onNotificationsPress = onNavigateToNotification,
                onMessagesPress = { /* Navigate to Messages */ },
                unreadNotificationsCount = state.unreadNotificationsCount,
                unreadMessagesCount = state.unreadMessagesCount
            )
        },
        bottomBar = {
            GlassBottomNavigation(
                currentRoute = selectedBottomNav,
                onNavigate = { route ->
                    selectedBottomNav = route
                    when (route) {
                        "search" -> onNavigateToSearch()
                        "profile" -> onNavigateToProfile()
                        "history" -> onNavigateToBookingHistory()
                        else -> {}
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── 1. SEARCH BAR & GLASS FILTER BUTTON ──
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(onClick = onNavigateToSearch),
                        shape = RoundedCornerShape(26.dp),
                        elevation = 4.dp
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            placeholder = { Text("Search venues, sports...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = SportynixGreenPrimary
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                            enabled = false,
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    GlassCard(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(onClick = onNavigateToSearch),
                        shape = RoundedCornerShape(16.dp),
                        elevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                    sportsCategories.forEach { (catName, emoji) ->
                        val isSelected = (state.selectedCategory == catName.uppercase()) || (catName == "Popular" && state.selectedCategory == "ALL")
                        val chipBg = if (isSelected) {
                            if (isDark) NeonGreen.copy(alpha = 0.25f) else SportynixGreenPrimary
                        } else {
                            if (isDark) GlassCardDark else GlassCardLight
                        }

                        val chipBorder = if (isSelected) {
                            if (isDark) NeonGreen else SportynixGreenPrimary
                        } else {
                            borderCol
                        }

                        GlassCard(
                            modifier = Modifier.clickable {
                                val catQuery = if (catName == "Popular") "ALL" else catName.uppercase()
                                viewModel.refreshVenues(catQuery)
                            },
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = chipBg,
                            borderColor = chipBorder,
                            elevation = if (isSelected) 6.dp else 2.dp
                        ) {
                            Text(
                                text = "$emoji $catName",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (isDark) NeonGreen else Color.White
                                } else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // ── 3. ANNOUNCEMENTS GLASS CARD BANNER ──
            item {
                AnimatedVisibility(visible = showAnnouncement) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Announcements",
                                tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Announcements",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = 8.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800",
                                    contentDescription = "Announcement Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.88f),
                                                    Color.Black.copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NeonGreen.copy(alpha = 0.25f))
                                                .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "REWARD",
                                                color = NeonGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { showAnnouncement = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Follow Sportynix & Earn 100 Points",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Follow our Facebook page and get 100 Sportynix Points.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Learn More",
                                            color = NeonGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Learn More",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. GENERAL VENUES SECTION ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "General Venues",
                        tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "General Venues",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNavigateToSearch) {
                        Text(
                            text = "See All",
                            color = if (isDark) NeonGreen else SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                if (state.isLoading) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(4) {
                            ShimmerSkeleton(
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(260.dp),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                } else {
                    val venues = if (state.nearbyVenues.isNotEmpty()) state.nearbyVenues else state.featuredVenues
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(venues) { venue ->
                            VenueCard(
                                venue = venue,
                                onClick = { onNavigateToVenueDetail(venue.id) }
                            )
                        }
                    }
                }
            }

            // ── 5. NEARBY VENUES SECTION ──
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Nearby Venues",
                        tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nearby Venues",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNavigateToSearch) {
                        Text(
                            text = "See All",
                            color = if (isDark) NeonGreen else SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                val nearbyVenuesList = if (state.nearbyVenues.isNotEmpty()) state.nearbyVenues else listOf(
                    Venue(
                        id = "v1",
                        name = "Sportynix sport's complex",
                        description = "Indoor sports complex",
                        sportType = "Football",
                        location = "Warana Rd, Kalagedihena, Gampaha",
                        address = "Warana Rd, Kalagedihena",
                        pricePerHour = 2500.0,
                        rating = 5.0f,
                        reviewCount = 12,
                        imageUrls = listOf("https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=500")
                    ),
                    Venue(
                        id = "v2",
                        name = "Royal Arena",
                        description = "Outdoor turf and courts",
                        sportType = "Badminton",
                        location = "Doolmala, Thihariya",
                        address = "Doolmala, Thihariya",
                        pricePerHour = 3000.0,
                        rating = 4.8f,
                        reviewCount = 8,
                        imageUrls = listOf("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500")
                    )
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(nearbyVenuesList.size) { index ->
                        val venue = nearbyVenuesList[index]
                        VenueCard(
                            venue = venue,
                            onClick = { onNavigateToVenueDetail(venue.id) },
                            distanceKm = if (index == 0) 0.0 else 0.5,
                            showViewMapButton = true,
                            onViewMapClick = { onNavigateToVenueDetail(venue.id) }
                        )
                    }
                }
            }

            // ── 6. FEATURED CAROUSEL BANNERS ──
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Featured",
                        tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Featured",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val pagerState = rememberPagerState(pageCount = { 2 })
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) { page ->
                        val bgGradient = if (page == 0) {
                            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            elevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bgGradient)
                                    .padding(20.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (page == 0) "Challenge Teams" else "New Features",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = if (page == 0) "Compete with other teams and prove your skills!" else "Faster bookings, live scores & exclusive offers.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Learn More",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Learn More",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (page == 0) Icons.Default.EmojiEvents else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pager Indicator Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { index ->
                            val active = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (active) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) (if (isDark) NeonGreen else SportynixGreenPrimary)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            // ── 7. RECENT MATCHES SECTION ──
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = "Recent Matches",
                        tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recent Matches",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { }) {
                        Text(
                            text = "See All",
                            color = if (isDark) NeonGreen else SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .width(310.dp)
                                .clickable { },
                            shape = RoundedCornerShape(24.dp),
                            elevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WebXKey Masters Lea... • Match 10",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // LIVE Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33EF4444))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "• LIVE",
                                            color = Color(0xFFEF4444),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SportynixGreenPrimary.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("League", color = SportynixGreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Softball", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Team 1 Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(SportynixGreenPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("K", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "KeyMaster Tit...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "164/3 (10.0)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Team 2 Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("C", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Cipher Breakers",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "73/9 (6.1)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = borderCol.copy(alpha = 0.3f))

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Cipher Breakers need 92 runs from 23 balls",
                                    fontSize = 12.sp,
                                    color = SportynixGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── 8. QUICK ACTIONS SECTION ──
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Quick Actions",
                        tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Create Team Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { },
                        shape = RoundedCornerShape(22.dp),
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SportynixGreenPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Create Team",
                                    tint = SportynixGreenPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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

                    // Challenge Card
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { },
                        shape = RoundedCornerShape(22.dp),
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Challenge",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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

package com.sportynix.app.presentation.venue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.OpeningHourEntryDto
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.data.remote.dto.VenueSportDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import com.sportynix.app.presentation.venue.components.ReviewCard
import com.sportynix.app.presentation.venue.components.ReviewImagePreviewModal
import com.sportynix.app.presentation.venue.components.WriteReviewBottomSheet
import java.util.*

@Composable
fun VenueDetailScreen(
    venueId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSportDetail: (venueId: String, sportId: String) -> Unit,
    onNavigateToBooking: (venueId: String, sportId: String, sportName: String, sportPrice: String, sportImageURL: String) -> Unit,
    onNavigateToMap: (venueId: String, lat: Double, lng: Double, name: String, location: String, rating: Int, image: String) -> Unit,
    onNavigateToLeagueDetail: (String) -> Unit = {},
    onNavigateToTournamentDetail: (String) -> Unit = {},
    viewModel: VenueViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)

    val intVenueId = remember(venueId) { venueId.toIntOrNull() ?: 1 }

    LaunchedEffect(intVenueId) {
        if (state.venueId != intVenueId) {
            viewModel.initVenue(intVenueId)
        }
    }

    val venue = state.venueData ?: VenueDto(
        id = venueId,
        name = "Loading Venue...",
        description = "Loading venue details...",
        sportType = null,
        location = "",
        address = "",
        county = null,
        postalCode = null,
        contactNumber = null,
        emailAddress = null,
        website = null,
        pricePerHour = null,
        rating = 5.0f,
        reviewCount = 0,
        imageUrl = null,
        imageUrlSecure = null,
        imageUrlsList = null,
        galleryImagesList = null,
        availableSlots = null,
        amenities = null,
        terms = null,
        isFeatured = null,
        distance = null,
        distanceDisplay = null,
        sports = null,
        openingHours = null,
        reviewsList = null,
        ratingBreakdown = null
    )

    val tabs = listOf("Sports", "Gallery", "Info", "Events")

    Scaffold(
        containerColor = bgClr
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. BANNER IMAGE
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = venue.imageUrlSecure ?: venue.imageUrl ?: "",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // 2. VENUE INFO HEADER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                            Text("%.1f".format(venue.rating ?: 0f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("(${venue.reviewCount ?: state.reviews.size} reviews)", fontSize = 13.sp, color = textSecondary)
                        }

                        Text(venue.name ?: "Venue", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
                            Text(
                                text = venue.formattedLocationLine.ifEmpty { venue.address ?: venue.location ?: "" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary
                            )
                        }
                    }
                }

                // 3. TAB BAR
                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            tabs.forEachIndexed { idx, tab ->
                                val tabEnum = VenueTab.values()[idx]
                                val isSelected = state.selectedTab == tabEnum
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setTab(tabEnum) }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) primaryGreen else textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isSelected) primaryGreen else Color.Transparent)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = borderClr)
                    }
                }

                // 4. TAB CONTENT
                item {
                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        when (state.selectedTab) {
                            VenueTab.SPORTS -> SportsTabContent(
                                sports = venue.sports ?: emptyList(),
                                venue = venue,
                                primaryGreen = primaryGreen,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                cardBg = cardBg,
                                borderClr = borderClr,
                                onSportClick = { sId -> onNavigateToSportDetail(venueId, sId.toString()) },
                                onBookNowClick = { sId, name, price, img -> onNavigateToBooking(venueId, sId.toString(), name, price, img) }
                            )
                            VenueTab.GALLERY -> GalleryTabContent(
                                images = venue.galleryImagesList ?: emptyList(),
                                primaryGreen = primaryGreen,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                cardBg = cardBg
                            )
                            VenueTab.INFO -> InfoTabContent(
                                venue = venue,
                                primaryGreen = primaryGreen,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                cardBg = cardBg,
                                borderClr = borderClr,
                                isDark = isDark,
                                onOpenMap = { lat, lng ->
                                    onNavigateToMap(
                                        venueId,
                                        lat,
                                        lng,
                                        venue.name ?: "Venue",
                                        venue.address ?: venue.location ?: "",
                                        (venue.rating ?: 5f).toInt(),
                                        venue.imageUrlSecure ?: venue.imageUrl ?: ""
                                    )
                                }
                            )
                            VenueTab.EVENTS -> EventsTabContent(
                                events = state.venueEvents,
                                isLoading = state.eventsLoading,
                                filter = state.eventFilter,
                                primaryGreen = primaryGreen,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                cardBg = cardBg,
                                borderClr = borderClr,
                                onFilterSelect = { viewModel.setEventFilter(it) },
                                onRefresh = { viewModel.fetchVenueEvents() },
                                onLeagueClick = onNavigateToLeagueDetail,
                                onTournamentClick = onNavigateToTournamentDetail
                            )
                        }
                    }
                }

                // 5. REVIEWS SECTION
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Reviews", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("${state.reviews.size} reviews", fontSize = 13.sp, color = textSecondary)
                            }

                            Button(
                                onClick = { viewModel.openWriteReviewSheet() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                            ) {
                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Review", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Average Rating Summary Bar
                        AverageRatingSummaryBar(
                            reviews = state.reviews,
                            primaryGreen = primaryGreen,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            cardBg = cardBg,
                            borderClr = borderClr
                        )

                        // Review List
                        if (state.reviews.isEmpty()) {
                            Text("No reviews yet. Be the first to review!", fontSize = 14.sp, color = textSecondary, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            state.reviews.forEach { r ->
                                ReviewCard(
                                    review = r,
                                    currentUserId = null,
                                    onEdit = { viewModel.openWriteReviewSheet(r) },
                                    onDelete = { r.id?.let { viewModel.deleteReview(it) } },
                                    onPhotoTap = { urls, idx -> viewModel.openImagePreview(urls, idx) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // FLOATING NAV BUTTONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { viewModel.toggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (state.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // MODALS
            if (state.showWriteReviewSheet) {
                WriteReviewBottomSheet(
                    titleName = venue.name ?: "Venue",
                    existingReview = state.editingReview,
                    onDismiss = { viewModel.dismissWriteReviewSheet() },
                    onSubmit = { r, c, sTags, cTags, files, rec, keepIds ->
                        viewModel.submitReview(r, c, rec, sTags + cTags, files, keepIds)
                    }
                )
            }

            if (state.showImagePreview) {
                ReviewImagePreviewModal(
                    imageUrls = state.previewImageUrls,
                    initialIndex = state.previewImageIndex,
                    onDismiss = { viewModel.dismissImagePreview() }
                )
            }
        }
    }
}

@Composable
private fun SportsTabContent(
    sports: List<VenueSportDto>,
    venue: VenueDto,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    onSportClick: (Int) -> Unit,
    onBookNowClick: (Int, String, String, String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Choose Your Games", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

        if (sports.isEmpty()) {
            Text("No sports available for this venue.", fontSize = 14.sp, color = textSecondary)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.heightIn(max = 2000.dp)
            ) {
                items(sports) { sport ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                    ) {
                        // Sport Image & Price Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { onSportClick(sport.id) }
                        ) {
                            AsyncImage(
                                model = sport.imageSecure ?: sport.image ?: "",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(primaryGreen)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Rs. ${sport.price ?: "0"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Info
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .clickable { onSportClick(sport.id) },
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(sport.name ?: "Sport", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                                Text("%.1f".format(sport.averageRating ?: 5.0f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Text("(${sport.reviewsCount ?: 0} reviews)", fontSize = 11.sp, color = textSecondary)
                            }
                        }

                        // BOOK NOW Button
                        Button(
                            onClick = {
                                onBookNowClick(sport.id, sport.name ?: "Sport", "Rs. ${sport.price ?: "0"}", sport.imageSecure ?: sport.image ?: "")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("BOOK NOW", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTabContent(
    images: List<com.sportynix.app.data.remote.dto.VenueGalleryImageDto>,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Photo Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

        if (images.isEmpty()) {
            Text("No gallery photos available.", fontSize = 14.sp, color = textSecondary)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 1000.dp)
            ) {
                items(images) { img ->
                    AsyncImage(
                        model = img.imageUrlSecure ?: img.imageUrl ?: "",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTabContent(
    venue: VenueDto,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    isDark: Boolean,
    onOpenMap: (Double, Double) -> Unit
) {
    val venueLat = remember(venue) {
        venue.location?.split(",")?.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 7.118318
    }
    val venueLng = remember(venue) {
        venue.location?.split(",")?.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 80.079777
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // About
        venue.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(desc, fontSize = 14.sp, color = textSecondary, lineHeight = 20.sp)
            }
        }

        // Contact Info
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Contact Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

            venue.contactNumber?.takeIf { it.isNotBlank() }?.let { infoRow(Icons.Default.Phone, "Phone", it, primaryGreen, textPrimary, textSecondary, cardBg, borderClr) }
            venue.emailAddress?.takeIf { it.isNotBlank() }?.let { infoRow(Icons.Default.Email, "Email", it, primaryGreen, textPrimary, textSecondary, cardBg, borderClr) }
            venue.website?.takeIf { it.isNotBlank() }?.let { infoRow(Icons.Default.Language, "Website", it, primaryGreen, textPrimary, textSecondary, cardBg, borderClr) }
            infoRow(Icons.Default.LocationOn, "Location", venue.formattedLocationLine.ifEmpty { venue.address ?: "" }, primaryGreen, textPrimary, textSecondary, cardBg, borderClr)
        }

        // Opening Hours Table
        venue.openingHours?.takeIf { it.isNotEmpty() }?.let { hours ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Opening Hours", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, borderClr, RoundedCornerShape(12.dp))
                ) {
                    val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
                    days.forEachIndexed { idx, day ->
                        val entry = hours[day]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day.replaceFirstChar { it.uppercase() }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text(
                                text = entry?.displayString ?: "Closed",
                                fontSize = 14.sp,
                                color = if (entry?.isClosed == true) Color.Red else textSecondary
                            )
                        }
                        if (idx < days.size - 1) HorizontalDivider(color = borderClr)
                    }
                }
            }
        }

        // Amenities
        venue.amenities?.takeIf { it.isNotEmpty() }?.let { amenities ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amenities & Facilities", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(amenities) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryGreen.copy(alpha = 0.08f))
                                .border(1.dp, primaryGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(16.dp))
                            Text(item, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        }
                    }
                }
            }
        }

        // Map Button
        Button(
            onClick = { onOpenMap(venueLat, venueLng) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
        ) {
            Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Map & Directions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun EventsTabContent(
    events: List<VenueEventItem>,
    isLoading: Boolean,
    filter: VenueEventFilter,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    onFilterSelect: (VenueEventFilter) -> Unit,
    onRefresh: () -> Unit,
    onLeagueClick: (String) -> Unit,
    onTournamentClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Events at This Venue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = primaryGreen)
            }
        }

        // Filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(VenueEventFilter.values()) { f ->
                val isSelected = filter == f
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) primaryGreen else cardBg)
                        .border(1.dp, if (isSelected) Color.Transparent else borderClr, CircleShape)
                        .clickable { onFilterSelect(f) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(f.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textSecondary)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else if (events.isEmpty()) {
            Text("No events found at this venue.", fontSize = 14.sp, color = textSecondary, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(1.dp, borderClr, RoundedCornerShape(14.dp))
                            .clickable {
                                if (event.type == VenueEventType.LEAGUE) onLeagueClick(event.id)
                                else onTournamentClick(event.id)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (event.type == VenueEventType.LEAGUE) primaryGreen.copy(alpha = 0.12f) else Color.Blue.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (event.type == VenueEventType.LEAGUE) Icons.Default.Shield else Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (event.type == VenueEventType.LEAGUE) primaryGreen else Color.Blue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (event.type == VenueEventType.LEAGUE) "LEAGUE" else "TOURNAMENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (event.type == VenueEventType.LEAGUE) primaryGreen else Color.Blue
                                )
                                if (event.isVenueHosted) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(primaryGreen)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Venue Hosted", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            Text(event.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("${event.sportType.replaceFirstChar { it.uppercase() }} · ${event.startDate ?: ""}", fontSize = 12.sp, color = textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AverageRatingSummaryBar(
    reviews: List<com.sportynix.app.data.remote.dto.VenueReviewDto>,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color
) {
    val total = reviews.fold(0.0) { acc, r -> acc + (r.rating ?: 0f) }
    val avg = if (reviews.isNotEmpty()) total / reviews.size else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("%.1f".format(avg), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (star in 1..5) {
                    Icon(
                        imageVector = if (star <= avg.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text("${reviews.size} reviews", fontSize = 11.sp, color = textSecondary)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (5 downTo 1).forEach { star ->
                val count = reviews.count { (it.rating ?: 0f).toInt() == star }
                val ratio = if (reviews.isNotEmpty()) count.toFloat() / reviews.size else 0f
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$star", fontSize = 11.sp, color = textSecondary, modifier = Modifier.width(10.dp))
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(8.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = primaryGreen,
                        trackColor = borderClr
                    )
                    Text("$count", fontSize = 11.sp, color = textSecondary, modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
private fun infoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(primaryGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(18.dp))
        }

        Column {
            Text(title, fontSize = 12.sp, color = textSecondary)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        }
    }
}

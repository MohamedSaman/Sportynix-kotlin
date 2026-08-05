package com.sportynix.app.presentation.venue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.VenueReviewDto
import com.sportynix.app.domain.repository.VenueRepository
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import com.sportynix.app.presentation.venue.components.ReviewCard
import com.sportynix.app.presentation.venue.components.ReviewImagePreviewModal
import com.sportynix.app.presentation.venue.components.WriteReviewBottomSheet
import kotlinx.coroutines.launch

@Composable
fun SportDetailScreen(
    sportId: String,
    venueId: String,
    onNavigateBack: () -> Unit,
    onNavigateToBooking: (venueId: String, sportId: String, sportName: String, sportPrice: String, sportImageURL: String) -> Unit,
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

    val intSportId = remember(sportId) { sportId.toIntOrNull() ?: 1 }
    val intVenueId = remember(venueId) { venueId.toIntOrNull() ?: 1 }

    val scope = rememberCoroutineScope()
    val venueRepository = viewModel.let {
        // Access repository through ViewModel flow or direct state
        remember { it }
    }

    LaunchedEffect(intVenueId) {
        viewModel.initVenue(intVenueId)
    }

    val matchedSport = remember(state.venueData, intSportId) {
        state.venueData?.sports?.firstOrNull { it.id == intSportId }
    }

    val sportName = matchedSport?.name ?: "Sport"
    val sportPriceStr = matchedSport?.price ?: "Rs. 500"
    val sportImage = matchedSport?.imageSecure ?: matchedSport?.image ?: state.venueData?.imageUrlSecure ?: state.venueData?.imageUrl ?: ""
    val venueName = state.venueData?.name ?: "Sportynix Complex"
    val venueLocation = state.venueData?.formattedLocationLine?.ifEmpty { state.venueData?.address ?: state.venueData?.location ?: "" } ?: ""
    val venueRating = (state.venueData?.rating ?: 5.0f).toDouble()
    val venueReviewCount = state.venueData?.reviewCount ?: 0

    // Sport Reviews State
    var sportReviews by remember { mutableStateOf<List<VenueReviewDto>>(emptyList()) }
    var isLoadingReviews by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var favoriteId by remember { mutableStateOf<Int?>(null) }
    var showWriteReviewSheet by remember { mutableStateOf(false) }
    var editingReview by remember { mutableStateOf<VenueReviewDto?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewImageIndex by remember { mutableStateOf(0) }

    fun loadSportReviews() {
        isLoadingReviews = true
        // Fetch via ViewModel / Repository flow
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(bgClr)
            ) {
                Button(
                    onClick = {
                        onNavigateToBooking(venueId, sportId, sportName, sportPriceStr, sportImage)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                ) {
                    Text("Book Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. HERO IMAGE
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = sportImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 2. VENUE INFO SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                        Text("%.1f".format(venueRating), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("($venueReviewCount reviews)", fontSize = 13.sp, color = textSecondary)
                    }

                    Text(venueName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
                        Text(venueLocation, fontSize = 14.sp, color = textSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = borderClr)

                // 3. SPORT DETAIL TITLE
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(sportName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = borderClr)

                // 4. PRICING SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Per Hour", fontSize = 14.sp, color = textSecondary)
                    Text("LKR ${sportPriceStr.replace("Rs. ", "")}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = primaryGreen)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                        Text("%.1f".format(matchedSport?.averageRating ?: 5.0f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("(${matchedSport?.reviewsCount ?: 0} reviews)", fontSize = 13.sp, color = textSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = borderClr)

                // 5. REVIEWS SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reviews (${sportReviews.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    if (sportReviews.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Text("No reviews yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text("Be the first to share your experience!", fontSize = 14.sp, color = textSecondary)
                        }
                    } else {
                        sportReviews.forEach { r ->
                            ReviewCard(
                                review = r,
                                currentUserId = null,
                                onEdit = {
                                    editingReview = r
                                    showWriteReviewSheet = true
                                },
                                onDelete = {
                                    // Delete sport review
                                },
                                onPhotoTap = { urls, idx ->
                                    previewImageUrls = urls
                                    previewImageIndex = idx
                                    showImagePreview = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // FLOATING TOP BUTTONS
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
                        .clickable { isFavorite = !isFavorite },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showImagePreview) {
                ReviewImagePreviewModal(
                    imageUrls = previewImageUrls,
                    initialIndex = previewImageIndex,
                    onDismiss = { showImagePreview = false }
                )
            }
        }
    }
}

package com.sportynix.app.presentation.venue

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.ShimmerSkeleton
import com.sportynix.app.presentation.theme.*

@Composable
fun VenueDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSlotPicker: (String) -> Unit,
    viewModel: VenueViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val venue = state.venue
    val isDark = isSystemInDarkTheme()
    val accentGreen = if (isDark) NeonGreen else SportynixGreenPrimary
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentGreen)
            }
        } else if (venue != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                // ── 1. TOP HERO COVER IMAGE HEADER ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    AsyncImage(
                        model = venue.imageUrl.ifEmpty { venue.imageUrls.firstOrNull() },
                        contentDescription = venue.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // Back button (Top Left)
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Heart Favorite button (Top Right)
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = if (state.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (state.isFavorited) Color.Red else Color.White
                        )
                    }
                }

                // ── 2. VENUE HEADER INFORMATION ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${venue.rating}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${venue.reviewCount} reviews)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = venue.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (venue.address.isNotEmpty()) venue.address else "Warana Rd, Kalagedihena, Gampaha, Sri Lanka",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── 3. GLASS TABS (Sports, Gallery, Info, Events) ──
                TabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        if (state.activeTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[state.activeTab.ordinal]),
                                color = accentGreen,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(color = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.06f))
                    }
                ) {
                    VenueTab.entries.forEach { tab ->
                        Tab(
                            selected = state.activeTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (state.activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (state.activeTab == tab) accentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 4. TAB CONTENTS ──
                when (state.activeTab) {
                    VenueTab.SPORTS -> {
                        SportsTabContent(
                            sportsList = state.sportsList,
                            accentGreen = accentGreen,
                            isDark = isDark,
                            onBookSport = { onNavigateToSlotPicker(venue.id) }
                        )
                    }
                    VenueTab.INFO -> {
                        InfoTabContent(
                            venue = venue,
                            accentGreen = accentGreen,
                            isDark = isDark,
                            context = context
                        )
                    }
                    VenueTab.GALLERY -> {
                        GalleryTabContent(
                            images = if (venue.imageUrls.isNotEmpty()) venue.imageUrls else listOf(venue.imageUrl),
                            accentGreen = accentGreen
                        )
                    }
                    VenueTab.EVENTS -> {
                        EventsTabContent(
                            events = state.eventsList,
                            accentGreen = accentGreen,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

// ── SPORTS TAB CONTENT (Choose Your Games) ──
@Composable
private fun SportsTabContent(
    sportsList: List<VenueSportItem>,
    accentGreen: Color,
    isDark: Boolean,
    onBookSport: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Choose Your Games",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sportsList.take(2).forEach { sport ->
                Box(modifier = Modifier.weight(1f)) {
                    SportCard(
                        sport = sport,
                        accentGreen = accentGreen,
                        isDark = isDark,
                        onBookNow = onBookSport
                    )
                }
            }
        }

        if (sportsList.size > 2) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sportsList.drop(2).forEach { sport ->
                    Box(modifier = Modifier.weight(1f)) {
                        SportCard(
                            sport = sport,
                            accentGreen = accentGreen,
                            isDark = isDark,
                            onBookNow = onBookSport
                        )
                    }
                }
                if (sportsList.size % 2 != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SportCard(
    sport: VenueSportItem,
    accentGreen: Color,
    isDark: Boolean,
    onBookNow: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Column {
            // Sport Image with Price Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                AsyncImage(
                    model = sport.imageUrl,
                    contentDescription = sport.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Top Green Price Tag Pill
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentGreen)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Rs. ${sport.price.toInt()}.00",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = sport.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${sport.rating} (${sport.reviewCount} reviews)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onBookNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BOOK NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(0.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── INFO TAB CONTENT (Contact info & Opening hours) ──
@Composable
private fun InfoTabContent(
    venue: com.sportynix.app.domain.model.Venue,
    accentGreen: Color,
    isDark: Boolean,
    context: android.content.Context
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // About Section
        Column {
            Text(
                text = "About",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = venue.description.ifEmpty { "Modern indoor sports complex with multi-purpose courts and training facilities." },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }

        // Contact Information Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Contact Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ContactInfoCard(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = "0112345678",
                accentGreen = accentGreen,
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0112345678"))
                    context.startActivity(intent)
                }
            )
            ContactInfoCard(
                icon = Icons.Default.Email,
                label = "Email",
                value = "kanzul@test.com",
                accentGreen = accentGreen,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kanzul@test.com"))
                    context.startActivity(intent)
                }
            )
            ContactInfoCard(
                icon = Icons.Default.Language,
                label = "Website",
                value = "https://example.com",
                accentGreen = accentGreen,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
                    context.startActivity(intent)
                }
            )
            ContactInfoCard(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = if (venue.address.isNotEmpty()) venue.address else "Warana Rd, Kalagedihena, Gampaha, Western, Sri Lanka 00300",
                accentGreen = accentGreen,
                onClick = {}
            )
            ContactInfoCard(
                icon = Icons.Default.Business,
                label = "Country",
                value = "Sri Lanka",
                accentGreen = accentGreen,
                onClick = {}
            )
            ContactInfoCard(
                icon = Icons.Default.MarkunreadMailbox,
                label = "Postal Code",
                value = "00300",
                accentGreen = accentGreen,
                onClick = {}
            )
        }

        // Opening Hours Section
        Column {
            Text(
                text = "Opening Hours",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Monday" to "05:00 - 23:00",
                        "Tuesday" to "06:00 - 23:00",
                        "Wednesday" to "06:00 - 21:00",
                        "Thursday" to "06:00 - 23:00",
                        "Friday" to "06:00 - 23:00",
                        "Saturday" to "05:00 - 23:00",
                        "Sunday" to "05:00 - 23:00"
                    ).forEach { (day, hours) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(hours, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentGreen: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accentGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
            }
        }
    }
}

// ── GALLERY TAB CONTENT ──
@Composable
private fun GalleryTabContent(images: List<String>, accentGreen: Color) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Venue Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        images.chunked(2).forEach { rowImages ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowImages.forEach { img ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(model = img, contentDescription = "Gallery Image", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                if (rowImages.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── EVENTS TAB CONTENT ──
@Composable
private fun EventsTabContent(events: List<VenueEventItem>, accentGreen: Color, isDark: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hosted Events & Leagues", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        events.forEach { event ->
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = accentGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(event.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${event.type} • ${event.startDate ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (event.status == "LIVE") Color.Red.copy(0.2f) else accentGreen.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(event.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (event.status == "LIVE") Color.Red else accentGreen)
                    }
                }
            }
        }
    }
}

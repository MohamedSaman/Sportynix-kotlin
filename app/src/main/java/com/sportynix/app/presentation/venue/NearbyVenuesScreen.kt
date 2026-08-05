package com.sportynix.app.presentation.venue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.util.*

@Composable
fun NearbyVenuesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVenueDetail: (String) -> Unit,
    viewModel: NearbyVenuesViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)

    fun fetchUserLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.let { lm ->
                val isGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNet = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                var loc: Location? = null
                if (isGps) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc == null && isNet) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                loc?.let {
                    viewModel.updateUserLocation(it.latitude, it.longitude)
                } ?: run {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(l: Location) {
                            viewModel.updateUserLocation(l.latitude, l.longitude)
                            lm.removeUpdates(this)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                    try {
                        if (isNet) lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
                        else if (isGps) lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchUserLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fetchUserLocation()
        }
        viewModel.reloadDiscover()
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "All Complexes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Title
            item {
                Text(
                    text = "Find Your Perfect Venue Here",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Tabs Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabButton(
                        label = "Nearby",
                        isSelected = state.activeTab == NearbyTab.NEARBY,
                        primaryGreen = primaryGreen,
                        textSecondary = textSecondary,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(NearbyTab.NEARBY) }
                    )
                    TabButton(
                        label = "Search",
                        isSelected = state.activeTab == NearbyTab.SEARCH,
                        primaryGreen = primaryGreen,
                        textSecondary = textSecondary,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setTab(NearbyTab.SEARCH) }
                    )
                }
            }

            // Search Mode Controls
            if (state.activeTab == NearbyTab.SEARCH) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search venues, city, or address", fontSize = 14.sp, color = textSecondary) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(imageVector = Icons.Default.Cancel, contentDescription = "Clear", tint = textSecondary)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryGreen,
                                    unfocusedBorderColor = borderClr
                                )
                            )

                            // Filter Button
                            val filterCount = (if (state.selectedSport != "all") 1 else 0) + (if (state.selectedCategory != "all") 1 else 0)
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primaryGreen)
                                        .clickable { viewModel.openFiltersModal() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(22.dp))
                                }

                                if (filterCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$filterCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Active Filter Chips
                        if (state.searchQuery.isNotEmpty() || state.selectedSport != "all" || state.selectedCategory != "all") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (state.searchQuery.isNotEmpty()) {
                                    item { ActiveChip(state.searchQuery, primaryGreen = primaryGreen) }
                                }
                                if (state.selectedSport != "all") {
                                    item { ActiveChip(state.selectedSport.replaceFirstChar { it.uppercase() }, primaryGreen = primaryGreen) }
                                }
                                if (state.selectedCategory != "all") {
                                    item { ActiveChip(state.selectedCategory.replaceFirstChar { it.uppercase() }, primaryGreen = primaryGreen) }
                                }
                                item {
                                    TextButton(onClick = { viewModel.clearFilters() }) {
                                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                                    }
                                }
                            }
                        }

                        // Recent Searches History
                        if (state.searchHistory.isNotEmpty() && state.searchQuery.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Recent searches", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                        Text("Clear history", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                                    }
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.searchHistory) { item ->
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .border(1.dp, primaryGreen.copy(alpha = 0.35f), CircleShape)
                                                .clickable { viewModel.submitSearch(item) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(item, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = primaryGreen.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { viewModel.removeSearchHistoryItem(item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.isInlineLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = primaryGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Updating results...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                            }
                        }
                    }
                }
            }

            // Loading / Empty / List Section
            if (state.isLoading && state.venues.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryGreen)
                    }
                }
            } else if (state.venues.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("No venues available", fontSize = 15.sp, color = textSecondary)
                        Button(
                            onClick = { viewModel.reloadDiscover() },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Refresh", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                items(state.venues) { venue ->
                    VenueCardItem(
                        venue = venue,
                        userLocation = state.userLocation,
                        primaryGreen = primaryGreen,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        borderClr = borderClr,
                        onClick = { onNavigateToVenueDetail(venue.id) }
                    )
                }

                if (state.hasNext) {
                    item {
                        LaunchedEffect(Unit) {
                            viewModel.loadMore()
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryGreen, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Filter Modal Sheet
        if (state.showFiltersModal) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissFiltersModal() },
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Filter Nearby Venues", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    // Venue Type
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Venue Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("all", "indoor", "outdoor").forEach { type ->
                                val isSelected = state.draftCategory == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) primaryGreen else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                                        .clickable { viewModel.setDraftCategory(type) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(if (type == "all") "All" else type.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else textPrimary)
                                }
                            }
                        }
                    }

                    // Sport Type
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sport", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        val sportsOptions = listOf("all", "badminton", "cricket", "futsal", "football", "tennis", "basketball", "volleyball")
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(130.dp)
                        ) {
                            items(sportsOptions) { sport ->
                                val isSelected = state.draftSport == sport
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) primaryGreen else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                                        .clickable { viewModel.setDraftSport(sport) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (sport == "all") "All" else sport.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else textPrimary)
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }

                        Button(
                            onClick = { viewModel.applyFilters() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                        ) {
                            Text("Apply", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    isSelected: Boolean,
    primaryGreen: Color,
    textSecondary: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) primaryGreen else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else textSecondary
        )
    }
}

@Composable
private fun ActiveChip(label: String, primaryGreen: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, primaryGreen.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
    }
}

@Composable
private fun VenueCardItem(
    venue: VenueDto,
    userLocation: Pair<Double, Double>?,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    onClick: () -> Unit
) {
    val distanceText = remember(venue, userLocation) {
        computedDistance(venue, userLocation)
    }

    val locationText = remember(venue) {
        if (venue.formattedLocationLine.isNotEmpty()) venue.formattedLocationLine
        else venue.address?.takeIf { it.isNotBlank() } ?: venue.location ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        // Image Section with Distance Badge
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            AsyncImage(
                model = venue.imageUrlSecure ?: venue.imageUrl ?: "",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Distance Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Text(distanceText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Info Section
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rating Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                Text("%.1f".format(venue.rating ?: 0f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("(${venue.reviewCount ?: 0})", fontSize = 14.sp, color = textSecondary)
            }

            // Name
            Text(venue.name ?: "Unknown Venue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

            // Location Address
            Text(locationText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textSecondary)

            // Sport Price Chips
            venue.sports?.takeIf { it.isNotEmpty() }?.let { sports ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    items(sports) { sport ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(primaryGreen.copy(alpha = 0.08f))
                                .border(1.dp, primaryGreen.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = getSportIcon(sport.name ?: ""), contentDescription = null, tint = primaryGreen, modifier = Modifier.size(14.dp))
                            Text("Rs. ${sport.price ?: "0"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                    }
                }
            }

            // Book Now Button
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
            ) {
                Text("Book Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun computedDistance(venue: VenueDto, userLoc: Pair<Double, Double>?): String {
    venue.distance?.let { dist ->
        return if (dist < 1.0) "%.0f m away".format(dist * 1000.0)
        else "%.1f km away".format(dist)
    }

    if (userLoc != null && !venue.location.isNullOrEmpty()) {
        val parts = venue.location.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size >= 2) {
            val lat = parts[0]
            val lng = parts[1]
            val r = 6371000.0
            val dLat = Math.toRadians(lat - userLoc.first)
            val dLon = Math.toRadians(lng - userLoc.second)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(userLoc.first)) * Math.cos(Math.toRadians(lat)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val meters = r * c
            return if (meters < 1000.0) "%.0f m away".format(meters)
            else "%.1f km away".format(meters / 1000.0)
        }
    }
    return "N/A"
}

private fun getSportIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("football") || lower.contains("futsal") || lower.contains("soccer") -> Icons.Default.SportsSoccer
        lower.contains("cricket") -> Icons.Default.SportsCricket
        lower.contains("basketball") -> Icons.Default.SportsBasketball
        lower.contains("tennis") -> Icons.Default.SportsTennis
        lower.contains("volleyball") -> Icons.Default.SportsVolleyball
        else -> Icons.Default.Sports
    }
}

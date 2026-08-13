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
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyVenuesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVenueDetail: (String) -> Unit,
    viewModel: NearbyVenuesViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgClr = if (isDark) DarkBackground else LightBackground

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

    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Complexes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SportynixGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgClr)
            )
        },
        containerColor = bgClr
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Switcher (Nearby vs Search)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabButton(
                    label = "Nearby",
                    isSelected = state.activeTab == NearbyTab.NEARBY,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(NearbyTab.NEARBY) }
                )
                TabButton(
                    label = "Search",
                    isSelected = state.activeTab == NearbyTab.SEARCH,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(NearbyTab.SEARCH) }
                )
            }

            // Search Bar & Filter Trigger (When Search Tab is Active)
            AnimatedVisibility(visible = state.activeTab == NearbyTab.SEARCH) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search venues by name, city...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SportynixGreenPrimary) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportynixGreenPrimary,
                                unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorderLight,
                                focusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
                                unfocusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (state.selectedSport != "all" || state.selectedCategory != "all") SportynixGreenPrimary else (if (isDark) GlassSurfaceDark else GlassSurfaceLight))
                                .border(1.dp, if (isDark) GlassBorderDark else GlassBorderLight, RoundedCornerShape(16.dp))
                                .clickable { viewModel.openFiltersModal() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = if (state.selectedSport != "all" || state.selectedCategory != "all") Color.Black else SportynixGreenPrimary
                            )
                        }
                    }

                    // Search History Pills
                    if (state.searchHistory.isNotEmpty() && state.searchQuery.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.labelMedium, color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                            TextButton(onClick = viewModel::clearSearchHistory) { Text("Clear All", style = MaterialTheme.typography.labelSmall, color = StatusError) }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.searchHistory) { historyItem ->
                                SuggestionChip(
                                    onClick = { viewModel.onSearchQueryChanged(historyItem) },
                                    label = { Text(historyItem, style = MaterialTheme.typography.bodySmall) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content List
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            } else if (state.venues.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No venues found nearby", style = MaterialTheme.typography.titleMedium, color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.venues, key = { it.id }) { venue ->
                        VenueListItemCard(
                            venue = venue,
                            onClick = { onNavigateToVenueDetail(venue.id) }
                        )
                    }

                    if (state.isInlineLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SportynixGreenPrimary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Modal Sheet
    if (state.showFiltersModal) {
        VenueFilterBottomSheet(
            state = state,
            onDismiss = viewModel::dismissFiltersModal,
            onSelectSport = viewModel::setDraftSport,
            onSelectCategory = viewModel::setDraftCategory,
            onApply = viewModel::applyFilters,
            onClear = viewModel::clearFilters
        )
    }
}

@Composable
fun TabButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isSelected) SportynixGreenPrimary else (if (isDark) GlassSurfaceDark else GlassSurfaceLight)
    val textColor = if (isSelected) Color.Black else (if (isDark) TextPrimaryDark else TextPrimaryLight)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (isSelected) SportynixGreenPrimary else (if (isDark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun VenueListItemCard(
    venue: VenueDto,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val paymentBadges = remember(venue) { venue.buildPaymentBadges() }

    AnimatedGlassCard(onClick = onClick) {
        Column {
            // Venue Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1C2A40) else Color(0xFFE2E8F0))
            ) {
                val img = venue.imageUrl ?: venue.imageUrlSecure ?: ""
                if (img.isNotEmpty()) {
                    AsyncImage(
                        model = img,
                        contentDescription = venue.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.SportsBasketball, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(48.dp))
                    }
                }

                // Distance Badge Overlay
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = venue.formattedDistanceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = SportynixGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Venue Name & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = venue.name ?: "Sports Venue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if ((venue.rating ?: 0f) > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", venue.rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                        Text(
                            text = " (${venue.reviewCount ?: 0})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Address / Location
            Text(
                text = venue.formattedLocationLine.ifEmpty { venue.address ?: "Location available" },
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Policy Badges
            if (paymentBadges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paymentBadges.forEach { badge ->
                        val badgeBg = when (badge.tone) {
                            "warning" -> StatusWarning.copy(alpha = 0.15f)
                            "success" -> StatusSuccess.copy(alpha = 0.15f)
                            else -> Color.Gray.copy(alpha = 0.15f)
                        }
                        val badgeTxt = when (badge.tone) {
                            "warning" -> StatusWarning
                            "success" -> StatusSuccess
                            else -> if (isDark) TextSecondaryDark else TextSecondaryLight
                        }

                        SportynixBadge(
                            text = badge.label,
                            backgroundColor = badgeBg,
                            contentColor = badgeTxt
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueFilterBottomSheet(
    state: NearbyVenuesUiState,
    onDismiss: () -> Unit,
    onSelectSport: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    val sportsList = listOf("all", "badminton", "cricket", "futsal", "football", "tennis", "basketball", "volleyball")
    val categories = listOf("all" to "All Categories", "indoor" to "Indoor", "outdoor" to "Outdoor")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Filter Venues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                categories.forEach { (key, label) ->
                    val selected = state.draftCategory == key
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(key) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Sport Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(sportsList) { sport ->
                    val selected = state.draftSport == sport
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSport(sport) },
                        label = { Text(sport.replaceFirstChar { it.uppercase() }) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Clear All")
                }
                SportynixGradientButton(
                    text = "Apply Filters",
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

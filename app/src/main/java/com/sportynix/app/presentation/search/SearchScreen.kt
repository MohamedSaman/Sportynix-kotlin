package com.sportynix.app.presentation.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Venue
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.ShimmerSkeleton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVenueDetail: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = isSystemInDarkTheme()
    val accentGreen = if (isDark) NeonGreen else SportynixGreenPrimary
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val filteredVenues = when (state.activeTab) {
        SearchTab.VENUES -> state.venues
        SearchTab.ALL -> state.venues
        else -> emptyList()
    }
    val filteredTeams = when (state.activeTab) {
        SearchTab.TEAMS -> state.teams
        SearchTab.ALL -> state.teams
        else -> emptyList()
    }
    val totalResults = filteredVenues.size + filteredTeams.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF0D1B2A) else Color.White
                    )
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { focusManager.clearFocus(); onNavigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                        // Glass Search Field
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text("Venues, teams, sports...", fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = accentGreen, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentGreen,
                                unfocusedBorderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.1f),
                                focusedContainerColor = if (isDark) Color.White.copy(0.05f) else Color.White,
                                unfocusedContainerColor = if (isDark) Color.White.copy(0.04f) else Color(0xFFF8F8F8),
                                cursorColor = accentGreen
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Tab Row
                    if (state.query.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = state.activeTab.ordinal,
                            containerColor = Color.Transparent,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (state.activeTab.ordinal < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.activeTab.ordinal]),
                                        color = accentGreen,
                                        height = 3.dp
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            listOf("All", "Venues", "Teams").forEachIndexed { idx, label ->
                                val count = when (idx) {
                                    1 -> state.venues.size
                                    2 -> state.teams.size
                                    else -> totalResults
                                }
                                Tab(
                                    selected = state.activeTab.ordinal == idx,
                                    onClick = { viewModel.setActiveTab(SearchTab.entries[idx]) },
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                label,
                                                fontWeight = if (state.activeTab.ordinal == idx)
                                                    FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = if (state.activeTab.ordinal == idx) accentGreen
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (state.hasSearched && count > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(accentGreen.copy(alpha = if (state.activeTab.ordinal == idx) 0.2f else 0.1f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text("$count", fontSize = 10.sp, color = accentGreen,
                                                        fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.05f))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                // Loading
                state.isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { ShimmerSearchCard() }
                    }
                }

                // Initial / empty query
                state.query.isEmpty() -> {
                    SearchInitialState(accentGreen = accentGreen, isDark = isDark)
                }

                // No results
                state.hasSearched && totalResults == 0 -> {
                    NoSearchResultsState(query = state.query, accentGreen = accentGreen)
                }

                // Results
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Venues section
                        if (filteredVenues.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Venues",
                                    count = filteredVenues.size,
                                    accentGreen = accentGreen
                                )
                            }
                            items(filteredVenues, key = { "venue_${it.id}" }) { venue ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    SearchVenueCard(
                                        venue = venue,
                                        accentGreen = accentGreen,
                                        isDark = isDark,
                                        onTap = { onNavigateToVenueDetail(venue.id) }
                                    )
                                }
                            }
                        }

                        // Teams section
                        if (filteredTeams.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Teams",
                                    count = filteredTeams.size,
                                    accentGreen = accentGreen
                                )
                            }
                            items(filteredTeams, key = { "team_${it.id}" }) { team ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    SearchTeamCard(
                                        team = team,
                                        accentGreen = accentGreen,
                                        isDark = isDark
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

@Composable
private fun SectionHeader(title: String, count: Int, accentGreen: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(8.dp))
        Text("($count)", fontSize = 13.sp, color = accentGreen, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchVenueCard(venue: Venue, accentGreen: Color, isDark: Boolean, onTap: () -> Unit) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clickable { onTap() },
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Image
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            ) {
                AsyncImage(
                    model = venue.imageUrl,
                    contentDescription = venue.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, if (isDark) Color.Black.copy(0.3f) else Color.Transparent)
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(venue.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                if (venue.address.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            tint = accentGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(venue.address, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (venue.rating > 0) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Text(" ${venue.rating}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (venue.pricePerHour > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Rs. ${venue.pricePerHour.toInt()}/hr", fontSize = 11.sp,
                                color = accentGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onTap,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                ) {
                    Text("Book Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SearchTeamCard(team: TeamSearchResult, accentGreen: Color, isDark: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentGreen.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!team.logoUrl.isNullOrEmpty()) {
                    AsyncImage(model = team.logoUrl, contentDescription = team.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Text("⚽", fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                if (!team.sport.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentGreen.copy(0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(team.sport, fontSize = 11.sp, color = accentGreen)
                    }
                }
                if (!team.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Text(" ${team.location}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Group, contentDescription = null,
                    tint = accentGreen, modifier = Modifier.size(16.dp))
                Text("${team.memberCount}", fontSize = 11.sp, color = accentGreen,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchInitialState(accentGreen: Color, isDark: Boolean) {
    val popularSearches = listOf("Football", "Cricket", "Badminton", "Tennis", "Basketball")
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Popular Searches", fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularSearches) { tag ->
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(tag, fontSize = 13.sp) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = accentGreen.copy(0.1f),
                        labelColor = accentGreen
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = accentGreen.copy(0.2f)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SearchOff, contentDescription = null,
                    tint = accentGreen.copy(0.35f), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Search for venues, teams & more",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoSearchResultsState(query: String, accentGreen: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null,
            tint = accentGreen.copy(0.4f), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No results for \"$query\"", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Try a different keyword or sport name",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShimmerSearchCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row {
            ShimmerSkeleton(modifier = Modifier.width(100.dp).fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)))
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp)
                    .clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp)
                    .clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp)
                    .clip(RoundedCornerShape(4.dp)))
            }
        }
    }
}

package com.sportynix.app.presentation.leagues

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.remote.dto.TournamentDto
import com.sportynix.app.domain.model.LiveMatchSnapshot
import com.sportynix.app.domain.model.LiveMatchTeam
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToTournamentDetail: (String) -> Unit = {},
    onNavigateToCreateTournament: () -> Unit = {},
    onNavigateToMatchDetails: (String) -> Unit = {},
    onNavigateToLiveMatchScoring: (String) -> Unit = {},
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF0E1111) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val context = LocalContext.current

    var showLeagueFilterSheet by remember { mutableStateOf(false) }
    var showTournamentFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(bg)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Leagues & Tournaments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                )

                // Spring-Animated Top Tab Bar
                TopTabBar(
                    selectedTab = uiState.activeTab,
                    onTabSelected = viewModel::onTabSelected,
                    isDark = isDark
                )
            }
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.activeTab) {
                0 -> {
                    MatchesTabContent(
                        uiState = uiState,
                        onMatchSportFilterChanged = viewModel::onMatchSportFilterChanged,
                        onMatchStatusFilterChanged = viewModel::onMatchStatusFilterChanged,
                        onNavigateToLiveMatchScoring = onNavigateToLiveMatchScoring,
                        onNavigateToMatchDetails = onNavigateToMatchDetails,
                        isDark = isDark
                    )
                }
                1 -> {
                    LeaguesTabContent(
                        uiState = uiState,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onOpenFilters = { showLeagueFilterSheet = true },
                        onNavigateToDetail = onNavigateToDetail,
                        onDeleteLeague = { leagueId, leagueName ->
                            // Custom long press deletion alert helper
                            viewModel.deleteLeague(
                                leagueId = leagueId,
                                onSuccess = {
                                    Toast.makeText(context, "\"$leagueName\" deleted successfully", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        isDark = isDark,
                        onCreateLeague = onNavigateToCreate
                    )
                }
                2 -> {
                    TournamentsTabContent(
                        uiState = uiState,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onOpenFilters = { showTournamentFilterSheet = true },
                        onNavigateToDetail = onNavigateToTournamentDetail,
                        isDark = isDark,
                        onCreateTournament = onNavigateToCreateTournament
                    )
                }
            }
        }
    }

    if (showLeagueFilterSheet) {
        LeagueFilterSheet(
            selectedSport = uiState.selectedLeagueSport,
            selectedFormat = uiState.selectedLeagueFormat,
            selectedStatus = uiState.selectedLeagueStatus,
            selectedFeatured = uiState.selectedLeagueFeatured,
            selectedSort = uiState.selectedLeagueSort,
            onDismiss = { showLeagueFilterSheet = false },
            onApply = { sport, format, status, featured, sort ->
                viewModel.updateLeagueFilters(sport, format, status, featured, sort)
                showLeagueFilterSheet = false
            },
            isDark = isDark
        )
    }

    if (showTournamentFilterSheet) {
        TournamentFilterSheet(
            selectedStatus = uiState.tournamentStatusFilter,
            selectedVariant = uiState.cricketVariantFilter,
            selectedFormat = uiState.formatFilter,
            selectedApproval = uiState.approvalStatusFilter,
            onDismiss = { showTournamentFilterSheet = false },
            onApply = { status, variant, format, approval ->
                viewModel.updateTournamentFilters(status, variant, format, approval)
                showTournamentFilterSheet = false
            },
            isDark = isDark
        )
    }
}

// MARK: - Animated Top Tab Bar
@Composable
fun TopTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val activeColor = Color(0xFF22C55E)
    val indicatorColor = Color(0xFF22C55E)
    val tabs = listOf("Matches", "Leagues", "Tournaments")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) activeColor else textSecondary
                    )
                }
            }
        }

        // Underline Indicator with Spring Animation
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            val tabWidth = maxWidth / 3
            val paddingOffset = tabWidth * selectedTab
            val animatedOffset by animateDpAsState(
                targetValue = paddingOffset,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "indicatorOffset"
            )

            HorizontalDivider(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(tabWidth)
                    .padding(horizontal = 15.dp) // width - 30 equivalent
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(indicatorColor)
                    .align(Alignment.BottomStart)
            )
        }
    }
}

// MARK: - Section Header Component
@Composable
fun SectionHeader(
    title: String,
    count: Int,
    isDark: Boolean
) {
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.15f else 0.1f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22C55E)
            )
        }
    }
}

// MARK: - Tab 1: Matches
@Composable
fun MatchesTabContent(
    uiState: LeagueListUiState,
    onMatchSportFilterChanged: (String) -> Unit,
    onMatchStatusFilterChanged: (String) -> Unit,
    onNavigateToLiveMatchScoring: (String) -> Unit,
    onNavigateToMatchDetails: (String) -> Unit,
    isDark: Boolean
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight

    val sportFilters = listOf("All", "Cricket", "Football")
    val statusFilters = listOf("Live", "Upcoming", "Completed", "League", "Tournament", "Friendly")

    // Client-side mapping & filter matches
    val clientFilteredMatches = remember(uiState.liveMatches, uiState.selectedSportFilter, uiState.selectedStatusFilter) {
        uiState.liveMatches.filter { match ->
            val matchesSport = if (uiState.selectedSportFilter == "All") {
                true
            } else {
                match.sportType.equals(uiState.selectedSportFilter, ignoreCase = true)
            }

            val matchesStatus = when (uiState.selectedStatusFilter) {
                "Live" -> match.status.lowercase() in listOf("live", "in_progress")
                "Upcoming" -> match.status.lowercase() !in listOf("live", "in_progress", "completed")
                "Completed" -> match.status.lowercase() == "completed"
                "League" -> (match.competitionType ?: "").lowercase().trim() == "league"
                "Tournament" -> (match.competitionType ?: "").lowercase().trim() == "tournament" || match.leagueName.lowercase().contains("tournament")
                "Friendly" -> (match.matchType ?: "").lowercase().trim() == "friendly"
                else -> true
            }

            matchesSport && matchesStatus
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Row 1 Filters (Sport Chips)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sportFilters) { sport ->
                val isSelected = uiState.selectedSportFilter == sport
                val icon = when (sport) {
                    "Cricket" -> Icons.Default.SportsCricket
                    "Football" -> Icons.Default.SportsSoccer
                    else -> null
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onMatchSportFilterChanged(sport) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) Color.White else textPrimary
                                )
                            }
                            Text(sport, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22C55E),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent,
                        labelColor = textPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        selectedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        // Row 2 Filters (Status Chips)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(statusFilters) { status ->
                val isSelected = uiState.selectedStatusFilter == status
                val dotColor = when (status) {
                    "Live" -> Color(0xFFEF4444)
                    "Upcoming" -> Color(0xFF3B82F6)
                    "Completed" -> Color(0xFF6B7280)
                    else -> Color(0xFF22C55E)
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onMatchStatusFilterChanged(status) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (status in listOf("Live", "Upcoming", "Completed")) {
                                if (status == "Live") {
                                    // Pulser indicator
                                    PulsingLiveDot()
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                            Text(status, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22C55E),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent,
                        labelColor = textPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        selectedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        // Content
        if (uiState.isLoading && clientFilteredMatches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF22C55E))
            }
        } else if (clientFilteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = textSecondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matches found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(clientFilteredMatches, key = { it.matchId }) { match ->
                    APIMatchCard(
                        match = match,
                        onClick = {
                            val isLiveOrComp = match.status.lowercase() in listOf("live", "in_progress", "completed")
                            if (isLiveOrComp) {
                                onNavigateToLiveMatchScoring(match.matchId)
                            } else {
                                onNavigateToMatchDetails(match.matchId)
                            }
                        },
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingLiveDot() {
    var isDotVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            isDotVisible = !isDotVisible
        }
    }
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(if (isDotVisible) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.2f))
    )
}

@Composable
fun APIMatchCard(
    match: LiveMatchSnapshot,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val isLive = match.status.lowercase() in listOf("live", "in_progress")
    val isCompleted = match.status.lowercase() == "completed"
    val statusColor = if (isLive) Color(0xFF22C55E) else if (isCompleted) Color(0xFF6B7280) else Color(0xFF3B82F6)
    val statusText = if (isLive) "LIVE" else if (isCompleted) "Completed" else "Upcoming"

    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val bg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val border = if (isDark) Color.White.opacity(0.08f) else Color.Black.opacity(0.06f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // League name & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.leagueName.ifEmpty { "Match" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22C55E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLive) {
                        PulsingLiveDot()
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Match tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val compLabel = (match.competitionType ?: "").replace("_", " ").capitalizeWords()
                val variantLabel = (match.cricketVariant ?: match.sportType).replace("_", " ").capitalizeWords()

                if (compLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(compLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }
                }
                if (variantLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(variantLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }
                }
            }

            HorizontalDivider(color = if (isDark) Color(0xFF2D2D2D) else Color(0xFFEEEEEE))

            // Teams display block
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TeamMatchRow(
                    team = match.team1,
                    isBatting = match.battingTeamId == match.team1.id,
                    scoreText = match.team1.score,
                    textPrimary = textPrimary,
                    isDark = isDark
                )
                TeamMatchRow(
                    team = match.team2,
                    isBatting = match.battingTeamId == match.team2.id,
                    scoreText = match.team2.score,
                    textPrimary = textPrimary,
                    isDark = isDark
                )
            }

            // Summary
            val summary = if (isLive) {
                match.displayMessage ?: match.tossText ?: match.chaseStatus ?: ""
            } else if (isCompleted) {
                val winText = match.winnerName?.trim()
                val marginText = match.margin?.trim() ?: ""
                if (!winText.isNullOrEmpty()) {
                    if (marginText.isNotEmpty()) "$winText Won by $marginText" else "$winText Won"
                } else {
                    match.result ?: match.displayMessage ?: ""
                }
            } else ""

            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) Color(0xFFEF4444) else if (isLive) Color(0xFF06B6D4) else textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            HorizontalDivider(color = if (isDark) Color(0xFF2D2D2D) else Color(0xFFEEEEEE))

            // Compact Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MatchMetaPill(icon = Icons.Default.CalendarToday, text = formatApiDate(match.scheduledDate), isDark = isDark, modifier = Modifier.weight(1f))
                MatchMetaPill(icon = Icons.Default.Schedule, text = formatApiTime(match.scheduledTime), isDark = isDark)
                MatchMetaPill(icon = Icons.Default.Place, text = match.venue?.ifEmpty { "Venue TBA" } ?: "Venue TBA", isDark = isDark, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TeamMatchRow(
    team: LiveMatchTeam,
    isBatting: Boolean,
    scoreText: String,
    textPrimary: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!team.logo.isNullOrEmpty()) {
                    AsyncImage(
                        model = team.logo,
                        contentDescription = team.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = team.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = team.name,
                fontSize = 16.sp,
                fontWeight = if (isBatting) FontWeight.Bold else FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isBatting) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
            }
        }

        if (scoreText.isNotEmpty()) {
            Text(
                text = scoreText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
    }
}

fun formatApiDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "TBA"
    try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        val date = parser.parse(dateStr)
        return date?.let { formatter.format(it).uppercase(java.util.Locale.getDefault()) } ?: dateStr
    } catch (e: Exception) {
        return dateStr
    }
}

fun formatApiTime(timeStr: String?): String {
    if (timeStr.isNullOrEmpty()) return "TBA"
    try {
        val parser = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val time = parser.parse(timeStr)
        return time?.let { formatter.format(it) } ?: timeStr
    } catch (e: Exception) {
        try {
            val parser2 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            val time = parser2.parse(timeStr)
            return time?.let { formatter.format(it) } ?: timeStr
        } catch(e2: Exception) {
            return timeStr
        }
    }
}

@Composable
fun MatchMetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val border = if (isDark) Color.White.opacity(0.15f) else Color.Black.opacity(0.1f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFF1F5F9))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = Color(0xFF22C55E)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MARK: - Tab 2: Leagues
@Composable
fun LeaguesTabContent(
    uiState: LeagueListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onDeleteLeague: (String, String) -> Unit,
    isDark: Boolean,
    onCreateLeague: () -> Unit
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight

    // Search query & Client-side filtering/sorting
    val sortedLeagues = remember(uiState.leagues, uiState.selectedLeagueFeatured, uiState.selectedLeagueSort) {
        var list = uiState.leagues

        if (uiState.selectedLeagueFeatured == "★ Featured Only") {
            list = list.filter { it.isFeatured == true }
        }

        when (uiState.selectedLeagueSort) {
            "Name (A-Z)" -> list = list.sortedBy { it.name }
            "Start Date (Newest)" -> list = list.sortedByDescending { it.startDate }
            "Start Date (Oldest)" -> list = list.sortedBy { it.startDate }
            "Recently Created" -> list = list.sortedByDescending { it.createdAt }
            "Oldest Created" -> list = list.sortedBy { it.createdAt }
            else -> {}
        }
        list
    }

    val myLeagues = remember(sortedLeagues) {
        sortedLeagues.filter { it.isCreator == true || it.isAdmin == true || it.isModerator == true }
    }
    val otherLeagues = remember(sortedLeagues) {
        sortedLeagues.filter { !(it.isCreator == true || it.isAdmin == true || it.isModerator == true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Headers
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = "Available Leagues",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Join an existing league or create your own",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }

        // Search + Filter Trigger
        SearchAndFilterRow(
            query = uiState.searchQuery,
            onQueryChanged = onSearchQueryChanged,
            onFilterClicked = onOpenFilters,
            placeholderText = "Search leagues...",
            isDark = isDark
        )

        // Content
        if (uiState.isLoading && sortedLeagues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF22C55E))
            }
        } else if (sortedLeagues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = textSecondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No leagues found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (myLeagues.isNotEmpty()) {
                    item {
                        SectionHeader(title = "My Leagues", count = myLeagues.size, isDark = isDark)
                    }
                    items(myLeagues, key = { it.id }) { league ->
                        LeagueCardItem(
                            league = league,
                            onClick = { onNavigateToDetail(league.id) },
                            onLongClick = { onDeleteLeague(league.id, league.name) },
                            isDark = isDark
                        )
                    }
                }

                if (otherLeagues.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Available Leagues", count = otherLeagues.size, isDark = isDark)
                    }
                    items(otherLeagues, key = { it.id }) { league ->
                        LeagueCardItem(
                            league = league,
                            onClick = { onNavigateToDetail(league.id) },
                            onLongClick = { onDeleteLeague(league.id, league.name) },
                            isDark = isDark
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreateLeague,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Create League", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeagueCardItem(
    league: FullLeagueDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isDark: Boolean
) {
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val statusColor = when (league.status.lowercase()) {
        "in_progress", "live", "approved" -> Color(0xFF22C55E)
        "upcoming" -> Color(0xFF3B82F6)
        "pending" -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }

    AnimatedGlassCard(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo Image with fallback
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.12f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!league.logo.isNullOrEmpty()) {
                        AsyncImage(
                            model = league.logo,
                            contentDescription = league.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val sportIcon = when (league.sportType.lowercase()) {
                            "football" -> Icons.Default.SportsSoccer
                            else -> Icons.Default.SportsCricket
                        }
                        Icon(
                            imageVector = sportIcon,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = league.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Status badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = league.status.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.12f else 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(league.sportType.capitalizeWords(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                        }
                        if (!league.cricketVariant.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.12f else 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(league.cricketVariant.replace("_", " ").capitalizeWords(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF7C60F6).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(league.format.replace("_", " ").capitalizeWords(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C60F6))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = textSecondary)
                            Text("${league.teamsCount ?: 0} Teams", fontSize = 12.sp, color = textSecondary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(14.dp), tint = textSecondary)
                            Text("${league.totalMatchesPlanned ?: 0} Matches", fontSize = 12.sp, color = textSecondary)
                        }
                        if (!league.startDate.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = textSecondary)
                                Text(league.startDate, fontSize = 12.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }

            // Progress bar
            val completed = league.matchesCompleted ?: 0
            val total = league.totalMatchesPlanned ?: 0
            val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            if (progress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF2D2D2D) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF22C55E))
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}% Completed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

// MARK: - Tab 3: Tournaments
@Composable
fun TournamentsTabContent(
    uiState: LeagueListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    isDark: Boolean,
    onCreateTournament: () -> Unit
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight

    // Client-side filtering
    val clientFilteredTournaments = remember(
        uiState.tournaments,
        uiState.tournamentStatusFilter,
        uiState.cricketVariantFilter,
        uiState.formatFilter,
        uiState.approvalStatusFilter,
        uiState.searchQuery
    ) {
        uiState.tournaments.filter { t ->
            val matchSearch = if (uiState.searchQuery.trim().isEmpty()) {
                true
            } else {
                t.title.contains(uiState.searchQuery, ignoreCase = true)
            }

            val matchStatus = when (uiState.tournamentStatusFilter) {
                "Upcoming" -> true // Since most in list are upcoming/ongoing
                "Ongoing" -> true
                "Completed" -> false
                else -> true
            }

            val matchVariant = if (uiState.cricketVariantFilter == "All") {
                true
            } else {
                t.sport.equals("cricket", ignoreCase = true) // just a variant tag mock check
            }

            val matchFormat = if (uiState.formatFilter == "All") {
                true
            } else {
                t.format.equals(uiState.formatFilter.replace(" ", "_").replace("+", "").lowercase(), ignoreCase = true)
            }

            matchSearch && matchStatus && matchVariant && matchFormat
        }
    }

    val myTournaments = remember(clientFilteredTournaments) {
        clientFilteredTournaments // Mock split since creator ID is not always present in DTO
    }
    val otherTournaments = remember(clientFilteredTournaments) {
        emptyList<TournamentDto>() // Mock split
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Headers
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = "Tournaments",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Explore ongoing and upcoming tournaments",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }

        // Search Row
        SearchAndFilterRow(
            query = uiState.searchQuery,
            onQueryChanged = onSearchQueryChanged,
            onFilterClicked = onOpenFilters,
            placeholderText = "Search tournaments...",
            isDark = isDark
        )

        // Content
        if (uiState.isLoading && clientFilteredTournaments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF22C55E))
            }
        } else if (clientFilteredTournaments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = textSecondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tournaments found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (myTournaments.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Available Tournaments", count = myTournaments.size, isDark = isDark)
                    }
                    items(myTournaments, key = { it.id }) { tournament ->
                        TournamentCardItem(
                            tournament = tournament,
                            onClick = { onNavigateToDetail(tournament.id) },
                            isDark = isDark
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreateTournament,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Create Tournament", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCardItem(
    tournament: TournamentDto,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val statusColor = Color(0xFF3B82F6) // blue for upcoming/ongoing default

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, if (isDark) Color.White.opacity(0.08f) else Color.Black.opacity(0.06f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport Logo Fallback
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.12f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val sportIcon = when (tournament.sport.lowercase()) {
                    "football" -> Icons.Default.SportsSoccer
                    else -> Icons.Default.SportsCricket
                }
                Icon(
                    imageVector = sportIcon,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tournament.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = "Ongoing",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22C55E).copy(alpha = if (isDark) 0.12f else 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(tournament.sport.capitalizeWords(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF7C60F6).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(tournament.format.replace("_", " ").capitalizeWords(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C60F6))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = textSecondary)
                        Text("${tournament.registeredTeamsCount}/${tournament.maxTeams} Teams", fontSize = 12.sp, color = textSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = textSecondary)
                        Text(tournament.startDate, fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}

// MARK: - Search And Filter Row
@Composable
fun SearchAndFilterRow(
    query: String,
    onQueryChanged: (String) -> Unit,
    onFilterClicked: () -> Unit,
    placeholderText: String,
    isDark: Boolean
) {
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val containerBg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
    val border = if (isDark) Color.White.opacity(0.1f) else Color.Black.opacity(0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = { Text(placeholderText, fontSize = 15.sp, color = textSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF22C55E),
                unfocusedBorderColor = border,
                focusedContainerColor = containerBg,
                unfocusedContainerColor = containerBg
            ),
            singleLine = true
        )

        // Filter button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(containerBg)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .clickable(onClick = onFilterClicked),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter Icon",
                tint = textSecondary
            )
        }
    }
}

// MARK: - Filter Sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueFilterSheet(
    selectedSport: String,
    selectedFormat: String,
    selectedStatus: String,
    selectedFeatured: String,
    selectedSort: String,
    onDismiss: () -> Unit,
    onApply: (sport: String, format: String, status: String, featured: String, sort: String) -> Unit,
    isDark: Boolean
) {
    var sport by remember { mutableStateOf(selectedSport) }
    var format by remember { mutableStateOf(selectedFormat) }
    var status by remember { mutableStateOf(selectedStatus) }
    var featured by remember { mutableStateOf(selectedFeatured) }
    var sort by remember { mutableStateOf(selectedSort) }

    val sports = listOf("All Sports", "Cricket", "Football")
    val statuses = listOf("All Statuses", "Draft", "Upcoming", "Registration", "In Progress", "Completed", "Suspended", "Cancelled")
    val formats = listOf("All Formats", "Round Robin", "Knockout", "Group + Knockout", "League + Playoff", "Custom")
    val featureds = listOf("All", "★ Featured Only")
    val sorts = listOf("Default", "Start Date (Newest)", "Start Date (Oldest)", "Recently Created", "Oldest Created", "Name (A-Z)", "Status")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Leagues", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FilterSection("SPORT TYPE", sports, sport) { sport = it }
                FilterSection("STATUS", statuses, status) { status = it }
                FilterSection("FORMAT", formats, format) { format = it }
                FilterSection("FEATURED", featureds, featured) { featured = it }
                FilterSection("SORT BY", sorts, sort) { sort = it }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        sport = "All Sports"
                        format = "All Formats"
                        status = "All Statuses"
                        featured = "All"
                        sort = "Default"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9), contentColor = if (isDark) Color.White else Color.Black)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onApply(sport, format, status, featured, sort) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.White)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentFilterSheet(
    selectedStatus: String,
    selectedVariant: String,
    selectedFormat: String,
    selectedApproval: String,
    onDismiss: () -> Unit,
    onApply: (status: String, variant: String, format: String, approval: String) -> Unit,
    isDark: Boolean
) {
    var status by remember { mutableStateOf(selectedStatus) }
    var variant by remember { mutableStateOf(selectedVariant) }
    var format by remember { mutableStateOf(selectedFormat) }
    var approval by remember { mutableStateOf(selectedApproval) }

    val statuses = listOf("All", "Upcoming", "Ongoing", "Completed")
    val variants = listOf("All", "Softball", "Hardball")
    val formats = listOf("All", "Knockout", "Group + Knockout", "Round Robin", "Swiss", "Custom")
    val approvals = listOf("All", "Approved", "Pending", "Rejected")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Tournaments", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FilterSection("TOURNAMENT STATUS", statuses, status) { status = it }
                FilterSection("CRICKET VARIANT", variants, variant) { variant = it }
                FilterSection("FORMAT", formats, format) { format = it }
                FilterSection("APPROVAL STATUS", approvals, approval) { approval = it }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        status = "All"
                        variant = "All"
                        format = "All"
                        approval = "All"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9), contentColor = if (isDark) Color.White else Color.Black)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onApply(status, variant, format, approval) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.White)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF22C55E) else Color.Gray.copy(alpha = 0.1f))
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

// Helpers
private fun Float.opacity(alpha: Float): Color {
    return Color.Transparent // fallback helper
}

private fun Color.opacity(alpha: Float): Color {
    return this.copy(alpha = alpha)
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

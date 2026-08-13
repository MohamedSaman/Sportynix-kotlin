package com.sportynix.app.presentation.cricket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.PlayerMatchStatDto
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.cricket.components.SportynixEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerId: String,
    playerName: String? = null,
    playerRole: String? = null,
    battingStyle: String? = null,
    bowlingStyle: String? = null,
    profileImage: String? = null,
    viewModel: PlayerProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFullImageOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playerId) {
        viewModel.initializePlayer(
            playerId = playerId,
            name = playerName,
            role = playerRole,
            batting = battingStyle,
            bowling = bowlingStyle,
            image = profileImage
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.playerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share Profile */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.recentMatchStats.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = SportynixEmerald
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card with Avatar
                    item {
                        PlayerHeaderCard(
                            name = uiState.playerName,
                            role = uiState.playerRole ?: "Cricket Player",
                            battingStyle = uiState.battingStyle ?: "Right-hand bat",
                            bowlingStyle = uiState.bowlingStyle ?: "Right-arm medium",
                            imageUrl = uiState.profileImage,
                            onImageClick = { isFullImageOpen = true }
                        )
                    }

                    // Filter Bar
                    item {
                        FilterBarSection(
                            variant = uiState.cricketVariant,
                            context = uiState.contextFilter,
                            venue = uiState.venueCategory,
                            onVariantChange = { viewModel.setCricketVariantFilter(it) },
                            onContextChange = { viewModel.setContextFilter(it) },
                            onVenueChange = { viewModel.setVenueCategoryFilter(it) }
                        )
                    }

                    // Career Stats Grid
                    item {
                        CareerStatsSection(career = uiState.careerStats)
                    }

                    // Recent Matches Header
                    item {
                        Text(
                            text = "RECENT MATCHES",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SportynixEmerald
                        )
                    }

                    // Recent Matches List
                    items(uiState.recentMatchStats) { matchStat ->
                        RecentMatchItemCard(matchStat = matchStat)
                    }

                    // Load More Button
                    if (uiState.hasMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(color = SportynixEmerald)
                                } else {
                                    OutlinedButton(onClick = { viewModel.fetchPlayerStats() }) {
                                        Text("LOAD MORE MATCHES", color = SportynixEmerald)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullImageOpen && !uiState.profileImage.isNullOrBlank()) {
        Dialog(onDismissRequest = { isFullImageOpen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uiState.profileImage,
                    contentDescription = "Full Player Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PlayerHeaderCard(
    name: String,
    role: String,
    battingStyle: String,
    bowlingStyle: String,
    imageUrl: String?,
    onImageClick: () -> Unit
) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SportynixEmerald.copy(alpha = 0.2f))
                    .border(2.dp, SportynixEmerald, CircleShape)
                    .clickable(onClick = onImageClick),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportynixEmerald
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelMedium,
                    color = SportynixEmerald,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$battingStyle | $bowlingStyle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FilterBarSection(
    variant: String,
    context: String,
    venue: String,
    onVariantChange: (String) -> Unit,
    onContextChange: (String) -> Unit,
    onVenueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all", "softball", "hardball").forEach { v ->
                FilterChip(
                    selected = variant == v,
                    onClick = { onVariantChange(v) },
                    label = { Text(v.uppercase()) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all", "league", "tournament").forEach { c ->
                FilterChip(
                    selected = context == c,
                    onClick = { onContextChange(c) },
                    label = { Text(c.uppercase()) }
                )
            }
            listOf("indoor", "outdoor").forEach { ven ->
                FilterChip(
                    selected = venue == ven,
                    onClick = { onVenueChange(ven) },
                    label = { Text(ven.uppercase()) }
                )
            }
        }
    }
}

@Composable
fun CareerStatsSection(career: com.sportynix.app.data.repository.CareerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Batting Career Card
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BATTING CAREER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("Matches", career.batting.matches.toString())
                    StatBox("Runs", career.batting.runs.toString())
                    StatBox("Avg", career.batting.average.toString())
                    StatBox("SR", career.batting.strikeRate.toString())
                    StatBox("HS", career.batting.highest.toString())
                }
            }
        }

        // Bowling Career Card
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BOWLING CAREER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("Wickets", career.bowling.wickets.toString())
                    StatBox("Econ", career.bowling.economy.toString())
                    StatBox("Avg", career.bowling.average.toString())
                    StatBox("Best", career.bowling.bestFigures)
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecentMatchItemCard(matchStat: PlayerMatchStatDto) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = matchStat.match?.matchName ?: "Cricket Match",
                    fontWeight = FontWeight.Bold
                )
                if (matchStat.isManOfMatch) {
                    Text(
                        text = "⭐ MOM",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            matchStat.battingStats?.let { bs ->
                Text(
                    text = "Batting: ${bs.runsScored} runs (${bs.ballsFaced} b) 4s: ${bs.fours} 6s: ${bs.sixes}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            matchStat.bowlingStats?.let { bw ->
                Text(
                    text = "Bowling: ${bw.wickets}/${bw.runsConceded} (${bw.overs} ov)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

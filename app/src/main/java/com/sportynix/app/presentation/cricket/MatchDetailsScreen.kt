package com.sportynix.app.presentation.cricket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.cricket.components.GlassScoreHeaderCard
import com.sportynix.app.presentation.cricket.components.SportynixEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
    matchId: String,
    viewModel: MatchDetailsViewModel,
    onBack: () -> Unit,
    onNavigateToScoring: (String) -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.initializeMatchDetails(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Match Details",
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
                    IconButton(onClick = { viewModel.openEditModal() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Match")
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
            if (uiState.isLoading && uiState.liveState == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = SportynixEmerald
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scorecard Header
                    GlassScoreHeaderCard(
                        liveState = uiState.liveState,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Dynamic Tabs Strip
                    ScrollableTabRow(
                        selectedTabIndex = uiState.availableTabs.indexOf(uiState.selectedTab).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        contentColor = SportynixEmerald,
                        indicator = { tabPositions ->
                            val index = uiState.availableTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
                            if (index < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                    color = SportynixEmerald
                                )
                            }
                        }
                    ) {
                        uiState.availableTabs.forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    Text(
                                        text = tab.name,
                                        fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // Tab Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        when (uiState.selectedTab) {
                            MatchDetailsTab.INFO -> MatchInfoTabContent(
                                liveState = uiState.liveState,
                                onStartScoring = { onNavigateToScoring(matchId) }
                            )
                            MatchDetailsTab.LIVE -> MatchLiveTabContent(liveState = uiState.liveState)
                            MatchDetailsTab.SCORECARD -> ScorecardTabContent(
                                scorecard = uiState.scorecard,
                                onPlayerClick = onPlayerClick
                            )
                            MatchDetailsTab.COMMENTARY -> CommentaryTabContent(commentary = uiState.commentary)
                            MatchDetailsTab.SQUADS -> SquadsTabContent(
                                team1Squad = uiState.team1Squad,
                                team2Squad = uiState.team2Squad,
                                onPlayerClick = onPlayerClick
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditModalOpen) {
        EditMatchModal(
            currentDate = uiState.updatedDate ?: "",
            currentTime = uiState.updatedTime ?: "",
            currentVenue = uiState.updatedVenue ?: "",
            onDismiss = { viewModel.closeEditModal() },
            onSave = { d, t, v -> viewModel.updateMatchInfo(d, t, v) }
        )
    }
}

@Composable
fun MatchInfoTabContent(
    liveState: LiveStateDto?,
    onStartScoring: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MATCH INFORMATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Status:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = liveState?.status?.uppercase() ?: "SCHEDULED", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Toss Decision:", style = MaterialTheme.typography.bodyMedium)
                    Text(text = liveState?.tossDecision ?: "Pending", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        SportynixGradientButton(
            text = "OPEN LIVE SCORING",
            onClick = onStartScoring,
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MatchLiveTabContent(liveState: LiveStateDto?) {
    val activeInning = if (liveState?.currentInnings == 2) liveState.inning2 else liveState?.inning1
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActivePlayersCard(
            striker = activeInning?.striker,
            nonStriker = activeInning?.nonStriker,
            bowler = activeInning?.bowler
        )
    }
}

@Composable
fun ScorecardTabContent(
    scorecard: ScorecardDto?,
    onPlayerClick: (String) -> Unit
) {
    if (scorecard == null || scorecard.innings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No scorecard available yet.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(scorecard.innings) { inning ->
                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${inning.teamName} Innings (${inning.totalRuns}/${inning.wickets} in ${inning.overs} ov)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SportynixEmerald
                        )

                        Text(
                            text = "BATTING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )

                        inning.batsmen.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayerClick(b.playerId) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = b.name, fontWeight = FontWeight.SemiBold)
                                    Text(text = b.dismissalText, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text(text = "${b.runs} (${b.balls})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentaryTabContent(commentary: List<BallByBallBallDto>) {
    if (commentary.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No commentary available.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(commentary) { ball ->
                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ball.ballNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SportynixEmerald
                        )
                        Column {
                            Text(
                                text = "${ball.bowlerName} to ${ball.batsmanName}",
                                fontWeight = FontWeight.Bold
                            )
                            ball.commentary?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SquadsTabContent(
    team1Squad: List<PlayingXIPlayerDto>,
    team2Squad: List<PlayingXIPlayerDto>,
    onPlayerClick: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TEAM 1 PLAYING XI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )
                team1Squad.forEach { p ->
                    Text(
                        text = p.name + if (p.isCaptain) " (C)" else "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(p.id) }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
        item {
            SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TEAM 2 PLAYING XI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )
                team2Squad.forEach { p ->
                    Text(
                        text = p.name + if (p.isCaptain) " (C)" else "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(p.id) }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditMatchModal(
    currentDate: String,
    currentTime: String,
    currentVenue: String,
    onDismiss: () -> Unit,
    onSave: (date: String, time: String, venue: String) -> Unit
) {
    var date by remember { mutableStateOf(currentDate) }
    var time by remember { mutableStateOf(currentTime) }
    var venue by remember { mutableStateOf(currentVenue) }

    Dialog(onDismissRequest = onDismiss) {
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "EDIT MATCH DETAILS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Scheduled Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Scheduled Time") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(date, time, venue) },
                        colors = ButtonDefaults.buttonColors(containerColor = SportynixEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

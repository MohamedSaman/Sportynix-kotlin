package com.sportynix.app.presentation.cricket

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import com.sportynix.app.presentation.cricket.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCricketScoringScreen(
    matchId: String,
    viewModel: CricketScoringViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.initializeScoring(matchId)
    }

    val activeInning = if (uiState.liveState?.currentInnings == 2) uiState.liveState?.inning2 else uiState.liveState?.inning1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live Cricket Scoring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isSocketConnected) SportynixEmerald else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isSocketConnected) "Live WebSocket Connected" else "Reconnecting...",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isSocketConnected) SportynixEmerald else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchFullState() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scorecard Header
                    GlassScoreHeaderCard(liveState = uiState.liveState)

                    // Active Batsmen & Bowler Section
                    ActivePlayersCard(
                        striker = activeInning?.striker,
                        nonStriker = activeInning?.nonStriker,
                        bowler = activeInning?.bowler
                    )

                    // This Over Bar
                    GlassThisOverBar(recentBalls = activeInning?.recentBalls ?: emptyList())

                    // Scoring Pad
                    GlassScoringPad(
                        onRunClick = { runs ->
                            viewModel.recordBall(ballType = "legal", runs = runs)
                        },
                        onWicketClick = { viewModel.toggleWicketSheet(true) },
                        onExtrasClick = { viewModel.toggleExtrasSheet(true) },
                        onSwapClick = { viewModel.swapBatsmen() },
                        onUndoClick = { viewModel.undoLastBall() },
                        isFreeHit = uiState.isFreeHit,
                        enabled = !uiState.isSubmitting
                    )
                }
            }

            // Error Snackbar
            uiState.error?.let { err ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    containerColor = SportynixWicketRed
                ) {
                    Text(text = err, color = Color.White)
                }
            }
        }
    }

    // Dialogs & Bottom Sheets
    if (uiState.showTossDialog) {
        TossSelectionModal(
            team1Id = uiState.liveState?.inning1?.battingTeamId ?: "team1",
            team1Name = uiState.liveState?.inning1?.battingTeamName ?: "Team 1",
            team2Id = uiState.liveState?.inning1?.bowlingTeamId ?: "team2",
            team2Name = uiState.liveState?.inning1?.bowlingTeamName ?: "Team 2",
            onConfirm = { winnerId, decision ->
                viewModel.startMatch(winnerId, decision)
            }
        )
    }

    if (uiState.showWicketSheet) {
        WicketSelectionModal(
            eligibleBatsmen = uiState.eligibleBatsmen,
            onDismiss = { viewModel.toggleWicketSheet(false) },
            onConfirm = { wicketType, dismissedId, fielderId ->
                viewModel.recordBall(
                    ballType = "legal",
                    runs = 0,
                    isWicket = true,
                    wicketType = wicketType,
                    dismissedBatsmanId = dismissedId,
                    fielderId = fielderId
                )
            }
        )
    }

    if (uiState.showExtrasSheet) {
        ExtrasSelectionModal(
            onDismiss = { viewModel.toggleExtrasSheet(false) },
            onConfirm = { type, runs ->
                viewModel.recordBall(
                    ballType = type,
                    runs = if (type == "wide" || type == "no_ball") 0 else runs,
                    extraRuns = if (type == "wide" || type == "no_ball") runs + 1 else runs
                )
            }
        )
    }
}

@Composable
fun ActivePlayersCard(
    striker: LivePlayerDto?,
    nonStriker: LivePlayerDto?,
    bowler: LivePlayerDto?
) {
    val isDark = isSystemInDarkTheme()

    SportynixGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderColor = SportynixEmerald.copy(alpha = 0.3f),
        backgroundColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.85f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "BATSMEN ON CREASE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SportynixEmerald
            )

            // Striker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏏 ${striker?.name ?: "Striker"} *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${striker?.runs ?: 0} (${striker?.balls ?: 0}) | 4s: ${striker?.fours ?: 0} 6s: ${striker?.sixes ?: 0} SR: ${striker?.strikeRate ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Non-Striker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nonStriker?.name ?: "Non-Striker",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${nonStriker?.runs ?: 0} (${nonStriker?.balls ?: 0}) | SR: ${nonStriker?.strikeRate ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Text(
                text = "BOWLING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SportynixEmerald
            )

            // Bowler Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚾ ${bowler?.name ?: "Bowler"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bowler?.wickets ?: 0}/${bowler?.runsConceded ?: 0} (${bowler?.overs ?: 0.0} ov) Econ: ${bowler?.economy ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TossSelectionModal(
    team1Id: String,
    team1Name: String,
    team2Id: String,
    team2Name: String,
    onConfirm: (winnerId: String, decision: String) -> Unit
) {
    var selectedWinnerId by remember { mutableStateOf(team1Id) }
    var selectedDecision by remember { mutableStateOf("bat") }

    Dialog(onDismissRequest = {}) {
        SportynixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOSS & DECISION",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixEmerald
                )

                Text(
                    text = "Select Toss Winner:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedWinnerId == team1Id,
                        onClick = { selectedWinnerId = team1Id },
                        label = { Text(team1Name) }
                    )
                    FilterChip(
                        selected = selectedWinnerId == team2Id,
                        onClick = { selectedWinnerId = team2Id },
                        label = { Text(team2Name) }
                    )
                }

                Text(
                    text = "Toss Decision:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = selectedDecision == "bat",
                        onClick = { selectedDecision = "bat" },
                        label = { Text("BAT FIRST") }
                    )
                    FilterChip(
                        selected = selectedDecision == "bowl",
                        onClick = { selectedDecision = "bowl" },
                        label = { Text("BOWL FIRST") }
                    )
                }

                SportynixGradientButton(
                    text = "START MATCH",
                    onClick = { onConfirm(selectedWinnerId, selectedDecision) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WicketSelectionModal(
    eligibleBatsmen: List<EligibleBatsmanDto>,
    onDismiss: () -> Unit,
    onConfirm: (wicketType: String, dismissedId: String?, fielderId: String?) -> Unit
) {
    var selectedWicketType by remember { mutableStateOf("bowled") }
    var selectedDismissedId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        SportynixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECORD WICKET",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixWicketRed
                )

                Text(text = "Select Wicket Type:", style = MaterialTheme.typography.bodyMedium)

                val wicketTypes = listOf("bowled", "caught", "lbw", "run_out", "stumped", "hit_wicket")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    wicketTypes.chunked(3).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { wt ->
                                FilterChip(
                                    selected = selectedWicketType == wt,
                                    onClick = { selectedWicketType = wt },
                                    label = { Text(wt.replace("_", " ").uppercase()) }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedWicketType, selectedDismissedId, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = SportynixWicketRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CONFIRM WICKET")
                    }
                }
            }
        }
    }
}

@Composable
fun ExtrasSelectionModal(
    onDismiss: () -> Unit,
    onConfirm: (type: String, runs: Int) -> Unit
) {
    var selectedType by remember { mutableStateOf("wide") }
    var selectedRuns by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        SportynixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EXTRAS & PENALTY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SportynixExtraOrange
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("wide", "no_ball", "bye", "leg_bye").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.replace("_", " ").uppercase()) }
                        )
                    }
                }

                Text(text = "Additional Runs:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 3, 4).forEach { runs ->
                        FilterChip(
                            selected = selectedRuns == runs,
                            onClick = { selectedRuns = runs },
                            label = { Text("+$runs") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(selectedType, selectedRuns) },
                        colors = ButtonDefaults.buttonColors(containerColor = SportynixExtraOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ADD EXTRAS")
                    }
                }
            }
        }
    }
}

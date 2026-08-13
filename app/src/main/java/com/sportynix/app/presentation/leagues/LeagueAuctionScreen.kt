package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueAuctionScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeagueAuctionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground

    LaunchedEffect(leagueId) {
        viewModel.initAuction(leagueId)
    }

    val tabs = listOf("Live Room", "Team Wallets", "Player Pool", "Commentary")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.snapshot?.leagueName ?: "League Auction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                    }
                },
                actions = {
                    SportynixBadge(
                        text = if (uiState.isWsConnected) "LIVE WS" else "RECONNECTING",
                        backgroundColor = if (uiState.isWsConnected) StatusSuccess.copy(0.2f) else StatusWarning.copy(0.2f),
                        contentColor = if (uiState.isWsConnected) StatusSuccess else StatusWarning
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        containerColor = bg
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SportynixGreenPrimary)
            }
        } else if (uiState.snapshot == null) {
            // Setup Screen if no session exists
            AuctionSetupView(
                canControl = uiState.canControl,
                onCreateSetup = viewModel::createAuctionSetup
            )
        } else {
            val snapshot = uiState.snapshot!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Host Controller Control Bar
                if (uiState.canControl) {
                    AuctionHostControlBar(
                        status = snapshot.status,
                        onStart = viewModel::startAuction,
                        onPause = viewModel::pauseAuction,
                        onResume = viewModel::resumeAuction,
                        onClose = viewModel::closeAuction,
                        onUndo = viewModel::undoAction
                    )
                }

                // Main Spotlight nomination hero card
                val currentNom = snapshot.currentNomination
                if (currentNom != null && currentNom.status == "active") {
                    AuctionNominationHeroCard(
                        nomination = currentNom,
                        canControl = uiState.canControl,
                        wallets = snapshot.teamWallets,
                        onRecordBid = viewModel::recordBid,
                        onMarkSold = viewModel::markSold,
                        onMarkUnsold = viewModel::markUnsold,
                        onInspectStats = { viewModel.openPlayerStats(currentNom.player) }
                    )
                } else {
                    SportynixGlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "No Player Currently Nominated",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = if (isDark) DarkSurface else LightSurface,
                    contentColor = SportynixGreenPrimary
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = uiState.selectedTab == idx,
                            onClick = { viewModel.selectTab(idx) },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (uiState.selectedTab) {
                        0 -> AuctionLiveFeedTab(snapshot = snapshot)
                        1 -> TeamWalletsTab(wallets = snapshot.teamWallets)
                        2 -> PlayerPoolTab(
                            players = snapshot.players,
                            canControl = uiState.canControl,
                            onNominate = viewModel::openNominateDialog,
                            onInspect = viewModel::openPlayerStats
                        )
                        3 -> CommentaryTab(commentary = snapshot.commentary, onPost = viewModel::postCommentary)
                    }
                }
            }
        }
    }

    // Nominate Dialog
    if (uiState.showNominateDialog && uiState.nominatedPlayer != null) {
        AlertDialog(
            onDismissRequest = viewModel::closeNominateDialog,
            title = { Text("Nominate ${uiState.nominatedPlayer?.displayName}") },
            text = { Text("Start bidding for this player?") },
            confirmButton = {
                Button(onClick = { viewModel.nominatePlayer(uiState.openingBidInput.toDoubleOrNull() ?: 100.0) }) {
                    Text("Nominate Now")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeNominateDialog) { Text("Cancel") } }
        )
    }
}

@Composable
fun AuctionHostControlBar(
    status: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClose: () -> Unit,
    onUndo: () -> Unit
) {
    Surface(color = SportynixGreenPrimary.copy(0.1f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (status.lowercase()) {
                    "draft" -> Button(onClick = onStart) { Text("Start Auction") }
                    "live" -> Button(onClick = onPause) { Text("Pause") }
                    "paused" -> Button(onClick = onResume) { Text("Resume") }
                }
                if (status.lowercase() != "completed") {
                    OutlinedButton(onClick = onClose) { Text("Close Session") }
                }
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Action", tint = SportynixGreenPrimary)
            }
        }
    }
}

@Composable
fun AuctionNominationHeroCard(
    nomination: AuctionNominationDto,
    canControl: Boolean,
    wallets: List<AuctionWalletDto>,
    onRecordBid: (String, Double) -> Unit,
    onMarkSold: () -> Unit,
    onMarkUnsold: () -> Unit,
    onInspectStats: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val player = nomination.player

    SportynixGlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SportynixGreenPrimary.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val picUrl = player.profilePictureUrl ?: ""
                if (picUrl.isNotEmpty()) {
                    AsyncImage(model = picUrl, contentDescription = player.displayName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(player.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${player.roleLabel ?: "Player"} • ${player.battingStyle ?: ""}", style = MaterialTheme.typography.bodySmall, color = SportynixGreenPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onInspectStats) { Text("View Full Statistics", style = MaterialTheme.typography.labelSmall) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Bid Display
        SportynixGlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SportynixGreenPrimary.copy(alpha = 0.1f),
            borderColor = SportynixGreenPrimary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT BID", style = MaterialTheme.typography.labelSmall, color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                    Text("₹${nomination.currentBid.toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                }
                val teamName = nomination.currentTeamName ?: ""
                if (teamName.isNotEmpty()) {
                    SportynixBadge(text = teamName, backgroundColor = SportynixGreenPrimary, contentColor = Color.Black)
                }
            }
        }

        if (canControl) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onMarkUnsold, colors = ButtonDefaults.buttonColors(containerColor = StatusWarning), modifier = Modifier.weight(1f)) {
                    Text("UNSOLD")
                }
                Button(onClick = onMarkSold, colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess), modifier = Modifier.weight(1f)) {
                    Text("SOLD")
                }
            }
        }
    }
}

@Composable
fun TeamWalletsTab(wallets: List<AuctionWalletDto>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(wallets, key = { it.id }) { wallet ->
            SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(wallet.teamName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Players Won: ${wallet.playersWonCount} / ${wallet.slotLimit}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${wallet.remainingPoints.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                        Text("Purse Remaining", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerPoolTab(
    players: List<AuctionPlayerDto>,
    canControl: Boolean,
    onNominate: (AuctionPlayerDto) -> Unit,
    onInspect: (AuctionPlayerDto) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(players, key = { it.id }) { player ->
            AnimatedGlassCard(onClick = { onInspect(player) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    SportynixBadge(text = player.status.uppercase())
                    if (canControl && player.status.lowercase() == "available") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onNominate(player) }) { Text("Nominate") }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentaryTab(commentary: List<AuctionCommentaryEntryDto>, onPost: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Add host commentary...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onPost(text); text = "" }) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = SportynixGreenPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(commentary, key = { it.id }) { entry ->
                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AuctionLiveFeedTab(snapshot: AuctionSessionSnapshotDto) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(snapshot.actionLogs, key = { it.id }) { log ->
            SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("${log.actorName ?: "System"}: ${log.actionType}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AuctionSetupView(canControl: Boolean, onCreateSetup: (Double, Double, Double) -> Unit) {
    var points by remember { mutableStateOf("10000") }
    var minBid by remember { mutableStateOf("100") }
    var increment by remember { mutableStateOf("50") }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Auction Room Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Starting Purse Points") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = minBid, onValueChange = { minBid = it }, label = { Text("Minimum Opening Bid") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = increment, onValueChange = { increment = it }, label = { Text("Bid Increment") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            if (canControl) {
                SportynixGradientButton(
                    text = "Initialize Auction Room",
                    onClick = {
                        onCreateSetup(points.toDoubleOrNull() ?: 10000.0, minBid.toDoubleOrNull() ?: 100.0, increment.toDoubleOrNull() ?: 50.0)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Waiting for League Admin to set up auction...", color = StatusWarning)
            }
        }
    }
}

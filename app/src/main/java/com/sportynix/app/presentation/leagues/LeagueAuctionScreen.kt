package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueAuctionScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeagueAuctionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme

    LaunchedEffect(leagueId) {
        viewModel.initAuction(leagueId)
    }

    val tabs = listOf("Live Room", "Team Wallets", "Player Pool", "Commentary")

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onNavigateBack,
                        shape = CircleShape,
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.7f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = uiState.snapshot?.leagueName ?: "Auction Room",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live Player Bidding Arena",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // WS Connection Status Badge
                    LiquidGlassBadge(
                        text = if (uiState.isWsConnected) "● LIVE" else "○ CONNECTING",
                        badgeColor = if (uiState.isWsConnected) green else StatusWarning
                    )
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green, strokeWidth = 3.dp)
                    }
                } else if (uiState.snapshot == null) {
                    // Auction Setup View if not created yet
                    AuctionSetupView(
                        canControl = uiState.canControl,
                        green = green,
                        onCreateSetup = viewModel::createAuctionSetup
                    )
                } else {
                    val snapshot = uiState.snapshot!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Host Controller Bar
                        if (uiState.canControl) {
                            AuctionHostControlBar(
                                status = snapshot.status,
                                green = green,
                                onStart = viewModel::startAuction,
                                onPause = viewModel::pauseAuction,
                                onResume = viewModel::resumeAuction,
                                onClose = viewModel::closeAuction,
                                onUndo = viewModel::undoAction
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Main Spotlight Active Nomination Card
                        val currentNom = snapshot.currentNomination
                        if (currentNom != null && currentNom.status == "active") {
                            AuctionNominationHeroCard(
                                nomination = currentNom,
                                canControl = uiState.canControl,
                                wallets = snapshot.teamWallets,
                                green = green,
                                onRecordBid = viewModel::recordBid,
                                onMarkSold = viewModel::markSold,
                                onMarkUnsold = viewModel::markUnsold,
                                onInspectStats = { viewModel.openPlayerStats(currentNom.player) }
                            )
                        } else {
                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Gavel, contentDescription = null, tint = green, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "No Player Currently on the Stage",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (uiState.canControl) "Select a player from the Player Pool tab to nominate." else "Waiting for the auctioneer to nominate the next player.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Tab Row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) DarkSurface else LightSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                            )
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(tabs.indices.toList()) { idx ->
                                    val isSelected = uiState.selectedTab == idx
                                    val bgCol = if (isSelected) green else Color.Transparent
                                    val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(bgCol)
                                            .clickable { viewModel.selectTab(idx) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabs[idx],
                                            color = textCol,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.selectedTab) {
                                0 -> AuctionLiveFeedTab(snapshot = snapshot, green = green)
                                1 -> TeamWalletsTab(wallets = snapshot.teamWallets, green = green)
                                2 -> PlayerPoolTab(
                                    players = snapshot.players,
                                    canControl = uiState.canControl,
                                    green = green,
                                    onNominate = viewModel::openNominateDialog,
                                    onInspect = viewModel::openPlayerStats
                                )
                                3 -> CommentaryTab(commentary = snapshot.commentary, green = green, onPost = viewModel::postCommentary)
                            }
                        }
                    }
                }
            }

            // Nominate Confirmation Dialog
            if (uiState.showNominateDialog && uiState.nominatedPlayer != null) {
                var openingBid by remember { mutableStateOf(uiState.openingBidInput) }
                LiquidGlassDialog(onDismissRequest = viewModel::closeNominateDialog) {
                    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                        Text(
                            text = "Nominate Player",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nominate ${uiState.nominatedPlayer?.displayName} to begin active bidding.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Opening Base Bid (Pts)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LiquidGlassTextField(
                            value = openingBid,
                            onValueChange = { openingBid = it },
                            placeholder = "e.g. 100",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = viewModel::closeNominateDialog) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.nominatePlayer(openingBid.toDoubleOrNull() ?: 100.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Bidding", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Player Stats Modal
            if (uiState.showPlayerStatsModal && uiState.statsPlayer != null) {
                val p = uiState.statsPlayer!!
                LiquidGlassDialog(onDismissRequest = viewModel::closePlayerStats) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.displayName, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            LiquidGlassBadge(text = p.status.uppercase(), badgeColor = green)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                        Text("Role: ${p.roleLabel ?: "All-Rounder"}", fontSize = 13.sp)
                        Text("Batting: ${p.battingStyle ?: "Right-hand"}", fontSize = 13.sp)
                        Text("Bowling: ${p.bowlingStyle ?: "Right-arm"}", fontSize = 13.sp)
                        if (p.soldFor > 0) {
                            Text("Sold Price: ₹${p.soldFor.toInt()}", fontWeight = FontWeight.Bold, color = green, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::closePlayerStats,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuctionHostControlBar(
    status: String,
    green: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClose: () -> Unit,
    onUndo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = green.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (status.lowercase()) {
                    "draft" -> Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = green), shape = RoundedCornerShape(10.dp)) {
                        Text("Start Auction", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    "live" -> Button(onClick = onPause, colors = ButtonDefaults.buttonColors(containerColor = StatusWarning), shape = RoundedCornerShape(10.dp)) {
                        Text("Pause", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    "paused" -> Button(onClick = onResume, colors = ButtonDefaults.buttonColors(containerColor = green), shape = RoundedCornerShape(10.dp)) {
                        Text("Resume", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (status.lowercase() != "completed") {
                    OutlinedButton(onClick = onClose, shape = RoundedCornerShape(10.dp)) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo Action", tint = green)
            }
        }
    }
}

@Composable
private fun AuctionNominationHeroCard(
    nomination: AuctionNominationDto,
    canControl: Boolean,
    wallets: List<AuctionWalletDto>,
    green: Color,
    onRecordBid: (String, Double) -> Unit,
    onMarkSold: () -> Unit,
    onMarkUnsold: () -> Unit,
    onInspectStats: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val player = nomination.player
    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id ?: "") }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(green.copy(alpha = 0.2f))
                        .border(1.dp, green.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!player.profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(model = player.profilePictureUrl, contentDescription = player.displayName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = green, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(player.displayName, fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${player.roleLabel ?: "Player"} • ${player.battingStyle ?: ""}", fontSize = 11.sp, color = green, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onInspectStats, contentPadding = PaddingValues(0.dp)) {
                        Text("View Player Profile ↗", fontSize = 11.sp, color = Color(0xFF3B82F6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Highest Bid Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = green.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT HIGHEST BID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("₹${nomination.currentBid.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = green)
                    }
                    if (!nomination.currentTeamName.isNullOrBlank()) {
                        LiquidGlassBadge(text = nomination.currentTeamName!!, badgeColor = green)
                    }
                }
            }

            // Host Actions
            if (canControl) {
                Spacer(modifier = Modifier.height(10.dp))

                // Select Bidding Team & Quick Increment Bid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val wId = selectedWalletId.ifBlank { wallets.firstOrNull()?.id ?: "" }
                            if (wId.isNotBlank()) {
                                onRecordBid(wId, nomination.currentBid + 50.0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+50 Bid", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onMarkUnsold,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("UNSOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onMarkSold,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamWalletsTab(wallets: List<AuctionWalletDto>, green: Color) {
    if (wallets.isEmpty()) {
        LiquidGlassEmptyState(
            title = "No Team Wallets",
            description = "Team purse balances will appear here.",
            icon = Icons.Outlined.AccountBalanceWallet
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(wallets, key = { it.id }) { wallet ->
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wallet.teamName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Players Won: ${wallet.playersWonCount} / ${wallet.slotLimit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${wallet.remainingPoints.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = green)
                            Text("Remaining Purse", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPoolTab(
    players: List<AuctionPlayerDto>,
    canControl: Boolean,
    green: Color,
    onNominate: (AuctionPlayerDto) -> Unit,
    onInspect: (AuctionPlayerDto) -> Unit
) {
    var filterStatus by remember { mutableStateOf("all") }
    val filtered = remember(players, filterStatus) {
        if (filterStatus == "all") players else players.filter { it.status.equals(filterStatus, true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            listOf("all" to "All", "available" to "Available", "sold" to "Sold", "unsold" to "Unsold").forEach { (key, label) ->
                val isSel = filterStatus == key
                LiquidGlassFilterChip(selected = isSel, onClick = { filterStatus = key }, label = label)
            }
        }

        if (filtered.isEmpty()) {
            LiquidGlassEmptyState(title = "No Players Found", description = "No players match the selected pool filter.", icon = Icons.Outlined.Person)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(filtered, key = { it.id }) { player ->
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onInspect(player) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${player.roleLabel ?: "Player"}${if (player.soldFor > 0) " • Sold ₹${player.soldFor.toInt()}" else ""}", fontSize = 11.sp, color = Color.Gray)
                            }
                            LiquidGlassBadge(
                                text = player.status.uppercase(),
                                badgeColor = if (player.status.equals("available", true)) green else if (player.status.equals("sold", true)) Color(0xFF3B82F6) else StatusWarning
                            )
                            if (canControl && player.status.equals("available", true)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onNominate(player) },
                                    colors = ButtonDefaults.buttonColors(containerColor = green),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Nominate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun CommentaryTab(
    commentary: List<AuctionCommentaryEntryDto>,
    green: Color,
    onPost: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LiquidGlassTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Post auction announcement...",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = { if (text.isNotBlank()) { onPost(text); text = "" } },
                shape = CircleShape,
                color = green,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (commentary.isEmpty()) {
            LiquidGlassEmptyState(title = "No Commentary", description = "Live bids and auction events will stream here.", icon = Icons.Outlined.Chat)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(commentary, key = { it.id }) { entry ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("📢", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entry.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuctionLiveFeedTab(snapshot: AuctionSessionSnapshotDto, green: Color) {
    if (snapshot.actionLogs.isEmpty()) {
        LiquidGlassEmptyState(title = "No Live Events Yet", description = "Activity logs and bids will stream here in real time.", icon = Icons.Outlined.History)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(snapshot.actionLogs, key = { it.id }) { log ->
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(green))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${log.actorName ?: "System"}: ${log.actionType}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuctionSetupView(
    canControl: Boolean,
    green: Color,
    onCreateSetup: (Double, Double, Double) -> Unit
) {
    var points by remember { mutableStateOf("10000") }
    var minBid by remember { mutableStateOf("100") }
    var increment by remember { mutableStateOf("50") }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Auction Room Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Configure auction purse limits and bid increments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Starting Purse Points Per Team", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LiquidGlassTextField(value = points, onValueChange = { points = it }, placeholder = "10000", singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Text("Minimum Opening Bid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LiquidGlassTextField(value = minBid, onValueChange = { minBid = it }, placeholder = "100", singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Text("Default Bid Increment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LiquidGlassTextField(value = increment, onValueChange = { increment = it }, placeholder = "50", singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(18.dp))

                if (canControl) {
                    Button(
                        onClick = {
                            onCreateSetup(points.toDoubleOrNull() ?: 10000.0, minBid.toDoubleOrNull() ?: 100.0, increment.toDoubleOrNull() ?: 50.0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Initialize Auction Arena", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusWarning.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassEmpty, null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Waiting for League Organizer to initialize auction room...", color = StatusWarning, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

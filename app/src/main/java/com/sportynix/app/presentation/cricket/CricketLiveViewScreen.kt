package com.sportynix.app.presentation.cricket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.cricket.components.GlassScoreHeaderCard
import com.sportynix.app.presentation.cricket.components.SportynixEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketLiveViewScreen(
    matchId: String,
    viewModel: CricketLiveViewViewModel,
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.initializeLiveView(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live Match Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isSocketConnected) "Live WebSocket Connected" else "30s Fallback Polling",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isSocketConnected) SportynixEmerald else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchData(matchId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassScoreHeaderCard(liveState = uiState.liveState)

                    TabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = SportynixEmerald
                    ) {
                        LiveViewTab.values().forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.name.replace("_", " ")) }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (uiState.selectedTab) {
                            LiveViewTab.LIVE -> MatchLiveTabContent(liveState = uiState.liveState)
                            LiveViewTab.SCORECARD -> ScorecardTabContent(
                                scorecard = uiState.scorecard,
                                onPlayerClick = onPlayerClick
                            )
                            LiveViewTab.BALL_BY_BALL -> CommentaryTabContent(commentary = uiState.ballByBall)
                            LiveViewTab.STATS -> {
                                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "MATCH STATS & ANALYTICS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SportynixEmerald
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Run Rate Graphs & Wicket Analytics coming soon.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.sportynix.app.presentation.cricket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.cricket.components.GlassScoreHeaderCard
import com.sportynix.app.presentation.cricket.components.SportynixEmerald
import com.sportynix.app.presentation.cricket.components.SportynixExtraOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScoringHubScreen(
    matchId: String,
    viewModel: MatchScoringHubViewModel,
    onBack: () -> Unit,
    onNavigateToScoring: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.initializeHub(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scoring Control Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

                    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "MATCH CONTROLS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SportynixEmerald
                            )

                            SportynixGradientButton(
                                text = "OPEN SCORING CONSOLE",
                                onClick = { onNavigateToScoring(matchId) },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.pauseMatch() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("PAUSE MATCH", color = SportynixExtraOrange)
                                }

                                Button(
                                    onClick = { viewModel.resumeMatch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SportynixEmerald),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("RESUME MATCH")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

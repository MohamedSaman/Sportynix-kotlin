package com.sportynix.app.presentation.cricket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCricketScoringScreen(
    matchId: String,
    onNavigateBack: () -> Unit,
    viewModel: CricketScoringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.startLiveScoring(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Cricket Scoring", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undoLastBall(matchId) }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            state.liveScorecard?.let { score ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${score.battingTeam} vs ${score.bowlingTeam}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${score.runs} / ${score.wickets}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SportynixGreenPrimary
                        )
                        Text(
                            text = "Overs: ${score.overs}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Striker: ${score.strikerName ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Runs: ${score.strikerRuns}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text("Bowler: ${score.bowlerName ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (score.recentBalls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Recent Balls:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(score.recentBalls) { ball ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                if (ball == "W") MaterialTheme.colorScheme.error
                                                else if (ball == "4" || ball == "6") SportynixGreenPrimary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(ball, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Control Panel (Record Ball)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Runs Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0, 1, 2, 3).forEach { run ->
                        Button(
                            onClick = { viewModel.recordBall(matchId, run) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("$run")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.recordBall(matchId, 4) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SportynixGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("4 (Boundary)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.recordBall(matchId, 6) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SportynixGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("6 (SIX!)", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.recordBall(matchId, 0, isWicket = true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("WICKET", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.recordBall(matchId, 1, isWide = true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("WIDE")
                    }
                    Button(
                        onClick = { viewModel.recordBall(matchId, 1, isNoBall = true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NO BALL")
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            }
        }
    }
}

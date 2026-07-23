package com.sportynix.app.presentation.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueDetailScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fixtures", "Standings")

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueDetails(leagueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedLeague?.name ?: "League Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SportynixGreenPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            } else {
                when (selectedTab) {
                    0 -> FixturesList(fixtures = state.fixtures)
                    1 -> StandingsTable(standings = state.standings)
                }
            }
        }
    }
}

@Composable
fun FixturesList(fixtures: List<com.sportynix.app.data.remote.dto.FixtureDto>) {
    if (fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No scheduled fixtures.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(fixtures) { fixture ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fixture.team1Name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("VS", color = SportynixGreenPrimary, fontWeight = FontWeight.ExtraBold)
                            Text(fixture.team2Name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Date: ${fixture.matchDate} • Status: ${fixture.status}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsTable(standings: List<com.sportynix.app.data.remote.dto.StandingDto>) {
    if (standings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No standings recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(standings) { standing ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(standing.teamName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("P: ${standing.played}", fontSize = 13.sp)
                            Text("W: ${standing.won}", fontSize = 13.sp)
                            Text("L: ${standing.lost}", fontSize = 13.sp)
                            Text("Pts: ${standing.points}", fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                        }
                    }
                }
            }
        }
    }
}

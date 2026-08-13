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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.remote.dto.FullLeagueTeamDto
import com.sportynix.app.data.remote.dto.FullStandingDto
import com.sportynix.app.data.remote.dto.PlayerStatDto
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueDetailScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToTeamDetail: (String, String) -> Unit,
    onNavigateToApplications: (String) -> Unit,
    onNavigateToAuction: (String) -> Unit,
    viewModel: LeagueDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueDetail(leagueId)
    }

    val tabs = listOf("Teams", "Matches", "Points", "Profile & Rules")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.league?.name ?: "League Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToAuction(leagueId) }) {
                        Icon(Icons.Default.Gavel, contentDescription = "Auction Room", tint = SportynixGreenPrimary)
                    }
                    if (uiState.isCreator || uiState.isAdmin) {
                        IconButton(onClick = { onNavigateToApplications(leagueId) }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Manage Applications", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                        IconButton(onClick = { onNavigateToEdit(leagueId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit League", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                        }
                    }
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
        } else if (uiState.league != null) {
            val league = uiState.league!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Glass Card
                LeagueHeaderSection(
                    league = league,
                    uiState = uiState,
                    onApplyClick = { viewModel.toggleApplyModal(true) },
                    onWithdrawClick = viewModel::withdrawApplication,
                    onLifecycleAction = viewModel::executeLifecycleAction
                )

                // Tab Bar
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = if (isDark) DarkSurface else LightSurface,
                    contentColor = SportynixGreenPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            color = SportynixGreenPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (uiState.selectedTab) {
                        0 -> TeamsTabContent(teams = uiState.teams, onTeamClick = { teamId -> onNavigateToTeamDetail(leagueId, teamId) })
                        1 -> MatchesTabContent(fixtures = uiState.fixtures)
                        2 -> StandingsTabContent(standings = uiState.standings)
                        3 -> ProfileTabContent(league = league)
                    }
                }
            }
        }
    }

    // Apply Player Bottom Sheet
    if (uiState.showApplyModal) {
        ApplyPlayerModal(
            uiState = uiState,
            onDismiss = { viewModel.toggleApplyModal(false) },
            onUpdateForm = viewModel::updateApplicationForm,
            onSubmit = viewModel::submitPlayerApplication
        )
    }
}

@Composable
fun LeagueHeaderSection(
    league: FullLeagueDto,
    uiState: LeagueDetailUiState,
    onApplyClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onLifecycleAction: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    SportynixGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SportynixGreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val logoUrl = league.logo ?: ""
                if (logoUrl.isNotEmpty()) {
                    AsyncImage(model = logoUrl, contentDescription = league.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.SportsCricket, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(league.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isDark) TextPrimaryDark else TextPrimaryLight)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SportynixBadge(text = league.status.uppercase())
                    val variant = league.cricketVariant ?: ""
                    if (variant.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(variant, style = MaterialTheme.typography.bodySmall, color = SportynixGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Creator / Admin Lifecycle Controls
        if (uiState.isCreator || uiState.isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (league.status.lowercase()) {
                    "draft" -> {
                        Button(onClick = { onLifecycleAction("publish") }, modifier = Modifier.weight(1f)) { Text("Publish") }
                    }
                    "registration", "upcoming" -> {
                        Button(onClick = { onLifecycleAction("start") }, modifier = Modifier.weight(1f)) { Text("Start League") }
                    }
                    "in_progress", "live" -> {
                        Button(onClick = { onLifecycleAction("complete") }, modifier = Modifier.weight(1f)) { Text("Complete") }
                    }
                }
            }
        }

        // Apply as Player / Application Status Pill
        if (!uiState.isCreator && !uiState.isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            when (uiState.userApplicationStatus?.lowercase()) {
                "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SportynixBadge(text = "APPLICATION PENDING", backgroundColor = StatusWarning.copy(0.2f), contentColor = StatusWarning)
                        TextButton(onClick = onWithdrawClick) { Text("Withdraw", color = StatusError) }
                    }
                }
                "approved" -> {
                    SportynixBadge(text = "APPLICATION APPROVED", backgroundColor = StatusSuccess.copy(0.2f), contentColor = StatusSuccess)
                }
                else -> {
                    SportynixGradientButton(
                        text = "Apply as Player",
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TeamsTabContent(teams: List<FullLeagueTeamDto>, onTeamClick: (String) -> Unit) {
    val isDark = isSystemInDarkTheme()

    if (teams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No teams registered yet", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(teams, key = { it.id }) { team ->
                AnimatedGlassCard(onClick = { onTeamClick(team.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = team.teamNameOverride ?: team.team?.fullName ?: "Team",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${team.squadSize ?: 0} Players",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SportynixGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchesTabContent(fixtures: List<com.sportynix.app.data.remote.dto.FixtureDto>) {
    val isDark = isSystemInDarkTheme()

    if (fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No matches scheduled", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(fixtures, key = { it.id }) { match ->
                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.team1Name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("VS", color = SportynixGreenPrimary, fontWeight = FontWeight.Bold)
                        Text(match.team2Name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    val summary = match.scoreSummary ?: ""
                    if (summary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(summary, style = MaterialTheme.typography.bodySmall, color = SportynixGreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun StandingsTabContent(standings: List<FullStandingDto>) {
    val isDark = isSystemInDarkTheme()

    if (standings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Points table not updated yet", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
        }
    } else {
        SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("#", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
                Text("Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("P", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                Text("W", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                Text("L", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                Text("NRR", modifier = Modifier.width(44.dp), fontWeight = FontWeight.Bold)
                Text("PTS", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
            }
            Divider(color = if (isDark) GlassBorderDark else GlassBorderLight)
            standings.forEach { s ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.rank}", modifier = Modifier.width(24.dp))
                    Text(s.teamName ?: "Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${s.matchesPlayed}", modifier = Modifier.width(28.dp))
                    Text("${s.wins}", modifier = Modifier.width(28.dp))
                    Text("${s.losses}", modifier = Modifier.width(28.dp))
                    Text(String.format("%.2f", s.netRunRate ?: 0.0), modifier = Modifier.width(44.dp), style = MaterialTheme.typography.bodySmall)
                    Text("${s.points}", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                }
            }
        }
    }
}

@Composable
fun ProfileTabContent(league: FullLeagueDto) {
    val isDark = isSystemInDarkTheme()

    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("About League", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(league.description ?: "No description provided", color = if (isDark) TextSecondaryDark else TextSecondaryLight)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Rules & Regulations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(league.rulesText ?: "Standard rules apply.", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
    }
}

@Composable
fun ApplyPlayerModal(
    uiState: LeagueDetailUiState,
    onDismiss: () -> Unit,
    onUpdateForm: (String, String, String, String, String) -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply as Player") },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.applyNote,
                    onValueChange = { onUpdateForm(it, uiState.preferredVariant, uiState.primaryRole, uiState.battingStyle, uiState.bowlingStyle) },
                    label = { Text("Application Note / Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !uiState.isSubmittingApplication) {
                Text("Submit Application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

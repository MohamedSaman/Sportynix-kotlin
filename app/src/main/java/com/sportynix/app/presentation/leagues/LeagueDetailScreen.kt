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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.data.remote.dto.FullLeagueTeamDto
import com.sportynix.app.data.remote.dto.FullStandingDto
import com.sportynix.app.data.remote.dto.FixtureDto
import com.sportynix.app.presentation.components.*
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
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueDetail(leagueId)
    }

    val tabs = listOf("Teams", "Matches", "Points Table", "Profile & Rules")

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
                            text = uiState.league?.name ?: "League Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.league?.sportType?.replaceFirstChar { it.uppercase() } ?: "League"} Competition",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action Icons
                    Surface(
                        onClick = { onNavigateToAuction(leagueId) },
                        shape = CircleShape,
                        color = green.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.4f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Auction Room",
                                tint = green,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (uiState.isCreator || uiState.isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { onNavigateToApplications(leagueId) },
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
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = "Manage Applications",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { onNavigateToEdit(leagueId) },
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
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit League",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green, strokeWidth = 3.dp)
                    }
                } else if (uiState.league != null) {
                    val league = uiState.league!!

                    // Scrollable Header + Tabs Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Hero Header Card
                        LeagueHeroHeader(
                            league = league,
                            uiState = uiState,
                            green = green,
                            onApplyClick = { viewModel.toggleApplyModal(true) },
                            onWithdrawClick = viewModel::withdrawApplication,
                            onLifecycleAction = viewModel::executeLifecycleAction
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Capsule Tab Row
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
                                items(tabs.indices.toList()) { index ->
                                    val isSelected = uiState.selectedTab == index
                                    val bgCol = if (isSelected) green else Color.Transparent
                                    val textCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(bgCol)
                                            .clickable { viewModel.selectTab(index) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabs[index],
                                            color = textCol,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.selectedTab) {
                                0 -> TeamsTabContent(
                                    teams = uiState.teams,
                                    green = green,
                                    onTeamClick = { teamId -> onNavigateToTeamDetail(leagueId, teamId) }
                                )
                                1 -> MatchesTabContent(
                                    fixtures = uiState.fixtures,
                                    green = green
                                )
                                2 -> StandingsTabContent(
                                    standings = uiState.standings,
                                    green = green
                                )
                                3 -> ProfileTabContent(
                                    league = league,
                                    green = green
                                )
                            }
                        }
                    }
                }
            }

            // Success / Message Notification
            uiState.actionSuccessMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2500)
                    viewModel.clearMessages()
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    LiquidGlassBadge(text = msg, badgeColor = green)
                }
            }

            // Error Dialog
            uiState.error?.let { err ->
                LiquidGlassDialog(onDismissRequest = viewModel::clearMessages) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.15f))
                                .border(1.5.dp, StatusError.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Notice", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(err, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::clearMessages,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Player Application Modal
            if (uiState.showApplyModal) {
                ApplyPlayerModal(
                    uiState = uiState,
                    green = green,
                    onDismiss = { viewModel.toggleApplyModal(false) },
                    onUpdateForm = viewModel::updateApplicationForm,
                    onSubmit = viewModel::submitPlayerApplication
                )
            }
        }
    }
}

@Composable
private fun LeagueHeroHeader(
    league: FullLeagueDto,
    uiState: LeagueDetailUiState,
    green: Color,
    onApplyClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onLifecycleAction: (String) -> Unit
) {
    val isDark = LocalThemeController.current.isDark

    val statusColor = when (league.status.lowercase()) {
        "live", "in_progress", "active" -> green
        "upcoming", "registration" -> Color(0xFF3B82F6)
        "completed" -> AccentGold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // League Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(green.copy(alpha = 0.25f), green.copy(alpha = 0.05f))
                            )
                        )
                        .border(1.dp, green.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!league.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = league.logo,
                            contentDescription = league.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = getLeagueSportEmoji(league.sportType),
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = league.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGlassBadge(
                            text = league.status.uppercase(),
                            badgeColor = statusColor
                        )

                        val variant = league.cricketVariant ?: ""
                        if (variant.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = green.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (variant.equals("softball", true)) "Soft Ball" else "Hard Ball",
                                    color = green,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!league.startDate.isNullOrBlank()) {
                        Text(
                            text = "Starts: ${league.startDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Quick Info Grid
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(
                    icon = Icons.Outlined.Groups,
                    label = "Teams",
                    value = "${league.numTeams}",
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    icon = Icons.Outlined.Person,
                    label = "Squad Size",
                    value = "${league.squadSize ?: 15}",
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    icon = Icons.Outlined.EmojiEvents,
                    label = "Prize Pool",
                    value = if (!league.prizePool.isNullOrBlank()) "Rs. ${league.prizePool}" else "Trophy",
                    modifier = Modifier.weight(1f)
                )
            }

            // Creator / Admin Lifecycle Controls
            if (uiState.isCreator || uiState.isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (league.status.lowercase()) {
                        "draft" -> {
                            Button(
                                onClick = { onLifecycleAction("publish") },
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Publish League", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        "registration", "upcoming" -> {
                            Button(
                                onClick = { onLifecycleAction("start") },
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Start League", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        "in_progress", "live" -> {
                            Button(
                                onClick = { onLifecycleAction("complete") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete League", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Participant Apply Button / Application Status
            if (!uiState.isCreator && !uiState.isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                when (uiState.userApplicationStatus?.lowercase()) {
                    "pending" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AccentGold.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassEmpty, null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Application Pending", fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 12.sp)
                                }
                                TextButton(onClick = onWithdrawClick) {
                                    Text("Withdraw", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    "approved" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = green.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Player Application Approved", fontWeight = FontWeight.Bold, color = green, fontSize = 13.sp)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onApplyClick,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply as Player", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier
) {
    val isDark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TeamsTabContent(
    teams: List<FullLeagueTeamDto>,
    green: Color,
    onTeamClick: (String) -> Unit
) {
    if (teams.isEmpty()) {
        LiquidGlassEmptyState(
            title = "No Teams Registered",
            description = "Teams participating in this league will appear here.",
            icon = Icons.Outlined.Groups
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(teams, key = { it.id }) { team ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onTeamClick(team.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(green.copy(alpha = 0.15f))
                                .border(1.dp, green.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (team.teamNameOverride ?: team.team?.fullName ?: "T").take(1).uppercase(),
                                color = green,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = team.teamNameOverride ?: team.team?.fullName ?: "Team Name",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${team.squadSize ?: 0} Players in squad",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchesTabContent(
    fixtures: List<FixtureDto>,
    green: Color
) {
    if (fixtures.isEmpty()) {
        LiquidGlassEmptyState(
            title = "No Fixtures Scheduled",
            description = "Match schedules and scorecards will be published once the league begins.",
            icon = Icons.Outlined.SportsCricket
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(fixtures, key = { it.id }) { match ->
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LiquidGlassBadge(
                                text = if (!match.status.isNullOrBlank()) match.status.uppercase() else "SCHEDULED",
                                badgeColor = if (match.status.equals("live", true)) green else Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = match.matchDate ?: "Date TBD",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = match.team1Name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(green.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("VS", color = green, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }

                            Text(
                                text = match.team2Name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val summary = match.scoreSummary ?: ""
                        if (summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = summary,
                                fontSize = 12.sp,
                                color = green,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsTabContent(
    standings: List<FullStandingDto>,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark

    if (standings.isEmpty()) {
        LiquidGlassEmptyState(
            title = "Points Table Empty",
            description = "Standings will be updated automatically as matches conclude.",
            icon = Icons.Outlined.Leaderboard
        )
    } else {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", modifier = Modifier.width(22.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("P", modifier = Modifier.width(26.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("W", modifier = Modifier.width(26.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = green, textAlign = TextAlign.Center)
                    Text("L", modifier = Modifier.width(26.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StatusError, textAlign = TextAlign.Center)
                    Text("NRR", modifier = Modifier.width(42.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("PTS", modifier = Modifier.width(34.dp), fontWeight = FontWeight.Black, fontSize = 11.sp, color = green, textAlign = TextAlign.Center)
                }

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))

                standings.forEachIndexed { idx, s ->
                    val isTopQualified = idx < 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${s.rank ?: (idx + 1)}",
                            modifier = Modifier.width(22.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isTopQualified) green else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.teamName ?: "Team",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("${s.matchesPlayed}", modifier = Modifier.width(26.dp), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("${s.wins}", modifier = Modifier.width(26.dp), fontSize = 12.sp, color = green, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("${s.losses}", modifier = Modifier.width(26.dp), fontSize = 12.sp, color = StatusError, textAlign = TextAlign.Center)
                        Text(String.format(java.util.Locale.US, "%.2f", s.netRunRate ?: 0.0), modifier = Modifier.width(42.dp), fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Text("${s.points}", modifier = Modifier.width(34.dp), fontWeight = FontWeight.Black, fontSize = 13.sp, color = green, textAlign = TextAlign.Center)
                    }
                    if (idx < standings.size - 1) {
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    league: FullLeagueDto,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About League", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!league.description.isNullOrBlank()) league.description!! else "No description provided for this league.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Rules & Regulations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!league.rulesText.isNullOrBlank()) league.rulesText!! else "Standard ICC / tournament sports regulations apply.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        if (league.isVenueHosted == true || league.primaryVenue != null) {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Venue Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = league.primaryVenue?.name ?: "Official Tournament Venue",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplyPlayerModal(
    uiState: LeagueDetailUiState,
    green: Color,
    onDismiss: () -> Unit,
    onUpdateForm: (String, String, String, String, String) -> Unit,
    onSubmit: () -> Unit
) {
    val roles = listOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper")
    val variants = listOf("softball" to "Soft Ball", "hardball" to "Hard Ball")
    val battingStyles = listOf("Right-hand Bat", "Left-hand Bat")
    val bowlingStyles = listOf(
        "Right-arm Fast", "Right-arm Medium", "Right-arm Off Spin", "Right-arm Leg Spin",
        "Left-arm Fast", "Left-arm Medium", "Left-arm Orthodox"
    )

    LiquidGlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(6.dp)
        ) {
            Text(
                text = "Apply as League Player",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Submit your cricket profile to join the player draft / auction pool.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ball Variant Selector
            Text("Preferred Ball Variant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                variants.forEach { (key, label) ->
                    val selected = uiState.preferredVariant == key
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { onUpdateForm(uiState.applyNote, key, uiState.primaryRole, uiState.battingStyle, uiState.bowlingStyle) },
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Role
            Text("Primary Role", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(roles) { r ->
                    val selected = uiState.primaryRole.equals(r, true)
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { onUpdateForm(uiState.applyNote, uiState.preferredVariant, r.lowercase(), uiState.battingStyle, uiState.bowlingStyle) },
                        label = r
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Batting Style
            Text("Batting Style", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                battingStyles.forEach { bat ->
                    val selected = uiState.battingStyle.equals(bat, true)
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { onUpdateForm(uiState.applyNote, uiState.preferredVariant, uiState.primaryRole, bat, uiState.bowlingStyle) },
                        label = bat
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bowling Style
            Text("Bowling Style", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(bowlingStyles) { bowl ->
                    val selected = uiState.bowlingStyle.equals(bowl, true)
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { onUpdateForm(uiState.applyNote, uiState.preferredVariant, uiState.primaryRole, uiState.battingStyle, bowl) },
                        label = bowl
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bio / Application Note
            Text("Player Bio / Experience Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.applyNote,
                onValueChange = { onUpdateForm(it, uiState.preferredVariant, uiState.primaryRole, uiState.battingStyle, uiState.bowlingStyle) },
                placeholder = "Describe your playing experience...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmittingApplication,
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSubmittingApplication) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Submit Application", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun getLeagueSportEmoji(sport: String?): String {
    val name = sport?.lowercase() ?: ""
    return when {
        name.contains("cricket") -> "🏏"
        name.contains("football") -> "⚽"
        name.contains("basketball") -> "🏀"
        name.contains("tennis") -> "🎾"
        name.contains("badminton") -> "🏸"
        name.contains("volleyball") -> "🏐"
        else -> "🏆"
    }
}

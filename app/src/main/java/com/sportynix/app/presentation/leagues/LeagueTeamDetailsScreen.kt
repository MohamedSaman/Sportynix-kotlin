package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.FullLeagueTeamDto
import com.sportynix.app.data.remote.dto.SquadMemberDto
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueTeamDetailsScreen(
    leagueId: String,
    teamId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeagueTeamsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight

    val snackbarHostState = remember { SnackbarHostState() }
    var memberPendingRemoval by remember { mutableStateOf<SquadMemberDto?>(null) }

    LaunchedEffect(teamId) {
        viewModel.loadTeamDetail(teamId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val squadFilterOptions = listOf(
        "all" to "All Squad",
        "captains" to "Captains",
        "batsman" to "Batsmen",
        "bowler" to "Bowlers",
        "all-rounder" to "All-Rounders"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.team?.teamNameOverride ?: "Team Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.squad.size} Squad Members",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTeamDetail(teamId, isRefresh = true) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SportynixGreenPrimary
                        )
                    }
                    if (uiState.canManage) {
                        IconButton(onClick = { viewModel.toggleAddPlayerModal(true) }) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Add Player",
                                tint = SportynixGreenPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        containerColor = bg
    ) { padding ->
        if (uiState.isLoading && uiState.team == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SportynixGreenPrimary)
            }
        } else if (uiState.team != null) {
            val team = uiState.team!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Hero Team Overview Card
                TeamOverviewHeroCard(
                    team = team,
                    canManage = uiState.canManage,
                    onManageCoAdmins = { viewModel.toggleCoAdminModal(true) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Squad Bar
                LiquidGlassSearchBar(
                    query = uiState.squadSearchQuery,
                    onQueryChange = { viewModel.setSquadSearchQuery(it) },
                    placeholder = "Search squad by name, jersey, role...",
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(squadFilterOptions) { (key, label) ->
                        val selected = uiState.squadRoleFilter == key
                        LiquidGlassFilterChip(
                            selected = selected,
                            onClick = { viewModel.setSquadRoleFilter(key) },
                            label = label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Squad Members List
                if (uiState.filteredSquad.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassEmptyState(
                            title = "No Squad Members Found",
                            description = if (uiState.squadSearchQuery.isNotEmpty())
                                "No player matches '${uiState.squadSearchQuery}'"
                            else
                                "This team does not have any squad members registered yet.",
                            icon = Icons.Outlined.GroupOff,
                            actionText = if (uiState.canManage) "Add Player" else null,
                            onActionClick = if (uiState.canManage) { { viewModel.toggleAddPlayerModal(true) } } else null
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.filteredSquad, key = { it.id }) { member ->
                            SquadMemberCardItem(
                                member = member,
                                canManage = uiState.canManage,
                                onEdit = { viewModel.selectMemberForEdit(member) },
                                onRemove = { memberPendingRemoval = member }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Player Dialog
    if (uiState.showAddPlayerModal) {
        AddPlayerLiquidDialog(
            onDismiss = { viewModel.toggleAddPlayerModal(false) },
            onConfirm = { userId, jersey, role, pos ->
                viewModel.addPlayerToSquad(userId, jersey, role, pos)
            }
        )
    }

    // Edit Member Dialog
    uiState.selectedMemberForEdit?.let { member ->
        EditMemberLiquidDialog(
            member = member,
            onDismiss = { viewModel.selectMemberForEdit(null) },
            onConfirm = { role, jersey, pos ->
                viewModel.updatePlayerRole(member.id, role, jersey, pos)
            }
        )
    }

    // Remove Confirmation Dialog
    memberPendingRemoval?.let { member ->
        LiquidGlassDialog(
            onDismissRequest = { memberPendingRemoval = null }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Remove Player",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = "Are you sure you want to remove '${member.user.fullName ?: member.user.name ?: "this player"}' from the team squad?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { memberPendingRemoval = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val memberId = member.id
                            memberPendingRemoval = null
                            viewModel.removePlayerFromSquad(memberId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamOverviewHeroCard(
    team: FullLeagueTeamDto,
    canManage: Boolean,
    onManageCoAdmins: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val teamName = team.teamNameOverride ?: team.team?.fullName ?: team.team?.name ?: "Team"
    val initials = teamName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        .ifEmpty { "TM" }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Team Crest Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SportynixGreenPrimary.copy(alpha = 0.35f),
                                    Color(0xFF0072FF).copy(alpha = 0.35f)
                                )
                            )
                        )
                        .border(1.5.dp, SportynixGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SportynixGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!team.teamShortName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            LiquidGlassBadge(text = team.teamShortName)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (team.captain != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Captain: ${team.captain.fullName ?: team.captain.name ?: "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                    if (team.viceCaptain != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = SportynixGreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vice Captain: ${team.viceCaptain.fullName ?: team.viceCaptain.name ?: "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassBadge(
                        text = "${team.squadSize ?: team.squad?.size ?: 0} PLAYERS",
                        badgeColor = SportynixGreenPrimary
                    )

                    if (!team.jerseyColor.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Jersey: ${team.jerseyColor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }
                    }
                }

                if (canManage) {
                    LiquidGlassBadge(
                        text = "ADMIN",
                        badgeColor = AccentGold
                    )
                }
            }
        }
    }
}

@Composable
fun SquadMemberCardItem(
    member: SquadMemberDto,
    canManage: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val isCaptain = member.role.equals("captain", ignoreCase = true)
    val isViceCaptain = member.role.equals("vice_captain", ignoreCase = true)

    val roleBadgeColor = when {
        isCaptain -> AccentGold
        isViceCaptain -> SportynixGreenPrimary
        member.role.equals("coach", ignoreCase = true) -> Color(0xFF00C9FF)
        member.role.equals("manager", ignoreCase = true) -> Color(0xFF9D4EDD)
        else -> SportynixGreenDark
    }

    val playerName = member.user.fullName ?: member.user.name ?: "Player"

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jersey Number Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                roleBadgeColor.copy(alpha = 0.25f),
                                roleBadgeColor.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(1.dp, roleBadgeColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (member.jerseyNumber != null) "#${member.jerseyNumber}" else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = roleBadgeColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCaptain) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Captain",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (isViceCaptain) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Vice Captain",
                            tint = SportynixGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassBadge(
                        text = member.role.replace("_", " ").uppercase(),
                        badgeColor = roleBadgeColor
                    )

                    if (!member.playingPosition.isNullOrBlank()) {
                        Text(
                            text = "•  ${member.playingPosition}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                    }
                }
            }

            // Management Actions
            if (canManage) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Role",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = StatusError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPlayerLiquidDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String, String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var jerseyStr by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("player") }
    var position by remember { mutableStateOf("Batsman") }

    val roleOptions = listOf(
        "player" to "Player",
        "captain" to "Captain",
        "vice_captain" to "Vice Captain",
        "coach" to "Coach",
        "manager" to "Manager"
    )

    val posOptions = listOf("Batsman", "Bowler", "All-Rounder", "Wicket Keeper")

    LiquidGlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add Squad Member",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            LiquidGlassTextField(
                value = userId,
                onValueChange = { userId = it },
                placeholder = "User ID / Username",
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SportynixGreenPrimary)
                }
            )

            LiquidGlassTextField(
                value = jerseyStr,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) jerseyStr = it },
                placeholder = "Jersey Number (e.g. 18)",
                leadingIcon = {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = SportynixGreenPrimary)
                }
            )

            Text(
                text = "Assign Role",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roleOptions) { (key, label) ->
                    LiquidGlassFilterChip(
                        selected = role == key,
                        onClick = { role = key },
                        label = label
                    )
                }
            }

            Text(
                text = "Playing Position",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(posOptions) { pos ->
                    LiquidGlassFilterChip(
                        selected = position == pos,
                        onClick = { position = pos },
                        label = pos
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                LiquidGlassButton(
                    text = "Add Player",
                    onClick = {
                        onConfirm(userId.trim(), jerseyStr.toIntOrNull(), role, position)
                    },
                    enabled = userId.isNotBlank(),
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

@Composable
private fun EditMemberLiquidDialog(
    member: SquadMemberDto,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String) -> Unit
) {
    var jerseyStr by remember { mutableStateOf(member.jerseyNumber?.toString() ?: "") }
    var role by remember { mutableStateOf(member.role) }
    var position by remember { mutableStateOf(member.playingPosition ?: "Batsman") }

    val roleOptions = listOf(
        "player" to "Player",
        "captain" to "Captain",
        "vice_captain" to "Vice Captain",
        "coach" to "Coach",
        "manager" to "Manager"
    )

    val posOptions = listOf("Batsman", "Bowler", "All-Rounder", "Wicket Keeper")

    LiquidGlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Edit Member Role",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Text(
                text = "Player: ${member.user.fullName ?: member.user.name ?: "Player"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = SportynixGreenPrimary
            )

            LiquidGlassTextField(
                value = jerseyStr,
                onValueChange = { if (it.all { ch -> ch.isDigit() }) jerseyStr = it },
                placeholder = "Jersey Number (e.g. 7)",
                leadingIcon = {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = SportynixGreenPrimary)
                }
            )

            Text(
                text = "Role",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(roleOptions) { (key, label) ->
                    LiquidGlassFilterChip(
                        selected = role == key,
                        onClick = { role = key },
                        label = label
                    )
                }
            }

            Text(
                text = "Playing Position",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(posOptions) { pos ->
                    LiquidGlassFilterChip(
                        selected = position == pos,
                        onClick = { position = pos },
                        label = pos
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                LiquidGlassButton(
                    text = "Save",
                    onClick = {
                        onConfirm(role, jerseyStr.toIntOrNull(), position)
                    },
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

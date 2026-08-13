package com.sportynix.app.presentation.leagues

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.SquadMemberDto
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.components.SportynixGradientButton
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

    LaunchedEffect(teamId) {
        viewModel.loadTeamDetail(teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.team?.teamNameOverride ?: "Team Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                    }
                },
                actions = {
                    if (uiState.canManage) {
                        IconButton(onClick = { viewModel.toggleAddPlayerModal(true) }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Player", tint = SportynixGreenPrimary)
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
        } else if (uiState.team != null) {
            val team = uiState.team!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Team Header Card
                SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SportynixGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = team.teamNameOverride ?: "Team Name",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextPrimaryDark else TextPrimaryLight
                            )
                            if (team.captain != null) {
                                Text(
                                    text = "Captain: ${team.captain.fullName ?: team.captain.name ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SportynixGreenPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Squad Members (${uiState.squad.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.squad, key = { it.id }) { member ->
                        SquadMemberRow(
                            member = member,
                            canManage = uiState.canManage,
                            onRemove = { viewModel.removePlayerFromSquad(member.id) },
                            onMakeCaptain = { viewModel.updatePlayerRole(member.id, "captain", member.jerseyNumber) }
                        )
                    }
                }
            }
        }
    }

    // Add Player Modal
    if (uiState.showAddPlayerModal) {
        AddPlayerDialog(
            onDismiss = { viewModel.toggleAddPlayerModal(false) },
            onConfirm = { userId, jersey, role ->
                viewModel.addPlayerToSquad(userId, jersey, role)
            }
        )
    }
}

@Composable
fun SquadMemberRow(
    member: SquadMemberDto,
    canManage: Boolean,
    onRemove: () -> Unit,
    onMakeCaptain: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SportynixGreenPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${member.jerseyNumber ?: "#"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SportynixGreenPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.user.fullName ?: member.user.name ?: "Player",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                )
                Text(
                    text = member.role.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (member.role.lowercase() == "captain") StatusWarning else SportynixGreenPrimary
                )
            }

            if (canManage) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusError)
                }
            }
        }
    }
}

@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var jerseyStr by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("player") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Squad Member") },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jerseyStr,
                    onValueChange = { jerseyStr = it },
                    label = { Text("Jersey Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(userId, jerseyStr.toIntOrNull(), role)
                },
                enabled = userId.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

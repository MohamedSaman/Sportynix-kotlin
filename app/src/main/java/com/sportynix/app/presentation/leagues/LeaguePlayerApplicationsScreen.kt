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
import com.sportynix.app.data.remote.dto.LeaguePlayerApplicationDto
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguePlayerApplicationsScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeaguePlayerApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground
    val surfaceColor = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(leagueId) {
        viewModel.loadApplications(leagueId)
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

    val filterTabs = listOf(
        Triple("pending", "Pending", uiState.pendingCount),
        Triple("approved", "Approved", uiState.approvedCount),
        Triple("rejected", "Rejected", uiState.rejectedCount),
        Triple("all", "All", uiState.totalCount)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Player Applications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Text(
                            text = "${uiState.totalCount} Total Registrations",
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
                    IconButton(onClick = { viewModel.loadApplications(leagueId, isRefresh = true) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SportynixGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.selectedAppIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = surfaceColor,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportynixGreenPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.selectedAppIds.size} Selected",
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Bulk Actions",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.bulkReview("rejected") },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusError)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject All", fontWeight = FontWeight.Bold)
                            }

                            LiquidGlassButton(
                                text = "Approve All",
                                onClick = { viewModel.bulkReview("approved") },
                                icon = Icons.Default.Check
                            )
                        }
                    }
                }
            }
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats summary card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ApplicationStatItem(
                        label = "Total",
                        count = uiState.totalCount,
                        color = textPrimary
                    )
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    ApplicationStatItem(
                        label = "Pending",
                        count = uiState.pendingCount,
                        color = AccentGold
                    )
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    ApplicationStatItem(
                        label = "Approved",
                        count = uiState.approvedCount,
                        color = StatusSuccess
                    )
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    ApplicationStatItem(
                        label = "Rejected",
                        count = uiState.rejectedCount,
                        color = StatusError
                    )
                }
            }

            // Search Bar
            LiquidGlassSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search player name, role, variant...",
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(filterTabs) { (key, label, count) ->
                    val selected = uiState.selectedFilter == key
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(key) },
                        label = "$label ($count)"
                    )
                }
            }

            // Quick select bar for Pending
            if (uiState.selectedFilter == "pending" && uiState.pendingCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pending Applicant List",
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (uiState.selectedAppIds.size == uiState.pendingCount && uiState.pendingCount > 0)
                            "Clear Selection" else "Select All Pending",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (uiState.selectedAppIds.size == uiState.pendingCount) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAllPending()
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area
            if (uiState.isLoading && uiState.applications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            } else if (uiState.filteredApplications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassEmptyState(
                        title = "No Applications Found",
                        description = if (uiState.searchQuery.isNotEmpty())
                            "No applications match '${uiState.searchQuery}'"
                        else
                            "No applications under '${uiState.selectedFilter.replaceFirstChar { it.uppercase() }}' category.",
                        icon = Icons.Outlined.PersonOff,
                        actionText = "Refresh List",
                        onActionClick = { viewModel.loadApplications(leagueId, isRefresh = true) }
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = if (uiState.selectedAppIds.isNotEmpty()) 90.dp else 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.filteredApplications, key = { it.id }) { app ->
                        val isSelected = uiState.selectedAppIds.contains(app.id)
                        ApplicationCardItem(
                            app = app,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (app.status.equals("pending", ignoreCase = true)) {
                                    viewModel.toggleAppSelection(app.id)
                                }
                            },
                            onApprove = { viewModel.reviewSingle(app.id, "approved") },
                            onReject = { viewModel.reviewSingle(app.id, "rejected") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationStatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ApplicationCardItem(
    app: LeaguePlayerApplicationDto,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isPending = app.status.equals("pending", ignoreCase = true)
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight

    val statusBadgeColor = when (app.status.lowercase()) {
        "approved" -> StatusSuccess
        "rejected" -> StatusError
        else -> AccentGold
    }

    val playerName = app.user.fullName ?: app.user.name ?: "Applicant Player"
    val initial = playerName.firstOrNull()?.uppercase() ?: "P"

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    width = 1.5.dp,
                    color = SportynixGreenPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        onClick = if (isPending) onToggleSelect else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar, Name, Status Badge, Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPending) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SportynixGreenPrimary,
                            uncheckedColor = textSecondary.copy(alpha = 0.5f),
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                // Avatar Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SportynixGreenPrimary.copy(alpha = 0.3f),
                                    SportynixGreenDark.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(1.dp, SportynixGreenPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SportynixGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!app.user.username.isNullOrBlank()) {
                        Text(
                            text = "@${app.user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                LiquidGlassBadge(
                    text = app.status.replaceFirstChar { it.uppercase() },
                    badgeColor = statusBadgeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attributes Grid Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!app.cricketPrimaryRole.isNullOrBlank()) {
                    PlayerMetaPill(
                        icon = "🏏",
                        label = app.cricketPrimaryRole.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!app.cricketPreferredVariant.isNullOrBlank()) {
                    PlayerMetaPill(
                        icon = "🏆",
                        label = app.cricketPreferredVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!app.cricketBattingStyle.isNullOrBlank() || !app.cricketBowlingStyle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!app.cricketBattingStyle.isNullOrBlank()) {
                        PlayerMetaPill(
                            icon = "⚡",
                            label = app.cricketBattingStyle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!app.cricketBowlingStyle.isNullOrBlank()) {
                        PlayerMetaPill(
                            icon = "🎯",
                            label = app.cricketBowlingStyle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Note/Bio quote container if available
            if (!app.applicationNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) DarkSurfaceVariant.copy(alpha = 0.5f) else LightSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            tint = SportynixGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = app.applicationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Inline Action buttons for pending applications
            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusError.copy(alpha = 0.15f),
                            contentColor = StatusError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusSuccess.copy(alpha = 0.15f),
                            contentColor = StatusSuccess
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerMetaPill(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant.copy(alpha = 0.6f)
    val textColor = if (isDark) TextPrimaryDark else TextPrimaryLight

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(icon, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

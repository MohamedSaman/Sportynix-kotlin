package com.sportynix.app.presentation.tournaments

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
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
fun TournamentDetailScreen(
    tournamentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMatchDetails: (String) -> Unit = {},
    onNavigateToLiveScoring: (String) -> Unit = {},
    viewModel: TournamentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showApproveConfirmDialog by remember { mutableStateOf(false) }
    var showRejectConfirmDialog by remember { mutableStateOf(false) }
    var approvalNoteText by remember { mutableStateOf("") }

    LaunchedEffect(tournamentId) {
        viewModel.loadTournamentDetail(tournamentId)
    }

    val tabs = listOf("Teams", "Matches & Brackets", "Points Table", "Profile & Rules")

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
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
                                text = uiState.tournament?.displayTitle ?: "Tournament Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.tournament?.actualSportType?.replaceFirstChar { it.uppercase() } ?: "Tournament"} Cup",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Host Applications Review action button
                        if (uiState.isHost || uiState.isStaff) {
                            Surface(
                                onClick = { viewModel.toggleApplicationsReviewModal(true) },
                                shape = CircleShape,
                                color = green.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.4f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GroupAdd,
                                        contentDescription = "Manage Applications",
                                        tint = green,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Options Menu Button
                        Box {
                            Surface(
                                onClick = { showOptionsMenu = true },
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
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                if (uiState.isHost) {
                                    DropdownMenuItem(
                                        text = { Text("Request Edit from Admin") },
                                        leadingIcon = { Icon(Icons.Default.EditNote, null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.toggleRequestEditModal(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Tournament", color = StatusError) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = StatusError) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                                if (uiState.isStaff) {
                                    if (!uiState.isApproved) {
                                        DropdownMenuItem(
                                            text = { Text("Approve Tournament", color = green) },
                                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = green) },
                                            onClick = {
                                                showOptionsMenu = false
                                                showApproveConfirmDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Reject Tournament", color = StatusError) },
                                            leadingIcon = { Icon(Icons.Default.Cancel, null, tint = StatusError) },
                                            onClick = {
                                                showOptionsMenu = false
                                                showRejectConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (uiState.canApply) {
                        Surface(
                            color = if (isDark) DarkSurface else LightSurface,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleApplyModal(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = green),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apply Team to Tournament", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                },
                containerColor = bg
            ) { padding ->
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green, strokeWidth = 3.dp)
                    }
                } else if (uiState.tournament != null) {
                    val tournament = uiState.tournament!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Hero Header Card
                        TournamentHeroCard(
                            tournament = tournament,
                            uiState = uiState,
                            green = green,
                            onOpenApplications = viewModel::openApplications,
                            onCloseApplications = viewModel::closeApplications,
                            onFinalizeTeams = viewModel::finalizeTeams,
                            onGenerateMatches = viewModel::generateMatches,
                            onApprove = { showApproveConfirmDialog = true },
                            onReject = { showRejectConfirmDialog = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Capsule Tab Bar
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
                                0 -> TournamentTeamsTab(
                                    participations = uiState.participations,
                                    applications = uiState.applications,
                                    green = green
                                )
                                1 -> TournamentMatchesTab(
                                    matches = uiState.matches,
                                    green = green,
                                    isHostOrStaff = uiState.isHost || uiState.isStaff,
                                    onAttachScoringLink = viewModel::attachScoringLink,
                                    onNavigateToMatchDetails = onNavigateToMatchDetails,
                                    onNavigateToLiveScoring = onNavigateToLiveScoring
                                )
                                2 -> TournamentPointsTab(
                                    participations = uiState.participations,
                                    green = green
                                )
                                3 -> TournamentProfileTab(
                                    tournament = tournament,
                                    green = green
                                )
                            }
                        }
                    }
                }
            }

            // Success Toast Notification
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

            // Team Application Modal
            if (uiState.showApplyModal) {
                TournamentApplyModal(
                    uiState = uiState,
                    green = green,
                    onDismiss = { viewModel.toggleApplyModal(false) },
                    onSelectTeam = viewModel::selectTeamForApplication,
                    onToggleMember = viewModel::toggleRosterMember,
                    onSelectCaptain = viewModel::selectCaptain,
                    onUpdateNote = viewModel::updateApplicationNote,
                    onSubmit = viewModel::submitApplication
                )
            }

            // Host Applications Review Modal
            if (uiState.showApplicationsReviewModal) {
                TournamentApplicationsReviewModal(
                    uiState = uiState,
                    green = green,
                    onDismiss = { viewModel.toggleApplicationsReviewModal(false) },
                    onSelectTab = viewModel::selectApplicationTab,
                    onReview = viewModel::reviewApplication
                )
            }

            // Request Edit Modal
            if (uiState.showRequestEditModal) {
                LiquidGlassDialog(onDismissRequest = { viewModel.toggleRequestEditModal(false) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("Request Tournament Edit", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Explain requested changes to the admin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        LiquidGlassTextField(
                            value = uiState.requestEditReason,
                            onValueChange = viewModel::updateRequestEditReason,
                            placeholder = "Describe modifications (dates, capacity, rules)...",
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.toggleRequestEditModal(false) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = viewModel::submitRequestEdit,
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Delete Confirm Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Delete Tournament", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to delete this tournament? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                viewModel.deleteTournament(onSuccess = onNavigateBack)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Approve Confirm Dialog
            if (showApproveConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showApproveConfirmDialog = false },
                    title = { Text("Approve Tournament", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("This will approve the tournament and publish it for team applications.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = approvalNoteText,
                                onValueChange = { approvalNoteText = it },
                                label = { Text("Approval Note (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showApproveConfirmDialog = false
                                viewModel.approveTournament(approvalNoteText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = green)
                        ) {
                            Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showApproveConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Reject Confirm Dialog
            if (showRejectConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRejectConfirmDialog = false },
                    title = { Text("Reject Tournament", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Are you sure you want to reject this tournament?")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = approvalNoteText,
                                onValueChange = { approvalNoteText = it },
                                label = { Text("Rejection Note / Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRejectConfirmDialog = false
                                viewModel.rejectTournament(approvalNoteText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRejectConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TournamentHeroCard(
    tournament: FullTournamentDto,
    uiState: TournamentDetailUiState,
    green: Color,
    onOpenApplications: () -> Unit,
    onCloseApplications: () -> Unit,
    onFinalizeTeams: () -> Unit,
    onGenerateMatches: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark

    val effectiveStatus = tournament.effectiveStatus ?: tournament.status
    val statusColor = when (effectiveStatus.lowercase()) {
        "applications_open", "ongoing", "live", "approved" -> green
        "applications_closed", "finalized" -> Color(0xFF3B82F6)
        "pending_approval", "pending" -> AccentGold
        "rejected", "cancelled" -> StatusError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Banner or Logo
            if (!tournament.banner.isNullOrBlank() || !tournament.image.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                ) {
                    AsyncImage(
                        model = tournament.banner ?: tournament.image,
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiquidGlassBadge(
                            text = effectiveStatus.replace("_", " ").uppercase(),
                            badgeColor = statusColor
                        )

                        val variant = tournament.cricketVariant ?: ""
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

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF7C60F6).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = tournament.format.replace("_", " ").uppercase(),
                                color = Color(0xFF7C60F6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TournamentInfoPill(
                    icon = Icons.Outlined.Groups,
                    label = "Teams",
                    value = "${tournament.actualApprovedCount}/${tournament.actualTeamCapacity}",
                    modifier = Modifier.weight(1f)
                )
                TournamentInfoPill(
                    icon = Icons.Outlined.Assignment,
                    label = "Applications",
                    value = "${tournament.applicationsCount}",
                    modifier = Modifier.weight(1f)
                )
                TournamentInfoPill(
                    icon = Icons.Outlined.EmojiEvents,
                    label = "Prize Pool",
                    value = if (tournament.prizePool != null) "₹${tournament.prizePool}" else "Trophy",
                    modifier = Modifier.weight(1f)
                )
            }

            // Host Lifecycle Quick Action Bar
            if (uiState.isHost) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tournament.applicationsOpen) {
                        Button(
                            onClick = onCloseApplications,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close Registrations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onOpenApplications,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Registrations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (tournament.status.equals("applications_closed", true)) {
                        Button(
                            onClick = onFinalizeTeams,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C60F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finalize Teams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (tournament.status.equals("finalized", true)) {
                        Button(
                            onClick = onGenerateMatches,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Generate Matches", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Admin Approval Action Bar
            if (uiState.isStaff && !uiState.isApproved) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentInfoPill(
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
private fun TournamentTeamsTab(
    participations: List<TournamentParticipationDto>,
    applications: List<TournamentApplicationDto>,
    green: Color
) {
    if (participations.isEmpty() && applications.none { it.status == "approved" }) {
        LiquidGlassEmptyState(
            title = "No Teams Confirmed",
            description = "Teams approved by the tournament host will appear here.",
            icon = Icons.Outlined.Groups
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (participations.isNotEmpty()) {
                items(participations, key = { it.id }) { part ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    text = (part.teamName ?: "T").take(1).uppercase(),
                                    color = green,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.teamName ?: "Team",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val seedInfo = if (part.seed != null) "Seed #${part.seed} • " else ""
                                Text(
                                    text = "${seedInfo}${part.rosterMembers.size} Players in roster",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LiquidGlassBadge(text = "CONFIRMED", badgeColor = green)
                        }
                    }
                }
            } else {
                val approvedApps = applications.filter { it.status == "approved" }
                items(approvedApps, key = { it.id }) { app ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    text = (app.teamName ?: "T").take(1).uppercase(),
                                    color = green,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.teamName ?: "Team",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${app.submittedRosterCount} Registered Players",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LiquidGlassBadge(text = "APPROVED", badgeColor = green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentMatchesTab(
    matches: List<FullTournamentMatchDto>,
    green: Color,
    isHostOrStaff: Boolean,
    onAttachScoringLink: (String) -> Unit,
    onNavigateToMatchDetails: (String) -> Unit,
    onNavigateToLiveScoring: (String) -> Unit
) {
    if (matches.isEmpty()) {
        LiquidGlassEmptyState(
            title = "Brackets & Fixtures Not Generated",
            description = "Fixtures and brackets will be generated once team registrations are finalized.",
            icon = Icons.Outlined.SportsCricket
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(matches, key = { it.id }) { match ->
                val isLive = match.status.lowercase() in listOf("live", "in_progress", "ongoing")
                val isCompleted = match.status.lowercase() == "completed"

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val linkedId = match.linkedLeagueMatch ?: match.id
                        if (isLive || isCompleted) {
                            onNavigateToLiveScoring(linkedId)
                        } else {
                            onNavigateToMatchDetails(linkedId)
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LiquidGlassBadge(
                                text = (match.stage ?: match.round ?: "MATCH").uppercase(),
                                badgeColor = if (isLive) green else Color(0xFF3B82F6)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = match.scheduledAt ?: "Schedule TBD",
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
                                text = match.team1Name ?: "Team 1",
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
                                text = match.team2Name ?: "Team 2",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!match.winnerName.isNullOrBlank() || !match.resultNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = match.resultNote ?: "${match.winnerName} Won",
                                fontSize = 12.sp,
                                color = green,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (isHostOrStaff && match.linkedLeagueMatch.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onAttachScoringLink(match.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Default.Link, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attach Live Scoring Engine", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentPointsTab(
    participations: List<TournamentParticipationDto>,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark

    if (participations.isEmpty()) {
        LiquidGlassEmptyState(
            title = "Points Table Not Available",
            description = "Points table is active during league/group tournament stages.",
            icon = Icons.Outlined.Leaderboard
        )
    } else {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("P", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("PTS", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Black, fontSize = 11.sp, color = green, textAlign = TextAlign.Center)
                }

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))

                participations.forEachIndexed { idx, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(p.teamName ?: "Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text("0", modifier = Modifier.width(28.dp), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("0", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Black, fontSize = 13.sp, color = green, textAlign = TextAlign.Center)
                    }
                    if (idx < participations.size - 1) {
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentProfileTab(
    tournament: FullTournamentDto,
    green: Color
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tournament Overview & Rules", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(10.dp))

            if (!tournament.description.isNullOrBlank()) {
                Text("About", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = green)
                Text(tournament.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text("Venue Ground", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = green)
            val venueName = tournament.customVenueText?.ifBlank { null } ?: tournament.venues?.firstOrNull()?.name ?: "Venue Ground"
            Text(venueName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(14.dp))

            Text("Schedule & Dates", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = green)
            Text("Applications Open: ${tournament.applicationOpenDate ?: "TBD"}", fontSize = 12.sp)
            Text("Application Deadline: ${tournament.applicationDeadline ?: "TBD"}", fontSize = 12.sp)
            Text("Tournament Timeline: ${tournament.startDate ?: "TBD"} to ${tournament.endDate ?: "TBD"}", fontSize = 12.sp)
        }
    }
}

@Composable
private fun TournamentApplyModal(
    uiState: TournamentDetailUiState,
    green: Color,
    onDismiss: () -> Unit,
    onSelectTeam: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onSelectCaptain: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val t = uiState.tournament ?: return

    LiquidGlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(8.dp)
        ) {
            Text("Apply Team for Tournament", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Select your eligible team, squad roster, and team captain", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Step A: Select Eligible Team
            Text("1. Choose Your Team *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.eligibleTeams.isEmpty()) {
                Text("No eligible teams found. Create or join a team first.", fontSize = 11.sp, color = StatusError)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.eligibleTeams) { team ->
                        val isSelected = uiState.selectedTeamId == team.id
                        LiquidGlassFilterChip(
                            selected = isSelected,
                            onClick = { onSelectTeam(team.id) },
                            label = team.name
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step B: Roster Members Checklist
            if (uiState.selectedTeamId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. Build Squad Roster *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${uiState.selectedMemberIds.size} / ${t.minRosterSize}-${t.rosterSizeLimit} Players",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.selectedMemberIds.size >= t.minRosterSize) green else StatusError
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (uiState.loadingTeamMembers) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green, modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.teamMembers, key = { it.memberId }) { member ->
                            val isChecked = uiState.selectedMemberIds.contains(member.memberId)
                            val isCaptain = uiState.selectedCaptainId == member.memberId

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) green.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isChecked) green.copy(alpha = 0.4f) else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().clickable { onToggleMember(member.memberId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleMember(member.memberId) },
                                        colors = CheckboxDefaults.colors(checkedColor = green)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(member.role ?: "Player", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (isChecked) {
                                        Surface(
                                            onClick = { onSelectCaptain(member.memberId) },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCaptain) green else Color.Transparent,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, green)
                                        ) {
                                            Text(
                                                text = if (isCaptain) "CAPTAIN" else "Make Captain",
                                                color = if (isCaptain) Color.White else green,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step C: Team Note
                LiquidGlassTextField(
                    value = uiState.applicationTeamNote,
                    onValueChange = onUpdateNote,
                    placeholder = "Team note / remarks for host...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmittingApplication,
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSubmittingApplication) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Submit Application", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentApplicationsReviewModal(
    uiState: TournamentDetailUiState,
    green: Color,
    onDismiss: () -> Unit,
    onSelectTab: (String) -> Unit,
    onReview: (String, String, String?) -> Unit
) {
    val filterTabs = listOf("pending" to "Pending", "approved" to "Approved", "rejected" to "Rejected", "waitlisted" to "Waitlisted")

    val filteredApps = uiState.applications.filter {
        it.status.equals(uiState.selectedApplicationTab, ignoreCase = true)
    }

    LiquidGlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(8.dp)
        ) {
            Text("Manage Team Applications", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Review, approve, reject or waitlist incoming team registrations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filterTabs) { (key, label) ->
                    val isSelected = uiState.selectedApplicationTab == key
                    LiquidGlassFilterChip(
                        selected = isSelected,
                        onClick = { onSelectTab(key) },
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No ${uiState.selectedApplicationTab} applications found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.teamName ?: "Team", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        Text("Captain / Applicant: ${app.appliedByName ?: "Applicant"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${app.submittedRosterCount} Registered Squad Members", fontSize = 10.sp, color = green, fontWeight = FontWeight.Bold)
                                    }
                                    LiquidGlassBadge(
                                        text = app.status.uppercase(),
                                        badgeColor = if (app.status == "approved") green else AccentGold
                                    )
                                }

                                if (!app.teamNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Note: ${app.teamNote}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (app.status == "pending") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { onReview(app.id, "approved", null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = green),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("Approve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { onReview(app.id, "waitlisted", null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("Waitlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { onReview(app.id, "rejected", null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("Reject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

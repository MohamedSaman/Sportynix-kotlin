package com.sportynix.app.presentation.profile

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.R
import com.sportynix.app.data.remote.dto.LocationCityDto
import com.sportynix.app.domain.model.location.LocationCity
import com.sportynix.app.domain.model.location.LocationDistrict
import com.sportynix.app.domain.model.location.LocationProvince
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val SPORT_OPTIONS = listOf("Cricket", "Football", "Basketball", "Badminton", "Volleyball", "Tennis", "Chess", "Table Tennis")
private val TEAM_TYPE_OPTIONS = listOf(
    "friends" to "Friends",
    "community" to "Community",
    "competitive" to "Competitive"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    onNavigateBack: () -> Unit,
    initialTab: Int = 0,
    inviteToken: String? = null,
    teamId: Int? = null,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: TeamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isDark = LocalThemeController.current.isDark
    val green = if (isDark) NeonGreen else SportynixGreenPrimary

    var imageTarget by remember { mutableStateOf<String?>(null) }
    var logoPart by remember { mutableStateOf<TeamImagePart?>(null) }
    var coverPart by remember { mutableStateOf<TeamImagePart?>(null) }
    var logoUriPreview by remember { mutableStateOf<Uri?>(null) }
    var coverUriPreview by remember { mutableStateOf<Uri?>(null) }
    var removeLogoMarked by remember { mutableStateOf(false) }
    var removeCoverMarked by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf<TeamUi?>(null) }
    var showLeaveConfirmDialog by remember { mutableStateOf<TeamUi?>(null) }
    var showClearChatDialog by remember { mutableStateOf<Pair<TeamUi, String>?>(null) }
    var selectedJoinTeamForSheet by remember { mutableStateOf<TeamUi?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val part = uri.toMultipart(context, imageTarget ?: "image")
            if (imageTarget == "logo") {
                logoPart = part
                logoUriPreview = uri
                removeLogoMarked = false
            } else {
                coverPart = part
                coverUriPreview = uri
                removeCoverMarked = false
            }
        }
    }

    LaunchedEffect(initialTab) {
        viewModel.selectTab(TeamTab.values().getOrElse(initialTab.coerceIn(0, 2)) { TeamTab.MY_TEAMS })
    }

    LaunchedEffect(inviteToken) {
        if (!inviteToken.isNullOrBlank()) {
            viewModel.resolveInvite(inviteToken)
        }
    }

    LaunchedEffect(teamId, state.teams) {
        if (teamId != null && teamId > 0) {
            state.teams.firstOrNull { it.id == teamId }?.let { viewModel.openDetails(it) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TeamEvent.OpenChat -> onNavigateToChat(event.conversationId)
                is TeamEvent.ShareInvite -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my team on Sportynix!\n${event.link}")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Invite Link"))
                }
            }
        }
    }

    val error = state.error
    val message = state.message

    if (state.showDetails && state.selected != null) {
        TeamInfoScreen(
            state = state,
            green = green,
            vm = viewModel,
            onDeleteClick = { team -> showDeleteConfirmDialog = team },
            onLeaveClick = { team -> showLeaveConfirmDialog = team },
            onClearChatClick = { team, chatType -> showClearChatDialog = team to chatType }
        )
    } else {
        Scaffold(
            containerColor = if (isDark) DarkBackground else LightBackground,
            floatingActionButton = {
                if (state.tab == TeamTab.MY_TEAMS) {
                    FloatingActionButton(
                        onClick = {
                            removeLogoMarked = false
                            removeCoverMarked = false
                            logoPart = null
                            coverPart = null
                            logoUriPreview = null
                            coverUriPreview = null
                            viewModel.openCreate()
                        },
                        containerColor = green,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Team", modifier = Modifier.size(26.dp))
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Bar (Screenshot 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Team",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Main Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isDark) GlassSurfaceDark else GlassSurfaceLight)
                        .border(
                            width = 1.dp,
                            color = if (isDark) GlassBorderDark else GlassBorderLight,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TeamTab.values().forEach { tab ->
                        val selected = tab == state.tab
                        val countBadge = when (tab) {
                            TeamTab.MY_TEAMS -> if (state.teams.isNotEmpty()) state.teams.size.toString() else null
                            TeamTab.JOIN -> null
                            TeamTab.INVITATIONS -> if (state.received.isNotEmpty()) state.received.size.toString() else null
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (selected) {
                                        Brush.horizontalGradient(listOf(Color(0xFF00B86B), Color(0xFF00E58A)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { viewModel.selectTab(tab) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (countBadge != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) Color.White.copy(alpha = 0.3f) else green.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = countBadge,
                                            color = if (selected) Color.White else green,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Content
                Crossfade(targetState = state.tab, label = "TeamTabTransition") { tab ->
                    when (tab) {
                        TeamTab.MY_TEAMS -> MyTeamsSection(
                            state = state,
                            green = green,
                            vm = viewModel,
                            onDeleteClick = { showDeleteConfirmDialog = it },
                            onLeaveClick = { showLeaveConfirmDialog = it },
                            onClearChatClick = { team, type -> showClearChatDialog = team to type }
                        )
                        TeamTab.JOIN -> DiscoverTeamsSection(
                            state = state,
                            green = green,
                            vm = viewModel,
                            onOpenJoinSheet = { selectedJoinTeamForSheet = it }
                        )
                        TeamTab.INVITATIONS -> InvitationsSection(state = state, green = green, vm = viewModel)
                    }
                }
            }
        }
    }

    // Join Team Details Sheet (Screenshot 5)
    if (selectedJoinTeamForSheet != null) {
        JoinTeamDetailsSheet(
            team = selectedJoinTeamForSheet!!,
            green = green,
            vm = viewModel,
            onDismiss = { selectedJoinTeamForSheet = null }
        )
    }

    // Full Screen Image Preview Overlay
    if (state.previewImageUrl != null) {
        Dialog(
            onDismissRequest = viewModel::closeImagePreview,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { viewModel.closeImagePreview() },
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = viewModel::closeImagePreview,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                AsyncImage(
                    model = state.previewImageUrl,
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Create / Edit Dialog
    if (state.showForm) {
        TeamFormDialog(
            state = state,
            green = green,
            vm = viewModel,
            onPickLogo = {
                imageTarget = "logo"
                picker.launch("image/*")
            },
            onPickCover = {
                imageTarget = "cover"
                picker.launch("image/*")
            },
            logoPart = logoPart,
            coverPart = coverPart,
            logoPreview = logoUriPreview,
            coverPreview = coverUriPreview,
            removeLogoMarked = removeLogoMarked,
            removeCoverMarked = removeCoverMarked,
            onRemoveLogo = {
                logoPart = null
                logoUriPreview = null
                removeLogoMarked = true
            },
            onRemoveCover = {
                coverPart = null
                coverUriPreview = null
                removeCoverMarked = true
            },
            onSave = {
                viewModel.saveTeam(logoPart, coverPart, removeLogoMarked, removeCoverMarked)
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        val teamToDelete = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Team", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${teamToDelete.name}'? Active members must be removed first or force delete used.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = null
                            viewModel.delete(teamToDelete, force = true)
                        }
                    ) {
                        Text("Force Delete", color = StatusError, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = null
                            viewModel.delete(teamToDelete, force = false)
                        }
                    ) {
                        Text("Delete", color = StatusError)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Leave Confirmation Dialog
    if (showLeaveConfirmDialog != null) {
        val teamToLeave = showLeaveConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = null },
            title = { Text("Leave Team", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave '${teamToLeave.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmDialog = null
                        viewModel.leave(teamToLeave)
                    }
                ) {
                    Text("Leave", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog != null) {
        val (teamToClear, chatType) = showClearChatDialog!!
        val typeLabel = if (chatType == "channel") "Channel Announcements" else "Group Chat"
        AlertDialog(
            onDismissRequest = { showClearChatDialog = null },
            title = { Text("Clear $typeLabel", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all messages in $typeLabel for '${teamToClear.name}'? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = null
                        viewModel.clearTeamChat(teamToClear.id, chatType)
                    }
                ) {
                    Text("Clear Messages", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Message / Error Snackbar Alert
    if (!error.isNullOrBlank() || !message.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(if (error != null) "Team Alert" else "Success", fontWeight = FontWeight.Bold) },
            text = { Text(error ?: message.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text("OK", color = green, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ============================================================================
// TAB 1: MY TEAMS
// ============================================================================

@Composable
private fun MyTeamsSection(
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onDeleteClick: (TeamUi) -> Unit,
    onLeaveClick: (TeamUi) -> Unit,
    onClearChatClick: (TeamUi, String) -> Unit
) {
    val filteredTeams = remember(state.teams, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.teams
        else state.teams.filter {
            it.name.contains(state.searchQuery, ignoreCase = true) ||
            it.location.contains(state.searchQuery, ignoreCase = true) ||
            it.type.contains(state.searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.teams.isNotEmpty()) {
            item {
                LiquidGlassSearchBar(
                    query = state.searchQuery,
                    onQueryChange = vm::setSearchQuery,
                    placeholder = "Search my teams...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (filteredTeams.isEmpty()) {
            item {
                if (state.teams.isEmpty()) {
                    LiquidGlassEmptyState(
                        title = "No Teams Joined Yet",
                        description = "Create a team to invite your squad, manage matches, and join leagues!",
                        icon = Icons.Outlined.Groups,
                        actionText = "Create a Team",
                        onActionClick = vm::openCreate
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No teams matching '${state.searchQuery}'", color = Color.Gray)
                    }
                }
            }
        }

        items(filteredTeams, key = { it.id }) { team ->
            MyTeamCard(
                team = team,
                green = green,
                onClick = { vm.openDetails(team) },
                onEdit = { vm.openEdit(team) },
                onDeleteClick = { onDeleteClick(team) },
                onLeaveClick = { onLeaveClick(team) },
                onClearChatClick = { chatType -> onClearChatClick(team, chatType) }
            )
        }
    }
}

// ============================================================================
// TAB 2: DISCOVER / JOIN TEAMS (Screenshot 4)
// ============================================================================

@Composable
private fun DiscoverTeamsSection(
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onOpenJoinSheet: (TeamUi) -> Unit
) {
    val filteredDiscover = remember(state.discover, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.discover
        else state.discover.filter {
            it.name.contains(state.searchQuery, ignoreCase = true) ||
            it.location.contains(state.searchQuery, ignoreCase = true) ||
            it.type.contains(state.searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.invitePreview != null) {
            item {
                InvitePreviewCard(
                    team = state.invitePreview,
                    requiresAuth = state.inviteRequiresAuth,
                    green = green,
                    vm = vm
                )
            }
        }

        item {
            LiquidGlassSearchBar(
                query = state.searchQuery,
                onQueryChange = vm::setSearchQuery,
                placeholder = "Search teams...",
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filteredDiscover.isEmpty() && state.invitePreview == null) {
            item {
                LiquidGlassEmptyState(
                    title = "No Public Teams Found",
                    description = "There are currently no public teams available to join in your area.",
                    icon = Icons.Outlined.TravelExplore,
                    actionText = "Refresh List",
                    onActionClick = vm::loadDiscover
                )
            }
        }

        items(filteredDiscover, key = { it.id }) { team ->
            JoinTeamCard(
                team = team,
                green = green,
                onClick = { onOpenJoinSheet(team) },
                onJoinClick = {
                    if (team.joinStatus == JoinStatus.REQUESTED) vm.cancelJoin(team) else vm.requestJoin(team)
                }
            )
        }
    }
}

// ============================================================================
// TAB 3: INVITATIONS
// ============================================================================

@Composable
private fun InvitationsSection(state: TeamState, green: Color, vm: TeamViewModel) {
    val isDark = LocalThemeController.current.isDark
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                InvitationsSubTab.RECEIVED to "Received (${state.received.size})",
                InvitationsSubTab.SENT to "Sent (${state.sent.size})"
            ).forEach { (subTab, label) ->
                val selected = state.invitationsTab == subTab
                Surface(
                    onClick = { vm.selectInvitationsSubTab(subTab) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) green.copy(alpha = 0.18f) else if (isDark) GlassSurfaceDark else GlassSurfaceLight,
                    border = BorderStroke(1.dp, if (selected) green else if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) green else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        val list = if (state.invitationsTab == InvitationsSubTab.SENT) state.sent else state.received

        if (list.isEmpty()) {
            LiquidGlassEmptyState(
                title = if (state.invitationsTab == InvitationsSubTab.SENT) "No Sent Invitations" else "No Pending Invitations",
                description = if (state.invitationsTab == InvitationsSubTab.SENT)
                    "Team invitations you send to players will appear here."
                else "Invitations sent to you by team captains will appear here.",
                icon = Icons.Outlined.MailOutline
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    InvitationCard(
                        item = item,
                        isSent = state.invitationsTab == InvitationsSubTab.SENT,
                        green = green,
                        vm = vm
                    )
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTS: CARDS & PREVIEWS (Enhanced with Default Asset Banner)
// ============================================================================

@Composable
private fun PremiumTeamCard(
    team: TeamUi,
    green: Color,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onChat: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    joinAction: (() -> Unit)? = null
) {
    val isDark = LocalThemeController.current.isDark
    val isRequested = team.joinStatus == JoinStatus.REQUESTED

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp
    ) {
        Column {
            // Cover Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                if (!team.cover.isNullOrBlank()) {
                    AsyncImage(
                        model = team.cover,
                        contentDescription = "Team Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.team_banner_placeholder),
                        contentDescription = "Default Team Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                // Role badge or capacity badge on top right of cover
                if (team.role != null) {
                    val roleBg = when (team.role) {
                        "Captain" -> Color(0xFFFFB300)
                        "Admin" -> green
                        else -> Color.Gray
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = roleBg
                    ) {
                        Text(
                            text = team.role.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "${team.membersCount}/${team.maxMembers}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Details section below banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team Logo Avatar
                if (!team.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = team.logo,
                        contentDescription = team.name,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, green, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(green)
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        // Friends / Type Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(green.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = team.type.replaceFirstChar { it.uppercase() },
                                color = green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (team.isPublic != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(green.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (team.isPublic) "Public" else "Private",
                                    color = green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "${team.membersCount}/${team.maxMembers} members",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    if (team.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = team.location,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action buttons on right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onChat != null && team.role != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(green)
                                .clickable { onChat() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Chat",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onEdit != null && (team.role == "Captain" || team.role == "Admin")) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onEdit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (joinAction != null) {
                        Button(
                            onClick = joinAction,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRequested) Color(0xFFD97706) else green,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isRequested) "Requested" else "Join",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Wide Action Button if requested on Join tab
            if (joinAction != null && isRequested) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Button(
                        onClick = joinAction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = green,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Request", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    item: TeamMembershipUi,
    isSent: Boolean,
    green: Color,
    vm: TeamViewModel
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imgUrl = if (isSent) item.avatar else item.teamLogo
            if (!imgUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(green.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSent) Icons.Default.Person else Icons.Default.Groups,
                        contentDescription = null,
                        tint = green
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.teamName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSent) "Invitation to ${item.name}" else "Invited by ${item.invitedBy.ifBlank { "Team Captain" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                LiquidGlassBadge(
                    text = item.status.replaceFirstChar { it.uppercase() },
                    badgeColor = when (item.status) {
                        "approved" -> green
                        "rejected" -> StatusError
                        else -> Color(0xFFFFB300)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!isSent && item.status == "invited") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { vm.acceptInvitation(item) }) {
                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = green)
                    }
                    IconButton(onClick = { vm.rejectInvitation(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = StatusError)
                    }
                }
            } else if (isSent && (item.status == "requested" || item.status == "invited")) {
                IconButton(onClick = { vm.cancelSentInvitation(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel Invitation", tint = StatusError)
                }
            }
        }
    }
}

@Composable
private fun InvitePreviewCard(
    team: TeamUi,
    requiresAuth: Boolean,
    green: Color,
    vm: TeamViewModel
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        borderColor = green.copy(alpha = 0.5f),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = green)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Invite Link Preview", color = green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(team.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${team.membersCount}/${team.maxMembers} members", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))
            LiquidGlassButton(
                text = if (requiresAuth) "Sign In to Join" else if (team.joinStatus == JoinStatus.REQUESTED) "Request Sent" else "Request to Join",
                onClick = { if (!requiresAuth) vm.requestInviteJoin() },
                enabled = !requiresAuth && team.joinStatus == JoinStatus.NONE,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// TEAM DETAILS BOTTOM SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamInfoScreen(
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onDeleteClick: (TeamUi) -> Unit,
    onLeaveClick: (TeamUi) -> Unit,
    onClearChatClick: (TeamUi, String) -> Unit
) {
    val team = state.selected ?: return
    val isDark = LocalThemeController.current.isDark
    val canManage = team.role == "Captain" || team.role == "Admin"
    val isCaptain = team.role == "Captain"

    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showPendingRequestsSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Inappropriate Content") }

    Scaffold(
        containerColor = if (isDark) DarkBackground else LightBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) DarkSurface else Color.White)
                        .border(1.dp, if (isDark) GlassBorderDark else Color(0xFFE5E7EB), CircleShape)
                        .clickable { vm.closeDetails() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Team Info",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))

                if (canManage) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(green)
                            .clickable { vm.openEdit(team) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Team",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(38.dp))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Cover & Overlapping Avatar (Screenshots 1 & 2)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        // Cover Photo Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            if (!team.cover.isNullOrBlank()) {
                                AsyncImage(
                                    model = team.cover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { vm.openImagePreview(team.cover) },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.team_banner_placeholder),
                                    contentDescription = "Default Team Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Dark Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(0.3f), Color.Black.copy(0.65f))
                                        )
                                    )
                            )

                            // Role Badge top right of cover if Captain/Admin (Screenshot 2)
                            if (team.role != null && (team.role == "Captain" || team.role == "Admin")) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    shape = CircleShape,
                                    color = if (isCaptain) Color(0xFFFFB300) else green
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCaptain) Icons.Default.EmojiEvents else Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = team.role.uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            // Top Left Close Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable { vm.closeDetails() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Centered Overlapping Avatar (Screenshots 1 & 2)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(green)
                                .border(4.dp, if (isDark) DarkBackground else LightBackground, CircleShape)
                                .clickable { if (!team.logo.isNullOrBlank()) vm.openImagePreview(team.logo) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!team.logo.isNullOrBlank()) {
                                AsyncImage(
                                    model = team.logo,
                                    contentDescription = team.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = team.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Team Name & Location Pill Centered (Screenshots 1 & 2)
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = team.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (team.location.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = green,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = team.location,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = " • ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                imageVector = if (team.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (team.isPublic) "Public" else "Private",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3 Stat Cards Row (Members, Max, Created) (Screenshots 1 & 2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailStatCard(
                            icon = Icons.Default.Groups,
                            iconBg = green.copy(alpha = 0.15f),
                            iconTint = green,
                            value = "${team.membersCount}",
                            valueColor = green,
                            label = "Members",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatCard(
                            icon = Icons.Default.GroupAdd,
                            iconBg = green.copy(alpha = 0.15f),
                            iconTint = green,
                            value = "${team.maxMembers}",
                            valueColor = green,
                            label = "Max",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatCard(
                            icon = Icons.Default.CalendarToday,
                            iconBg = green.copy(alpha = 0.15f),
                            iconTint = green,
                            value = "Created",
                            valueColor = green,
                            label = "19 Aug 2026",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick Actions Section (Screenshots 1 & 2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (canManage) {
                            QuickActionButton(
                                icon = Icons.Default.PersonAdd,
                                label = "Add\nMember",
                                color = green,
                                onClick = { showAddMemberSheet = true }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Schedule,
                                label = "Pending\nMembers",
                                color = Color(0xFFF59E0B),
                                onClick = { showPendingRequestsSheet = true }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Chat,
                                label = "Message\nTeam",
                                color = Color(0xFFA855F7),
                                onClick = { vm.openChat(team) }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Link,
                                label = "Invite\nLink",
                                color = Color(0xFF3B82F6),
                                onClick = { vm.generateInvite(team) }
                            )
                        } else {
                            QuickActionButton(
                                icon = Icons.Default.Groups,
                                label = "Team\nMembers",
                                color = green,
                                onClick = { showAddMemberSheet = true }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Chat,
                                label = "Message\nTeam",
                                color = Color(0xFFA855F7),
                                onClick = { vm.openChat(team) }
                            )
                        }
                    }
                }

                // Details Card Section (Screenshots 1 & 2)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF131F1A) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(green),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Team Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))

                        DetailRowItem(icon = Icons.Default.LocationOn, label = "Location", value = team.location.ifBlank { "Not set" })
                        DetailRowItem(icon = Icons.Default.LocalOffer, label = "Team Type", value = team.type.replaceFirstChar { it.uppercase() })
                        DetailRowItem(icon = Icons.Default.Groups, label = "Members", value = "${team.membersCount} / ${team.maxMembers}")
                        DetailRowItem(icon = Icons.Default.CalendarToday, label = "Created", value = "19 Aug 2026, 4:28 PM")
                        DetailRowItem(icon = Icons.Default.ConfirmationNumber, label = "Total Booking", value = "0")
                        DetailRowItem(icon = Icons.Default.Public, label = "Visibility", value = if (team.isPublic) "Public Team" else "Private Team")
                        DetailRowItem(
                            icon = Icons.Default.Shield,
                            label = "Your Role",
                            value = team.role ?: "Member",
                            valueColor = if (isCaptain) Color(0xFFFFB300) else if (canManage) green else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Members Card Section (Screenshots 3 & 4)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF131F1A) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${state.members.size} Members",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (canManage) {
                                TextButton(onClick = { showAddMemberSheet = true }) {
                                    Text("+ Add", color = green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        state.members.forEach { member ->
                            MemberRow(
                                member = member,
                                teamRole = team.role,
                                green = green,
                                onRemove = { vm.removeMember(member) },
                                onMakeAdmin = { vm.addAdmin(member) },
                                onRemoveAdmin = { vm.removeAdmin(member) }
                            )
                        }
                    }
                }

                // Media, Links & Docs Section (Screenshots 3 & 4)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF131F1A) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Media, Links & Docs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E3A5F)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Photo, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Photos", fontSize = 12.sp, color = Color.White)
                                    Text("0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF3F3218)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Events", fontSize = 12.sp, color = Color.White)
                                    Text("0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Danger Zone Section (Screenshots 3 & 4)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF131F1A) else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        if (canManage) {
                            DangerZoneItem(
                                title = "Clear Group Chat",
                                subtitle = "Delete all messages in group chat",
                                icon = Icons.Default.DeleteSweep,
                                iconColor = Color(0xFFF59E0B),
                                onClick = { onClearChatClick(team, "team_group") }
                            )
                            HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                            DangerZoneItem(
                                title = "Clear Channel",
                                subtitle = "Delete all announcements",
                                icon = Icons.Default.DeleteSweep,
                                iconColor = Color(0xFFF59E0B),
                                onClick = { onClearChatClick(team, "channel") }
                            )
                            HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                        }

                        if (isCaptain) {
                            DangerZoneItem(
                                title = "Delete Team",
                                subtitle = "Permanently delete this team",
                                icon = Icons.Default.Delete,
                                iconColor = StatusError,
                                isDestructive = true,
                                onClick = { onDeleteClick(team) }
                            )
                        } else {
                            DangerZoneItem(
                                title = "Leave Team",
                                subtitle = "Leave this team",
                                icon = Icons.Default.Logout,
                                iconColor = StatusError,
                                isDestructive = true,
                                onClick = { onLeaveClick(team) }
                            )
                            HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                            DangerZoneItem(
                                title = "Report Team",
                                subtitle = "Report inappropriate behavior",
                                icon = Icons.Default.Warning,
                                iconColor = StatusError.copy(alpha = 0.8f),
                                isDestructive = true,
                                onClick = { showReportDialog = true }
                            )
                        }
                    }
                }
            }

            // Bottom Sticky Primary Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0D1612) else Color(0xFFF9FAFB))
                    .padding(16.dp)
            ) {
                if (canManage) {
                    Button(
                        onClick = { vm.openEdit(team) },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Edit Team Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else if (team.role == "Member") {
                    Button(
                        onClick = { onLeaveClick(team) },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Leave Team", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    val isRequested = team.joinStatus == JoinStatus.REQUESTED
                    Button(
                        onClick = {
                            if (isRequested) vm.cancelJoin(team) else vm.requestJoin(team)
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRequested) Color(0xFFD97706) else green
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (isRequested) "Cancel Request" else "Request to Join",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Add / Invite Members Sheet (Screenshot 5)
    if (showAddMemberSheet) {
        AddMemberSheet(
            team = team,
            state = state,
            green = green,
            vm = vm,
            onDismiss = { showAddMemberSheet = false },
            onOpenPending = {
                showAddMemberSheet = false
                showPendingRequestsSheet = true
            }
        )
    }

    // Pending Requests Sheet
    if (showPendingRequestsSheet) {
        PendingRequestsSheet(
            state = state,
            green = green,
            vm = vm,
            onDismiss = { showPendingRequestsSheet = false }
        )
    }

    // Report Team Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Team", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reason for reporting:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("Inappropriate Content", "Spam or Scam", "Harassment", "Fake Team", "Other").forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = reportReason == reason, onClick = { reportReason = reason })
                            Text(reason, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                    }
                ) {
                    Text("Submit Report", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DangerZoneItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) iconColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    team: TeamUi,
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onDismiss: () -> Unit,
    onOpenPending: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val bgDark = if (isDark) Color(0xFF0D1612) else Color(0xFFF9FAFB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgDark,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Gradient Header (Screenshot 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F7A4A), Color(0xFF18A665), Color(0xFF31C86F))
                        )
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(team.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${state.members.size} Member",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Invite New Members", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                // Search Input Field (Screenshot 5)
                LiquidGlassSearchBar(
                    query = state.inviteSearchQuery,
                    onQueryChange = vm::onInviteQueryChanged,
                    placeholder = "Search by name, email or phone...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Option Card 1: Share Invite Link (Screenshot 5)
                Surface(
                    onClick = { vm.generateInvite(team) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF131F1A) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(green.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = green, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share Invite Link", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Anyone with this link can join", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Share, contentDescription = null, tint = green, modifier = Modifier.size(18.dp))
                    }
                }

                // Option Card 2: Pending Members (Screenshot 5)
                Surface(
                    onClick = onOpenPending,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF131F1A) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pending Members", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("View and approve requests", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Searched Users Results
                if (state.searchedUsers.isNotEmpty()) {
                    Text("Search Results", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.searchedUsers.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(green),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (user.email.isNotBlank()) {
                                    Text(user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = { vm.invite(user) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingRequestsSheet(
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onDismiss: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF0D1612) else Color(0xFFF9FAFB),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pending Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (state.pending.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pending join requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                state.pending.forEach { request ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(green),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(request.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(request.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Requested to join", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.approve(request) }) {
                            Icon(Icons.Default.Check, contentDescription = "Approve", tint = green)
                        }
                        IconButton(onClick = { vm.reject(request) }) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", tint = StatusError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val isDark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
        border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailStatCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    valueColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131F1A) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun MemberRow(
    member: TeamMemberUi,
    teamRole: String?,
    green: Color,
    onRemove: () -> Unit,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!member.avatar.isNullOrBlank()) {
            AsyncImage(
                model = member.avatar,
                contentDescription = member.name,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(green),
                contentAlignment = Alignment.Center
            ) {
                Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "${member.role}${if (member.username.isBlank()) "" else " • @${member.username}"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        if ((teamRole == "Captain" || teamRole == "Admin") && member.role != "Captain") {
            if (teamRole == "Captain") {
                TextButton(onClick = if (member.role == "Admin") onRemoveAdmin else onMakeAdmin) {
                    Text(if (member.role == "Admin") "Demote" else "Promote", fontSize = 12.sp, color = green)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = StatusError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RecentMatchCard(match: RecentMatchUi, green: Color) {
    val resultColor = when (match.result.lowercase()) {
        "win" -> green
        "loss" -> StatusError
        else -> Color.Gray
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "vs ${match.opponentName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LiquidGlassBadge(
                        text = match.result.uppercase(),
                        badgeColor = resultColor
                    )
                }
                if (match.matchDate.isNotBlank()) {
                    Text(match.matchDate, color = Color.Gray, fontSize = 11.sp)
                }
                if (match.teamScore != null && match.opponentScore != null) {
                    Text(
                        text = "Score: ${match.teamScore.runs}/${match.teamScore.wickets} vs ${match.opponentScore.runs}/${match.opponentScore.wickets}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ============================================================================
// REDESIGNED CREATE / EDIT TEAM FORM (Matches Reference Screenshots & Theme)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamFormDialog(
    state: TeamState,
    green: Color,
    vm: TeamViewModel,
    onPickLogo: () -> Unit,
    onPickCover: () -> Unit,
    logoPart: TeamImagePart?,
    coverPart: TeamImagePart?,
    logoPreview: Uri?,
    coverPreview: Uri?,
    removeLogoMarked: Boolean,
    removeCoverMarked: Boolean,
    onRemoveLogo: () -> Unit,
    onRemoveCover: () -> Unit,
    onSave: () -> Unit
) {
    val form = state.form
    var cityMenuExpanded by remember { mutableStateOf(false) }

    val isDark = LocalThemeController.current.isDark
    val bgDark = if (isDark) DarkBackground else LightBackground
    val cardDark = if (isDark) DarkSurface else Color(0xFFFFFFFF)
    val borderDark = if (isDark) GlassBorderDark else Color(0xFFD0E0D6)
    val textPrimary = if (isDark) Color.White else Color(0xFF111814)
    val textSecondary = if (isDark) Color(0xFF8A9A90) else Color(0xFF5A6A60)
    val accentGreen = if (isDark) Color(0xFF00B86B) else Color(0xFF0D8A4F)

    Dialog(
        onDismissRequest = vm::closeForm,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgDark
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. STICKY HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgDark)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = vm::closeForm,
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, borderDark)
                    ) {
                        Text(
                            text = "Cancel",
                            color = accentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = if (state.editing) "Edit Team" else "Create Team",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(60.dp))
                }

                // 2. SCROLLABLE FORM CONTENT
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: COVER PHOTO
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.Image,
                            text = "Cover Photo",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, accentGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .clickable { onPickCover() }
                        ) {
                            if (coverPreview != null) {
                                AsyncImage(
                                    model = coverPreview,
                                    contentDescription = "Cover Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!form.coverUrl.isNullOrBlank() && !removeCoverMarked) {
                                AsyncImage(
                                    model = form.coverUrl,
                                    contentDescription = "Cover Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.team_banner_placeholder),
                                        contentDescription = "Default Cover Banner",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Add Cover",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap to add cover",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: TEAM PHOTO (AVATAR)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clickable { onPickLogo() }
                            ) {
                                if (logoPreview != null) {
                                    AsyncImage(
                                        model = logoPreview,
                                        contentDescription = "Team Photo",
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, accentGreen, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (!form.logoUrl.isNullOrBlank() && !removeLogoMarked) {
                                    AsyncImage(
                                        model = form.logoUrl,
                                        contentDescription = "Team Photo",
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, accentGreen, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF0A3D28) else Color(0xFFE2F3EA))
                                            .border(2.dp, accentGreen.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = "Team Photo",
                                            tint = accentGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accentGreen)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add Team Photo",
                                color = accentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onPickLogo() }
                            )
                        }
                    }

                    // SECTION 3: TEAM NAME
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.Edit,
                            text = "Team Name",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CreateTeamInputField(
                            value = form.name,
                            onValueChange = { value -> vm.updateForm { it.copy(name = value) } },
                            placeholder = "Enter team name",
                            singleLine = true,
                            cardDark = cardDark,
                            borderDark = borderDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen
                        )
                    }

                    // SECTION 4: TEAM VISIBILITY CARD
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.Public,
                            text = "Team Visibility",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = cardDark,
                            border = BorderStroke(1.dp, borderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Public Team",
                                        color = textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Anyone can find and request to join",
                                        color = textSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                Switch(
                                    checked = form.isPublic,
                                    onCheckedChange = { value -> vm.updateForm { it.copy(isPublic = value) } },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = accentGreen,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = if (isDark) Color(0xFF1E2A22) else Color(0xFFE0E0E0)
                                    )
                                )
                            }
                        }
                    }

                    // SECTION 5: CITY SELECTOR
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.LocationCity,
                            text = "City",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            onClick = { vm.openLocationPicker() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = cardDark,
                            border = BorderStroke(1.dp, borderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = form.city.ifBlank { "Search and select a city" },
                                    color = if (form.city.isBlank()) textSecondary.copy(alpha = 0.7f) else textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (form.city.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search City",
                                    tint = accentGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // SECTION 6: DISTRICT & PROVINCE ROW
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                CreateTeamSectionLabel(
                                    icon = Icons.Default.Place,
                                    text = "District",
                                    color = accentGreen,
                                    textColor = textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CreateTeamInputField(
                                    value = form.district,
                                    onValueChange = {},
                                    placeholder = "Auto-filled from city",
                                    readOnly = true,
                                    singleLine = true,
                                    cardDark = cardDark,
                                    borderDark = borderDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    accentGreen = accentGreen
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                CreateTeamSectionLabel(
                                    icon = Icons.Default.Map,
                                    text = "Province",
                                    color = accentGreen,
                                    textColor = textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CreateTeamInputField(
                                    value = form.province,
                                    onValueChange = {},
                                    placeholder = "Auto-filled from city",
                                    readOnly = true,
                                    singleLine = true,
                                    cardDark = cardDark,
                                    borderDark = borderDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    accentGreen = accentGreen
                                )
                            }
                        }
                    }

                    // SECTION 7: TEAM TYPE
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.LocalOffer,
                            text = "Team Type",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TEAM_TYPE_OPTIONS.forEach { (value, label) ->
                                val selected = form.teamType == value
                                Surface(
                                    onClick = { vm.updateForm { it.copy(teamType = value) } },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) accentGreen else cardDark,
                                    border = BorderStroke(1.dp, if (selected) accentGreen else borderDark)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) Color.White else textPrimary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 8: MAX MEMBERS
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.People,
                            text = "Max Members",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CreateTeamInputField(
                            value = form.maxMembers,
                            onValueChange = { value ->
                                val digits = value.filter { it.isDigit() }
                                val clamped = digits.toIntOrNull()?.coerceIn(1, 100)?.toString() ?: digits
                                vm.updateForm { it.copy(maxMembers = clamped) }
                            },
                            placeholder = "20",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cardDark = cardDark,
                            borderDark = borderDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen
                        )
                    }

                    // SECTION 9: INVITE LINK EXPIRY
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.AccessTime,
                            text = "Invite Link Expiry",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Days (0-100)", color = textSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                                CreateTeamInputField(
                                    value = form.expiryDays,
                                    onValueChange = { value ->
                                        val digits = value.filter { it.isDigit() }
                                        val clamped = digits.toIntOrNull()?.coerceIn(0, 100)?.toString() ?: digits
                                        vm.updateForm { it.copy(expiryDays = clamped) }
                                    },
                                    placeholder = "14",
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cardDark = cardDark,
                                    borderDark = borderDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    accentGreen = accentGreen
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hours (0-23)", color = textSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                                CreateTeamInputField(
                                    value = form.expiryHours,
                                    onValueChange = { value ->
                                        val digits = value.filter { it.isDigit() }
                                        val clamped = digits.toIntOrNull()?.coerceIn(0, 23)?.toString() ?: digits
                                        vm.updateForm { it.copy(expiryHours = clamped) }
                                    },
                                    placeholder = "0",
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cardDark = cardDark,
                                    borderDark = borderDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    accentGreen = accentGreen
                                )
                            }
                        }
                    }

                    // SECTION 10: DESCRIPTION
                    item {
                        CreateTeamSectionLabel(
                            icon = Icons.Default.Notes,
                            text = "Description (optional)",
                            color = accentGreen,
                            textColor = textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CreateTeamInputField(
                            value = form.description,
                            onValueChange = { value -> vm.updateForm { it.copy(description = value) } },
                            placeholder = "Describe your team...",
                            singleLine = false,
                            cardDark = cardDark,
                            borderDark = borderDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen,
                            modifier = Modifier.height(120.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 3. BOTTOM CREATE TEAM BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgDark)
                        .padding(16.dp)
                ) {
                    Surface(
                        onClick = onSave,
                        enabled = !state.saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = accentGreen
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (state.editing) "Saving Changes..." else "Creating Team...",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.editing) "Save Changes" else "Create Team",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (state.showLocationPickerSheet) {
                LocationPickerBottomSheet(
                    state = state,
                    green = accentGreen,
                    vm = vm
                )
            }
        }
    }
}

@Composable
private fun CreateTeamSectionLabel(
    icon: ImageVector,
    text: String,
    color: Color = Color(0xFF00E58A),
    textColor: Color = Color(0xFF8A9A90)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CreateTeamInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    cardDark: Color = Color(0xFF111814),
    borderDark: Color = Color(0xFF1B2E24),
    textPrimary: Color = Color.White,
    textSecondary: Color = Color(0xFF8A9A90),
    accentGreen: Color = Color(0xFF00B86B),
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        placeholder = { Text(placeholder, color = textSecondary.copy(alpha = 0.7f), fontSize = 14.sp) },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp)) }
        },
        trailingIcon = trailingIcon?.let {
            { Icon(it, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp)) }
        },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentGreen,
            unfocusedBorderColor = borderDark,
            disabledBorderColor = borderDark,
            focusedContainerColor = cardDark,
            unfocusedContainerColor = cardDark,
            disabledContainerColor = cardDark,
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary,
            disabledTextColor = textSecondary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

private fun Uri.toMultipart(context: android.content.Context, field: String): TeamImagePart? = runCatching {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(this)?.use { it.readBytes() } ?: return null
    val type = resolver.getType(this) ?: "image/jpeg"
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "jpg"
    TeamImagePart(
        MultipartBody.Part.createFormData(
            field,
            "team_${field}_${System.currentTimeMillis()}.$ext",
            bytes.toRequestBody(type.toMediaType())
        )
    )
}.getOrNull()

private val TeamTab.label: String
    get() = when (this) {
        TeamTab.MY_TEAMS -> "My Teams"
        TeamTab.JOIN -> "Join"
        TeamTab.INVITATIONS -> "Invitations"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerBottomSheet(
    state: TeamState,
    green: Color,
    vm: TeamViewModel
) {
    val isDark = LocalThemeController.current.isDark
    val bgDark = if (isDark) Color(0xFF07100C) else Color(0xFFF4F7F5)
    val cardDark = if (isDark) Color(0xFF111814) else Color(0xFFFFFFFF)
    val borderDark = if (isDark) Color(0xFF1B2E24) else Color(0xFFD0E0D6)
    val textPrimary = if (isDark) Color.White else Color(0xFF111814)
    val textSecondary = if (isDark) Color(0xFF8A9A90) else Color(0xFF5A6A60)

    ModalBottomSheet(
        onDismissRequest = vm::closeLocationPicker,
        containerColor = bgDark,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = borderDark
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = vm::closeLocationPicker,
                    shape = RoundedCornerShape(18.dp),
                    color = cardDark,
                    border = BorderStroke(1.dp, borderDark)
                ) {
                    Text(
                        text = "Close",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Select Home City",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. PROVINCE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PROVINCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    if (state.loadingProvinces) {
                        CircularProgressIndicator(
                            color = green,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp).padding(vertical = 4.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.provinces.forEach { province ->
                                val isSelected = state.selectedProvinceId == province.id
                                Surface(
                                    onClick = { vm.selectProvince(province.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) green else cardDark,
                                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else borderDark)
                                ) {
                                    Text(
                                        text = province.nameEn,
                                        color = if (isSelected) Color.White else textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. DISTRICT SECTION (Shown when a province is selected)
                if (state.selectedProvinceId != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DISTRICT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            letterSpacing = 0.5.sp
                        )

                        if (state.loadingDistricts) {
                            CircularProgressIndicator(
                                color = green,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp).padding(vertical = 4.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.districts.forEach { district ->
                                    val isSelected = state.selectedDistrictId == district.id
                                    Surface(
                                        onClick = { vm.selectDistrict(district.id) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) green else cardDark,
                                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else borderDark)
                                    ) {
                                        Text(
                                            text = district.nameEn,
                                            color = if (isSelected) Color.White else textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SEARCH CITY SECTION
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SEARCH CITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    CreateTeamInputField(
                        value = state.locationSearchQuery,
                        onValueChange = vm::onLocationSearchQueryChanged,
                        placeholder = "Type city name",
                        leadingIcon = Icons.Default.Search,
                        singleLine = true,
                        cardDark = cardDark,
                        borderDark = borderDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentGreen = green
                    )
                }

                // 4. CITIES LIST SECTION
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.loadingCities) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = green, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                        }
                    } else if (state.locationCities.isNotEmpty()) {
                        state.locationCities.forEach { city ->
                            val isSelected = state.form.cityId == city.id
                            Surface(
                                onClick = { vm.onLocationCitySelected(city) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = cardDark,
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) green else borderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = city.nameEn,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${city.districtName}, ${city.provinceName}",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "Selected",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = green
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val trimmed = state.locationSearchQuery.trim()
                        val hasFilter = state.selectedDistrictId != null || trimmed.isNotEmpty()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasFilter) "No matching cities found." else "Type a city name or pick a province/district to narrow the list.",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MY TEAM CARD & OPTIONS MODAL (Screenshots 1, 2, 3)
// ============================================================================

@Composable
private fun MyTeamCard(
    team: TeamUi,
    green: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onClearChatClick: (String) -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val cardBg = if (isDark) DarkSurface else Color.White
    val borderCol = if (isDark) GlassBorderDark else Color(0xFFE5E7EB)
    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Banner Section (Screenshot 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
        ) {
            if (!team.cover.isNullOrBlank()) {
                AsyncImage(
                    model = team.cover,
                    contentDescription = "Team Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.team_banner_placeholder),
                    contentDescription = "Default Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.5f))
                        )
                    )
            )

            // Role Badge top-right e.g. CAPTAIN or MEMBER (Screenshot 1)
            val roleText = team.role ?: if (team.joinStatus == JoinStatus.MEMBER) "MEMBER" else null
            if (roleText != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2E26).copy(alpha = 0.85f)
                ) {
                    Text(
                        text = roleText.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Details Section (Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Logo Avatar
            if (!team.logo.isNullOrBlank()) {
                AsyncImage(
                    model = team.logo,
                    contentDescription = team.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(2.dp, green, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = team.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    // Friends Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(green.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = team.type.replaceFirstChar { it.uppercase() },
                            color = green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Member count pill e.g. 1 Members (amber pill) or 3 Members (grey pill) (Screenshot 1)
                    val isSingle = team.membersCount == 1
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSingle) Color(0xFFFFB300).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${team.membersCount} Members",
                            color = if (isSingle) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (team.location.isNotBlank()) {
                    Text(
                        text = team.location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // 3-Dots Options Menu Icon (Screenshot 1)
            IconButton(onClick = { showOptionsMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showOptionsMenu) {
        TeamOptionsModal(
            team = team,
            green = green,
            onViewMembers = onClick,
            onEdit = onEdit,
            onClearChat = onClearChatClick,
            onDelete = onDeleteClick,
            onLeave = onLeaveClick,
            onDismiss = { showOptionsMenu = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamOptionsModal(
    team: TeamUi,
    green: Color,
    onViewMembers: () -> Unit,
    onEdit: (() -> Unit)?,
    onClearChat: ((String) -> Unit)?,
    onDelete: (() -> Unit)?,
    onLeave: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val canManage = team.role == "Captain" || team.role == "Admin"
    val isCaptain = team.role == "Captain"
    val bgDark = if (isDark) DarkBackground else LightBackground
    val cardBg = if (isDark) DarkSurface else Color.White
    val borderCol = if (isDark) GlassBorderDark else Color(0xFFE5E7EB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgDark,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row (Screenshots 2 & 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!team.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = team.logo,
                        contentDescription = team.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(2.dp, green, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${team.role ?: "Member"} • ${team.membersCount} Members",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1C2A40) else Color(0xFFE5E7EB))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // SECTION 1: GENERAL (Screenshots 2 & 3)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GENERAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                ) {
                    OptionRowItem(
                        icon = Icons.Default.Group,
                        iconColor = green,
                        title = "View Members",
                        onClick = {
                            onDismiss()
                            onViewMembers()
                        }
                    )

                    if (!canManage && onLeave != null) {
                        HorizontalDivider(color = if (isDark) Color(0xFF1C2A40) else Color(0xFFF0F2F0))
                        OptionRowItem(
                            icon = Icons.Default.Logout,
                            iconColor = StatusError,
                            title = "Leave Team",
                            titleColor = StatusError,
                            onClick = {
                                onDismiss()
                                onLeave()
                            }
                        )
                    }
                }
            }

            // SECTION 2: MANAGE TEAM (if Captain or Admin) (Screenshot 2)
            if (canManage) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MANAGE TEAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                    ) {
                        if (onEdit != null) {
                            OptionRowItem(
                                icon = Icons.Default.Edit,
                                iconColor = green,
                                title = "Edit Team Details",
                                onClick = {
                                    onDismiss()
                                    onEdit()
                                }
                            )
                        }

                        if (onClearChat != null) {
                            HorizontalDivider(color = if (isDark) Color(0xFF1C2A40) else Color(0xFFF0F2F0))
                            OptionRowItem(
                                icon = Icons.Default.Chat,
                                iconColor = Color(0xFFF59E0B),
                                title = "Clear Group Chat",
                                onClick = {
                                    onDismiss()
                                    onClearChat("team_group")
                                }
                            )
                            HorizontalDivider(color = if (isDark) Color(0xFF1C2A40) else Color(0xFFF0F2F0))
                            OptionRowItem(
                                icon = Icons.Default.Campaign,
                                iconColor = Color(0xFFF59E0B),
                                title = "Clear Channel",
                                onClick = {
                                    onDismiss()
                                    onClearChat("channel")
                                }
                            )
                        }
                    }
                }
            }

            // SECTION 3: DANGER ZONE (if Captain) (Screenshot 2)
            if (isCaptain && onDelete != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DANGER ZONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                    ) {
                        OptionRowItem(
                            icon = Icons.Default.Delete,
                            iconColor = StatusError,
                            title = "Delete Team",
                            titleColor = StatusError,
                            onClick = {
                                onDismiss()
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================================
// JOIN TEAM CARD & DETAILS BOTTOM SHEET (Screenshots 4 & 5)
// ============================================================================

@Composable
private fun JoinTeamCard(
    team: TeamUi,
    green: Color,
    onClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val isRequested = team.joinStatus == JoinStatus.REQUESTED
    val isFull = team.membersCount >= team.maxMembers
    val cardBg = if (isDark) DarkSurface else Color.White
    val borderCol = if (isDark) GlassBorderDark else Color(0xFFE5E7EB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Banner Section (Screenshot 4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
        ) {
            if (!team.cover.isNullOrBlank()) {
                AsyncImage(
                    model = team.cover,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.team_banner_placeholder),
                    contentDescription = "Default Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.5f))
                        )
                    )
            )

            // Top Right Capacity Badge e.g. 3/20 (Screenshot 4)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    text = "${team.membersCount}/${team.maxMembers}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Details Section (Screenshot 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Logo Avatar
            if (!team.logo.isNullOrBlank()) {
                AsyncImage(
                    model = team.logo,
                    contentDescription = team.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(2.dp, green, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = team.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    // Type Badge e.g. Friends
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(green.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = team.type.replaceFirstChar { it.uppercase() },
                            color = green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Visibility Badge e.g. Public
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(green.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (team.isPublic) "Public" else "Private",
                            color = green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (team.description.isNullOrBlank()) "No description" else team.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (team.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = team.location,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right side Join Button (Screenshot 4)
            Button(
                onClick = onJoinClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRequested) Color(0xFFD97706) else if (isFull) Color.Gray else green,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isRequested) "Requested" else if (isFull) "Full" else "Join",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinTeamDetailsSheet(
    team: TeamUi,
    green: Color,
    vm: TeamViewModel,
    onDismiss: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val bgDark = if (isDark) DarkBackground else LightBackground
    val cardBg = if (isDark) DarkSurface else Color.White
    val borderCol = if (isDark) GlassBorderDark else Color(0xFFE5E7EB)
    val isRequested = team.joinStatus == JoinStatus.REQUESTED
    val isFull = team.membersCount >= team.maxMembers

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgDark,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner & Large Overlapping Avatar (Screenshot 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    if (!team.cover.isNullOrBlank()) {
                        AsyncImage(
                            model = team.cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.team_banner_placeholder),
                            contentDescription = "Default Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(0.5f))
                                )
                            )
                    )
                }

                // Centered Large Logo Avatar (Screenshot 5)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(green)
                        .border(4.dp, bgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!team.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = team.logo,
                            contentDescription = team.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = team.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Team Name & Location Subtitle (Screenshot 5)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = team.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (team.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = team.location,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3 Stat Cards Row (Screenshot 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailStatCard(
                    icon = Icons.Default.Groups,
                    iconBg = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    iconTint = Color(0xFF3B82F6),
                    value = "${team.membersCount}",
                    valueColor = Color(0xFF3B82F6),
                    label = "MEMBERS",
                    modifier = Modifier.weight(1f)
                )
                DetailStatCard(
                    icon = Icons.Default.OpenInFull,
                    iconBg = green.copy(alpha = 0.15f),
                    iconTint = green,
                    value = "${team.maxMembers}",
                    valueColor = green,
                    label = "CAPACITY",
                    modifier = Modifier.weight(1f)
                )
                DetailStatCard(
                    icon = Icons.Default.LocalOffer,
                    iconBg = Color(0xFFFFB300).copy(alpha = 0.15f),
                    iconTint = Color(0xFFFFB300),
                    value = team.type.replaceFirstChar { it.uppercase() },
                    valueColor = Color(0xFFFFB300),
                    label = "TYPE",
                    modifier = Modifier.weight(1f)
                )
            }

            // Details Card Section (Screenshot 5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = borderCol)

                DetailRowItem(icon = Icons.Default.LocationOn, label = "Location", value = team.location.ifBlank { "Not set" })
                DetailRowItem(icon = Icons.Default.LocalOffer, label = "Team Type", value = team.type.replaceFirstChar { it.uppercase() })
                DetailRowItem(icon = Icons.Default.Groups, label = "Members", value = "${team.membersCount} / ${team.maxMembers}")
                DetailRowItem(icon = Icons.Default.CalendarToday, label = "Created", value = "10 Aug 2026, 11:00 AM")
                DetailRowItem(icon = Icons.Default.ConfirmationNumber, label = "Total Booking", value = "0")
                DetailRowItem(icon = Icons.Default.Public, label = "Visibility", value = if (team.isPublic) "Public Team" else "Private Team")
            }

            // Bottom Sticky Request to Join Button (Screenshot 5)
            Button(
                onClick = {
                    onDismiss()
                    if (isRequested) vm.cancelJoin(team) else vm.requestJoin(team)
                },
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRequested) Color(0xFFD97706) else if (isFull) Color.Gray else green
                ),
                enabled = !isFull || isRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRequested) Icons.Default.Schedule else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRequested) "Cancel Request" else if (isFull) "Team Full" else "Request to Join",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

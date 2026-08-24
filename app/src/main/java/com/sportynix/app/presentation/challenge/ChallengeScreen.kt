package com.sportynix.app.presentation.challenge

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.R
import com.sportynix.app.presentation.components.LiquidGlassCard
import com.sportynix.app.presentation.components.LiquidGlassButton
import com.sportynix.app.presentation.components.LiquidGlassTextField
import com.sportynix.app.presentation.components.LiquidGlassFilterChip
import com.sportynix.app.presentation.components.LiquidGlassSearchBar
import com.sportynix.app.presentation.components.LiquidGlassBadge
import com.sportynix.app.presentation.components.LiquidGlassEmptyState
import com.sportynix.app.presentation.components.LiquidGlassErrorState
import com.sportynix.app.presentation.components.LiquidGlassDialog
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToScoring: (String) -> Unit = {},
    vm: ChallengeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme

    var showConfirmAccept by remember { mutableStateOf<ChallengeUi?>(null) }
    var showConfirmDecline by remember { mutableStateOf<ChallengeUi?>(null) }
    var showConfirmCancel by remember { mutableStateOf<ChallengeUi?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
            if (event is ChallengeEvent.OpenChat) {
                onNavigateToChat(event.conversationId)
            }
        }
    }

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                ChallengeHeader(state, green, onNavigateBack, vm)
                
                AnimatedContent(
                    targetState = state.tab,
                    transitionSpec = {
                        if (targetState == ChallengeTab.MY_CHALLENGES) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "tabTransition",
                    modifier = Modifier.weight(1f)
                ) { currentTab ->
                    if (currentTab == ChallengeTab.FIND_TEAMS) {
                        FindTeamsContent(state, green, vm)
                    } else {
                        MyChallengesContent(state, green, vm, 
                            onAccept = { showConfirmAccept = it },
                            onDecline = { showConfirmDecline = it },
                            onCancel = { showConfirmCancel = it },
                            onOpenScoring = onNavigateToScoring
                        )
                    }
                }
            }

            // Global messages/errors
            state.error?.let { msg ->
                AlertDialog(
                    onDismissRequest = vm::dismissMessage,
                    title = { Text("Challenge Warning", fontWeight = FontWeight.Bold) },
                    text = { Text(msg) },
                    confirmButton = {
                        TextButton(onClick = vm::dismissMessage) {
                            Text("OK", color = green, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            state.message?.let { msg ->
                if (msg.contains("sent", ignoreCase = true) || msg.contains("success", ignoreCase = true)) {
                    LiquidGlassDialog(onDismissRequest = vm::dismissMessage) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(green.copy(alpha = 0.2f))
                                    .border(2.dp, green, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = green, modifier = Modifier.size(36.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Challenge Sent! 🎉", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Your challenge has been sent to ${state.selectedOpponent?.name ?: "the opponent team"}.\nThey will be notified shortly.\n\nOnce accepted, you can discuss venue and time in the rivalry chat.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = vm::dismissMessage,
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Text("OK", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                } else {
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2500)
                        vm.dismissMessage()
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) {
                        LiquidGlassBadge(text = msg, badgeColor = green)
                    }
                }
            }

            // Create Challenge bottom sheet stepper modal
            if (state.creating) {
                CreateChallengeSheet(state, green, vm)
            }

            // Challenge Details Dialog
            state.selectedDetail?.let { challenge ->
                ChallengeDetailDialog(
                    challenge = challenge,
                    green = green,
                    vm = vm,
                    onAccept = {
                        vm.closeDetails()
                        showConfirmAccept = challenge
                    },
                    onDecline = {
                        vm.closeDetails()
                        showConfirmDecline = challenge
                    },
                    onCancel = {
                        vm.closeDetails()
                        showConfirmCancel = challenge
                    },
                    onOpenScoring = onNavigateToScoring
                )
            }

            // Team Statistics & Details Preview Dialog
            if (state.selectedTeamDetail != null) {
                TeamPreviewSheet(state = state, green = green, vm = vm)
            }

            // Confirmation Dialogs
            showConfirmAccept?.let { challenge ->
                ConfirmActionDialog(
                    title = "Accept Challenge",
                    message = "Are you sure you want to accept this challenge? This will create a rivalry chat with the opponent.",
                    confirmLabel = "Accept",
                    confirmColor = green,
                    isLoading = state.acceptingChallengeId == challenge.id,
                    onConfirm = {
                        vm.accept(challenge)
                        showConfirmAccept = null
                    },
                    onDismiss = { showConfirmAccept = null }
                )
            }

            showConfirmDecline?.let { challenge ->
                ConfirmActionDialog(
                    title = "Decline Challenge",
                    message = "Are you sure you want to decline this challenge from ${challenge.challenger}?",
                    confirmLabel = "Decline",
                    confirmColor = MaterialTheme.colorScheme.error,
                    isLoading = state.decliningChallengeId == challenge.id,
                    onConfirm = {
                        vm.decline(challenge)
                        showConfirmDecline = null
                    },
                    onDismiss = { showConfirmDecline = null }
                )
            }

            showConfirmCancel?.let { challenge ->
                ConfirmActionDialog(
                    title = "Cancel Challenge",
                    message = "Are you sure you want to cancel this challenge to ${challenge.challenged}?",
                    confirmLabel = "Yes, Cancel",
                    confirmColor = MaterialTheme.colorScheme.error,
                    isLoading = state.cancellingChallengeId == challenge.id,
                    onConfirm = {
                        vm.cancel(challenge)
                        showConfirmCancel = null
                    },
                    onDismiss = { showConfirmCancel = null }
                )
            }
        }
    }
}

@Composable
private fun ChallengeHeader(
    state: ChallengeState,
    green: Color,
    back: () -> Unit,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Challenges", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text("Find your next rivalry", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            IconButton(onClick = vm::refresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = green,
                    modifier = Modifier.scale(if (state.refreshing) 0.9f else 1.0f)
                )
            }
            FilledIconButton(
                onClick = vm::openCreate,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = green),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        
        // Premium Capsule Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) DarkSurface else LightSurfaceVariant)
                .padding(4.dp)
        ) {
            val modifierFind = Modifier.weight(1f)
            val modifierMy = Modifier.weight(1f)
            
            SegmentTab(
                text = "Find Teams",
                selected = state.tab == ChallengeTab.FIND_TEAMS,
                green = green,
                modifier = modifierFind,
                onClick = { vm.setTab(ChallengeTab.FIND_TEAMS) }
            )
            SegmentTab(
                text = "My Challenges",
                selected = state.tab == ChallengeTab.MY_CHALLENGES,
                green = green,
                modifier = modifierMy,
                onClick = { vm.setTab(ChallengeTab.MY_CHALLENGES) }
            )
        }
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    green: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val bgCol by animateColorAsState(
        targetValue = if (selected) green else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBg"
    )
    val textCol by animateColorAsState(
        targetValue = if (selected) Color.White else if (isDark) TextSecondaryDark else TextSecondaryLight,
        label = "tabText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgCol)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textCol,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindTeamsContent(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LiquidGlassSearchBar(
                    query = state.search,
                    onQueryChange = vm::search,
                    placeholder = "Search opponents...",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )
            }

            if (state.loading && state.opponents.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green)
                    }
                }
            } else if (!state.loading && state.opponents.isEmpty()) {
                item {
                    LiquidGlassEmptyState(
                        title = "No Teams Found",
                        description = "Try searching for another team name or check back later.",
                        icon = Icons.Outlined.Group,
                        actionText = "Clear Search",
                        onActionClick = { vm.search("") }
                    )
                }
            } else {
                items(state.opponents, key = { it.id }) { team ->
                    TeamChallengeCard(
                        team = team,
                        myTeamId = state.selectedTeam?.id ?: state.myTeams.firstOrNull()?.id,
                        green = green,
                        vm = vm,
                        onPreview = { vm.openTeam(team) },
                        onChallenge = { vm.selectOpponentFromPreview(team); vm.nextStep() }
                    )
                }

                if (state.hasMoreOpponents && state.opponents.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loadingMoreOpponents) {
                                CircularProgressIndicator(color = green, modifier = Modifier.size(24.dp))
                            } else {
                                OutlinedButton(
                                    onClick = vm::loadMoreOpponents,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.5f))
                                ) {
                                    Text("Load More Teams", color = green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyChallengesContent(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel,
    onAccept: (ChallengeUi) -> Unit,
    onDecline: (ChallengeUi) -> Unit,
    onCancel: (ChallengeUi) -> Unit,
    onOpenScoring: (String) -> Unit
) {
    val list = when (state.section) {
        ChallengeSection.INCOMING -> state.incoming
        ChallengeSection.SENT -> state.sent
        ChallengeSection.HISTORY -> state.history
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Section Subtab Filter Row matching Screenshots 2 & 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChallengeSection.entries.forEach { section ->
                val count = when (section) {
                    ChallengeSection.INCOMING -> state.incoming.size
                    ChallengeSection.SENT -> state.sent.size
                    ChallengeSection.HISTORY -> state.history.size
                }
                val rawLabel = section.name.lowercase().replaceFirstChar { it.uppercase() }
                val label = if (count > 0 && section != ChallengeSection.HISTORY) "$rawLabel ($count)" else rawLabel
                val isSelected = state.section == section

                LiquidGlassFilterChip(
                    selected = isSelected,
                    onClick = { vm.setSection(section) },
                    label = label,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section Description Header
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            val title = when (state.section) {
                ChallengeSection.INCOMING -> "Challenges from other teams"
                ChallengeSection.SENT -> "Sent Challenges"
                ChallengeSection.HISTORY -> "Challenge History"
            }
            val subtitle = when (state.section) {
                ChallengeSection.INCOMING -> "Accept or decline challenges sent to your teams"
                ChallengeSection.SENT -> "Challenges sent by your team members. Cancel option only available for your own challenges."
                ChallengeSection.HISTORY -> "Past completed, declined, and cancelled challenges"
            }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.weight(1f)
        ) {
            if (state.loading && list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = green)
                }
            } else if (list.isEmpty()) {
                val emptyMsg = when (state.section) {
                    ChallengeSection.INCOMING -> "No incoming challenges pending"
                    ChallengeSection.SENT -> "You haven't sent any challenges yet"
                    ChallengeSection.HISTORY -> "No challenge history found"
                }
                LiquidGlassEmptyState(
                    title = "No Challenges",
                    description = emptyMsg,
                    icon = Icons.Outlined.EmojiEvents
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(list, key = { it.id }) { challenge ->
                        ChallengeCard(
                            c = challenge,
                            green = green,
                            vm = vm,
                            onAccept = { onAccept(challenge) },
                            onDecline = { onDecline(challenge) },
                            onCancel = { onCancel(challenge) },
                            onOpenScoring = onOpenScoring
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamChallengeCard(
    team: ChallengeTeamUi,
    myTeamId: Int?,
    green: Color,
    vm: ChallengeViewModel,
    onPreview: () -> Unit,
    onChallenge: () -> Unit
) {
    val blockReason = vm.getChallengeBlockReason(myTeamId, team.id)
    val isDisabled = blockReason != null
    val buttonText = when (blockReason) {
        "existing" -> "Already in challenge"
        "pending" -> "Pending Challenge"
        else -> "Challenge"
    }

    val isDark = LocalThemeController.current.isDark

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPreview
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogoImage(logoUrl = team.logo, teamName = team.name, green = green, size = 52)
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${team.members} members${if (team.location.isNotBlank()) " • ${team.location}" else ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (team.sport.isNotBlank()) team.sport else "Multi-sport",
                        color = green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (isDisabled) {
                        Text(
                            text = if (blockReason == "existing") "Already connected in challenge/chat" else "Pending challenge already exists with this opponent",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        if (!isDisabled) onChallenge()
                    },
                    enabled = !isDisabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDisabled) Color.Gray.copy(alpha = 0.3f) else green,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Stats row (5 columns) matching Screenshot 1
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(8.dp))
            val stats = team.challengeStats ?: ChallengeStatsUi(0, 0, 0, 0, 0.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatBox(label = "PLAYED", value = "${stats.totalMatches}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                StatBox(label = "WINS", value = "${stats.wins}", color = green, modifier = Modifier.weight(1f))
                StatBox(label = "LOSSES", value = "${stats.losses}", color = StatusError, modifier = Modifier.weight(1f))
                StatBox(label = "NO RESULT", value = "${stats.noResults}", color = AccentGold, modifier = Modifier.weight(1f))
                StatBox(label = "WIN %", value = "${String.format(java.util.Locale.US, "%.1f", stats.winPercentage)}%", color = green, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    c: ChallengeUi,
    green: Color,
    vm: ChallengeViewModel,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onOpenScoring: (String) -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val statusColor = when (c.status.lowercase()) {
        "accepted" -> green
        "declined", "cancelled", "expired" -> StatusError
        else -> StatusWarning
    }

    val canOpenScoring = c.enableScoring || !c.cricketScoringMatchId.isNullOrBlank() || (c.sport.contains("cricket", ignoreCase = true) && (c.status.equals("accepted", true) || c.status.equals("completed", true)))

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { vm.openDetails(c) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidGlassBadge(
                    text = c.status.replaceFirstChar { it.uppercase() },
                    badgeColor = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = c.date?.let { "Date $it" } ?: "Date TBD",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Challenger VS Opponent Layout matching Screenshots 2 & 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoImage(logoUrl = null, teamName = c.challenger, green = green, size = 48)
                    Text(c.challenger, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Text("Challenger", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = "VS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoImage(logoUrl = null, teamName = c.challenged, green = green, size = 48)
                    Text(c.challenged, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Text("Opponent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sport & Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getSportEmoji(c.sport), fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(c.sport.ifBlank { "Cricket & Football" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (c.venue.isNotBlank()) c.venue else "Details to be decided in chat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (c.stake > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MonetizationOn, null, tint = green, modifier = Modifier.size(14.dp))
                    Text("Stake: Rs. ${"%.2f".format(c.stake)}", color = green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
            }

            // Cricket Scoring Quick Access Button
            if (canOpenScoring) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onOpenScoring(c.cricketScoringMatchId ?: c.id.toString()) },
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏏", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!c.cricketScoringMatchId.isNullOrBlank()) "Open Cricket Scoring" else "Live Cricket Scoring",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Quick Actions Block
            if (c.canAccept || c.canDecline || c.canCancel || (c.status == "accepted" && c.chatId != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (c.canAccept) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("Accept", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (c.canDecline) {
                        OutlinedButton(
                            onClick = onDecline,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("Decline", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (c.canCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (c.status == "accepted" && c.chatId != null) {
                        Button(
                            onClick = { vm.openChat(c) },
                            colors = ButtonDefaults.buttonColors(containerColor = green.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = green, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Chat", color = green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamLogoImage(
    logoUrl: String?,
    teamName: String,
    green: Color,
    size: Int
) {
    val isDark = LocalThemeController.current.isDark
    Box(contentAlignment = Alignment.BottomEnd) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "Team Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(green, green.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = teamName.firstOrNull()?.uppercase()?.toString() ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size * 0.4).sp
                )
            }
        }

        // Small sport ball overlay matching Screenshot 1
        Box(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size((size * 0.35).dp)
                .clip(CircleShape)
                .background(if (isDark) DarkSurface else Color.White)
                .border(1.dp, green, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("⚾", fontSize = (size * 0.2).sp)
        }
    }
}

// ── Stepper Challenge Creation modal ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChallengeSheet(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark
    val currentStepIndex = ChallengeStep.entries.indexOf(state.step) + 1

    ModalBottomSheet(
        onDismissRequest = vm::closeCreate,
        containerColor = if (isDark) DarkSurface else LightSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Bar matching Screenshots 1, 2, 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                        .clickable {
                            if (state.step == ChallengeStep.MY_TEAM) vm.closeCreate() else vm.previousStep()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Challenge",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "STEP $currentStepIndex OF 4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                        .clickable(onClick = vm::closeCreate),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            // Green Progress Line
            val progressFraction = currentStepIndex / 4f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(green)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .weight(1f, fill = false)
                    .heightIn(max = 420.dp)
            ) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "stepperContent"
                ) { step ->
                    when (step) {
                        ChallengeStep.MY_TEAM -> {
                            StepperMyTeamStep(state = state, green = green, vm = vm)
                        }
                        ChallengeStep.OPPONENT -> {
                            StepperOpponentStep(state = state, green = green, vm = vm)
                        }
                        ChallengeStep.SPORT -> {
                            StepperSportStep(state = state, green = green, vm = vm)
                        }
                        ChallengeStep.REVIEW -> {
                            StepperReviewStep(state = state, green = green, vm = vm)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 16.dp)
            ) {
                val isNextEnabled = when (state.step) {
                    ChallengeStep.MY_TEAM -> state.selectedTeam != null
                    ChallengeStep.OPPONENT -> state.selectedOpponent != null
                    ChallengeStep.SPORT -> state.selectedSport != null
                    ChallengeStep.REVIEW -> true
                }

                Button(
                    onClick = vm::nextStep,
                    enabled = isNextEnabled && !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = green,
                        disabledContainerColor = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB)
                    )
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.step == ChallengeStep.REVIEW) {
                                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Challenge", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            } else {
                                Text("Continue", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperTracker(currentStep: ChallengeStep, green: Color) {
    val isDark = LocalThemeController.current.isDark
    val steps = ChallengeStep.entries
    val currentIdx = steps.indexOf(currentStep)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        steps.forEachIndexed { idx, step ->
            val isActive = idx <= currentIdx
            val dotColor = if (isActive) green else if (isDark) GlassCardDark else LightSurfaceVariant
            val textColor = if (isActive) green else Color.Gray

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (idx < currentIdx) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text((idx + 1).toString(), color = if (isActive) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = when(step) {
                        ChallengeStep.MY_TEAM -> "My Team"
                        ChallengeStep.OPPONENT -> "Opponent"
                        ChallengeStep.SPORT -> "Details"
                        ChallengeStep.REVIEW -> "Review"
                    },
                    fontSize = 9.sp,
                    color = textColor,
                    fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (idx < steps.size - 1) {
                val lineColor = if (idx < currentIdx) green else if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 6.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun StepperMyTeamStep(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Your Team", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Choose which of your teams will issue this challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        if (state.loadingMyTeams && state.myTeams.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = green)
            }
        } else if (state.myTeams.isEmpty()) {
            LiquidGlassEmptyState(
                title = "No Teams Found",
                description = "You must be a member of at least one team to issue challenges."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.myTeams) { team ->
                    val isSelected = state.selectedTeam?.id == team.id
                    val isDark = LocalThemeController.current.isDark
                    val borderCol = if (isSelected) green else Color.Transparent
                    
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = borderCol,
                        onClick = { vm.selectTeam(team) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TeamLogoImage(logoUrl = team.logo, teamName = team.name, green = green, size = 44)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${team.members} members • ${team.location.ifBlank { "Multi-location" }}", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                if (state.hasMoreMyTeams && state.myTeams.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = vm::loadMoreMyTeams) {
                                Text("Load More My Teams", color = green, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperOpponentStep(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val relationshipMap = state.relationships
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Opponent Team", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Select a team to receive your challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        if (state.opponents.isEmpty()) {
            LiquidGlassEmptyState(
                title = "No Teams Available",
                description = "No opponent teams discovered yet."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.opponents) { team ->
                    val isSelected = state.selectedOpponent?.id == team.id
                    val isDark = LocalThemeController.current.isDark
                    
                    // Relationship Check
                    val key = listOf(state.selectedTeam?.id ?: 0, team.id).sorted().joinToString(":")
                    val relationship = relationshipMap[key]
                    val isBlocked = relationship != null
                    
                    val borderCol = if (isSelected) green else Color.Transparent
                    val alphaVal = if (isBlocked) 0.55f else 1.0f

                    Box(modifier = Modifier.scale(if (isBlocked) 0.98f else 1.0f).alpha(alphaVal)) {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = borderCol,
                            onClick = { 
                                if (!isBlocked) {
                                    vm.selectOpponent(team) 
                                } else {
                                    val blockMsg = if (relationship == "existing") "Already in active rivalry/challenge" else "Pending challenge already exists"
                                    vm.dismissMessage()
                                    // Set temporary VM error state to display to users
                                    vm.setDate(LocalDate.now()) // arbitrary VM call to trigger state
                                    vm.setStake("") // clear
                                    vm.dismissMessage()
                                    // Display alert
                                    // Trigger dialog
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamLogoImage(logoUrl = team.logo, teamName = team.name, green = green, size = 44)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${team.members} members • ${team.location.ifBlank { "Multi-location" }}", fontSize = 11.sp, color = Color.Gray)
                                    
                                    if (relationship != null) {
                                        Text(
                                            text = if (relationship == "existing") "⚠️ Already in active rivalry" else "🕒 Pending challenge exists",
                                            color = if (relationship == "existing") green else AccentGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                if (isSelected && !isBlocked) {
                                    Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun StepperSportStep(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark

    val availableSports = remember(state.selectedTeam, state.selectedOpponent, state.sports) {
        val list = mutableListOf<ChallengeSportUi>()
        val myTeamSport = state.selectedTeam?.sport?.lowercase()
        val oppTeamSport = state.selectedOpponent?.sport?.lowercase()
        
        state.sports.forEach { s ->
            val name = s.name.lowercase()
            if (myTeamSport?.contains(name) == true || oppTeamSport?.contains(name) == true) {
                list.add(0, s)
            } else {
                list.add(s)
            }
        }
        if (list.none { it.name.contains("Cricket", ignoreCase = true) }) {
            list.add(0, ChallengeSportUi(id = 2, name = "Cricket & Football"))
        }
        list.distinctBy { it.name }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Challenge Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Choose the sport for the challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sport", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(" *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 2-Column Grid matching Screenshot 1
        val rows = availableSports.chunked(2)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { sport ->
                        val isSelected = state.selectedSport?.id == sport.id || state.selectedSport?.name == sport.name
                        val borderCol = if (isSelected) green else if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB)
                        val cardBg = if (isSelected) green.copy(alpha = 0.12f) else if (isDark) Color(0xFF121E19) else Color(0xFFF9FAFB)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
                                .clickable { vm.selectSport(sport) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(getSportEmoji(sport.name), fontSize = 26.sp)
                                Text(
                                    text = sport.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) green else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Info Callout Box matching Screenshot 1 (Blue themed callout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Once the challenge is accepted, both teams can discuss and agree on the venue and time in the rivalry chat. You can then book any available slot that works for everyone.",
                fontSize = 11.sp,
                color = Color(0xFF93C5FD),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StepperReviewStep(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Review Challenge", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Confirm the details before sending", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        // Main Review Card with Green Border matching Screenshot 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF121E19) else Color.White)
                .border(1.5.dp, green, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // VS Representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoImage(logoUrl = state.selectedTeam?.logo, teamName = state.selectedTeam?.name ?: "Challenger", green = green, size = 56)
                        Text(state.selectedTeam?.name ?: "Your Team", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Your Team", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoImage(logoUrl = state.selectedOpponent?.logo, teamName = state.selectedOpponent?.name ?: "Opponent", green = green, size = 56)
                        Text(state.selectedOpponent?.name ?: "Opponent", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Opponent", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
                
                HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 14.dp))

                // Sport Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(green.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getSportEmoji(state.selectedSport?.name ?: ""), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Sport", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.selectedSport?.name ?: "Cricket & Football", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Venue & Time Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(green.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = green, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Venue & Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("To be decided in rivalry chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Optional Stake Input Field
        Text("Optional Stake (Rs.)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LiquidGlassTextField(
            value = state.stake,
            onValueChange = vm::setStake,
            placeholder = "Enter stake (e.g. 1000)",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Info Callout Box matching Screenshot 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "The opponent team will be notified and can accept or decline your challenge. Once accepted, you can discuss venue and time in the rivalry chat.",
                fontSize = 11.sp,
                color = Color(0xFF93C5FD),
                lineHeight = 15.sp
            )
        }
    }
}

// ── Team Statistics & Recent Matches details Sheet ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamPreviewSheet(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark
    
    ModalBottomSheet(
        onDismissRequest = vm::closeTeam,
        containerColor = if (isDark) DarkSurface else LightSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (state.loadingPreviewTeam) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = green)
                }
            } else if (state.previewTeamDetail != null) {
                val team = state.previewTeamDetail
                val isPublic = team.isPublic
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 520.dp)
                ) {
                    // Header Cover Banner matching Screenshots 4 & 5
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(green.copy(alpha = 0.45f), green.copy(alpha = 0.1f), Color.Transparent)
                                    )
                                )
                        ) {
                            if (!team.coverImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = team.coverImage,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // Top Badges & Actions Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isPublic) "🌐" else "🔒", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPublic) "Public" else "Private",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable(onClick = vm::closeTeam),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Centered Large Avatar Logo Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                TeamLogoImage(
                                    logoUrl = team.logo,
                                    teamName = team.name,
                                    green = green,
                                    size = 80
                                )
                            }
                        }
                    }

                    // Team Name & Location Pill Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = team.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (!team.location.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = team.location,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3 Stat Cards Row matching Screenshots 4 & 5
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
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
                                value = (team.teamType ?: "Friends").replaceFirstChar { it.uppercase() },
                                valueColor = Color(0xFFFFB300),
                                label = "TYPE",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Challenge Performance Card Section matching Screenshots 4 & 5
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color(0xFF121E19) else Color.White)
                                    .border(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Challenge Performance",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    val stats = team.challengeStats ?: ChallengeStatsUi(0, 0, 0, 0, 0.0)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatBox(label = "PLAYED", value = "${stats.totalMatches}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        StatBox(label = "WINS", value = "${stats.wins}", color = green, modifier = Modifier.weight(1f))
                                        StatBox(label = "LOSSES", value = "${stats.losses}", color = StatusError, modifier = Modifier.weight(1f))
                                        StatBox(label = "NO RESULT", value = "${stats.noResults}", color = AccentGold, modifier = Modifier.weight(1f))
                                        StatBox(label = "WIN %", value = "${String.format(java.util.Locale.US, "%.1f", stats.winPercentage)}%", color = green, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Details Card Section matching Screenshots 4 & 5
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isDark) Color(0xFF121E19) else Color.White)
                                    .border(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Info, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    HorizontalDivider(color = if (isDark) Color(0xFF1E2E27) else Color(0xFFF3F4F6))

                                    DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = team.location ?: "Nelukkulam, Vavuniya")
                                    DetailRow(icon = Icons.Default.Layers, label = "Team Type", value = team.teamType ?: "Friends")
                                    DetailRow(icon = Icons.Default.Public, label = "Visibility", value = if (isPublic) "Public Team" else "Private Team")
                                }
                            }
                        }
                    }

                    // Recent Matches
                    if (state.previewRecentMatches.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                                Text("Recent Matches", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        items(state.previewRecentMatches) { match ->
                            val resColor = when (match.result.lowercase()) {
                                "win" -> green
                                "loss" -> StatusError
                                else -> AccentGold
                            }
                            
                            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TeamLogoImage(logoUrl = match.opponentLogo, teamName = match.opponentName, green = green, size = 32)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("vs ${match.opponentName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            
                                            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = formatScore(match.teamScore),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = " - ",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = formatScore(match.opponentScore),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            if (!match.margin.isNullOrBlank()) {
                                                Text(match.margin, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(resColor.copy(alpha = 0.15f))
                                                .border(1.dp, resColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (match.result == "no_result") "NR" else match.result.uppercase(),
                                                color = resColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.previewRecentMatchesHasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.loadingMorePreviewMatches) {
                                        CircularProgressIndicator(color = green, modifier = Modifier.size(20.dp))
                                    } else {
                                        TextButton(onClick = vm::loadMorePreviewMatches) {
                                            Text("See More Matches", color = green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Bottom Sticky Action Button matching Screenshots 4 & 5
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    when (state.previewJoinStatus.lowercase()) {
                        "member", "approved" -> {
                            LiquidGlassButton(
                                text = "Open Team Chat",
                                onClick = vm::openTeamChat,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.ChatBubble
                            )
                        }
                        "requested" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AccentGold.copy(alpha = 0.08f))
                                    .border(1.dp, AccentGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassEmpty, null, tint = AccentGold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Join Request Pending", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Waiting for captain's approval", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    val selectedTeam = state.selectedTeamDetail
                                    vm.closeTeam()
                                    if (selectedTeam != null) {
                                        vm.selectOpponentFromPreview(selectedTeam)
                                        vm.nextStep()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(25.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.FlashOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Challenge", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
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
    modifier: Modifier
) {
    val isDark = LocalThemeController.current.isDark
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF121E19) else Color.White)
            .border(1.dp, if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkSurface else LightSurfaceVariant
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 1.dp))
    }
}

private fun formatScore(score: MatchScoreUi?): String {
    if (score == null) return "—"
    return "${score.runs}/${score.wickets} (${score.overs})"
}

@Composable
private fun ChallengeDetailDialog(
    challenge: ChallengeUi,
    green: Color,
    vm: ChallengeViewModel,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onOpenScoring: (String) -> Unit
) {
    val canOpenScoring = challenge.enableScoring || !challenge.cricketScoringMatchId.isNullOrBlank() || (challenge.sport.contains("cricket", ignoreCase = true) && (challenge.status.equals("accepted", true) || challenge.status.equals("completed", true)))

    LiquidGlassDialog(onDismissRequest = vm::closeDetails) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Challenge Details", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            
            Text("${challenge.challenger} vs ${challenge.challenged}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Status: ${challenge.status.replaceFirstChar { it.uppercase() }}", color = green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            
            if (challenge.sport.isNotBlank()) {
                Text("Sport: ${challenge.sport}", fontSize = 13.sp)
            }
            if (challenge.venue.isNotBlank()) {
                Text("Venue: ${challenge.venue}", fontSize = 13.sp)
            }
            challenge.date?.let {
                Text("Date: $it", fontSize = 13.sp)
            }
            if (challenge.stake > 0.0) {
                Text("Stake: Rs. ${"%.2f".format(challenge.stake)}", fontWeight = FontWeight.Bold, color = green, fontSize = 13.sp)
            }

            if (canOpenScoring) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        vm.closeDetails()
                        onOpenScoring(challenge.cricketScoringMatchId ?: challenge.id.toString())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏏", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!challenge.cricketScoringMatchId.isNullOrBlank()) "Open Cricket Scoring" else "Set Up Cricket Scoring",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (challenge.canAccept) {
                    TextButton(onClick = onAccept) {
                        Text("Accept", color = green, fontWeight = FontWeight.Bold)
                    }
                }
                if (challenge.canDecline) {
                    TextButton(onClick = onDecline) {
                        Text("Decline", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (challenge.canCancel) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = vm::closeDetails) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getSportEmoji(sport: String): String {
    val name = sport.lowercase()
    return when {
        name.contains("football") -> "⚽"
        name.contains("cricket") -> "🏏"
        name.contains("badminton") -> "🏸"
        name.contains("tennis") -> "🎾"
        name.contains("basketball") -> "🏀"
        name.contains("volleyball") -> "🏐"
        else -> "🏆"
    }
}

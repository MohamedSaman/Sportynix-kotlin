package com.sportynix.app.presentation.challenge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
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
                        MyChallengesContent(
                            state = state,
                            green = green,
                            vm = vm,
                            onAccept = { showConfirmAccept = it },
                            onDecline = { showConfirmDecline = it },
                            onCancel = { showConfirmCancel = it },
                            onOpenScoring = onNavigateToScoring
                        )
                    }
                }
            }

            // Global Error / Warning Dialog with Liquid Glass Style
            state.error?.let { msg ->
                LiquidGlassDialog(onDismissRequest = vm::dismissMessage) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.15f))
                                .border(1.5.dp, AccentGold.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Notice",
                                tint = AccentGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Challenge Notice",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = vm::dismissMessage,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Understood",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Global Success / Message Dialog
            state.message?.let { msg ->
                if (msg.contains("sent", ignoreCase = true) || msg.contains("success", ignoreCase = true)) {
                    LiquidGlassDialog(onDismissRequest = vm::dismissMessage) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(green.copy(alpha = 0.18f))
                                    .border(2.dp, green, CircleShape)
                                    .shadow(12.dp, CircleShape, ambientColor = green, spotColor = green),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = green,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Challenge Issued! ⚔️",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Your challenge was sent to ${state.selectedOpponent?.name ?: "the opponent team"}.\nThey will receive an instant notification to accept or decline.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Rivalry Chat callout
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF0F1B2E) else Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Once accepted, coordinate venue, date & time in the rivalry chat.",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = vm::dismissMessage,
                                colors = ButtonDefaults.buttonColors(containerColor = green),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
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

            // Create Challenge Stepper Bottom Sheet
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

            // Team Statistics & Details Preview Sheet
            if (state.selectedTeamDetail != null) {
                TeamPreviewSheet(state = state, green = green, vm = vm)
            }

            // Confirmation Dialogs
            showConfirmAccept?.let { challenge ->
                ConfirmActionDialog(
                    title = "Accept Challenge",
                    message = "Are you sure you want to accept this challenge from ${challenge.challenger}? This will create a rivalry chat with the opponent.",
                    confirmLabel = "Accept Challenge",
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
                    message = "Are you sure you want to cancel your challenge to ${challenge.challenged}?",
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

    val rotationAnim by animateFloatAsState(
        targetValue = if (state.refreshing) 360f else 0f,
        animationSpec = if (state.refreshing) {
            infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(300)
        },
        label = "refreshSpin"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                onClick = back,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Challenges",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡", fontSize = 16.sp)
                }
                Text(
                    text = "Find your next rivalry & compete",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                onClick = vm::refresh,
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
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = green,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAnim)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = vm::openCreate,
                shape = CircleShape,
                color = green,
                shadowElevation = 6.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Challenge",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Premium Capsule Tab Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) DarkSurface else LightSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                SegmentTab(
                    text = "Find Teams",
                    icon = Icons.Outlined.Search,
                    selected = state.tab == ChallengeTab.FIND_TEAMS,
                    green = green,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.setTab(ChallengeTab.FIND_TEAMS) }
                )
                SegmentTab(
                    text = "My Challenges",
                    icon = Icons.Outlined.EmojiEvents,
                    selected = state.tab == ChallengeTab.MY_CHALLENGES,
                    green = green,
                    badgeCount = state.incoming.size,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.setTab(ChallengeTab.MY_CHALLENGES) }
                )
            }
        }
    }
}

@Composable
private fun SegmentTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    green: Color,
    modifier: Modifier,
    badgeCount: Int = 0,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textCol,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = textCol,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 13.sp
            )
            if (badgeCount > 0 && !selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindTeamsContent(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    val isDark = LocalThemeController.current.isDark

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
                    placeholder = "Search opponent teams by name or city...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            if (state.loading && state.opponents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = green, strokeWidth = 3.dp)
                    }
                }
            } else if (!state.loading && state.opponents.isEmpty()) {
                item {
                    LiquidGlassEmptyState(
                        title = "No Opponent Teams Found",
                        description = if (state.search.isNotBlank()) "No teams match \"${state.search}\". Try clearing the search." else "No opponent teams available for challenge right now.",
                        icon = Icons.Outlined.Group,
                        actionText = if (state.search.isNotBlank()) "Clear Search" else null,
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
                        onChallenge = {
                            vm.selectOpponentFromPreview(team)
                            vm.nextStep()
                        }
                    )
                }

                if (state.hasMoreOpponents && state.opponents.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loadingMoreOpponents) {
                                CircularProgressIndicator(color = green, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                OutlinedButton(
                                    onClick = vm::loadMoreOpponents,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Load More Teams",
                                        color = green,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
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
    val isDark = LocalThemeController.current.isDark
    val list = when (state.section) {
        ChallengeSection.INCOMING -> state.incoming
        ChallengeSection.SENT -> state.sent
        ChallengeSection.HISTORY -> state.history
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Section Filter Chips Row
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

        // Section Description Banner
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            val title = when (state.section) {
                ChallengeSection.INCOMING -> "Incoming Challenges"
                ChallengeSection.SENT -> "Sent Challenges"
                ChallengeSection.HISTORY -> "Challenge History"
            }
            val subtitle = when (state.section) {
                ChallengeSection.INCOMING -> "Review and accept challenges received from other teams"
                ChallengeSection.SENT -> "Track pending challenges your teams have issued to opponents"
                ChallengeSection.HISTORY -> "Past completed, declined, and cancelled challenge matchups"
            }
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.weight(1f)
        ) {
            if (state.loading && list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = green, strokeWidth = 3.dp)
                }
            } else if (list.isEmpty()) {
                val emptyMsg = when (state.section) {
                    ChallengeSection.INCOMING -> "No incoming challenges pending right now."
                    ChallengeSection.SENT -> "You haven't sent any challenges yet. Find a team to challenge!"
                    ChallengeSection.HISTORY -> "No challenge history found."
                }
                LiquidGlassEmptyState(
                    title = "No Challenges",
                    description = emptyMsg,
                    icon = Icons.Outlined.EmojiEvents,
                    actionText = if (state.section == ChallengeSection.SENT) "Find Teams" else null,
                    onActionClick = { vm.setTab(ChallengeTab.FIND_TEAMS) }
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
        "existing" -> "Active"
        "pending" -> "Pending"
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
                TeamLogoImage(
                    logoUrl = team.logo,
                    teamName = team.name,
                    sport = team.sport,
                    green = green,
                    size = 52
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${team.members} members",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        if (team.location.isNotBlank()) {
                            Text(
                                text = " • ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = team.location,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Sport tag badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = green.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = getSportEmoji(team.sport),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = team.sport.ifBlank { "Multi-sport" },
                                    color = green,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isDisabled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentGold.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (blockReason == "existing") "Connected" else "Pending",
                                    color = AccentGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!isDisabled) onChallenge()
                    },
                    enabled = !isDisabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = green,
                        disabledContainerColor = if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isDisabled) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = buttonText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!isDisabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats Dashboard row (5 modern capsules)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
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
        "completed" -> Color(0xFF3B82F6)
        else -> StatusWarning
    }

    val canOpenScoring = c.enableScoring || !c.cricketScoringMatchId.isNullOrBlank() || (c.sport.contains("cricket", ignoreCase = true) && (c.status.equals("accepted", true) || c.status.equals("completed", true)))

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { vm.openDetails(c) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status & Date Tag Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidGlassBadge(
                    text = c.status.replaceFirstChar { it.uppercase() },
                    badgeColor = statusColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) DarkSurfaceVariant else LightSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = c.date?.let { "Date $it" } ?: "Date TBD",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Challenger VS Opponent Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoImage(
                        logoUrl = null,
                        teamName = c.challenger,
                        sport = c.sport,
                        green = green,
                        size = 50
                    )
                    Text(
                        text = c.challenger,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "Challenger",
                        fontSize = 10.sp,
                        color = green,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center VS Emblem
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(green, green.copy(alpha = 0.7f))
                            )
                        )
                        .shadow(8.dp, CircleShape, ambientColor = green, spotColor = green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoImage(
                        logoUrl = null,
                        teamName = c.challenged,
                        sport = c.sport,
                        green = green,
                        size = 50
                    )
                    Text(
                        text = c.challenged,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "Opponent",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sport & Details Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF0F1B16) else Color(0xFFF3F4F6),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(getSportEmoji(c.sport), fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.sport.ifBlank { "Multi-sport" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (c.venue.isNotBlank()) c.venue else "Venue to be coordinated in chat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (c.stake > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGold.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("🏆", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rs. ${"%.0f".format(c.stake)}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Accept", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    if (c.canDecline) {
                        OutlinedButton(
                            onClick = onDecline,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Decline", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    if (c.canCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (c.status == "accepted" && c.chatId != null) {
                        Button(
                            onClick = { vm.openChat(c) },
                            colors = ButtonDefaults.buttonColors(containerColor = green.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = green, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Chat", color = green, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
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
    sport: String? = null,
    green: Color,
    size: Int
) {
    val isDark = LocalThemeController.current.isDark
    val sportEmoji = getSportEmoji(sport ?: "")

    Box(contentAlignment = Alignment.BottomEnd) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "Team Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(green, green.copy(alpha = 0.75f), Color(0xFF00874D))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = teamName.firstOrNull()?.uppercase()?.toString() ?: "T",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size * 0.42).sp
                )
            }
        }

        // Dynamic sport ball badge (matches actual sport instead of hardcoded baseball)
        Box(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size((size * 0.38).dp)
                .clip(CircleShape)
                .background(if (isDark) DarkSurface else Color.White)
                .border(1.dp, green.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sportEmoji,
                fontSize = (size * 0.22).sp
            )
        }
    }
}

// ── Stepper Challenge Creation Modal Bottom Sheet ──
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(3.dp)
            ) {
                Box(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        if (state.step == ChallengeStep.MY_TEAM) vm.closeCreate() else vm.previousStep()
                    },
                    shape = CircleShape,
                    color = if (isDark) DarkSurfaceVariant else LightSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create Challenge",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "STEP $currentStepIndex OF 4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = green,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    onClick = vm::closeCreate,
                    shape = CircleShape,
                    color = if (isDark) DarkSurfaceVariant else LightSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Progress Line
            val animatedProgress by animateFloatAsState(
                targetValue = currentStepIndex / 4f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "stepProgress"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (isDark) Color(0xFF1E2E27) else Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(green.copy(alpha = 0.7f), green)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Container for Steps with IME Padding so Keyboard Never Obscures Content!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .weight(1f, fill = false)
                    .heightIn(max = 480.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + slideInHorizontally { 40 })
                            .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally { -40 })
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

            Spacer(modifier = Modifier.height(14.dp))

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
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.step == ChallengeStep.REVIEW) {
                                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Challenge", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            } else {
                                Text("Continue", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
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
    val isDark = LocalThemeController.current.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Your Team", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Choose which of your teams will issue this challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        if (state.loadingMyTeams && state.myTeams.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = green, strokeWidth = 3.dp)
            }
        } else if (state.myTeams.isEmpty()) {
            LiquidGlassEmptyState(
                title = "No Teams Found",
                description = "You must create or join at least one team to issue challenges."
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.myTeams.forEach { team ->
                    val isSelected = state.selectedTeam?.id == team.id
                    val borderCol = if (isSelected) green else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    val cardBg = if (isSelected) green.copy(alpha = 0.12f) else if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.selectTeam(team) },
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TeamLogoImage(
                                logoUrl = team.logo,
                                teamName = team.name,
                                sport = team.sport,
                                green = green,
                                size = 46
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${team.members} members • ${team.location.ifBlank { "Multi-location" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(green),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }
                    }
                }

                if (state.hasMoreMyTeams && state.myTeams.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = vm::loadMoreMyTeams) {
                            Text("Load More My Teams", color = green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    val isDark = LocalThemeController.current.isDark
    val relationshipMap = state.relationships

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Opponent Team", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Select a team to receive your challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        if (state.opponents.isEmpty()) {
            LiquidGlassEmptyState(
                title = "No Teams Available",
                description = "No opponent teams discovered yet."
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.opponents.forEach { team ->
                    val isSelected = state.selectedOpponent?.id == team.id

                    // Relationship Check
                    val key = listOf(state.selectedTeam?.id ?: 0, team.id).sorted().joinToString(":")
                    val relationship = relationshipMap[key]
                    val isBlocked = relationship != null

                    val borderCol = if (isSelected) green else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    val cardBg = if (isSelected) green.copy(alpha = 0.12f) else if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant
                    val alphaVal = if (isBlocked) 0.55f else 1.0f

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alphaVal)
                            .clickable {
                                if (!isBlocked) {
                                    vm.selectOpponent(team)
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TeamLogoImage(
                                logoUrl = team.logo,
                                teamName = team.name,
                                sport = team.sport,
                                green = green,
                                size = 46
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${team.members} members • ${team.location.ifBlank { "Multi-location" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(green),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else if (!isBlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                                )
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
        if (list.none { it.name.contains("Badminton", ignoreCase = true) }) {
            list.add(ChallengeSportUi(id = 1, name = "Badminton"))
        }
        if (list.none { it.name.contains("Basketball", ignoreCase = true) }) {
            list.add(ChallengeSportUi(id = 3, name = "Basketball"))
        }
        list.distinctBy { it.name }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Challenge Details", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Choose the sport for the challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sport", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(" *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 2-Column Grid of Sports
        val rows = availableSports.chunked(2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { sport ->
                        val isSelected = state.selectedSport?.id == sport.id || state.selectedSport?.name == sport.name
                        val borderCol = if (isSelected) green else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        val cardBg = if (isSelected) green.copy(alpha = 0.12f) else if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp)
                                .clickable { vm.selectSport(sport) },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(getSportEmoji(sport.name), fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sport.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) green else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(green),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    }
                                }
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

        // Blue Info Callout Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF0F1B2E) else Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Once the challenge is accepted, both teams can discuss and agree on the venue, date, and time in the rivalry chat.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                    lineHeight = 16.sp
                )
            }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val presetStakes = listOf("0", "500", "1000", "2000", "5000")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Review Challenge", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text("Confirm match details before sending", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))

        // Main Review VS Card with Glowing Green Border
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF101C17) else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, green),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        TeamLogoImage(
                            logoUrl = state.selectedTeam?.logo,
                            teamName = state.selectedTeam?.name ?: "Challenger",
                            sport = state.selectedSport?.name ?: state.selectedTeam?.sport,
                            green = green,
                            size = 54
                        )
                        Text(
                            text = state.selectedTeam?.name ?: "Your Team",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Your Team", color = green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(green, green.copy(alpha = 0.8f))
                                )
                            )
                            .shadow(8.dp, CircleShape, ambientColor = green, spotColor = green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoImage(
                            logoUrl = state.selectedOpponent?.logo,
                            teamName = state.selectedOpponent?.name ?: "Opponent",
                            sport = state.selectedSport?.name ?: state.selectedOpponent?.sport,
                            green = green,
                            size = 54
                        )
                        Text(
                            text = state.selectedOpponent?.name ?: "Opponent",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Opponent", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Sport Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(green.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getSportEmoji(state.selectedSport?.name ?: ""), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Selected Sport", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = state.selectedSport?.name ?: "Multi-sport",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Venue & Time Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Venue & Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("To be decided in rivalry chat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Optional Stake Row & Preset Chips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Optional Stake (Rs.)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            Text("Friendly / Custom", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Stake Quick Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetStakes) { amount ->
                val isSelected = (state.stake == amount) || (amount == "0" && state.stake.isBlank())
                val label = if (amount == "0") "Free (Rs. 0)" else "Rs. $amount"

                Surface(
                    onClick = {
                        vm.setStake(if (amount == "0") "" else amount)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) green else if (isDark) DarkSurfaceVariant else LightSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) green else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    )
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Stake Input Field
        OutlinedTextField(
            value = state.stake,
            onValueChange = vm::setStake,
            placeholder = { Text("Or enter custom stake (e.g. 1500)", fontSize = 13.sp, color = Color.Gray) },
            leadingIcon = {
                Text(
                    text = "Rs.",
                    fontWeight = FontWeight.Bold,
                    color = green,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                )
            },
            trailingIcon = {
                if (state.stake.isNotBlank()) {
                    IconButton(onClick = { vm.setStake("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f),
                focusedContainerColor = if (isDark) DarkSurfaceVariant.copy(alpha = 0.5f) else LightSurfaceVariant,
                unfocusedContainerColor = if (isDark) DarkSurfaceVariant.copy(alpha = 0.5f) else LightSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Info Callout Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF0F1B2E) else Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "The opponent team will receive an invite to accept or decline. Once accepted, you can discuss venue and time in the rivalry chat.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                    lineHeight = 15.sp
                )
            }
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                    CircularProgressIndicator(color = green, strokeWidth = 3.dp)
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
                    // Header Cover Banner
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
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
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

                                Surface(
                                    onClick = vm::closeTeam,
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Centered Large Avatar Logo
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                TeamLogoImage(
                                    logoUrl = team.logo,
                                    teamName = team.name,
                                    sport = team.teamType,
                                    green = green,
                                    size = 76
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
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!team.location.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = green, modifier = Modifier.size(13.dp))
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

                    // 3 Stat Cards Row
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
                                value = (team.teamType ?: "General").replaceFirstChar { it.uppercase() },
                                valueColor = Color(0xFFFFB300),
                                label = "TYPE",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Challenge Performance Card Section
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
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

                    // Details Card Section
                    item {
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Info, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))

                                    DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = team.location ?: "Not specified")
                                    DetailRow(icon = Icons.Default.Layers, label = "Team Type", value = team.teamType ?: "General")
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
                                        TeamLogoImage(
                                            logoUrl = match.opponentLogo,
                                            teamName = match.opponentName,
                                            green = green,
                                            size = 34
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("vs ${match.opponentName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                                            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = formatScore(match.teamScore),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(" - ", fontSize = 11.sp, color = Color.Gray)
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

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = resColor.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, resColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = if (match.result == "no_result") "NR" else match.result.uppercase(),
                                                color = resColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        CircularProgressIndicator(color = green, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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

                // Bottom Sticky Action Button
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
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = AccentGold.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassEmpty, null, tint = AccentGold)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Join Request Pending", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Waiting for captain's approval", color = Color.Gray, fontSize = 11.sp)
                                    }
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
                                Text("Challenge This Team", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
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
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = valueColor)
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
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
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
    val bg = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
        }
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
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Challenge Match",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                LiquidGlassBadge(
                    text = challenge.status.replaceFirstChar { it.uppercase() },
                    badgeColor = if (challenge.status == "accepted") green else StatusWarning
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Text(
                text = "${challenge.challenger} vs ${challenge.challenged}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (challenge.sport.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getSportEmoji(challenge.sport), fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sport: ${challenge.sport}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (challenge.venue.isNotBlank()) {
                Text("Venue: ${challenge.venue}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            challenge.date?.let {
                Text("Date: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (challenge.stake > 0.0) {
                Text(
                    text = "Stake: Rs. ${"%.2f".format(challenge.stake)}",
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontSize = 13.sp
                )
            }

            if (canOpenScoring) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        vm.closeDetails()
                        onOpenScoring(challenge.cricketScoringMatchId ?: challenge.id.toString())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = green),
                    shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (challenge.canAccept) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (challenge.canDecline) {
                    OutlinedButton(
                        onClick = onDecline,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Decline", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (challenge.canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = vm::closeDetails) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurface)
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
    LiquidGlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
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
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getSportEmoji(sport: String): String {
    val name = sport.lowercase()
    return when {
        name.contains("badminton") -> "🏸"
        name.contains("cricket") -> "🏏"
        name.contains("football") || name.contains("soccer") -> "⚽"
        name.contains("basketball") -> "🏀"
        name.contains("tennis") -> "🎾"
        name.contains("volleyball") -> "🏐"
        name.contains("table tennis") || name.contains("ping pong") -> "🏓"
        name.contains("pool") || name.contains("snooker") || name.contains("billiards") -> "🎱"
        name.contains("rugby") -> "🏉"
        name.contains("baseball") -> "⚾"
        name.contains("hockey") -> "🏑"
        name.contains("golf") -> "⛳"
        name.contains("boxing") || name.contains("mma") -> "🥊"
        name.contains("swimming") -> "🏊"
        name.contains("padel") -> "🎾"
        else -> "🏆"
    }
}

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
                            onCancel = { showConfirmCancel = it }
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
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2200)
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
                    }
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
                        green = green,
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
    onCancel: (ChallengeUi) -> Unit
) {
    val list = when (state.section) {
        ChallengeSection.INCOMING -> state.incoming
        ChallengeSection.SENT -> state.sent
        ChallengeSection.HISTORY -> state.history
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChallengeSection.entries.forEach { section ->
                val label = section.name.lowercase().replaceFirstChar { it.uppercase() }
                val isSelected = state.section == section
                LiquidGlassFilterChip(
                    selected = isSelected,
                    onClick = { vm.setSection(section) },
                    label = label,
                    modifier = Modifier.weight(1f)
                )
            }
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
                            onCancel = { onCancel(challenge) }
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
    green: Color,
    onPreview: () -> Unit,
    onChallenge: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPreview
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                if (team.sport.isNotBlank()) {
                    Text(
                        text = team.sport,
                        color = green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onChallenge,
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Challenge", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    onCancel: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val statusColor = when (c.status.lowercase()) {
        "accepted" -> green
        "declined", "cancelled", "expired" -> StatusError
        else -> StatusWarning
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { vm.openDetails(c) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = c.challenger,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassBadge(
                    text = c.status.replaceFirstChar { it.uppercase() },
                    badgeColor = statusColor
                )
            }
            Text(
                text = "vs  ${c.challenged}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Details Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sports,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = c.sport,
                    color = green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
                if (c.venue.isNotBlank()) {
                    Text(
                        text = " • ${c.venue}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            c.date?.let { dateStr ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${dateStr}${c.start?.let { " at $it" } ?: ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            if (c.stake > 0.0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MonetizationOn,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Stake: Rs. ${"%.2f".format(c.stake)}",
                        color = green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
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
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "Team Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size * 0.28).dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape((size * 0.28).dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size * 0.28).dp))
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
}

// ── Stepper Challenge Creation modal ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChallengeSheet(
    state: ChallengeState,
    green: Color,
    vm: ChallengeViewModel
) {
    ModalBottomSheet(
        onDismissRequest = vm::closeCreate,
        containerColor = if (LocalThemeController.current.isDark) DarkSurface else LightSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Create Challenge",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::closeCreate) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Stepper Visual Tracker
            StepperTracker(currentStep = state.step, green = green)
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 380.dp)
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                if (state.step != ChallengeStep.MY_TEAM) {
                    OutlinedButton(
                        onClick = vm::previousStep,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.5f))
                    ) {
                        Text("Back", color = green, fontWeight = FontWeight.Bold)
                    }
                }
                
                val isNextEnabled = when (state.step) {
                    ChallengeStep.MY_TEAM -> state.selectedTeam != null
                    ChallengeStep.OPPONENT -> state.selectedOpponent != null
                    ChallengeStep.SPORT -> state.selectedSport != null
                    ChallengeStep.REVIEW -> true
                }

                Button(
                    onClick = vm::nextStep,
                    enabled = isNextEnabled && !state.submitting,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = green)
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (state.step == ChallengeStep.REVIEW) "Send Challenge" else "Continue",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
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
    // Collect all sports available.
    // If team is selected, try to match team sports
    val availableSports = remember(state.selectedTeam, state.selectedOpponent, state.sports) {
        val list = mutableListOf<ChallengeSportUi>()
        val myTeamSport = state.selectedTeam?.sport?.lowercase()
        val oppTeamSport = state.selectedOpponent?.sport?.lowercase()
        
        state.sports.forEach { s ->
            val name = s.name.lowercase()
            if (myTeamSport?.contains(name) == true || oppTeamSport?.contains(name) == true) {
                list.add(0, s) // prioritize
            } else {
                list.add(s)
            }
        }
        
        list.distinctBy { it.name }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Select Sport Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Choose the sport for this challenge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableSports) { sport ->
                val isSelected = state.selectedSport?.id == sport.id
                val borderCol = if (isSelected) green else Color.Transparent
                
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = borderCol,
                    onClick = { vm.selectSport(sport) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getSportEmoji(sport.name),
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = sport.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Info Note matching React Native
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(green.copy(alpha = 0.08f))
                .border(1.dp, green.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = green, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Once accepted, both teams can discuss and choose the match venue/time in the rivalry chat.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Review Challenge", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Verify challenge details before sending", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // VS representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoImage(logoUrl = state.selectedTeam?.logo, teamName = state.selectedTeam?.name ?: "Challenger", green = green, size = 52)
                        Text(state.selectedTeam?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Challenger", color = Color.Gray, fontSize = 10.sp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(green.copy(alpha = 0.15f))
                            .border(1.dp, green.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VS", color = green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoImage(logoUrl = state.selectedOpponent?.logo, teamName = state.selectedOpponent?.name ?: "Opponent", green = green, size = 52)
                        Text(state.selectedOpponent?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Opponent", color = Color.Gray, fontSize = 10.sp)
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(getSportEmoji(state.selectedSport?.name ?: ""), fontSize = 18.sp)
                    Text(state.selectedSport?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Stake Input Field
        Text("Optional Stake (Rs.)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LiquidGlassTextField(
            value = state.stake,
            onValueChange = vm::setStake,
            placeholder = "Enter stake (e.g. 1000)",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
            )
        }
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
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = green)
                }
            } else if (state.previewTeamDetail != null) {
                val team = state.previewTeamDetail
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 500.dp)
                ) {
                    // Profile Cover Section
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(green.copy(alpha = 0.35f), Color.Transparent)
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
                            
                            // Absolute Position Logo & Name overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TeamLogoImage(logoUrl = team.logo, teamName = team.name, green = green, size = 60)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(team.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = team.location ?: "Multi-location",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stats Grid
                    item {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Challenge Performance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            team.challengeStats?.let { stats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatBox(label = "Played", value = stats.totalMatches.toString(), color = Color.Gray, modifier = Modifier.weight(1f))
                                    StatBox(label = "Wins", value = stats.wins.toString(), color = green, modifier = Modifier.weight(1f))
                                    StatBox(label = "Losses", value = stats.losses.toString(), color = StatusError, modifier = Modifier.weight(1f))
                                    StatBox(label = "Win %", value = "${"%.1f".format(stats.winPercentage)}%", color = AccentGold, modifier = Modifier.weight(1f))
                                }
                            } ?: run {
                                Text("No matches played yet.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Description / About
                    if (team.description.isNotBlank()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                                Text("About", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = team.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Recent Matches
                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                            Text("Recent Matches", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (state.previewRecentMatches.isEmpty()) {
                                Text("No recent match records.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                            }
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
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Actions area (Join request or open chat)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Challenge Team", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                
                                OutlinedButton(
                                    onClick = vm::requestJoinTeam,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = green)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Request to Join", fontWeight = FontWeight.Bold, color = green)
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
    onCancel: () -> Unit
) {
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

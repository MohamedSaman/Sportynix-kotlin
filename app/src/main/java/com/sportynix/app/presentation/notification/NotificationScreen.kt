package com.sportynix.app.presentation.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.NotificationDto
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.ShimmerSkeleton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = isSystemInDarkTheme()
    val accentGreen = if (isDark) NeonGreen else SportynixGreenPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = MaterialTheme.colorScheme.background

    val filtered = viewModel.filteredNotifications()
    val unreadCount = state.notifications.count { !it.isRead }

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = if (isDark) Color(0xFF1A1A2E) else Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Clear All", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text("Remove all notifications? This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAll(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = accentGreen)
                }
            }
        )
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            // Glass Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (isDark)
                            Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF0D1B2A).copy(alpha = 0.9f)))
                        else
                            Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.95f)))
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (unreadCount > 0) {
                            Text("$unreadCount unread", fontSize = 12.sp, color = accentGreen)
                        }
                    }
                    if (state.isSelectionMode) {
                        TextButton(onClick = { viewModel.deleteSelected() }) {
                            Text("Delete (${state.selectedIds.size})", color = Color.Red)
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        if (unreadCount > 0) {
                            IconButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Mark all read",
                                    tint = accentGreen)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Tab row
                ScrollableTabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (state.activeTab.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[state.activeTab.ordinal]),
                                color = accentGreen,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {}
                ) {
                    listOf("All", "Unread", "Booking", "Points").forEachIndexed { idx, label ->
                        Tab(
                            selected = state.activeTab.ordinal == idx,
                            onClick = { viewModel.setActiveTab(NotificationTab.entries[idx]) },
                            text = {
                                Text(label, fontWeight = if (state.activeTab.ordinal == idx)
                                    FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (state.activeTab.ordinal == idx) accentGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                // Shimmer loading
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) { ShimmerNotificationCard() }
                }
            }
            filtered.isEmpty() && !state.isLoading -> {
                EmptyNotificationsState(
                    tab = state.activeTab,
                    accentGreen = accentGreen,
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    state = rememberLazyListState().also { listState ->
                        LaunchedEffect(listState) {
                            snapshotFlow { listState.layoutInfo }
                                .collect { info ->
                                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    val total = info.totalItemsCount
                                    if (lastVisible >= total - 3) viewModel.loadMore()
                                }
                        }
                    }
                ) {
                    if (state.isRefreshing) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    items(filtered, key = { it.id }) { notification ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 24 })
                        ) {
                            NotificationCard(
                                notification = notification,
                                isSelected = notification.id in state.selectedIds,
                                isSelectionMode = state.isSelectionMode,
                                accentGreen = accentGreen,
                                isDark = isDark,
                                onTap = {
                                    if (state.isSelectionMode) {
                                        viewModel.toggleSelection(notification.id)
                                    } else {
                                        if (!notification.isRead) viewModel.markAsRead(notification.id)
                                    }
                                },
                                onLongPress = {
                                    if (!state.isSelectionMode) viewModel.toggleSelectionMode()
                                    viewModel.toggleSelection(notification.id)
                                },
                                onDelete = { viewModel.deleteNotification(notification.id) }
                            )
                        }
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = accentGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationCard(
    notification: NotificationDto,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    accentGreen: Color,
    isDark: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (isDark) Color(0xFF1A1A2E) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Notification", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Remove this notification?",
                color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = accentGreen)
                }
            }
        )
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val glassCardBg = if (isDark) {
        if (!notification.isRead) Color(0xFF1A2A3A) else Color(0xFF141420)
    } else {
        if (!notification.isRead) Color(0xFFF0FFF4) else Color.White
    }
    val borderColor = if (isSelected) accentGreen
    else if (!notification.isRead) accentGreen.copy(alpha = 0.3f)
    else if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        elevation = if (!notification.isRead) 4.dp else 1.dp,
        backgroundColor = glassCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon/Checkbox column
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTap() },
                    colors = CheckboxDefaults.colors(checkedColor = accentGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                NotificationIcon(
                    type = notification.type,
                    accentGreen = accentGreen,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentGreen)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.timeAgo ?: notification.createdAt?.take(10) ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (!isSelectionMode) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationIcon(type: String?, accentGreen: Color, isDark: Boolean) {
    val (icon, bgColor) = when {
        type?.contains("booking", true) == true ->
            Icons.Default.EventAvailable to Color(0xFF0EA5E9).copy(alpha = 0.15f)
        type?.contains("point", true) == true || type?.contains("reward", true) == true ->
            Icons.Default.Stars to Color(0xFFF59E0B).copy(alpha = 0.15f)
        type?.contains("team", true) == true ->
            Icons.Default.Group to Color(0xFF8B5CF6).copy(alpha = 0.15f)
        type?.contains("match", true) == true ->
            Icons.Default.SportsCricket to accentGreen.copy(alpha = 0.15f)
        type?.contains("cancel", true) == true ->
            Icons.Default.Cancel to Color.Red.copy(alpha = 0.15f)
        else -> Icons.Default.Notifications to accentGreen.copy(alpha = 0.15f)
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyNotificationsState(
    tab: NotificationTab,
    accentGreen: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ), label = "pulseScale"
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(accentGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null,
                tint = accentGreen, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = when (tab) {
                NotificationTab.UNREAD -> "All caught up!"
                NotificationTab.BOOKING -> "No booking notifications"
                NotificationTab.POINTS -> "No points activity"
                NotificationTab.ALL -> "No notifications yet"
            },
            fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We'll notify you when something happens",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShimmerNotificationCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            ShimmerSkeleton(modifier = Modifier.size(42.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerSkeleton(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
    }
}

package com.sportynix.app.presentation.notification

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportynix.app.data.remote.dto.NotificationDto
import com.sportynix.app.domain.model.AnnouncementDetailPayload

private val Green = Color(0xFF16A05D)

@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookingDetail: (Int) -> Unit = {},
    onNavigateToBookingHistory: () -> Unit = {},
    onNavigateToTeam: (Int?, Boolean) -> Unit = { _, _ -> },
    onNavigateToPoints: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToChat: (Int, String) -> Unit = { _, _ -> },
    onNavigateToMessageRequests: () -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<AnnouncementDetailPayload?>(null) }
    val dark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val bg = if (dark) Color(0xFF0A0B0E) else Color(0xFFF7F8FA)

    LaunchedEffect(viewModel) {
        viewModel.navigation.collect { destination ->
            when (destination) {
                is NotificationDestination.BookingDetail -> onNavigateToBookingDetail(destination.id)
                NotificationDestination.BookingHistory -> onNavigateToBookingHistory()
                is NotificationDestination.Team -> onNavigateToTeam(destination.teamId, destination.invitations)
                NotificationDestination.Points -> onNavigateToPoints()
                NotificationDestination.Challenges -> onNavigateToChallenges()
                is NotificationDestination.Chat -> onNavigateToChat(destination.id, destination.name)
                NotificationDestination.MessageRequests -> onNavigateToMessageRequests()
                is NotificationDestination.Announcement -> announcement = destination.payload
                else -> Unit
            }
        }
    }
    announcement?.let { payload ->
        AnnouncementDetailScreen(content = payload, onNavigateBack = { announcement = null })
        return
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear All Notifications") },
        text = { Text("Are you sure you want to delete all notifications? This action cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clearAll() }) { Text("Clear All", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )

    Scaffold(containerColor = bg, topBar = {
        NotificationHeader(
            unread = state.count(NotificationTab.UNREAD), empty = state.notifications.isEmpty(),
            marking = state.isMarkingAll, clearing = state.isClearingAll,
            onBack = onNavigateBack, onMarkAll = viewModel::markAllAsRead, onClear = { confirmClear = true }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            FilterTabs(state.activeTab, state, viewModel::setActiveTab)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
                state.errorMessage != null && state.notifications.isEmpty() -> ErrorState(state.errorMessage!!, { viewModel.fetchNotifications() })
                state.filtered.isEmpty() -> EmptyState(state.activeTab)
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.filtered, key = { it.id }) { item ->
                        NotificationCard(item, item.id in state.deletingIds, { viewModel.open(item) }, { viewModel.deleteNotification(item.id) })
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable private fun NotificationHeader(unread: Int, empty: Boolean, marking: Boolean, clearing: Boolean, onBack: () -> Unit, onMarkAll: () -> Unit, onClear: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(66.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Green) }
            Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundAction(Icons.Default.MarkEmailRead, Green, marking, unread == 0, onMarkAll)
                RoundAction(Icons.Default.Delete, Color.Red, clearing, empty, onClear)
            }
        }
    }
}

@Composable private fun RoundAction(icon: ImageVector, tint: Color, loading: Boolean, disabled: Boolean, click: () -> Unit) {
    FilledIconButton(onClick = click, enabled = !disabled && !loading, modifier = Modifier.size(38.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = tint.copy(alpha = .1f), disabledContainerColor = tint.copy(alpha = .04f))) {
        if (loading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = tint) else Icon(icon, null, Modifier.size(18.dp), tint = tint.copy(alpha = if (disabled) .35f else 1f))
    }
}

@Composable private fun FilterTabs(selected: NotificationTab, state: NotificationUiState, select: (NotificationTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        NotificationTab.entries.forEach { tab ->
            val active = selected == tab
            val underline by animateDpAsState(if (active) 2.dp else 0.dp, label = "filter")
            Column(Modifier.weight(1f).clickable { select(tab) }.padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val count = state.count(tab)
                val label = tab.name.lowercase().replaceFirstChar(Char::uppercase) + if (tab == NotificationTab.UNREAD && count == 0) "" else " ($count)"
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (active) Green else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(9.dp)); Box(Modifier.fillMaxWidth().height(underline).background(Green))
            }
        }
    }
}

private data class IconStyle(val icon: ImageVector, val color: Color)
private fun style(type: String?): IconStyle = when (type.orEmpty().lowercase()) {
    "booking_confirmed", "booking_completed" -> IconStyle(Icons.Default.EventAvailable, Green)
    "booking_cancelled", "booking_no_show" -> IconStyle(Icons.Default.EventBusy, Color(0xFFE5484D))
    "team_channel_followed", "team_approved", "team_joined", "team_rejected", "team_declined", "team_removed", "team_deleted", "team_assigned_success", "team_booking_assigned", "team_invitation" -> IconStyle(Icons.Default.Groups, Color(0xFF3B82F6))
    "points_earned", "referral_earned" -> IconStyle(Icons.Default.Stars, Color(0xFFF4A825))
    "points_deducted" -> IconStyle(Icons.Default.RemoveCircle, Color(0xFFE5484D))
    else -> IconStyle(Icons.Default.Notifications, Green)
}

@Composable private fun NotificationCard(item: NotificationDto, deleting: Boolean, open: () -> Unit, delete: () -> Unit) {
    val dark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark; val s = style(item.type)
    val cardColor by animateColorAsState(if (!item.isRead) (if (dark) Color(0xFF14231D) else Color(0xFFF1FFF7)) else MaterialTheme.colorScheme.surface, label = "read")
    Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(if (item.isRead) 2.dp else 5.dp)) {
        Row {
            Box(Modifier.width(4.dp).heightIn(min = 126.dp).background(if (item.isRead) Color.Transparent else Green))
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(s.color.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(s.icon, null, tint = s.color, modifier = Modifier.size(23.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!item.isRead) Box(Modifier.size(8.dp).clip(CircleShape).background(Green))
                    }
                    Spacer(Modifier.height(5.dp)); Text(item.message, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(9.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.timeAgo ?: item.createdAt.orEmpty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .75f), modifier = Modifier.weight(1f))
                        if (!item.isRead) Text("Tap to view", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Green)
                        IconButton(onClick = delete, enabled = !deleting, modifier = Modifier.size(30.dp)) { if (deleting) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp) else Icon(Icons.Default.DeleteOutline, "Delete", tint = Color.Red.copy(alpha = .8f), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}

@Composable private fun EmptyState(tab: NotificationTab) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) { Column(Modifier.padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.NotificationsOff, null, Modifier.size(52.dp), tint = Green.copy(alpha = .55f)); Spacer(Modifier.height(14.dp)); Text("No notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(6.dp)); Text(when(tab) { NotificationTab.UNREAD -> "You're all caught up!"; NotificationTab.BOOKING -> "No booking notifications."; NotificationTab.POINTS -> "No points notifications."; else -> "No all notifications." }, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ErrorOutline, null, tint = Color.Red); Spacer(Modifier.height(8.dp)); Text(message); TextButton(onClick = retry) { Text("Try Again", color = Green) } } } }

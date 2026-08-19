package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.sportynix.app.BuildConfig
import com.sportynix.app.domain.model.ChatMember
import com.sportynix.app.domain.model.PastGame
import com.sportynix.app.domain.model.ChatMessage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import android.graphics.Bitmap
import com.sportynix.app.presentation.messages.components.GlassBadge
import com.sportynix.app.presentation.messages.components.GlassCard
import com.sportynix.app.presentation.messages.components.LiquidGlassTheme
import com.sportynix.app.presentation.messages.components.PremiumMessagesBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    chatId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit = {},
    onNavigateToBookingDetail: (Long) -> Unit = {},
    onNavigateToBookingCancellation: (Long) -> Unit = {},
    onBookMatch: (Long, Long, Long) -> Unit = { _, _, _ -> },
    onNavigateToTeam: (Long) -> Unit = {},
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chat = uiState.chat
    val title = chat?.displayName ?: chat?.otherUserName ?: chat?.teamName ?: chat?.name ?: "Chat Info"
    val avatar = remember(chat, uiState.members, uiState.currentUserId) {
        val memberAvatar = uiState.members.firstOrNull { it.id == chat?.otherUserId }?.avatar
            ?: uiState.members.firstOrNull { it.id != uiState.currentUserId && !it.avatar.isNullOrBlank() }?.avatar
        val raw = if (chat?.chatType == "direct") {
            listOf(chat.otherUserAvatar, memberAvatar).firstOrNull { !it.isNullOrBlank() }
        } else {
            listOf(chat?.teamLogo, chat?.team?.logo, chat?.otherUserAvatar).firstOrNull { !it.isNullOrBlank() }
        }
        resolveChatInfoMediaUrl(raw)
    }
    val confirmedBooking = remember(chat?.pinnedMessages, uiState.eventsPreview) {
        (chat?.pinnedMessages.orEmpty() + uiState.eventsPreview)
            .distinctBy { it.id }
            .firstNotNullOfOrNull(::toConfirmedBooking)
    }
    val isTeamOwner = chat?.team?.admin?.id != null && chat.team.admin.id == uiState.currentUserId
    val isAdmin = isTeamOwner || chat?.isAdmin == true || chat?.canManage == true || uiState.members.any { it.id == uiState.currentUserId && it.isAdmin }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }
    var showProfileImage by remember { mutableStateOf(false) }
    var teamTab by remember { mutableIntStateOf(0) }
    var gameTab by remember { mutableIntStateOf(0) }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        PremiumMessagesBackground {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
            }
        } else if (uiState.errorMessage != null && chat == null) {
            Column(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp)); Text(uiState.errorMessage ?: "Failed to load chat info", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp)); Button(onClick = viewModel::loadInfo, colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) { Text("Retry") }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                item {
                    Surface(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = LiquidGlassTheme.cardBackground(),
                        border = BorderStroke(1.dp, LiquidGlassTheme.cardBorder())
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
                }
                // Swift-parity profile header
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().clickable { showProfileImage = true }.padding(vertical = 8.dp)
                    ) {
                            ChatInfoProfileAvatar(
                                avatarUrl = avatar,
                                title = title,
                                isTeam = chat?.chatType != "direct",
                                modifier = Modifier.size(136.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            Text(
                                if (chat?.chatType == "direct") {
                                    when {
                                        chat.blockedByMe == true -> "Blocked"
                                        chat.otherUserOnline == true -> "Online"
                                        !chat.otherUserLastSeen.isNullOrBlank() -> formatChatInfoLastSeen(chat.otherUserLastSeen)
                                        else -> "Offline"
                                    }
                                } else "${chat?.membersCount ?: uiState.members.size} members",
                                fontSize = 13.sp,
                                color = if (chat?.chatType == "direct" && chat.otherUserOnline == true) LiquidGlassTheme.PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    }
                }

                item {
                    Button(onClick = { onNavigateToChat(chatId) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) {
                        Icon(Icons.Default.Forum, null); Spacer(Modifier.width(8.dp)); Text("Open Chat", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
                    }
                }

                if (chat != null) {
                    item {
                        GlassCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (chat.chatType == "team_channel") {
                                    InfoAction(Icons.Default.NotificationsActive, if (chat.isFollowing == true) "Unfollow" else "Follow") { viewModel.toggleFollow() }
                                }
                                InfoAction(Icons.Default.PermMedia, "Media") { onNavigateToGallery(chatId) }
                                if (chat.chatType == "direct" && chat.otherUserId != null) {
                                    InfoAction(Icons.Default.Flag, "Report") { viewModel.showReportModal(true) }
                                    InfoAction(Icons.Default.Block, if (chat.blockedByMe == true) "Unblock" else "Block") { confirmation = "block" }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(onClick = { onNavigateToGallery(chatId) }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(LiquidGlassTheme.PrimaryGreen.copy(alpha=.14f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PermMedia, null, tint = LiquidGlassTheme.PrimaryGreen) }
                                Spacer(Modifier.width(12.dp)); Text("Media, Links and Docs", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    val resolvedTeamId = chat.team?.id ?: chat.teamId
                    if (chat.chatType == "team_group" && isAdmin && resolvedTeamId != null) {
                        item {
                            GlassCard(onClick = { onNavigateToTeam(resolvedTeamId) }) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PersonAdd, null, tint = LiquidGlassTheme.PrimaryGreen)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) { Text("Add team members", fontWeight = FontWeight.Bold); Text("Invite and manage members", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = LiquidGlassTheme.PrimaryGreen); Spacer(Modifier.width(10.dp)); Text("About", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(10.dp)); Text(chat.description?.takeIf { it.isNotBlank() } ?: if (chat.chatType == "direct") "Hey there! I am using Sportynix." else "No description", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isAdmin) {
                        item {
                            GlassCard {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Admin-only messages", fontWeight = FontWeight.Bold)
                                        Text("Only admins can post in this chat", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(checked = chat.adminOnly == true, onCheckedChange = { viewModel.toggleAdminOnly() }, enabled = !uiState.isActionBusy)
                                }
                            }
                        }
                    }
                }

                if (confirmedBooking != null && confirmedBooking.status.lowercase() !in setOf("cancelled", "canceled", "completed", "no-show", "noshow")) {
                    item {
                        ConfirmedBookingCard(
                            booking = confirmedBooking,
                            canCancel = confirmedBooking.bookedBy == uiState.currentUserId && confirmedBooking.bookingIds.isNotEmpty(),
                            onOpen = { confirmedBooking.bookingIds.firstOrNull()?.let(onNavigateToBookingDetail) },
                            onCancel = { confirmedBooking.bookingIds.firstOrNull()?.let(onNavigateToBookingCancellation) }
                        )
                    }
                }

                chat?.rivalryInfo?.let { rivalry ->
                    item {
                        GlassCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Rivalry", color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold)
                                    Text(if (rivalry.isActive == false) "Inactive" else "Active rivalry", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GlassBadge("${rivalry.totalChallenges ?: 0} challenges")
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                ChallengeTeamSummary(rivalry.teamA.name, rivalry.teamA.logo)
                                Text("VS", fontWeight = FontWeight.Black, color = LiquidGlassTheme.PrimaryGreen)
                                ChallengeTeamSummary(rivalry.teamB.name, rivalry.teamB.logo)
                            }
                        }
                    }
                }

                chat?.challengeInfo?.let { challenge ->
                    item {
                        GlassCard {
                            Text("Challenge", color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                ChallengeTeamSummary(challenge.challenger?.name ?: "Challenger", challenge.challenger?.logo)
                                Text("VS", fontWeight = FontWeight.Black, color = LiquidGlassTheme.PrimaryGreen)
                                ChallengeTeamSummary(challenge.challenged?.name ?: "Challenged", challenge.challenged?.logo)
                            }
                            Spacer(Modifier.height(12.dp))
                            challenge.sport?.name?.let { DetailLine(Icons.Default.Sports, "Sport", it) }
                            challenge.venue?.name?.let { DetailLine(Icons.Default.LocationOn, "Venue", it) }
                            challenge.matchDate?.let { DetailLine(Icons.Default.CalendarMonth, "Date", it) }
                            challenge.matchTime?.let { DetailLine(Icons.Default.Schedule, "Time", it) }
                        }
                    }
                    if (challenge.venue != null && challenge.sport != null && confirmedBooking == null) {
                        item {
                            Button(onClick = { onBookMatch(challenge.venue.id, challenge.sport.id, chatId) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) {
                                Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Book Match", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                    val challengeMembers = challenge.challengerMembers + challenge.challengedMembers
                    if (challengeMembers.isNotEmpty()) {
                        item { PremiumTabs(listOf("Challenger (${challenge.challengerMembers.size})", "Challenged (${challenge.challengedMembers.size})"), teamTab) { teamTab = it } }
                        val visibleMembers = if (teamTab == 0) challenge.challengerMembers else challenge.challengedMembers
                        items(visibleMembers, key = { "challenge_${it.id}_${it.name}" }) { member ->
                            GlassCard(onClick = { viewModel.openDirectChat(member.id, onNavigateToChat) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen), contentAlignment = Alignment.Center) { Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(member.name, fontWeight = FontWeight.Bold); Text(member.role ?: "Member", fontSize = 12.sp, color = Color.Gray) }
                                    Icon(Icons.Default.Chat, contentDescription = "Message", tint = LiquidGlassTheme.PrimaryGreen)
                                }
                            }
                        }
                    }
                    challenge.pastGames?.let { history ->
                        val games = history.results
                        val upcoming = games.filter { it.booking?.status?.contains("confirm", true) == true }
                        val cancelled = games.filter { it.booking?.status?.let { s -> s.contains("cancel", true) || s.contains("declin", true) } == true }
                        val other = games - upcoming.toSet() - cancelled.toSet()
                        item { Text("Past Games", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        item { PremiumTabs(listOf("Upcoming (${upcoming.size})", "Cancelled (${cancelled.size})", "Other (${other.size})"), gameTab) { gameTab = it } }
                        val visibleGames = when(gameTab) { 0 -> upcoming; 1 -> cancelled; else -> other }
                        if (visibleGames.isEmpty()) item { EmptyGamesCard() }
                        items(visibleGames, key = { "game_${it.id}" }) { game ->
                            GlassCard(onClick = { game.booking?.id?.let(onNavigateToBookingDetail) }) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = LiquidGlassTheme.PrimaryGreen)
                                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(game.venue?.name ?: "Match", fontWeight = FontWeight.Bold); Text(listOfNotNull(game.matchDate, game.matchTime).joinToString(" • "), fontSize = 12.sp, color = Color.Gray) }
                                    Column(horizontalAlignment = Alignment.End) { Text(game.booking?.status ?: game.status, fontSize = 11.sp, color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold); if (game.booking != null) { Spacer(Modifier.height(2.dp)); IconButton(onClick = { viewModel.openQr(game) }) { Icon(Icons.Default.QrCode2, "Open booking QR", tint = LiquidGlassTheme.PrimaryGreen) } } }
                                }
                                game.booking?.let { booking ->
                                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = LiquidGlassTheme.cardBorder())
                                    booking.team?.name?.let { DetailLine(Icons.Default.Groups, "Team", it) }
                                    booking.madeBy?.name?.let { DetailLine(Icons.Default.Person, "Booked by", it) }
                                    DetailLine(Icons.Default.Schedule, "Time", listOfNotNull(booking.startTime, booking.endTime).joinToString(" - "))
                                    game.courtNumber?.let { DetailLine(Icons.Default.Stadium, "Court", it) }
                                    if (game.isPermanent) DetailLine(Icons.Default.Repeat, "Booking", "Permanent")
                                }
                            }
                        }
                        if (gameTab != 1 && games.size < uiState.historyTotal) item {
                            OutlinedButton(onClick = viewModel::loadMoreGames, enabled = !uiState.isLoadingMore, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha=.45f))) {
                                if (uiState.isLoadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.ExpandMore, null)
                                Spacer(Modifier.width(8.dp)); Text(if (uiState.isLoadingMore) "Loading games…" else "Load More")
                            }
                        }
                    }
                }

                // Media Preview Section
                if (uiState.photosPreview.isNotEmpty() || uiState.eventsPreview.isNotEmpty()) {
                    item {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Media & Events (${uiState.photosPreview.size + uiState.eventsPreview.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                TextButton(onClick = { onNavigateToGallery(chatId) }) {
                                    Text("See All", color = LiquidGlassTheme.PrimaryGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.photosPreview) { photo ->
                                    AsyncImage(
                                        model = photo.localMediaPath ?: photo.fileUrl,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { onNavigateToGallery(chatId) }
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                items(uiState.eventsPreview) { event ->
                                    Surface(onClick = { event.bookingId?.let(onNavigateToBookingDetail) }, modifier = Modifier.size(72.dp), shape = RoundedCornerShape(14.dp), color = LiquidGlassTheme.PrimaryGreen.copy(alpha=.12f), border = BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha=.25f))) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, "Booking event", tint = LiquidGlassTheme.PrimaryGreen) } }
                                }
                            }
                        }
                    }
                }

                // Members List
                if (uiState.members.isNotEmpty()) {
                    item {
                        Text("Members (${uiState.members.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LiquidGlassTheme.PrimaryGreen)
                    }
                    items(uiState.members, key = { it.id }) { member ->
                        MemberRow(
                            member = member,
                            canManage = isAdmin && member.id != uiState.currentUserId,
                            onMessage = { viewModel.openDirectChat(member.id, onNavigateToChat) },
                            onToggleAdmin = { viewModel.setMemberAdmin(member, !member.isAdmin) }
                        )
                    }
                }

                // Danger Actions Section
                if (chat?.chatType != "challenge" && chat?.chatType != "rivalry") item {
                    GlassCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { confirmation = "clear" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Messages for Me", color = Color.Gray)
                            }
                            if ((chat?.chatType == "team_group" && !isTeamOwner) || (chat?.chatType == "team_channel" && chat.isFollowing == true)) TextButton(onClick = { confirmation = "leave" }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Logout, null, tint = Color.Red); Spacer(Modifier.width(8.dp)); Text(if (chat.chatType == "team_channel") "Unfollow Channel" else "Leave Chat", color = Color.Red)
                            }
                            if (chat?.chatType == "direct" || isTeamOwner) TextButton(
                                onClick = { confirmation = if (isTeamOwner) "delete_all" else "hide" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isTeamOwner) "Delete Chat for Everyone" else "Delete Chat for Me", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (confirmation != null) {
        val action = confirmation!!
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(when (action) { "block" -> if (chat?.blockedByMe == true) "Unblock user?" else "Block user?"; "clear" -> "Clear chat?"; "leave" -> "Leave this chat?"; else -> "Delete chat?" }) },
            text = { Text(when (action) { "block" -> "This changes whether direct messages can be exchanged."; "clear" -> "Messages will be cleared only for your account."; "leave" -> "You will no longer receive messages from this chat."; "delete_all" -> "This permanently deletes the chat for everyone."; else -> "This removes the chat from your account." }) },
            confirmButton = { Button(onClick = {
                when (action) {
                    "block" -> chat?.otherUserId?.let { viewModel.toggleBlockUser(it, chat.blockedByMe == true) }
                    "clear" -> viewModel.clearChatForMe(onNavigateBack)
                    "delete_all" -> viewModel.deleteChatForEveryone(onNavigateBack)
                    "hide" -> viewModel.hideChatForMe(onNavigateBack)
                    "leave" -> viewModel.leaveChat(onNavigateBack)
                }; confirmation = null
            }, colors = ButtonDefaults.buttonColors(containerColor = if (action == "block" && chat?.blockedByMe == true) LiquidGlassTheme.PrimaryGreen else Color.Red)) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
        )
    }

    if (uiState.showReportModal && chat?.otherUserId != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showReportModal(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Report ${chat.otherUserName ?: "user"}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Choose the reason that best describes the problem.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("Spam", "Abusive language", "Inappropriate content", "Harassment", "Other").forEach { reason ->
                    Surface(
                        onClick = { reportReason = reason },
                        shape = RoundedCornerShape(16.dp),
                        color = if (reportReason == reason) LiquidGlassTheme.PrimaryGreen.copy(alpha = .13f) else LiquidGlassTheme.cardBackground(),
                        border = BorderStroke(1.dp, if (reportReason == reason) LiquidGlassTheme.PrimaryGreen else LiquidGlassTheme.cardBorder())
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = reportReason == reason, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = LiquidGlassTheme.PrimaryGreen))
                            Spacer(Modifier.width(8.dp)); Text(reason, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                OutlinedTextField(reportNotes, { reportNotes = it.take(500) }, label = { Text("Additional details (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, shape = RoundedCornerShape(18.dp))
                Button(enabled = reportReason.isNotBlank() && !uiState.isActionBusy, onClick = { viewModel.submitReport(chat.otherUserId, reportReason, reportNotes) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)), shape = RoundedCornerShape(18.dp)) {
                    if (uiState.isActionBusy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Default.Flag, null)
                    Spacer(Modifier.width(8.dp)); Text("Submit Report", Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showProfileImage) {
        Dialog(onDismissRequest = { showProfileImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (!avatar.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = avatar,
                        contentDescription = "$title profile image",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen) } },
                        error = { Icon(if (chat?.chatType == "direct") Icons.Default.Person else Icons.Default.Groups, null, Modifier.size(150.dp), tint = Color.White.copy(alpha=.55f)) }
                    )
                } else Icon(if (chat?.chatType == "direct") Icons.Default.Person else Icons.Default.Groups, null, Modifier.size(150.dp), tint = Color.White.copy(alpha=.55f))
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showProfileImage = false }, modifier = Modifier.background(Color.Black.copy(alpha=.5f), CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                    Spacer(Modifier.weight(1f)); Text(title, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(48.dp))
                }
            }
        }
    }

    if (uiState.qrGame != null) {
        ModalBottomSheet(onDismissRequest = viewModel::closeQr, containerColor = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Booking QR Code", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(uiState.qrGame?.venue?.name ?: "Match entry", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(260.dp).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(18.dp), contentAlignment = Alignment.Center) {
                    when {
                        uiState.isQrLoading -> CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
                        !uiState.qrCode.isNullOrBlank() -> AsyncImage(uiState.qrCode, "Booking QR code", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        else -> Text("QR code unavailable", color = Color.DarkGray)
                    }
                }
                uiState.qrGame?.booking?.id?.let { Text("Booking #$it", color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold) }
                Button(onClick = viewModel::closeQr, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) { Text("Close", Modifier.padding(vertical = 4.dp)) }
            }
        }
    }

    if (uiState.successMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSuccess,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = LiquidGlassTheme.PrimaryGreen) },
            title = { Text("Done") },
            text = { Text(uiState.successMessage ?: "Completed") },
            confirmButton = { TextButton(onClick = viewModel::dismissSuccess) { Text("OK") } }
        )
    }

    if (uiState.errorMessage != null && chat != null) {
        AlertDialog(onDismissRequest = viewModel::dismissError, icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Something went wrong") }, text = { Text(uiState.errorMessage ?: "Request failed") }, confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } })
    }
}

@Composable
private fun ChatInfoProfileAvatar(
    avatarUrl: String?,
    title: String,
    isTeam: Boolean,
    modifier: Modifier = Modifier
) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(LiquidGlassTheme.PrimaryGreen.copy(alpha = .11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isTeam) Icons.Default.Groups else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(.46f),
                tint = LiquidGlassTheme.PrimaryGreen
            )
        }
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(LiquidGlassTheme.cardBackground())
            .border(3.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = .85f), CircleShape)
            .padding(5.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = "$title profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { fallback() },
                error = { fallback() }
            )
        }
    }
}

private fun resolveChatInfoMediaUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("content://") || value.startsWith("file://")) return value
    return BuildConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
}

private data class ConfirmedBookingUi(
    val bookingIds: List<Long>,
    val venue: String,
    val sport: String,
    val displayDate: String,
    val timeSlots: String,
    val bookedBy: Long,
    val status: String
)

private fun toConfirmedBooking(message: ChatMessage): ConfirmedBookingUi? {
    val metadata = message.metadata.orEmpty()
    val details = metadata["booking_details"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
    fun text(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        val value = metadata[key] ?: details[key]
        when (value) {
            is String -> value.takeIf { it.isNotBlank() }
            is Map<*, *> -> (value["name"] as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
    fun number(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        is Map<*, *> -> number(value["id"] ?: value["booking_id"])
        else -> null
    }
    val ids = buildList {
        message.bookingId?.let(::add)
        number(metadata["booking_id"] ?: details["id"])?.let(::add)
        (metadata["booking_ids"] as? List<*>)?.mapNotNull(::number)?.let(::addAll)
    }.distinct()
    val looksLikeBooking = ids.isNotEmpty() || metadata.containsKey("booking_status") || metadata.containsKey("booking_details") || message.message.contains("booking", true)
    if (!looksLikeBooking || message.messageType !in setOf("event", "system") && message.bookingId == null) return null

    fun fromBody(label: String): String? = message.message.lineSequence()
        .firstOrNull { it.contains(label, ignoreCase = true) }
        ?.substringAfter(":", missingDelimiterValue = "")
        ?.trim()?.takeIf { it.isNotBlank() }

    return ConfirmedBookingUi(
        bookingIds = ids,
        venue = text("venue_name", "venue") ?: fromBody("venue") ?: "Venue",
        sport = text("sport_name", "sportName", "sport", "game_name") ?: fromBody("sport") ?: "Sport",
        displayDate = text("display_date", "booking_date", "date") ?: fromBody("date") ?: "Date to be confirmed",
        timeSlots = text("time_slots", "display_time", "time", "slot_time") ?: fromBody("time") ?: "Time to be confirmed",
        bookedBy = number(metadata["booked_by"] ?: details["booked_by"]) ?: message.sender,
        status = text("booking_status", "status") ?: "confirmed"
    )
}

@Composable
private fun ConfirmedBookingCard(booking: ConfirmedBookingUi, canCancel: Boolean, onOpen: () -> Unit, onCancel: () -> Unit) {
    GlassCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PushPin, null, Modifier.size(18.dp), tint = LiquidGlassTheme.PrimaryGreen)
            Spacer(Modifier.width(7.dp)); Icon(Icons.Default.CheckCircle, null, tint = LiquidGlassTheme.PrimaryGreen)
            Spacer(Modifier.width(7.dp)); Text("Pinned Booking", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            GlassBadge(booking.status.uppercase())
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            BookingDetailCell(Icons.Default.LocationOn, booking.venue, Modifier.weight(1f))
            BookingDetailCell(Icons.Default.Sports, booking.sport, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            BookingDetailCell(Icons.Default.CalendarMonth, booking.displayDate, Modifier.weight(1f))
            BookingDetailCell(Icons.Default.Schedule, booking.timeSlots, Modifier.weight(1f))
        }
        if (canCancel) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = LiquidGlassTheme.cardBorder())
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE5484D).copy(alpha = .55f))) {
                Icon(Icons.Default.Cancel, null, tint = Color(0xFFE5484D)); Spacer(Modifier.width(7.dp)); Text("Cancel Booking", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookingDetailCell(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = LiquidGlassTheme.PrimaryGreen)
        Spacer(Modifier.width(7.dp)); Text(value, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatChatInfoLastSeen(raw: String): String = runCatching {
    val instant = java.time.OffsetDateTime.parse(raw.replace("Z", "+00:00")).toInstant()
    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
    val date = zoned.toLocalDate()
    val today = java.time.LocalDate.now()
    val time = java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(zoned)
    when (date) {
        today -> "Last seen today at $time"
        today.minusDays(1) -> "Last seen yesterday at $time"
        else -> "Last seen ${java.time.format.DateTimeFormatter.ofPattern("MMM d").format(date)} at $time"
    }
}.getOrElse { "Offline" }

@Composable
private fun PremiumTabs(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = LiquidGlassTheme.cardBackground(), border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.cardBorder())) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                Surface(onClick = { onSelected(index) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp), color = if (selected == index) LiquidGlassTheme.PrimaryGreen else Color.Transparent) {
                    Text(label, Modifier.padding(horizontal = 4.dp, vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium, color = if (selected == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun EmptyGamesCard() { GlassCard { Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.EventBusy, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)); Text("No games found", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
fun MemberRow(member: ChatMember, canManage: Boolean, onMessage: () -> Unit, onToggleAdmin: () -> Unit) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!member.avatar.isNullOrBlank()) AsyncImage(member.avatar, member.fullName, Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else Box(Modifier.size(44.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen.copy(alpha=.18f)), contentAlignment = Alignment.Center) { Text(member.fullName.take(1).uppercase(), color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(member.role, fontSize = 12.sp, color = Color.Gray)
            }
            if (member.isAdmin) {
                GlassBadge(text = "Admin")
            }
            IconButton(onClick = onMessage) { Icon(Icons.Default.Chat, contentDescription = "Message", tint = LiquidGlassTheme.PrimaryGreen) }
            if (canManage) IconButton(onClick = onToggleAdmin) { Icon(Icons.Default.AdminPanelSettings, contentDescription = if (member.isAdmin) "Remove admin" else "Make admin", tint = LiquidGlassTheme.PrimaryGreen) }
        }
    }
}

@Composable private fun InfoAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) { TextButton(onClick = onClick) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, label, tint = LiquidGlassTheme.PrimaryGreen); Spacer(Modifier.height(4.dp)); Text(label, fontSize = 11.sp) } } }
@Composable private fun ChallengeTeamSummary(name: String, logo: String?) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(112.dp)) { if (!logo.isNullOrBlank()) AsyncImage(logo, null, Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Box(Modifier.size(50.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen), contentAlignment = Alignment.Center) { Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(5.dp)); Text(name, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) { Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(18.dp), tint = LiquidGlassTheme.PrimaryGreen); Spacer(Modifier.width(8.dp)); Text("$label: ", fontSize = 12.sp, color = Color.Gray); Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

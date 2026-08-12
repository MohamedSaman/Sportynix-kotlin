package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatMember
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
    onBookMatch: (Long, Long, Long) -> Unit = { _, _, _ -> },
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chat = uiState.chat
    val title = chat?.displayName ?: chat?.otherUserName ?: chat?.teamName ?: chat?.name ?: "Chat Info"
    val avatar = chat?.otherUserAvatar ?: chat?.teamLogo
    var confirmation by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }
    var showProfileImage by remember { mutableStateOf(false) }
    var teamTab by remember { mutableIntStateOf(0) }
    var gameTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LiquidGlassTheme.cardBackground()
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Swift-parity profile header
                item {
                    GlassCard(onClick = { showProfileImage = true }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!avatar.isNullOrEmpty()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(112.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(112.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen.copy(alpha = .16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(
                                if (chat?.chatType == "direct") {
                                    when { chat.blockedByMe == true -> "Blocked"; chat.isBlocked == false -> "Online"; else -> "Offline" }
                                } else "${chat?.membersCount ?: uiState.members.size} members",
                                fontSize = 13.sp,
                                color = if (chat?.chatType == "direct" && chat.isBlocked == false) LiquidGlassTheme.PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Button(onClick = { onNavigateBack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) {
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

                    item {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = LiquidGlassTheme.PrimaryGreen); Spacer(Modifier.width(10.dp)); Text("About", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(10.dp)); Text(chat.description?.takeIf { it.isNotBlank() } ?: if (chat.chatType == "direct") "Hey there! I am using Sportynix." else "No description", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (chat.canManage == true || chat.isAdmin == true) {
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
                    if (challenge.venue != null && challenge.sport != null) {
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
                    challenge.pastGames?.results?.takeIf { it.isNotEmpty() }?.let { games ->
                        val upcoming = games.filter { it.booking?.status?.contains("confirm", true) == true }
                        val cancelled = games.filter { it.booking?.status?.let { s -> s.contains("cancel", true) || s.contains("declin", true) } == true }
                        val other = games - upcoming.toSet() - cancelled.toSet()
                        item { PremiumTabs(listOf("Upcoming (${upcoming.size})", "Cancelled (${cancelled.size})", "Other (${other.size})"), gameTab) { gameTab = it } }
                        val visibleGames = when(gameTab) { 0 -> upcoming; 1 -> cancelled; else -> other }
                        if (visibleGames.isEmpty()) item { EmptyGamesCard() }
                        items(visibleGames, key = { "game_${it.id}" }) { game ->
                            GlassCard(onClick = { game.booking?.id?.let(onNavigateToBookingDetail) }) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = LiquidGlassTheme.PrimaryGreen)
                                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(game.venue?.name ?: "Match", fontWeight = FontWeight.Bold); Text(listOfNotNull(game.matchDate, game.matchTime).joinToString(" • "), fontSize = 12.sp, color = Color.Gray) }
                                    Column(horizontalAlignment = Alignment.End) { Text(game.booking?.status ?: game.status, fontSize = 11.sp, color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold); if (game.booking != null) { Spacer(Modifier.height(5.dp)); Icon(Icons.Default.QrCode2, "Open booking QR", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                                }
                            }
                        }
                    }
                }

                // Media Preview Section
                if (uiState.photosPreview.isNotEmpty()) {
                    item {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Shared Media (${uiState.photosPreview.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
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
                            canManage = chat?.canManage == true || chat?.isAdmin == true,
                            onMessage = { viewModel.openDirectChat(member.id, onNavigateToChat) },
                            onToggleAdmin = { viewModel.setMemberAdmin(member, !member.isAdmin) }
                        )
                    }
                }

                // Danger Actions Section
                item {
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
                            TextButton(
                                onClick = { confirmation = if (chat?.canManage == true || chat?.isAdmin == true) "delete_all" else "hide" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (chat?.canManage == true || chat?.isAdmin == true) "Delete Chat for Everyone" else "Delete Chat for Me", color = Color.Red)
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
            title = { Text(when (action) { "block" -> if (chat?.blockedByMe == true) "Unblock user?" else "Block user?"; "clear" -> "Clear chat?"; else -> "Delete chat?" }) },
            text = { Text(when (action) { "block" -> "This changes whether direct messages can be exchanged."; "clear" -> "Messages will be cleared only for your account."; "delete_all" -> "This permanently deletes the chat for everyone."; else -> "This removes the chat from your account." }) },
            confirmButton = { Button(onClick = {
                when (action) {
                    "block" -> chat?.otherUserId?.let { viewModel.toggleBlockUser(it, chat.blockedByMe == true) }
                    "clear" -> viewModel.clearChatForMe(onNavigateBack)
                    "delete_all" -> viewModel.deleteChatForEveryone(onNavigateBack)
                    "hide" -> viewModel.hideChatForMe(onNavigateBack)
                }; confirmation = null
            }, colors = ButtonDefaults.buttonColors(containerColor = if (action == "block" && chat?.blockedByMe == true) LiquidGlassTheme.PrimaryGreen else Color.Red)) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
        )
    }

    if (uiState.showReportModal && chat?.otherUserId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showReportModal(false) },
            title = { Text("Report ${chat.otherUserName ?: "user"}") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select a reason", fontWeight = FontWeight.SemiBold)
                listOf("Spam", "Harassment", "Inappropriate content", "Impersonation", "Other").forEach { reason ->
                    FilterChip(selected = reportReason == reason, onClick = { reportReason = reason }, label = { Text(reason) })
                }
                OutlinedTextField(reportNotes, { reportNotes = it.take(500) }, label = { Text("Additional details (optional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
            } },
            confirmButton = { Button(enabled = reportReason.isNotBlank() && !uiState.isActionBusy, onClick = { viewModel.submitReport(chat.otherUserId, reportReason, reportNotes) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Submit Report") } },
            dismissButton = { TextButton(onClick = { viewModel.showReportModal(false) }) { Text("Cancel") } }
        )
    }

    if (showProfileImage) {
        Dialog(onDismissRequest = { showProfileImage = false }) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (!avatar.isNullOrBlank()) AsyncImage(avatar, "$title profile image", Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                else Icon(if (chat?.chatType == "direct") Icons.Default.Person else Icons.Default.Groups, null, Modifier.size(150.dp), tint = Color.White.copy(alpha=.55f))
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showProfileImage = false }, modifier = Modifier.background(Color.Black.copy(alpha=.5f), CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                    Spacer(Modifier.weight(1f)); Text(title, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(48.dp))
                }
            }
        }
    }

    if (uiState.errorMessage != null && chat != null) {
        AlertDialog(onDismissRequest = viewModel::dismissError, icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Something went wrong") }, text = { Text(uiState.errorMessage ?: "Request failed") }, confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } })
    }
}

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
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(member.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
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

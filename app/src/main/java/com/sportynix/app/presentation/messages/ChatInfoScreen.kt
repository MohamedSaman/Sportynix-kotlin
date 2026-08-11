package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatMember
import com.sportynix.app.presentation.messages.components.GlassBadge
import com.sportynix.app.presentation.messages.components.GlassCard
import com.sportynix.app.presentation.messages.components.LiquidGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    chatId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (Long) -> Unit,
    onNavigateToChat: (Long) -> Unit = {},
    onNavigateToBookingDetail: (Long) -> Unit = {},
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chat = uiState.chat
    val title = chat?.displayName ?: chat?.otherUserName ?: chat?.teamName ?: chat?.name ?: "Chat Info"
    val avatar = chat?.otherUserAvatar ?: chat?.teamLogo
    var confirmation by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }

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
        containerColor = LiquidGlassTheme.screenBackground()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Profile Card
                item {
                    GlassCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!avatar.isNullOrEmpty()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(80.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(chat?.chatType ?: "Direct Chat", fontSize = 13.sp, color = Color.Gray)
                        }
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
                    val challengeMembers = challenge.challengerMembers + challenge.challengedMembers
                    if (challengeMembers.isNotEmpty()) {
                        item { Text("Challenge Members (${challengeMembers.size})", fontWeight = FontWeight.Bold, color = LiquidGlassTheme.PrimaryGreen) }
                        items(challengeMembers, key = { "challenge_${it.id}_${it.name}" }) { member ->
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
                        item { Text("Past Games", fontWeight = FontWeight.Bold, color = LiquidGlassTheme.PrimaryGreen) }
                        items(games, key = { "game_${it.id}" }) { game ->
                            GlassCard(onClick = { game.booking?.id?.let(onNavigateToBookingDetail) }) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = LiquidGlassTheme.PrimaryGreen)
                                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(game.venue?.name ?: "Match", fontWeight = FontWeight.Bold); Text(listOfNotNull(game.matchDate, game.matchTime).joinToString(" • "), fontSize = 12.sp, color = Color.Gray) }
                                    Text(game.booking?.status ?: game.status, fontSize = 11.sp, color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold)
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
}

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

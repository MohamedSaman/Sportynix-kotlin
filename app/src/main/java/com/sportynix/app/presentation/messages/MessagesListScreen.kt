package com.sportynix.app.presentation.messages

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Chat
import com.sportynix.app.domain.model.ChatRequestItem
import com.sportynix.app.presentation.messages.components.GlassBadge
import com.sportynix.app.presentation.messages.components.GlassCard
import com.sportynix.app.presentation.messages.components.LiquidGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onNavigateToNewChat: () -> Unit,
    viewModel: MessagesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Messages", fontWeight = FontWeight.Black, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = LiquidGlassTheme.PrimaryGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LiquidGlassTheme.cardBackground()
                )
            )
        },
        containerColor = LiquidGlassTheme.screenBackground()
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Tab Bar: My Chats vs Discover
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiquidGlassTheme.cardBackground())
                    .border(1.dp, LiquidGlassTheme.cardBorder(), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                listOf("my_chats" to "My Chats", "discover" to "Discover").forEach { (tabId, label) ->
                    val selected = uiState.activeTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) LiquidGlassTheme.PrimaryGreen else Color.Transparent)
                            .clickable { viewModel.setActiveTab(tabId) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LiquidGlassTheme.PrimaryGreen) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LiquidGlassTheme.PrimaryGreen,
                    unfocusedBorderColor = LiquidGlassTheme.cardBorder(),
                    focusedContainerColor = LiquidGlassTheme.cardBackground(),
                    unfocusedContainerColor = LiquidGlassTheme.cardBackground()
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (uiState.activeTab == "my_chats") {
                // Filter Pills
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
                    val filters = listOf(
                        Triple("all", "All", Icons.Default.Forum), Triple("unread", "Unread", Icons.Default.Notifications),
                        Triple("groups", "Teams", Icons.Default.Groups), Triple("channels", "Channels", Icons.Default.Tag),
                        Triple("chat_requests", "Requests", Icons.Default.PersonAdd)
                    )
                    filters.forEach { (filterId, label, icon) ->
                        val selected = uiState.chatFilter == filterId
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { viewModel.setChatFilter(filterId) }.padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BadgedBox(badge = {
                                val count = when(filterId) { "unread" -> uiState.unreadCount; "chat_requests" -> uiState.chatRequestsReceived.count { it.status == "pending" }; else -> 0 }
                                if (count > 0) Badge(containerColor = LiquidGlassTheme.PrimaryGreen) { Text(if (count > 99) "99+" else count.toString()) }
                            }) { Icon(icon, label, tint = if (selected) LiquidGlassTheme.PrimaryGreen else Color.Gray, modifier = Modifier.size(23.dp)) }
                            Spacer(Modifier.height(3.dp)); Text(label, fontSize = 10.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium, color = if(selected) LiquidGlassTheme.PrimaryGreen else Color.Gray)
                        }
                    }
                }

                if (uiState.chatFilter == "chat_requests") {
                    // Requests Sub-tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TextButton(onClick = { viewModel.setChatRequestTab("received") }) {
                            Text(
                                "Received (${uiState.chatRequestsReceived.size})",
                                color = if (uiState.chatRequestTab == "received") LiquidGlassTheme.PrimaryGreen else Color.Gray,
                                fontWeight = if (uiState.chatRequestTab == "received") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        TextButton(onClick = { viewModel.setChatRequestTab("sent") }) {
                            Text(
                                "Sent (${uiState.chatRequestsSent.size})",
                                color = if (uiState.chatRequestTab == "sent") LiquidGlassTheme.PrimaryGreen else Color.Gray,
                                fontWeight = if (uiState.chatRequestTab == "sent") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Request Items List
                    val requests = if (uiState.chatRequestTab == "received") uiState.chatRequestsReceived else uiState.chatRequestsSent
                    if (requests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No ${uiState.chatRequestTab} requests", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(requests, key = { it.id }) { item ->
                                ChatRequestItemCard(
                                    item = item,
                                    isReceived = uiState.chatRequestTab == "received",
                                    onAccept = { viewModel.acceptChatRequest(item, onNavigateToChat) },
                                    onDecline = { viewModel.declineChatRequest(item) },
                                    onCancel = { viewModel.cancelChatRequest(item) }
                                )
                            }
                        }
                    }
                } else {
                    // Conversations List
                    val filteredList = uiState.conversations.filter { chat ->
                        val matchesSearch = uiState.searchQuery.isEmpty() || (chat.name ?: chat.teamName ?: chat.otherUserName ?: "").contains(uiState.searchQuery, ignoreCase = true)
                        val matchesFilter = when (uiState.chatFilter) {
                            "unread" -> chat.unreadCount > 0
                            "groups" -> chat.chatType == "team_group"
                            "channels" -> chat.chatType == "team_channel"
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (uiState.isLoading && filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No conversations found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredList, key = { it.id }) { chat ->
                                ConversationCard(
                                    chat = chat,
                                    onClick = { onNavigateToChat(chat.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Discover Tab (Channels vs Teams)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    listOf("teams" to "Teams", "channels" to "Channels").forEach { (id, title) ->
                        val selected = uiState.discoverSubTab == id
                        Column(Modifier.weight(1f).clickable { viewModel.setDiscoverSubTab(id) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, color = if(selected) LiquidGlassTheme.PrimaryGreen else Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(3.dp).background(if(selected) LiquidGlassTheme.PrimaryGreen else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.discoverSubTab == "teams") {
                        val teams = uiState.discoverTeams.filter { uiState.searchQuery.isBlank() || it.name.contains(uiState.searchQuery, true) || it.location.orEmpty().contains(uiState.searchQuery, true) }
                        items(teams, key = { "team_${it.id}" }) { team ->
                            DiscoverTeamCard(team, uiState.busyTeamId == team.id) { viewModel.toggleTeamJoin(team) }
                        }
                    } else {
                        val channels = uiState.discoverChannels.filter { uiState.searchQuery.isBlank() || (it.displayName ?: it.name.orEmpty()).contains(uiState.searchQuery, true) }
                        items(channels, key = { it.id }) { channel ->
                            DiscoverChannelCard(channel, { if (channel.isFollowing == true) viewModel.unfollowChannel(channel.id) else viewModel.followChannel(channel.id) }, { onNavigateToChat(channel.id) })
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNavigateToNewChat,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp),
            containerColor = LiquidGlassTheme.PrimaryGreen,
            contentColor = Color.White,
            shape = CircleShape
        ) { Icon(Icons.Default.Add, "New chat", Modifier.size(30.dp)) }
        uiState.errorMessage?.let { Snackbar(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 90.dp)) { Text(it) } }
        }
    }
}

@Composable
fun ConversationCard(
    chat: Chat,
    onClick: () -> Unit
) {
    val title = chat.displayName ?: chat.otherUserName ?: chat.teamName ?: chat.name ?: "Chat"
    val avatarUrl = chat.otherUserAvatar ?: chat.teamLogo
    val lastMsgText = when (val lm = chat.lastMessage) {
        null -> chat.lastMessageText ?: ""
        else -> when (lm.messageType) {
            "photo" -> "📷 Photo"
            "video" -> "🎥 Video"
            "voice" -> "🎙️ Voice message"
            "event" -> "📅 Event"
            else -> lm.message ?: ""
        }
    }

    GlassCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(LiquidGlassTheme.PrimaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(LiquidGlassTheme.PrimaryGreen)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = chat.lastMessageTime?.take(10) ?: "",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMsgText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DiscoverTeamCard(team: com.sportynix.app.domain.model.DiscoverTeam, busy: Boolean, onJoinToggle: () -> Unit) {
    val pending = team.joinStatus.equals("requested", true) || team.joinStatus.equals("pending", true)
    val member = team.joinStatus.equals("member", true) || team.joinStatus.equals("approved", true)
    GlassCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!team.logo.isNullOrBlank()) AsyncImage(team.logo, null, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                else Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(LiquidGlassTheme.PrimaryGreen.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text(team.name.take(1).uppercase(), color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp); if (!team.location.isNullOrBlank()) Text(team.location, fontSize = 12.sp, color = Color.Gray) }
                Button(onClick = onJoinToggle, enabled = !busy && !member, colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen), shape = RoundedCornerShape(18.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White) else Text(when { member -> "Joined"; pending -> "Cancel"; else -> "Join" })
                }
            }
            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) { Text("${team.membersCount}/${team.maxMembers} members", fontSize = 12.sp, color = Color.Gray); team.teamType?.let { Text(it, fontSize = 12.sp, color = Color.Gray) } }
        }
    }
}

@Composable
fun ChatRequestItemCard(
    item: ChatRequestItem,
    isReceived: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit
) {
    val targetUser = if (isReceived) item.fromUser else item.toUser
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(LiquidGlassTheme.PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = targetUser.fullName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(targetUser.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("@${targetUser.username}", fontSize = 12.sp, color = Color.Gray)
            }

            if (isReceived) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Accept", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onDecline,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Decline", fontSize = 12.sp, color = Color.Red)
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DiscoverChannelCard(
    channel: Chat,
    onFollowToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isFollowing = channel.isFollowing == true
    GlassCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LiquidGlassTheme.AccentGreen),
                contentAlignment = Alignment.Center
            ) {
                Text((channel.name ?: "C").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name ?: "Channel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${channel.followerCount ?: 0} followers", fontSize = 12.sp, color = Color.Gray)
            }

            Button(
                onClick = onFollowToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.Gray else LiquidGlassTheme.PrimaryGreen
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(if (isFollowing) "Following" else "Follow", fontSize = 12.sp)
            }
        }
    }
}

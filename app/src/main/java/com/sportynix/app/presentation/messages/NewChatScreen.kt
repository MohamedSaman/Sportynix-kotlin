package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatRequestItem
import com.sportynix.app.domain.model.MutualUser
import com.sportynix.app.domain.model.UserSearchResult
import com.sportynix.app.presentation.messages.components.GlassCard
import com.sportynix.app.presentation.messages.components.LiquidGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    viewModel: NewChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Chat", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = { Text("Search users by username...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LiquidGlassTheme.PrimaryGreen) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
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
                    .padding(16.dp)
            )

            if (uiState.isInitialLoading || uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val receivedPending = uiState.chatRequestsReceived.filter { it.status == "pending" }
                    val sentPending = uiState.chatRequestsSent.filter { it.status == "pending" }
                    val mutualIds = uiState.mutualUsers.map { it.id }.toSet()

                    if (receivedPending.isNotEmpty()) {
                        item { SectionLabel("Chat Requests") }
                        items(receivedPending, key = { "received_${it.id}" }) { request ->
                            ChatRequestRow(
                                request = request,
                                incoming = true,
                                busy = uiState.busyUserId == request.fromUser.id,
                                onPrimary = { viewModel.acceptChatRequest(request, onNavigateToChat) },
                                onSecondary = { viewModel.declineChatRequest(request) }
                            )
                        }
                    }

                    if (sentPending.isNotEmpty()) {
                        item { SectionLabel("Sent Requests") }
                        items(sentPending, key = { "sent_${it.id}" }) { request ->
                            ChatRequestRow(
                                request = request,
                                incoming = false,
                                busy = uiState.busyUserId == request.toUser.id,
                                onPrimary = { viewModel.cancelChatRequest(request) }
                            )
                        }
                    }

                    if (uiState.query.isNotEmpty() && uiState.searchResults.isNotEmpty()) {
                        item {
                            Text(
                                "Search Results",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LiquidGlassTheme.PrimaryGreen,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(uiState.searchResults.filterNot { it.id.toLongOrNull() in mutualIds }, key = { it.id }) { user ->
                            val uid = user.id.toLongOrNull() ?: 0L
                            val incoming = receivedPending.firstOrNull { it.fromUser.id == uid }
                            val outgoing = sentPending.firstOrNull { it.toUser.id == uid }
                            UserSearchRow(
                                user = user,
                                busy = uiState.busyUserId == uid,
                                actionLabel = when {
                                    incoming != null -> "Accept"
                                    outgoing != null -> "Pending"
                                    else -> "Request"
                                },
                                onOpenChat = { id -> viewModel.openDirectChat(id, onNavigateToChat) },
                                onRequestChat = { id ->
                                    if (incoming != null) viewModel.acceptChatRequest(incoming, onNavigateToChat)
                                    else if (outgoing == null) viewModel.sendChatRequest(id)
                                }
                            )
                        }
                    }

                    if (uiState.mutualUsers.isNotEmpty()) {
                        item {
                            Text(
                                "Mutual Group Members",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LiquidGlassTheme.PrimaryGreen,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(uiState.mutualUsers, key = { it.id }) { user ->
                            MutualUserRow(
                                user = user,
                                onClick = { viewModel.openDirectChat(user.id, onNavigateToChat) }
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(error) }
            }
        }
    }
}

@Composable
fun MutualUserRow(
    user: MutualUser,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!user.profilePicture.isNullOrEmpty()) {
                AsyncImage(
                    model = user.profilePicture,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("@${user.username}", fontSize = 12.sp, color = Color.Gray)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun UserSearchRow(
    user: UserSearchResult,
    busy: Boolean,
    actionLabel: String,
    onOpenChat: (Long) -> Unit,
    onRequestChat: (Long) -> Unit
) {
    val uid = user.id.toLongOrNull() ?: 0L
    val displayName = user.fullName ?: user.username ?: "User"

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(LiquidGlassTheme.AccentGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (user.username != null) {
                    Text("@${user.username}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Button(
                onClick = { onRequestChat(uid) },
                enabled = !busy && uid > 0 && actionLabel != "Pending",
                colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                else Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LiquidGlassTheme.PrimaryGreen, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun ChatRequestRow(
    request: ChatRequestItem,
    incoming: Boolean,
    busy: Boolean,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null
) {
    val person = if (incoming) request.fromUser else request.toUser
    GlassCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!person.profilePicture.isNullOrBlank()) {
                AsyncImage(person.profilePicture, null, Modifier.size(46.dp).clip(CircleShape))
            } else {
                Box(Modifier.size(46.dp).clip(CircleShape).background(LiquidGlassTheme.AccentGreen), contentAlignment = Alignment.Center) {
                    Text(person.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(person.fullName, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(if (incoming) "Wants to message you" else "Request pending", fontSize = 12.sp, color = Color.Gray)
            }
            if (onSecondary != null) {
                TextButton(onClick = onSecondary, enabled = !busy) { Text("Decline", color = Color.Red) }
            }
            Button(onClick = onPrimary, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)) {
                if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                else Text(if (incoming) "Accept" else "Cancel", fontSize = 12.sp)
            }
        }
    }
}

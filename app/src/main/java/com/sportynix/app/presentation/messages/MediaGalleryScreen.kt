package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.presentation.messages.components.GlassCard
import com.sportynix.app.presentation.messages.components.LiquidGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    chatId: Long,
    onNavigateBack: () -> Unit,
    onSeeInChat: (Long) -> Unit,
    viewModel: MediaGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Gallery", fontWeight = FontWeight.Bold) },
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
            // Tab Header (Images vs Events)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiquidGlassTheme.cardBackground())
                    .padding(4.dp)
            ) {
                listOf("images" to "Images (${uiState.photoMessages.size})", "events" to "Events (${uiState.eventMessages.size})").forEach { (tabId, label) ->
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

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LiquidGlassTheme.PrimaryGreen)
                }
            } else if (uiState.activeTab == "images") {
                if (uiState.photoMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No images shared yet", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.photoMessages) { index, photo ->
                            Box {
                                AsyncImage(
                                    model = photo.localMediaPath ?: photo.fileUrl,
                                    contentDescription = null,
                                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { viewModel.setSelectedImageIndex(index) },
                                    contentScale = ContentScale.Crop
                                )
                                if (uiState.downloadingMessageId == photo.id) {
                                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 2.dp) }
                                }
                            }
                        }
                    }
                }
            } else {
                if (uiState.eventMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No events shared yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.eventMessages, key = { it.id }) { eventMsg ->
                            EventGalleryCard(
                                message = eventMsg,
                                onSeeInChat = { onSeeInChat(eventMsg.id) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.selectedImageIndex != null && uiState.selectedImagePath != null) {
            Box(
                Modifier.fillMaxSize().zIndex(10f).background(Color.Black.copy(alpha = .96f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = uiState.selectedImagePath, contentDescription = "Full screen image", modifier = Modifier.fillMaxSize().padding(vertical = 72.dp), contentScale = ContentScale.Fit)
                IconButton(onClick = { viewModel.setSelectedImageIndex(null) }, modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).background(Color.Black.copy(alpha = .35f), androidx.compose.foundation.shape.CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                val current = uiState.selectedImageIndex ?: 0
                if (current > 0) IconButton(onClick = { viewModel.setSelectedImageIndex(current - 1) }, modifier = Modifier.align(Alignment.CenterStart).padding(10.dp)) { Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.White, modifier = Modifier.size(38.dp)) }
                if (current < uiState.photoMessages.lastIndex) IconButton(onClick = { viewModel.setSelectedImageIndex(current + 1) }, modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)) { Icon(Icons.Default.ChevronRight, "Next", tint = Color.White, modifier = Modifier.size(38.dp)) }
                Text("${current + 1} / ${uiState.photoMessages.size}", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp), fontWeight = FontWeight.Bold)
            }
        }

        uiState.errorMessage?.let { error -> Snackbar(Modifier.padding(16.dp)) { Text(error) } }
    }
}

@Composable
fun EventGalleryCard(
    message: ChatMessage,
    onSeeInChat: () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LiquidGlassTheme.PrimaryGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shared Event", fontWeight = FontWeight.Bold, color = LiquidGlassTheme.PrimaryGreen, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(message.message.ifEmpty { "Booking Event" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${message.senderName} • ${message.createdAt.take(10)}", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSeeInChat,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = LiquidGlassTheme.PrimaryGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text("See in Chat", color = LiquidGlassTheme.PrimaryGreen, fontSize = 12.sp)
            }
        }
    }
}

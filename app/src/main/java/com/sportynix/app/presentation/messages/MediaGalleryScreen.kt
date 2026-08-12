package com.sportynix.app.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.launch
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
import com.sportynix.app.presentation.messages.components.PremiumMessagesBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    chatId: Long,
    onNavigateBack: () -> Unit,
    onSeeInChat: (Long) -> Unit,
    onNavigateToBookingDetail: (Long) -> Unit = {},
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        PremiumMessagesBackground {
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
                                onSeeInChat = { onSeeInChat(eventMsg.id) },
                                onOpenBooking = { bookingIdFrom(eventMsg)?.let(onNavigateToBookingDetail) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.selectedImageIndex != null) {
            FullScreenChatGallery(uiState, { viewModel.setSelectedImageIndex(null) }, viewModel::setSelectedImageIndex, viewModel::ensureImage, viewModel::retrySelected)
        }

        uiState.errorMessage?.let { error -> Snackbar(Modifier.padding(16.dp)) { Text(error) } }
        }
    }
}

@Composable
fun EventGalleryCard(
    message: ChatMessage,
    onSeeInChat: () -> Unit,
    onOpenBooking: () -> Unit
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
                onClick = if (bookingIdFrom(message) != null) onOpenBooking else onSeeInChat,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = LiquidGlassTheme.PrimaryGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (bookingIdFrom(message) != null) "View Booking" else "See in Chat", color = LiquidGlassTheme.PrimaryGreen, fontSize = 12.sp)
            }
        }
    }
}

private fun bookingIdFrom(message: ChatMessage): Long? {
    message.bookingId?.let { return it }
    val keys = listOf("booking_id", "bookingId", "id")
    for (key in keys) {
        val value = message.metadata?.get(key) ?: continue
        when (value) { is Number -> return value.toLong(); is String -> value.toLongOrNull()?.let { return it }; is Map<*, *> -> (value["id"] as? Number)?.toLong()?.let { return it } }
    }
    return Regex("booking(?: ID)?[:# ]+(\\d+)", RegexOption.IGNORE_CASE).find(message.message)?.groupValues?.getOrNull(1)?.toLongOrNull()
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullScreenChatGallery(state: MediaGalleryUiState, onClose: () -> Unit, onSelect: (Int?) -> Unit, ensure: (Int) -> Unit, retry: () -> Unit) {
    val initial = state.selectedImageIndex ?: 0
    val pager = rememberPagerState(initialPage = initial, pageCount = { state.photoMessages.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { onSelect(pager.currentPage); ensure(pager.currentPage) }
    Box(Modifier.fillMaxSize().zIndex(10f).background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val msg = state.photoMessages[page]
            ZoomableGalleryImage(state.downloadedPaths[msg.id] ?: msg.localMediaPath, state.downloadingMessageId == msg.id, retry)
        }
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.background(Color.White.copy(alpha=.12f), androidx.compose.foundation.shape.CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            Spacer(Modifier.weight(1f)); Text("${pager.currentPage + 1} / ${state.photoMessages.size}", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(48.dp))
        }
        LazyRow(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().background(Color.Black.copy(alpha=.55f)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.photoMessages.size) { index -> val msg = state.photoMessages[index]; AsyncImage(state.downloadedPaths[msg.id] ?: msg.localMediaPath ?: msg.fileUrl, null, Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).clickable { scope.launch { pager.animateScrollToPage(index) } }.then(if(index == pager.currentPage) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier), contentScale = ContentScale.Crop) }
        }
    }
}

@Composable private fun ZoomableGalleryImage(path: String?, loading: Boolean, retry: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }; var x by remember { mutableFloatStateOf(0f) }; var y by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (path != null) AsyncImage(path, "Chat image", Modifier.fillMaxSize().padding(vertical = 86.dp).graphicsLayer(scaleX=scale, scaleY=scale, translationX=x, translationY=y).pointerInput(path) { detectTransformGestures { _, pan, zoom, _ -> scale=(scale*zoom).coerceIn(1f,5f); if(scale>1f){ x+=pan.x; y+=pan.y } else { x=0f;y=0f } } }.pointerInput(path) { detectTapGestures(onDoubleTap = { if(scale>1f){scale=1f;x=0f;y=0f}else scale=2.5f }) }, contentScale=ContentScale.Fit)
        else if (loading) Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color=Color.White); Spacer(Modifier.height(10.dp)); Text("Downloading…", color=Color.White.copy(alpha=.8f)) }
        else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.BrokenImage, null, Modifier.size(42.dp), tint=Color.White.copy(alpha=.6f)); Text("Couldn't load image", color=Color.White); TextButton(onClick=retry){Text("Retry", color=LiquidGlassTheme.PrimaryGreen)} }
    }
}

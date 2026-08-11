package com.sportynix.app.presentation.messages

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.presentation.messages.components.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: Long,
    scrollToMessageId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToInfo: (Long) -> Unit,
    onNavigateToGallery: (Long) -> Unit,
    onNavigateToBookingDetail: (Long) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingImagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCaption by remember { mutableStateOf("") }
    var showCaptionDialog by remember { mutableStateOf(false) }

    fun stageImages(paths: List<String>) {
        if (paths.isNotEmpty()) {
            pendingImagePaths = paths.take(5)
            pendingCaption = ""
            showCaptionDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        stageImages(uris.take(5).mapNotNull { copyUriToChatCache(context, it) })
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { stageImages(listOf(saveBitmapToChatCache(context, it))) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startVoiceRecording()
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(scrollToMessageId, uiState.messages) {
        val target = scrollToMessageId ?: return@LaunchedEffect
        val index = uiState.messages.indexOfFirst { it.id == target }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Scaffold(
        topBar = {
            val title = uiState.chatDetails?.displayName ?: uiState.chatDetails?.otherUserName ?: uiState.chatDetails?.name ?: "Chat"
            val avatar = uiState.chatDetails?.otherUserAvatar ?: uiState.chatDetails?.teamLogo

            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!avatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            Text(
                                when {
                                    uiState.typingUserName != null -> "${uiState.typingUserName} is typing…"
                                    uiState.isOtherUserOnline == true -> "Online"
                                    !uiState.otherUserLastSeen.isNullOrBlank() -> "Last seen ${uiState.otherUserLastSeen}"
                                    uiState.isRealtimeConnected -> uiState.chatDetails?.chatType ?: "Connected"
                                    else -> "Offline · messages will be queued"
                                },
                                fontSize = 11.sp,
                                color = if (uiState.typingUserName != null || uiState.isOtherUserOnline == true) LiquidGlassTheme.PrimaryGreen else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToGallery(chatId) }) {
                        Icon(Icons.Default.PermMedia, contentDescription = "Gallery", tint = LiquidGlassTheme.PrimaryGreen)
                    }
                    IconButton(onClick = { onNavigateToInfo(chatId) }) {
                        Icon(Icons.Default.Info, contentDescription = "Chat Info", tint = LiquidGlassTheme.PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LiquidGlassTheme.cardBackground()
                )
            )
        },
        bottomBar = {
            if (uiState.isBlocked) {
                Surface(
                    color = Color.Red.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.blockHintMessage ?: "Messaging is blocked",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Reply Banner Preview
                    if (uiState.replyingToMessage != null) {
                        val reply = uiState.replyingToMessage!!
                        Surface(
                            color = LiquidGlassTheme.cardBackground(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(4.dp).height(30.dp).background(LiquidGlassTheme.PrimaryGreen))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Replying to ${reply.senderName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LiquidGlassTheme.PrimaryGreen)
                                    Text(reply.message, fontSize = 12.sp, maxLines = 1, color = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Composer or Voice Recording Bar
                    if (uiState.isRecordingVoice) {
                        Surface(
                            color = LiquidGlassTheme.cardBackground(),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PulsingRecordingDot()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recording... ${uiState.voiceDurationSeconds}s", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.cancelVoiceRecording() }) {
                                    Text("Cancel", color = Color.Red)
                                }
                                Button(
                                    onClick = { viewModel.stopAndSendVoiceRecording() },
                                    colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)
                                ) {
                                    Text("Send")
                                }
                            }
                        }
                    } else {
                        // Floating Translucent Glass Composer
                        Surface(
                            color = LiquidGlassTheme.cardBackground(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.cardBorder()),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showAttachmentSheet = true }) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = LiquidGlassTheme.PrimaryGreen)
                                }

                                TextField(
                                    value = uiState.textInput,
                                    onValueChange = { viewModel.onTextChanged(it) },
                                    placeholder = { Text("Type a message...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                if (uiState.textInput.trim().isEmpty()) {
                                    IconButton(onClick = { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                                        Icon(Icons.Default.Mic, contentDescription = "Voice Record", tint = LiquidGlassTheme.PrimaryGreen)
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel.sendTextMessage() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(LiquidGlassTheme.PrimaryGreen)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = LiquidGlassTheme.screenBackground()
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pinned = uiState.messages.firstOrNull { it.isPinned && !it.isDeleted }
            if (pinned != null) {
                item(key = "pinned_${pinned.id}") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        color = LiquidGlassTheme.cardBackground(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = .32f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = LiquidGlassTheme.PrimaryGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Pinned message", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LiquidGlassTheme.PrimaryGreen)
                                Text(pinned.message.ifBlank { pinned.messageType.replaceFirstChar(Char::uppercase) }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                            IconButton(onClick = { viewModel.unpinMessage(pinned.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Unpin", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    currentUserId = uiState.currentUserId,
                    onLongClick = { viewModel.setSelectedMessageForMenu(message) },
                    onSwipeToReply = { viewModel.setReplyingTo(message) },
                    onPhotoClick = { onNavigateToGallery(chatId) },
                    onEventClick = { message.bookingId?.let(onNavigateToBookingDetail) }
                )
            }
            if (uiState.typingUserName != null) {
                item(key = "typing") {
                    Surface(color = LiquidGlassTheme.cardBackground(), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(76.dp)) {
                        Text("•••", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = LiquidGlassTheme.PrimaryGreen, fontSize = 18.sp)
                    }
                }
            }
        }

        // Message Actions Dialog/Modal
        if (uiState.selectedMessageForMenu != null) {
            val selected = uiState.selectedMessageForMenu!!
            AlertDialog(
                onDismissRequest = { viewModel.setSelectedMessageForMenu(null) },
                title = { Text("Message Options") },
                text = {
                    Column {
                        TextButton(onClick = {
                            viewModel.setReplyingTo(selected)
                            viewModel.setSelectedMessageForMenu(null)
                        }) {
                            Icon(Icons.Default.Reply, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reply")
                        }
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(selected.message))
                            viewModel.setSelectedMessageForMenu(null)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy")
                        }
                        TextButton(onClick = {
                            if (selected.isPinned) viewModel.unpinMessage(selected.id) else viewModel.pinMessage(selected.id)
                            viewModel.setSelectedMessageForMenu(null)
                        }) {
                            Icon(if (selected.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selected.isPinned) "Unpin Message" else "Pin Message")
                        }
                        TextButton(onClick = {
                            viewModel.deleteMessageForMe(selected.id)
                            viewModel.setSelectedMessageForMenu(null)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete for Me")
                        }
                        TextButton(onClick = {
                            viewModel.deleteMessage(selected.id)
                            viewModel.setSelectedMessageForMenu(null)
                        }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete for Everyone", color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setSelectedMessageForMenu(null) }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                containerColor = LiquidGlassTheme.cardBackground(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Text("Share media", modifier = Modifier.padding(horizontal = 24.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AttachmentChoice(Icons.Default.PhotoLibrary, "Photos", Modifier.weight(1f)) {
                        showAttachmentSheet = false
                        galleryLauncher.launch("image/*")
                    }
                    AttachmentChoice(Icons.Default.PhotoCamera, "Camera", Modifier.weight(1f)) {
                        showAttachmentSheet = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (showCaptionDialog) {
            AlertDialog(
                onDismissRequest = { showCaptionDialog = false },
                title = { Text(if (pendingImagePaths.size == 1) "Send photo" else "Send ${pendingImagePaths.size} photos") },
                text = {
                    Column {
                        AsyncImage(
                            model = pendingImagePaths.firstOrNull(),
                            contentDescription = "Selected photo preview",
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pendingCaption,
                            onValueChange = { pendingCaption = it },
                            label = { Text("Add a caption") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !uiState.isSendingMedia,
                        onClick = {
                            viewModel.sendImagePaths(pendingImagePaths, pendingCaption)
                            showCaptionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiquidGlassTheme.PrimaryGreen)
                    ) { Text("Send") }
                },
                dismissButton = { TextButton(onClick = { showCaptionDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    currentUserId: Long?,
    onLongClick: () -> Unit,
    onSwipeToReply: () -> Unit,
    onPhotoClick: () -> Unit,
    onEventClick: () -> Unit
) {
    val isMe = message.sender == currentUserId || message.senderName == "You"
    val align = if (isMe) Alignment.End else Alignment.Start
    val bg = if (isMe) LiquidGlassTheme.PrimaryGreen else LiquidGlassTheme.cardBackground()
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); dragTotal += amount },
                    onDragEnd = { if (dragTotal > 72.dp.toPx()) onSwipeToReply() }
                )
            }
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        horizontalAlignment = align
    ) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            border = if (!isMe) androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.cardBorder()) else null,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMe && message.senderName.isNotEmpty()) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = LiquidGlassTheme.PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                val reply = message.metadata?.get("reply_to") as? Map<*, *>
                if (reply != null && !message.isDeleted) {
                    Surface(
                        color = if (isMe) Color.Black.copy(alpha = .12f) else LiquidGlassTheme.PrimaryGreen.copy(alpha = .08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(reply["sender_name"]?.toString() ?: "Message", color = if (isMe) Color.White else LiquidGlassTheme.PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(reply["preview"]?.toString() ?: reply["message"]?.toString().orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = textColor.copy(alpha = .78f))
                        }
                    }
                }

                when (message.messageType) {
                    "photo" -> {
                        if (!message.fileUrl.isNullOrEmpty() || !message.localMediaPath.isNullOrEmpty()) {
                            AsyncImage(
                                model = message.localMediaPath ?: message.fileUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(onClick = onPhotoClick, onLongClick = onLongClick),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    "voice" -> {
                        val path = message.localMediaPath ?: message.fileUrl ?: ""
                        VoiceMessagePlayer(audioUrlOrPath = path, durationSeconds = message.duration ?: 0)
                    }
                    "event" -> {
                        BookingMessageCard(message = message, onClick = onEventClick)
                    }
                    else -> {}
                }

                if (message.message.isNotEmpty()) {
                    Text(
                        text = message.message,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp.takeLast(8).take(5),
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val tickIcon = if (message.isRead) Icons.Default.DoneAll else if (message.delivered) Icons.Default.DoneAll else Icons.Default.Done
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = null,
                            tint = if (message.isRead) Color.Cyan else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingMessageCard(message: ChatMessage, onClick: () -> Unit) {
    val metadata = message.metadata.orEmpty()
    val title = metadata["title"]?.toString() ?: "Booking Update"
    val status = metadata["status"]?.toString() ?: metadata["booking_status"]?.toString().orEmpty()
    val cancelled = status.equals("cancelled", true) || title.contains("cancel", true)
    Surface(
        onClick = onClick,
        color = if (cancelled) Color(0xFFEF4444).copy(alpha = .10f) else LiquidGlassTheme.PrimaryGreen.copy(alpha = .10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (cancelled) Color(0xFFEF4444).copy(alpha = .35f) else LiquidGlassTheme.PrimaryGreen.copy(alpha = .35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = if (cancelled) Color(0xFFEF4444) else LiquidGlassTheme.PrimaryGreen)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            listOf(
                Icons.Default.LocationOn to (metadata["venue"] ?: metadata["venue_name"]),
                Icons.Default.CalendarMonth to metadata["date"],
                Icons.Default.Schedule to (metadata["display_time"] ?: metadata["time"])
            ).forEach { (icon, value) ->
                if (value != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(7.dp))
                    Text(value.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (message.bookingId != null) Text("View booking", color = LiquidGlassTheme.PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AttachmentChoice(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        color = LiquidGlassTheme.PrimaryGreen.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = LiquidGlassTheme.PrimaryGreen)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun copyUriToChatCache(context: android.content.Context, uri: Uri): String? = runCatching {
    val file = File(context.cacheDir, "chat_photo_${System.currentTimeMillis()}_${uri.hashCode()}.jpg")
    context.contentResolver.openInputStream(uri)!!.use { input -> FileOutputStream(file).use(input::copyTo) }
    file.absolutePath
}.getOrNull()

private fun saveBitmapToChatCache(context: android.content.Context, bitmap: Bitmap): String {
    val file = File(context.cacheDir, "chat_camera_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    return file.absolutePath
}

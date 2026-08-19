package com.sportynix.app.presentation.messages

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.R
import com.sportynix.app.BuildConfig
import com.sportynix.app.presentation.messages.components.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingImagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedPreviewPath by remember { mutableStateOf<String?>(null) }
    var pendingCaption by remember { mutableStateOf("") }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var mediaSendObserved by remember { mutableStateOf(false) }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var highlightedMessageId by remember { mutableStateOf<Long?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < .5f
    var previousMessageCount by remember { mutableIntStateOf(0) }

    fun stageImages(paths: List<String>) {
        if (paths.isNotEmpty()) {
            pendingImagePaths = paths.take(5)
            selectedPreviewPath = paths.first()
            pendingCaption = ""
            mediaSendObserved = false
            showCaptionDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        stageImages(uris.take(5).mapNotNull { copyUriToChatCache(context, it) })
    }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) copyUriToChatCache(context, uri)?.let { stageImages(listOf(it)) }
        pendingCameraUri = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val captureFile = File(context.cacheDir, "chat_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", captureFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startVoiceRecording()
    }

    LaunchedEffect(uiState.isSendingMedia, mediaSendObserved) {
        if (uiState.isSendingMedia) mediaSendObserved = true
        if (mediaSendObserved && !uiState.isSendingMedia) {
            showCaptionDialog = false
            pendingImagePaths = emptyList()
            selectedPreviewPath = null
            pendingCaption = ""
            mediaSendObserved = false
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val initialLoad = previousMessageCount == 0
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val wasNearBottom = lastVisible >= (previousMessageCount - 3).coerceAtLeast(0)
            if (initialLoad || wasNearBottom) listState.animateScrollToItem(uiState.messages.size - 1)
        }
        previousMessageCount = uiState.messages.size
    }

    LaunchedEffect(scrollToMessageId, uiState.messages) {
        val target = scrollToMessageId ?: return@LaunchedEffect
        val index = uiState.messages.indexOfFirst { it.id == target }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    suspend fun jumpToMessage(messageId: Long) {
        val index = uiState.messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            highlightedMessageId = messageId
            kotlinx.coroutines.delay(2400)
            if (highlightedMessageId == messageId) highlightedMessageId = null
        }
    }

    Scaffold(
        topBar = {
            val title = uiState.chatDetails?.displayName ?: uiState.chatDetails?.otherUserName ?: uiState.chatDetails?.name ?: "Chat"
            val chatType = uiState.chatDetails?.chatType.orEmpty()
            val avatar = resolveChatMediaUrl(
                if (chatType == "direct") uiState.chatDetails?.otherUserAvatar
                else uiState.chatDetails?.teamLogo ?: uiState.chatDetails?.otherUserAvatar
            )

            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onNavigateToInfo(chatId) }) {
                        ChatHeaderAvatar(avatarUrl = avatar, title = title, isTeam = chatType != "direct")
                        Spacer(modifier = Modifier.width(11.dp))
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    uiState.typingUserName != null -> "${uiState.typingUserName} is typing..."
                                    chatType != "direct" && uiState.chatDetails?.membersCount != null -> "${uiState.chatDetails?.membersCount} members"
                                    chatType == "team_channel" -> "Team channel"
                                    chatType != "direct" -> "Team chat"
                                    uiState.isOtherUserOnline == true -> "online"
                                    !uiState.otherUserLastSeen.isNullOrBlank() -> formatPresenceLastSeen(uiState.otherUserLastSeen!!)
                                    uiState.isRealtimeConnected -> "online"
                                    else -> "tap for info"
                                },
                                fontSize = 12.sp,
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
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Default.MoreVert, "More", tint = LiquidGlassTheme.PrimaryGreen) }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(text = { Text("Chat info") }, leadingIcon = { Icon(Icons.Default.Info, null) }, onClick = { showMoreMenu = false; onNavigateToInfo(chatId) })
                            DropdownMenuItem(text = { Text("Media gallery") }, leadingIcon = { Icon(Icons.Default.PermMedia, null) }, onClick = { showMoreMenu = false; onNavigateToGallery(chatId) })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xEE0B100E) else Color(0xF5FFFFFF)
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
            } else if (uiState.chatDetails?.canPost == false) {
                Surface(color = LiquidGlassTheme.cardBackground(), modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.cardBorder())) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.Lock, null, tint = LiquidGlassTheme.PrimaryGreen); Spacer(Modifier.width(8.dp)); Text("Only admins can send messages", fontWeight = FontWeight.SemiBold) }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                    // Reply Banner Preview
                    if (uiState.replyingToMessage != null) {
                        val reply = uiState.replyingToMessage!!
                        Surface(
                            color = LiquidGlassTheme.cardBackground(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PulsingRecordingDot()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recording…", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("%d:%02d".format(uiState.voiceDurationSeconds / 60, uiState.voiceDurationSeconds % 60), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                IconButton(onClick = { viewModel.cancelVoiceRecording() }) { Icon(Icons.Default.Delete, "Cancel recording", tint = Color.Red) }
                                IconButton(
                                    onClick = { viewModel.stopAndSendVoiceRecording() },
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.Send, "Send voice", tint = Color.White)
                                }
                            }
                        }
                    } else {
                        // Floating translucent Sportynix composer dock.
                        Column(Modifier.fillMaxWidth().background(Color.Transparent)) {
                            AnimatedVisibility(showEmojiPicker, enter = expandVertically(), exit = shrinkVertically()) {
                                EmojiPanel(onEmoji = { viewModel.onTextChanged(uiState.textInput + it) })
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp)
                                    .shadow(if (isDark) 3.dp else 9.dp, RoundedCornerShape(30.dp)),
                                color = if (isDark) Color(0xE61A211E) else Color(0xEEFFFFFF),
                                shape = RoundedCornerShape(30.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    LiquidGlassTheme.PrimaryGreen.copy(alpha = if (isDark) .28f else .20f)
                                )
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(4.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Surface(
                                        onClick = { showAttachmentSheet = true },
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        color = LiquidGlassTheme.PrimaryGreen.copy(alpha = if (isDark) .14f else .10f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = .22f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, "Attach", tint = LiquidGlassTheme.PrimaryGreen, modifier = Modifier.size(27.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(5.dp))
                                    Surface(
                                        color = if (isDark) Color.White.copy(alpha = .055f) else Color(0xFFF7FAF8),
                                        modifier = Modifier.weight(1f).heightIn(min = 46.dp, max = 116.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            LiquidGlassTheme.PrimaryGreen.copy(alpha = if (uiState.textInput.isNotEmpty()) .36f else .14f)
                                        )
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            IconButton(onClick = { showEmojiPicker = !showEmojiPicker }, modifier = Modifier.size(44.dp)) {
                                                Icon(
                                                    if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                                    "Emoji",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .90f)
                                                )
                                            }
                                            TextField(
                                                value = uiState.textInput,
                                                onValueChange = viewModel::onTextChanged,
                                                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .66f)) },
                                                colors = TextFieldDefaults.colors(
                                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    cursorColor = LiquidGlassTheme.PrimaryGreen,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                modifier = Modifier.weight(1f),
                                                minLines = 1,
                                                maxLines = 5
                                            )
                                            IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.size(44.dp)) {
                                                Icon(Icons.Default.PhotoCamera, "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .88f))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(5.dp))
                                    IconButton(
                                        onClick = { if (uiState.textInput.isBlank()) microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) else viewModel.sendTextMessage() },
                                        modifier = Modifier.size(46.dp).shadow(7.dp, CircleShape).clip(CircleShape).background(LiquidGlassTheme.PrimaryGreen)
                                    ) {
                                        AnimatedContent(
                                            targetState = uiState.textInput.isBlank(),
                                            transitionSpec = {
                                                (fadeIn(tween(140)) + scaleIn(initialScale = .82f)) togetherWith
                                                    (fadeOut(tween(110)) + scaleOut(targetScale = .82f))
                                            },
                                            label = "composer_action"
                                        ) { isBlank ->
                                            Icon(
                                                if (isBlank) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                                                if (isBlank) "Record voice" else "Send",
                                                tint = Color.White,
                                                modifier = Modifier.size(23.dp)
                                            )
                                        }
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
                .paint(painterResource(if (isDark) R.drawable.chat_bg_dark else R.drawable.chat_bg_light), contentScale = ContentScale.Crop)
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
            itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                val photoRunOffset = if (message.messageType == "photo") {
                    var offset = 0
                    var cursor = index - 1
                    while (cursor >= 0 && uiState.messages[cursor].messageType == "photo" && uiState.messages[cursor].sender == message.sender) { offset++; cursor-- }
                    offset
                } else 0
                if (message.messageType == "photo" && photoRunOffset % 5 != 0) return@itemsIndexed
                val groupedMessage = if (message.messageType == "photo") {
                    val group = uiState.messages.drop(index).take(5).takeWhile { it.messageType == "photo" && it.sender == message.sender }
                    if (group.size > 1) {
                        val urls = group.flatMap(::messagePhotoUrls).distinct()
                        val last = group.last()
                        message.copy(
                            message = group.firstOrNull { it.message.isNotBlank() }?.message.orEmpty(),
                            timestamp = last.timestamp,
                            createdAt = last.createdAt,
                            delivered = group.all { it.delivered },
                            isRead = group.all { it.isRead },
                            queued = group.any { it.queued },
                            metadata = message.metadata.orEmpty() + ("images" to urls)
                        )
                    } else message
                } else message
                MessageBubble(
                    message = groupedMessage,
                    currentUserId = uiState.currentUserId,
                    showSenderName = uiState.chatDetails?.chatType != "direct",
                    onLongClick = {
                        if (!message.isDeleted && message.messageType.lowercase() !in setOf("system", "deleted")) {
                            viewModel.setSelectedMessageForMenu(message)
                        }
                    },
                    onSwipeToReply = { viewModel.setReplyingTo(message) },
                    onPhotoClick = { tappedUrl ->
                        val timelinePhotos = uiState.messages
                            .asSequence()
                            .filter { !it.isDeleted && it.messageType.lowercase() in setOf("photo", "image") }
                            .flatMap { messagePhotoUrls(it).asSequence() }
                            .distinct()
                            .toList()
                        viewerImages = (timelinePhotos + tappedUrl).distinct()
                        viewerStartIndex = viewerImages.indexOf(tappedUrl).coerceAtLeast(0)
                    },
                    onEventClick = { message.bookingId?.let(onNavigateToBookingDetail) },
                    highlighted = highlightedMessageId == message.id,
                    onReplyClick = { id -> scope.launch { jumpToMessage(id) } },
                    voiceAvatarUrl = resolveChatMediaUrl(message.senderAvatar ?: if (message.sender != uiState.currentUserId) uiState.chatDetails?.otherUserAvatar else null),
                    voiceAvatarFallback = message.senderName.takeIf { it.isNotBlank() && it != "Unknown" }
                        ?: uiState.chatDetails?.displayName
                        ?: uiState.chatDetails?.otherUserName
                        ?: uiState.chatDetails?.name
                        ?: "S",
                    onResolveVoiceSource = { viewModel.resolveVoiceSource(message) }
                )
            }
            if (uiState.typingUserName != null) {
                item(key = "typing_animated") { TypingBubble() }
            }
            if (false && uiState.typingUserName != null) {
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
            val isOwnMessage = selected.sender == uiState.currentUserId || selected.senderName == "You"
            val normalizedType = selected.messageType.lowercase()
            val isActionable = !selected.isDeleted && normalizedType !in setOf("system", "deleted")
            AlertDialog(
                onDismissRequest = { viewModel.setSelectedMessageForMenu(null) },
                title = { Text("Message Options") },
                text = {
                    Column {
                        if (isActionable) {
                            TextButton(onClick = {
                                viewModel.setReplyingTo(selected)
                                viewModel.setSelectedMessageForMenu(null)
                            }) {
                                Icon(Icons.Default.Reply, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reply")
                            }
                            if (normalizedType == "text" && selected.message.isNotBlank()) {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(selected.message))
                                    viewModel.setSelectedMessageForMenu(null)
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy")
                                }
                            }
                            if (selected.id > 0) {
                                TextButton(onClick = {
                                    if (selected.isPinned) viewModel.unpinMessage(selected.id) else viewModel.pinMessage(selected.id)
                                    viewModel.setSelectedMessageForMenu(null)
                                }) {
                                    Icon(if (selected.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (selected.isPinned) "Unpin Message" else "Pin Message")
                                }
                            }
                            TextButton(onClick = {
                                viewModel.deleteMessageForMe(selected.id)
                                viewModel.setSelectedMessageForMenu(null)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete for me")
                            }
                            if (isOwnMessage && selected.id > 0) {
                                TextButton(onClick = {
                                    viewModel.deleteMessage(selected.id)
                                    viewModel.setSelectedMessageForMenu(null)
                                }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete for everyone", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            Text("No actions are available for this message.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Choose up to 5 photos", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
            Dialog(
                onDismissRequest = { if (!uiState.isSendingMedia) showCaptionDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !uiState.isSendingMedia, dismissOnClickOutside = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF080D0B), shape = RoundedCornerShape(0.dp)) {
                    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(58.dp)) {
                            IconButton(enabled = !uiState.isSendingMedia, onClick = { showCaptionDialog = false }) { Icon(Icons.Default.Close, "Cancel", tint = Color.White) }
                            Text("Send photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.weight(1f))
                            Surface(color = LiquidGlassTheme.PrimaryGreen.copy(alpha = .18f), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = .35f))) {
                                Text("${pendingImagePaths.size}/5", modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = LiquidGlassTheme.PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        AsyncImage(
                            model = selectedPreviewPath ?: pendingImagePaths.firstOrNull(),
                            contentDescription = "Selected photo preview",
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF111714)),
                            contentScale = ContentScale.Fit
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                            items(pendingImagePaths, key = { it }) { path ->
                                Box(Modifier.clickable { selectedPreviewPath = path }) {
                                    AsyncImage(path, null, Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, if (selectedPreviewPath == path) LiquidGlassTheme.PrimaryGreen else Color.Transparent, RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                    IconButton(
                                        enabled = !uiState.isSendingMedia,
                                        onClick = {
                                            pendingImagePaths = pendingImagePaths - path
                                            if (selectedPreviewPath == path) selectedPreviewPath = pendingImagePaths.firstOrNull()
                                            if (pendingImagePaths.isEmpty()) showCaptionDialog = false
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(24.dp).background(Color.Black.copy(alpha=.78f), CircleShape)
                                    ) { Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp)) }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
                            OutlinedTextField(
                                pendingCaption,
                                { pendingCaption = it },
                                enabled = !uiState.isSendingMedia,
                                placeholder = { Text("Add a caption") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(26.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LiquidGlassTheme.PrimaryGreen,
                                    unfocusedBorderColor = Color.White.copy(alpha = .28f),
                                    focusedContainerColor = Color.White.copy(alpha = .06f),
                                    unfocusedContainerColor = Color.White.copy(alpha = .04f)
                                )
                            )
                            Spacer(Modifier.width(9.dp))
                            IconButton(
                                enabled = pendingImagePaths.isNotEmpty() && !uiState.isSendingMedia,
                                onClick = { viewModel.sendImagePaths(pendingImagePaths, pendingCaption) },
                                modifier = Modifier.size(56.dp).shadow(10.dp, CircleShape).background(LiquidGlassTheme.PrimaryGreen, CircleShape)
                            ) {
                                if (uiState.isSendingMedia) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                                else Icon(Icons.AutoMirrored.Filled.Send, "Send photos", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (viewerImages.isNotEmpty()) {
            ChatPhotoViewer(
                images = viewerImages,
                initialIndex = viewerStartIndex,
                onClose = { viewerImages = emptyList() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    currentUserId: Long?,
    showSenderName: Boolean = true,
    onLongClick: () -> Unit,
    onSwipeToReply: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onEventClick: () -> Unit,
    highlighted: Boolean,
    onReplyClick: (Long) -> Unit,
    voiceAvatarUrl: String? = null,
    voiceAvatarFallback: String = "S",
    onResolveVoiceSource: (suspend () -> String?)? = null
) {
    val isMe = message.sender == currentUserId || message.senderName == "You"
    val align = if (isMe) Alignment.End else Alignment.Start
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val bg = when {
        isMe && dark -> Color(0xFF0B3A2C)
        isMe -> Color(0xFFDDF5E7)
        dark -> Color(0xFF1B211E)
        else -> Color(0xFFFFFFFF)
    }
    val textColor = when {
        isMe && dark -> Color(0xFFF2FFF8)
        isMe -> Color(0xFF102019)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val metaColor = when {
        isMe && dark -> Color.White.copy(alpha = .62f)
        isMe -> Color(0xFF52645A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .76f)
    }
    var dragOffset by remember(message.id) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(dragOffset, spring(stiffness = Spring.StiffnessMediumLow), label = "replySwipe")
    val haptic = LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * .82f).dp.coerceAtMost(340.dp)
    val photoUrls = remember(message.id, message.fileUrl, message.metadata) { messagePhotoUrls(message) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { change, amount -> change.consume(); dragTotal += amount; dragOffset = dragTotal.coerceIn(0f, 76.dp.toPx()) },
                    onDragEnd = { if (dragTotal > 56.dp.toPx()) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSwipeToReply() }; dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f }
                )
            }
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(vertical = 1.dp),
        horizontalAlignment = align
    ) {
        Box(contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
            if (animatedOffset > 12f) Icon(Icons.Default.Reply, null, tint = LiquidGlassTheme.PrimaryGreen.copy(alpha = (animatedOffset / 100f).coerceIn(.25f, 1f)), modifier = Modifier.size(24.dp))
        Surface(
            color = if (highlighted) LiquidGlassTheme.PrimaryGreen.copy(alpha = .45f) else bg,
            shape = RoundedCornerShape(
                topStart = 17.dp,
                topEnd = 17.dp,
                bottomStart = if (isMe) 17.dp else 5.dp,
                bottomEnd = if (isMe) 5.dp else 17.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                0.75.dp,
                if (isMe) LiquidGlassTheme.PrimaryGreen.copy(alpha = if (dark) .38f else .20f)
                else LiquidGlassTheme.cardBorder().copy(alpha = if (dark) .55f else .72f)
            ),
            shadowElevation = if (dark) 0.dp else 1.5.dp,
            modifier = Modifier.widthIn(min = 52.dp, max = maxBubbleWidth).offset(x = with(density) { animatedOffset.toDp() })
        ) {
            Column(modifier = Modifier.padding(horizontal = if (message.messageType == "photo") 4.dp else 10.dp, vertical = if (message.messageType == "photo") 4.dp else 6.dp)) {
                if (!isMe && showSenderName && message.senderName.isNotEmpty()) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = LiquidGlassTheme.PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                val reply = message.metadata?.get("reply_to") as? Map<*, *>
                if (reply != null && !message.isDeleted) {
                    Surface(
                        onClick = { (reply["id"] as? Number)?.toLong()?.let(onReplyClick) },
                        color = if (isMe) Color.Black.copy(alpha = .12f) else LiquidGlassTheme.PrimaryGreen.copy(alpha = .08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(reply["sender_name"]?.toString() ?: "Message", color = if (isMe) textColor else LiquidGlassTheme.PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(reply["preview"]?.toString() ?: reply["message"]?.toString().orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = textColor.copy(alpha = .78f))
                        }
                    }
                }

                if (message.isDeleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Block, null, modifier = Modifier.size(16.dp), tint = textColor.copy(alpha=.7f)); Spacer(Modifier.width(6.dp)); Text("This message was deleted", color = textColor.copy(alpha=.72f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                } else when (message.messageType) {
                    "photo" -> {
                        if (photoUrls.isNotEmpty()) {
                            PhotoMessageGrid(photoUrls, message.queued, onPhotoClick, onLongClick, dark)
                            if (message.message.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    "voice" -> {
                        val path = message.localMediaPath ?: resolveChatMediaUrl(message.fileUrl) ?: ""
                        VoiceMessagePlayer(audioUrlOrPath = path, durationSeconds = message.duration ?: 0, isOutgoing = isMe, avatarUrl = voiceAvatarUrl, avatarFallback = voiceAvatarFallback, onResolveSource = onResolveVoiceSource)
                    }
                    "event" -> {
                        BookingMessageCard(message = message, onClick = onEventClick)
                    }
                    else -> {}
                }

                if (!message.isDeleted && message.message.isNotEmpty()) {
                    Text(
                        text = message.message,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        modifier = if (message.messageType == "photo") Modifier.padding(horizontal = 6.dp, vertical = 1.dp) else Modifier
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatChatMessageTime(message.timestamp.ifBlank { message.createdAt }),
                        fontSize = 10.sp,
                        color = metaColor
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        val tickIcon = when {
                            message.queued -> Icons.Default.Schedule
                            message.isRead || message.delivered -> Icons.Default.DoneAll
                            else -> Icons.Default.Done
                        }
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = null,
                            tint = if (message.isRead) Color(0xFF00A86B) else metaColor,
                            modifier = Modifier.size(if (message.queued) 11.dp else 13.dp)
                        )
                    }
                }
            }
        }
        }
    }
}

private fun messagePhotoUrls(message: ChatMessage): List<String> {
    val urls = mutableListOf<String>()
    (message.localMediaPath ?: message.fileUrl)?.takeIf { it.isNotBlank() }?.let(urls::add)
    listOf("images", "media_urls", "photos", "files").forEach { key ->
        val value = message.metadata?.get(key)
        (value as? List<*>)?.forEach { item ->
            val raw = when (item) {
                is String -> item
                is Map<*, *> -> item["url"]?.toString() ?: item["file_url"]?.toString() ?: item["path"]?.toString()
                else -> null
            }
            raw?.takeIf { it.isNotBlank() }?.let(urls::add)
        }
    }
    return urls.distinct()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoMessageGrid(urls: List<String>, queued: Boolean, onClick: (String) -> Unit, onLongClick: () -> Unit, dark: Boolean) {
    val visible = urls.take(4)
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(if (dark) Color(0xFF111714) else Color(0xFFF0F5F2)).border(.75.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha=.18f), shape)) {
        if (visible.size == 1) {
            val url = visible.first()
            AsyncImage(resolveChatMediaUrl(url), "Chat photo", Modifier.fillMaxWidth().heightIn(min=150.dp, max=360.dp).aspectRatio(4f/3f).combinedClickable(onClick={ onClick(url) }, onLongClick=onLongClick), contentScale=ContentScale.Crop)
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                visible.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth().height(if (visible.size <= 2) 190.dp else 142.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEachIndexed { columnIndex, url ->
                            val index = rowIndex * 2 + columnIndex
                            Box(Modifier.weight(1f).fillMaxHeight().combinedClickable(onClick={ onClick(url) }, onLongClick=onLongClick)) {
                                AsyncImage(resolveChatMediaUrl(url), "Photo ${index + 1} of ${urls.size}", Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
                                if (index == 3 && urls.size > 4) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.56f)), contentAlignment=Alignment.Center) { Text("+${urls.size - 3}", color=Color.White, fontSize=32.sp, fontWeight=FontWeight.SemiBold) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (queued) Surface(color=Color.Black.copy(alpha=.58f), shape=CircleShape, modifier=Modifier.align(Alignment.Center)) { CircularProgressIndicator(Modifier.padding(10.dp).size(24.dp), color=Color.White, strokeWidth=2.dp) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatPhotoViewer(images: List<String>, initialIndex: Int, onClose: () -> Unit) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0)),
        pageCount = { images.size }
    )
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().zIndex(20f).background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                ZoomableChatPhoto(images[page])
            }

            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.Black.copy(alpha = .46f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close photo", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = Color.Black.copy(alpha = .46f), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "${pagerState.currentPage + 1} / ${images.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            if (images.size > 1) {
                LazyRow(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                        .background(Color.Black.copy(alpha = .50f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(images, key = { index, url -> "$index:$url" }) { index, url ->
                        AsyncImage(
                            model = resolveChatMediaUrl(url),
                            contentDescription = "Open photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(9.dp))
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .then(
                                    if (index == pagerState.currentPage) Modifier.border(2.dp, LiquidGlassTheme.PrimaryGreen, RoundedCornerShape(9.dp))
                                    else Modifier.border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(9.dp))
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableChatPhoto(rawPath: String) {
    var scale by remember(rawPath) { mutableFloatStateOf(1f) }
    var translationX by remember(rawPath) { mutableFloatStateOf(0f) }
    var translationY by remember(rawPath) { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = resolveChatMediaUrl(rawPath),
            contentDescription = "Full-screen chat photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(vertical = 74.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = translationX,
                    translationY = translationY
                )
                .pointerInput(rawPath) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            translationX += pan.x
                            translationY += pan.y
                        } else {
                            translationX = 0f
                            translationY = 0f
                        }
                    }
                }
                .pointerInput(rawPath) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            translationX = 0f
                            translationY = 0f
                        } else {
                            scale = 2.5f
                        }
                    })
                }
        )
    }
}

private fun resolveChatMediaUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    if (raw.startsWith("/") || File(raw).exists()) return raw
    if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("content://") || raw.startsWith("file://")) return raw
    return BuildConfig.BASE_URL.trimEnd('/') + "/" + raw.trimStart('/')
}

private fun formatChatMessageTime(raw: String): String = runCatching {
    val instant = java.time.OffsetDateTime.parse(raw.replace("Z", "+00:00")).toInstant()
    java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(instant.atZone(java.time.ZoneId.systemDefault()))
}.getOrElse { raw.takeLast(8).take(5) }

private fun formatPresenceLastSeen(raw: String): String = runCatching {
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
}.getOrElse { "tap for info" }

@Composable
private fun ChatHeaderAvatar(avatarUrl: String?, title: String, isTeam: Boolean) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(LiquidGlassTheme.PrimaryGreen.copy(alpha = .14f)),
            contentAlignment = Alignment.Center
        ) {
            if (title.isNotBlank()) {
                Text(
                    title.trim().first().uppercase(),
                    color = LiquidGlassTheme.PrimaryGreen,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    if (isTeam) Icons.Default.Groups else Icons.Default.Person,
                    contentDescription = null,
                    tint = LiquidGlassTheme.PrimaryGreen,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }

    Surface(
        modifier = Modifier.size(50.dp).shadow(5.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(2.dp, LiquidGlassTheme.PrimaryGreen.copy(alpha = .82f))
    ) {
        Box(Modifier.fillMaxSize().padding(2.dp).clip(CircleShape)) {
            if (!avatarUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = "$title profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { fallback() },
                    error = { fallback() },
                    success = { SubcomposeAsyncImageContent() }
                )
            } else {
                fallback()
            }
        }
    }
}

@Composable
private fun EmojiPanel(onEmoji: (String) -> Unit) {
    val categories = remember { listOf(
        "😀" to listOf("😀","😃","😄","😁","😆","😅","😂","🙂","😊","😇","🥰","😍","🤩","😘","😗","😚","😋","😜","🤪","🤔","🤫","🤭","🫢","🫡","😐","😑","😶","🙄","😏","😣","😥","😮","🤐","😯","😪","😫","🥱","😴","🤤","😛","😒","😓","😔","🤑","😲","☹️","🙁"),
        "👋" to listOf("👋","🤚","🖐️","✋","🖖","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍","👎","✊","👊","🤛","🤜","👏","🙌","🫶","🤝","🙏"),
        "⚽" to listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","🏹","🎣","🤿","🥊","🥋","🎽","🛹","🛼","🛷","⛸️"),
        "🔥" to listOf("🔥","❤️","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","✨","⭐","🌟","💫","⚡","💥","🎉","🎊","✅","❌","💯","🏆","🥇")
    ) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).background(Color.Black.copy(alpha = .92f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { categories.forEachIndexed { index, pair -> TextButton(onClick = { selected = index }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(containerColor = if(selected == index) LiquidGlassTheme.PrimaryGreen.copy(alpha=.25f) else Color.Transparent)) { Text(pair.first, fontSize = 24.sp) } } }
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8), contentPadding = PaddingValues(8.dp), modifier = Modifier.fillMaxWidth()) {
            gridItems(categories[selected].second) { emoji -> Box(Modifier.aspectRatio(1f).clickable { onEmoji(emoji) }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 25.sp) } }
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
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typingDots")
    Surface(
        color = LiquidGlassTheme.cardBackground(),
        shape = RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 5.dp, bottomEnd = 17.dp),
        border = androidx.compose.foundation.BorderStroke(.75.dp, LiquidGlassTheme.cardBorder()),
        shadowElevation = if (MaterialTheme.colorScheme.background.luminance() < .5f) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = .28f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(520, delayMillis = index * 120, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "typingDot$index"
                )
                Box(Modifier.size(7.dp).background(LiquidGlassTheme.PrimaryGreen.copy(alpha = alpha), CircleShape))
            }
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
    val source = if (android.os.Build.VERSION.SDK_INT >= 28) {
        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            val original = info.size
            val largest = maxOf(original.width, original.height)
            if (largest > 1024) {
                val ratio = 1024f / largest.toFloat()
                decoder.setTargetSize((original.width * ratio).toInt(), (original.height * ratio).toInt())
            }
        }
    } else {
        @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    val scale = minOf(1f, 1024f / maxOf(source.width, source.height).toFloat())
    val resized = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source
    FileOutputStream(file).use { resized.compress(Bitmap.CompressFormat.JPEG, 50, it) }
    if (resized !== source) resized.recycle()
    source.recycle()
    file.absolutePath
}.getOrNull()

private fun saveBitmapToChatCache(context: android.content.Context, bitmap: Bitmap): String {
    val file = File(context.cacheDir, "chat_camera_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it) }
    return file.absolutePath
}

package com.sportynix.app.presentation.messages

import android.content.Context
import android.media.MediaRecorder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.websocket.WebSocketManager
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.domain.model.Chat
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class ChatUiState(
    val chatId: Long = 0,
    val chatDetails: Chat? = null,
    val messages: List<ChatMessage> = emptyList(),
    val textInput: String = "",
    val replyingToMessage: ChatMessage? = null,
    val selectedMessageForMenu: ChatMessage? = null,
    val isRecordingVoice: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val isBlocked: Boolean = false,
    val blockHintMessage: String? = null,
    val currentUserId: Long? = null,
    val typingUserName: String? = null,
    val isOtherUserOnline: Boolean? = null,
    val otherUserLastSeen: String? = null,
    val isRealtimeConnected: Boolean = false,
    val isSendingMedia: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: Long = savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ChatUiState(chatId = chatId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var voiceTimerJob: Job? = null
    private var currentVoiceFile: File? = null
    private var typingStopJob: Job? = null

    init {
        if (chatId > 0) {
            connectRealtime()
            loadChatDetails()
            loadMessages()
            observeWebSocketMessages()
            observeConnection()
        }
    }

    private fun connectRealtime() {
        viewModelScope.launch {
            combine(sessionManager.accessToken, sessionManager.userId) { token, userId -> token to userId }
                .first()
                .let { (token, userId) ->
                    _uiState.update { it.copy(currentUserId = userId?.toLongOrNull()) }
                    if (!token.isNullOrBlank()) webSocketManager.connectChat(chatId, token)
                }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            webSocketManager.isChatWsConnected.collect { connected ->
                _uiState.update { it.copy(isRealtimeConnected = connected) }
            }
        }
    }

    fun loadChatDetails() {
        viewModelScope.launch {
            chatRepository.getChatDetails(chatId).onSuccess { chat ->
                val blockMsg = formatBlockHint(chat)
                _uiState.update {
                    it.copy(
                        chatDetails = chat,
                        isBlocked = chat.blockedByMe == true || chat.blockedMe == true || chat.isBlocked == true,
                        blockHintMessage = blockMsg
                    )
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.fetchMessagesFromServer(chatId, 50).onSuccess { msgs ->
                _uiState.update { it.copy(messages = msgs, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }

            chatRepository.getMessagesWithSync(chatId, 50).collect { list ->
                _uiState.update { it.copy(messages = list) }
            }
        }
    }

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(textInput = text) }
        webSocketManager.sendTyping(text.isNotEmpty())
        typingStopJob?.cancel()
        if (text.isNotEmpty()) {
            typingStopJob = viewModelScope.launch {
                delay(1800)
                webSocketManager.sendTyping(false)
            }
        }
    }

    fun setReplyingTo(message: ChatMessage?) {
        _uiState.update { it.copy(replyingToMessage = message) }
    }

    fun setSelectedMessageForMenu(message: ChatMessage?) {
        _uiState.update { it.copy(selectedMessageForMenu = message) }
    }

    fun sendTextMessage() {
        val text = _uiState.value.textInput.trim()
        if (text.isEmpty() || _uiState.value.isBlocked || _uiState.value.chatDetails?.canPost == false) return

        val replyMsg = _uiState.value.replyingToMessage
        val metadata = replyMsg?.let {
            mapOf("reply_to" to mapOf("id" to it.id, "sender_name" to it.senderName, "preview" to replyPreview(it)))
        }

        _uiState.update { it.copy(textInput = "", replyingToMessage = null) }

        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                message = text,
                messageType = "text",
                metadata = metadata
            )
        }
    }

    private fun replyPreview(message: ChatMessage): String = when (message.messageType) {
        "photo", "image" -> "Photo"
        "voice" -> "Voice message"
        "event" -> message.metadata?.get("title")?.toString() ?: "Booking event"
        else -> message.message.take(160)
    }

    fun startVoiceRecording() {
        if (_uiState.value.isBlocked) return
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp3")
            currentVoiceFile = file

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _uiState.update { it.copy(isRecordingVoice = true, voiceDurationSeconds = 0) }

            voiceTimerJob = viewModelScope.launch {
                while (_uiState.value.isRecordingVoice) {
                    delay(1000)
                    _uiState.update { it.copy(voiceDurationSeconds = it.voiceDurationSeconds + 1) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting voice recorder")
            cancelVoiceRecording()
        }
    }

    fun stopAndSendVoiceRecording() {
        voiceTimerJob?.cancel()
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val file = currentVoiceFile
            val duration = _uiState.value.voiceDurationSeconds

            if (file != null && file.exists() && duration > 0) {
                viewModelScope.launch {
                    chatRepository.queueMediaMessage(
                        chatId = chatId,
                        mediaType = "voice",
                        localMediaPath = file.absolutePath,
                        caption = "",
                        duration = duration
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping voice recorder")
        } finally {
            _uiState.update { it.copy(isRecordingVoice = false, voiceDurationSeconds = 0) }
            currentVoiceFile = null
        }
    }

    fun sendImagePaths(paths: List<String>, caption: String) {
        if (paths.isEmpty() || _uiState.value.isBlocked || _uiState.value.isSendingMedia) return
        _uiState.update { it.copy(isSendingMedia = true, errorMessage = null) }
        viewModelScope.launch {
            paths.take(5).forEachIndexed { index, path ->
                chatRepository.queueMediaMessage(
                    chatId = chatId,
                    mediaType = "photo",
                    localMediaPath = path,
                    caption = if (index == 0) caption.trim() else ""
                ).onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Unable to queue image") }
                }
            }
            _uiState.update { it.copy(isSendingMedia = false) }
        }
    }

    fun cancelVoiceRecording() {
        voiceTimerJob?.cancel()
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentVoiceFile?.delete()
        } catch (e: Exception) {
            // ignore
        } finally {
            _uiState.update { it.copy(isRecordingVoice = false, voiceDurationSeconds = 0) }
            currentVoiceFile = null
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, messageId)
        }
    }

    fun deleteMessageForMe(messageId: Long) {
        viewModelScope.launch {
            chatRepository.deleteMessageForMe(chatId, messageId)
        }
    }

    fun pinMessage(messageId: Long) {
        viewModelScope.launch {
            chatRepository.pinMessage(chatId, messageId)
        }
    }

    fun unpinMessage(messageId: Long) {
        viewModelScope.launch {
            chatRepository.unpinMessage(chatId, messageId)
        }
    }

    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            webSocketManager.messagesFlow.collect { wsMsg ->
                val belongsToChat = wsMsg.chatId == null || wsMsg.chatId == chatId || wsMsg.conversationId == chatId
                if (!belongsToChat) return@collect
                when (wsMsg.type.lowercase()) {
                    "typing", "typing_indicator" -> {
                        val isMine = wsMsg.senderId == _uiState.value.currentUserId
                        _uiState.update {
                            it.copy(typingUserName = if (wsMsg.isTyping == true && !isMine) wsMsg.senderName ?: "Someone" else null)
                        }
                    }
                    "user_joined" -> if (wsMsg.userId == _uiState.value.chatDetails?.otherUserId) {
                        _uiState.update { it.copy(isOtherUserOnline = true) }
                    }
                    "user_left" -> if (wsMsg.userId == _uiState.value.chatDetails?.otherUserId) {
                        _uiState.update { it.copy(isOtherUserOnline = false, otherUserLastSeen = wsMsg.timestamp) }
                    }
                    "presence", "presence_update", "presence_snapshot", "user_status" -> _uiState.update {
                        it.copy(
                            isOtherUserOnline = wsMsg.online ?: wsMsg.metadata?.get("is_online") as? Boolean,
                            otherUserLastSeen = wsMsg.lastSeen ?: wsMsg.metadata?.get("last_seen")?.toString()
                        )
                    }
                    "message", "new_message", "chat_message" -> {
                        wsMsg.messageId?.let { messageId ->
                            if (wsMsg.senderId != _uiState.value.currentUserId) {
                                webSocketManager.sendDeliveredAck(messageId)
                                webSocketManager.markAsRead(messageId)
                                chatRepository.markAsRead(chatId, listOf(messageId))
                            }
                        }
                        refreshMessages()
                    }
                    "message_deleted", "message_updated", "message_read", "read_receipt",
                    "message_delivered", "delivered", "delivered_receipt", "message_pinned",
                    "message_unpinned", "pinned_message_updated", "pin_update", "chat_cleared" -> refreshMessages()
                }
            }
        }
    }

    private fun refreshMessages() {
        viewModelScope.launch {
            chatRepository.fetchMessagesFromServer(chatId, 50)
        }
    }

    override fun onCleared() {
        typingStopJob?.cancel()
        voiceTimerJob?.cancel()
        webSocketManager.sendTyping(false)
        webSocketManager.disconnectChat()
        mediaRecorder?.release()
        super.onCleared()
    }

    private fun formatBlockHint(chat: Chat): String? {
        if (chat.chatType != "direct") return null
        val other = chat.otherUserName ?: "this user"
        if (chat.blockedByMe == true) return "You blocked $other. Unblock $other to send messages."
        if (chat.blockedMe == true) return "$other blocked you. You cannot send messages."
        return chat.blockStatusMessage
    }
}

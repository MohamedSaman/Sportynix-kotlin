package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.sportynix.app.BuildConfig
import com.sportynix.app.domain.model.NewMessageNotification
import com.sportynix.app.domain.model.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var chatWebSocket: WebSocket? = null
    private var unreadCountsWebSocket: WebSocket? = null

    private var currentChatId: Long? = null
    private var currentToken: String? = null

    private val _messagesFlow = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 64)
    val messagesFlow: SharedFlow<WebSocketMessage> = _messagesFlow.asSharedFlow()

    private val _newNotificationFlow = MutableSharedFlow<NewMessageNotification>(extraBufferCapacity = 64)
    val newNotificationFlow: SharedFlow<NewMessageNotification> = _newNotificationFlow.asSharedFlow()

    private val _unreadCountsState = MutableStateFlow<Pair<Int, Int>>(0 to 0) // (messagesCount, notificationsCount)
    val unreadCountsState: StateFlow<Pair<Int, Int>> = _unreadCountsState.asStateFlow()

    private val _isChatWsConnected = MutableStateFlow(false)
    val isChatWsConnected: StateFlow<Boolean> = _isChatWsConnected.asStateFlow()

    private val _isUnreadWsConnected = MutableStateFlow(false)
    val isUnreadWsConnected: StateFlow<Boolean> = _isUnreadWsConnected.asStateFlow()

    private val recentlyMarkedRead = mutableSetOf<String>()
    private val recentlyMarkedDelivered = mutableSetOf<String>()

    fun connectChat(chatId: Long, token: String) {
        if (currentChatId == chatId && currentToken == token && _isChatWsConnected.value) {
            Timber.d("WebSocketManager: Chat WS already connected for chatId=$chatId")
            return
        }

        disconnectChat()

        currentChatId = chatId
        currentToken = token

        val wsBase = BuildConfig.WS_BASE_URL.removeSuffix("/")
        val url = "$wsBase/ws/chat/$chatId/?token=${encodeUriParam(token)}"

        Timber.d("WebSocketManager: Connecting to chat WS: $url")

        val request = Request.Builder().url(url).build()

        chatWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocketManager: Chat WS connected for chatId=$chatId")
                _isChatWsConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocketManager: Chat message received: $text")
                try {
                    val msg = gson.fromJson(text, WebSocketMessage::class.java)
                    scope.launch {
                        _messagesFlow.emit(msg)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing chat websocket message")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocketManager: Chat WS closing: $code / $reason")
                _isChatWsConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocketManager: Chat WS closed: $code / $reason")
                _isChatWsConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "WebSocketManager: Chat WS failure")
                _isChatWsConnected.value = false
                scheduleChatReconnect(chatId, token)
            }
        })
    }

    fun connectUnreadCounts(token: String) {
        if (currentToken == token && _isUnreadWsConnected.value) {
            return
        }

        disconnectUnreadCounts()
        currentToken = token

        val wsBase = BuildConfig.WS_BASE_URL.removeSuffix("/")
        val url = "$wsBase/ws/unread-counts/?token=${encodeUriParam(token)}"

        Timber.d("WebSocketManager: Connecting to unread counts WS: $url")

        val request = Request.Builder().url(url).build()

        unreadCountsWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocketManager: Unread counts WS connected")
                _isUnreadWsConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocketManager: Unread counts message received: $text")
                try {
                    val map = gson.fromJson(text, Map::class.java) ?: return
                    val type = map["type"] as? String ?: return

                    if (type == "counts_update" || type == "initial_counts" || type == "unread_counts_update") {
                        val messagesCount = (map["messages"] as? Double ?: map["messages_count"] as? Double ?: 0.0).toInt()
                        val notificationsCount = (map["notifications"] as? Double ?: map["notifications_count"] as? Double ?: 0.0).toInt()
                        _unreadCountsState.value = messagesCount to notificationsCount
                    }

                    if (type == "new_message" || type == "chat_message_notification") {
                        val notif = gson.fromJson(text, NewMessageNotification::class.java)
                        scope.launch {
                            _newNotificationFlow.emit(notif)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing unread counts WS message")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _isUnreadWsConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isUnreadWsConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "WebSocketManager: Unread counts WS failure")
                _isUnreadWsConnected.value = false
                scheduleUnreadReconnect(token)
            }
        })
    }

    fun sendMessage(msgMap: Map<String, Any?>) {
        val json = gson.toJson(msgMap)
        chatWebSocket?.send(json)
    }

    fun sendTyping(isTyping: Boolean) {
        val map = mapOf(
            "type" to "typing",
            "is_typing" to isTyping
        )
        sendMessage(map)
    }

    fun markAsRead(messageId: Long) {
        val key = "$messageId"
        if (recentlyMarkedRead.contains(key)) return
        recentlyMarkedRead.add(key)

        scope.launch {
            delay(5000)
            recentlyMarkedRead.remove(key)
        }

        val map = mapOf(
            "type" to "read_receipt",
            "message_id" to messageId
        )
        sendMessage(map)
    }

    fun sendDeliveredAck(messageId: Long) {
        val key = "$messageId"
        if (recentlyMarkedDelivered.contains(key)) return
        recentlyMarkedDelivered.add(key)

        scope.launch {
            delay(5000)
            recentlyMarkedDelivered.remove(key)
        }

        val map = mapOf(
            "type" to "delivered_ack",
            "message_id" to messageId
        )
        sendMessage(map)
    }

    fun requestCountsUpdate() {
        val map = mapOf("type" to "get_unread_counts")
        val json = gson.toJson(map)
        unreadCountsWebSocket?.send(json) ?: chatWebSocket?.send(json)
    }

    fun disconnectChat() {
        chatWebSocket?.close(1000, "Client disconnect")
        chatWebSocket = null
        currentChatId = null
        _isChatWsConnected.value = false
    }

    fun disconnectUnreadCounts() {
        unreadCountsWebSocket?.close(1000, "Client disconnect")
        unreadCountsWebSocket = null
        _isUnreadWsConnected.value = false
    }

    private fun scheduleChatReconnect(chatId: Long, token: String) {
        scope.launch {
            delay(3000)
            if (currentChatId == chatId && !_isChatWsConnected.value) {
                connectChat(chatId, token)
            }
        }
    }

    private fun scheduleUnreadReconnect(token: String) {
        scope.launch {
            delay(4000)
            if (currentToken == token && !_isUnreadWsConnected.value) {
                connectUnreadCounts(token)
            }
        }
    }

    private fun encodeUriParam(param: String): String {
        return java.net.URLEncoder.encode(param, "UTF-8")
    }
}

package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.network.NetworkConnectivityObserver
import com.sportynix.app.domain.model.websocket.NewMessageNotification
import com.sportynix.app.domain.model.websocket.WebSocketMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

data class UnreadCountsUpdate(val messages: Int, val notifications: Int)

@Singleton
class ChatWebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var chatSocket: WebSocket? = null
    private var unreadCountsSocket: WebSocket? = null

    private var chatId: Int? = null
    private var token: String? = null
    
    private var isConnecting = false
    private var isUnreadCountsConnected = false
    
    private var isOnline = true
    
    // Reconnect logic
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 1000L
    private val offlineReconnectDelay = 4000L
    private var reconnectJob: Job? = null

    private var unreadCountsReconnectAttempts = 0
    private val maxUnreadCountsReconnectAttempts = 6
    private val unreadCountsReconnectDelay = 2000L
    private var unreadCountsReconnectJob: Job? = null

    // Message queues
    private val messageQueue = ConcurrentLinkedQueue<String>()
    
    // Deduplication sets
    private val recentlyMarkedRead = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Int, Boolean>())
    private val recentlyMarkedDelivered = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Int, Boolean>())
    
    // Flows for observing
    private val _messages = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    private val _unreadCounts = MutableSharedFlow<UnreadCountsUpdate>(extraBufferCapacity = 64)
    val unreadCounts: SharedFlow<UnreadCountsUpdate> = _unreadCounts.asSharedFlow()

    private val _newMessageNotifications = MutableSharedFlow<NewMessageNotification>(extraBufferCapacity = 64)
    val newMessageNotifications: SharedFlow<NewMessageNotification> = _newMessageNotifications.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 64)
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    init {
        scope.launch {
            connectivityObserver.observe().collect { status ->
                isOnline = (status == NetworkConnectivityObserver.Status.Available)
                if (isOnline) {
                    if (chatId != null && token != null && chatSocket == null && !isConnecting) {
                        connect(chatId!!, token!!)
                    }
                    if (token != null && !isUnreadCountsConnected && unreadCountsSocket == null) {
                        connectToUnreadCounts(token!!)
                    }
                }
            }
        }
    }

    fun connect(chatId: Int, token: String) {
        if (isConnecting && this.chatId == chatId && this.token == token) {
            Timber.d("WebSocket: Already connecting to this chat...")
            return
        }

        this.chatId = chatId
        this.token = token

        if (!isOnline) {
            isConnecting = false
            scheduleReconnect(true)
            return
        }

        disconnect() // Always disconnect previous
        
        this.chatId = chatId
        this.token = token
        isConnecting = true
        
        val wsUrl = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/chat/$chatId/?token=$token"
        Timber.d("WebSocket: Connecting to $wsUrl")

        val request = Request.Builder().url(wsUrl).build()

        chatSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket: Connected successfully")
                isConnecting = false
                reconnectAttempts = 0
                flushMessageQueue()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocket: Message received: $text")
                try {
                    val typeMap = gson.fromJson(text, Map::class.java)
                    val type = typeMap["type"] as? String

                    if (type == "unread_counts_update") {
                        val messagesCount = (typeMap["messages_count"] as? Double)?.toInt() ?: 0
                        val notificationsCount = (typeMap["notifications_count"] as? Double)?.toInt() ?: 0
                        scope.launch {
                            _unreadCounts.emit(UnreadCountsUpdate(messagesCount, notificationsCount))
                        }
                        return
                    }

                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    scope.launch {
                        _messages.emit(message.copy(
                            conversationId = message.conversationId ?: chatId,
                            chatId = message.chatId ?: chatId
                        ))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "WebSocket: Error parsing message")
                    scope.launch { _errors.emit(e) }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket: Disconnected $code $reason")
                isConnecting = false
                chatSocket = null
                if (code != 1000 && this@ChatWebSocketService.chatId != null && this@ChatWebSocketService.token != null) {
                    if (!isOnline) {
                        scheduleReconnect(true)
                    } else if (reconnectAttempts < maxReconnectAttempts) {
                        scheduleReconnect(false)
                    } else {
                        Timber.w("WebSocket: Max reconnect attempts reached")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val offline = !isOnline
                if (!offline) {
                    Timber.w(t, "WebSocket: Connection error")
                    scope.launch { _errors.emit(t) }
                }
                isConnecting = false
            }
        })
    }

    private fun scheduleReconnect(forceOfflineWait: Boolean) {
        reconnectJob?.cancel()
        
        if (forceOfflineWait || !isOnline) {
            reconnectJob = scope.launch {
                delay(offlineReconnectDelay)
                if (chatId != null && token != null) connect(chatId!!, token!!)
            }
            return
        }

        reconnectAttempts++
        val delayMs = reconnectDelay * (2.0.pow(reconnectAttempts - 1)).toLong()
        Timber.d("WebSocket: Retry $reconnectAttempts/$maxReconnectAttempts in ${delayMs}ms")
        
        reconnectJob = scope.launch {
            delay(delayMs)
            if (chatId != null && token != null) connect(chatId!!, token!!)
        }
    }

    fun sendMessage(messagePayload: Any) {
        val json = if (messagePayload is String) messagePayload else gson.toJson(messagePayload)
        
        if (chatSocket != null && !isConnecting && isOnline) {
            chatSocket?.send(json)
            Timber.d("WebSocket: Message sent")
        } else {
            Timber.d("WebSocket: Queuing message")
            messageQueue.add(json)
            
            if (!isConnecting && chatId != null && token != null) {
                connect(chatId!!, token!!)
            }
        }
    }

    fun sendTypingIndicator(isTyping: Boolean) {
        val payload = mapOf("type" to "typing", "is_typing" to isTyping)
        chatSocket?.send(gson.toJson(payload))
    }

    fun markAsRead(messageId: Int) {
        if (recentlyMarkedRead.contains(messageId)) {
            Timber.d("WebSocket markAsRead: Message $messageId already marked recently, skipping")
            return
        }
        recentlyMarkedRead.add(messageId)
        scope.launch {
            delay(5000)
            recentlyMarkedRead.remove(messageId)
        }
        val payload = mapOf("type" to "read_receipt", "message_id" to messageId)
        chatSocket?.send(gson.toJson(payload))
    }

    fun sendDeliveredAck(messageId: Int) {
        if (recentlyMarkedDelivered.contains(messageId)) {
            Timber.d("WebSocket sendDeliveredAck: Message $messageId already marked recently, skipping")
            return
        }
        recentlyMarkedDelivered.add(messageId)
        scope.launch {
            delay(5000)
            recentlyMarkedDelivered.remove(messageId)
        }
        val payload = mapOf("type" to "delivered_ack", "message_id" to messageId)
        chatSocket?.send(gson.toJson(payload))
    }

    fun requestCountsUpdate() {
        if (unreadCountsSocket != null && isUnreadCountsConnected) {
            unreadCountsSocket?.send(gson.toJson(mapOf("type" to "get_unread_counts")))
        } else {
            chatSocket?.send(gson.toJson(mapOf("type" to "get_unread_counts")))
        }
    }

    private fun flushMessageQueue() {
        if (messageQueue.isEmpty()) return
        Timber.d("WebSocket: Flushing ${messageQueue.size} queued messages")
        while (messageQueue.isNotEmpty()) {
            val msg = messageQueue.poll()
            if (msg != null) chatSocket?.send(msg)
        }
    }

    fun connectToUnreadCounts(token: String) {
        disconnectFromUnreadCounts()

        if (!isOnline) {
            isUnreadCountsConnected = false
            scheduleUnreadCountsReconnect(token, true)
            return
        }

        val wsUrl = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/unread-counts/?token=$token"
        val request = Request.Builder().url(wsUrl).build()

        unreadCountsSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket: Unread counts connected")
                isUnreadCountsConnected = true
                unreadCountsReconnectJob?.cancel()
                unreadCountsReconnectAttempts = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val map = gson.fromJson(text, Map::class.java)
                    val type = map["type"] as? String
                    
                    if (type == "counts_update" || type == "initial_counts" || type == "unread_counts_update") {
                        val m = (map["messages"] as? Double ?: map["messages_count"] as? Double ?: 0.0).toInt()
                        val n = (map["notifications"] as? Double ?: map["notifications_count"] as? Double ?: 0.0).toInt()
                        scope.launch { _unreadCounts.emit(UnreadCountsUpdate(m, n)) }
                    }

                    if (type == "new_message" || type == "chat_message_notification" || (type == "counts_update" && map.containsKey("message_id"))) {
                        val notification = gson.fromJson(text, NewMessageNotification::class.java)
                        scope.launch { _newMessageNotifications.emit(notification) }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "WebSocket: Error parsing unread counts")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isUnreadCountsConnected = false
                scheduleUnreadCountsReconnect(token, !isOnline)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isUnreadCountsConnected = false
                scheduleUnreadCountsReconnect(token, !isOnline)
            }
        })
    }

    private fun scheduleUnreadCountsReconnect(token: String, forceOfflineWait: Boolean) {
        unreadCountsReconnectJob?.cancel()
        
        if (forceOfflineWait || !isOnline) {
            unreadCountsReconnectJob = scope.launch {
                delay(offlineReconnectDelay)
                connectToUnreadCounts(token)
            }
            return
        }

        unreadCountsReconnectAttempts++
        if (unreadCountsReconnectAttempts > maxUnreadCountsReconnectAttempts) return

        val delayMs = unreadCountsReconnectDelay * (2.0.pow(unreadCountsReconnectAttempts - 1)).toLong()
        unreadCountsReconnectJob = scope.launch {
            delay(delayMs)
            connectToUnreadCounts(token)
        }
    }

    fun disconnectFromUnreadCounts() {
        unreadCountsReconnectJob?.cancel()
        unreadCountsSocket?.close(1000, "Client disconnect")
        unreadCountsSocket = null
        isUnreadCountsConnected = false
        unreadCountsReconnectAttempts = 0
    }

    fun disconnect() {
        reconnectJob?.cancel()
        chatSocket?.close(1000, "Client disconnect")
        chatSocket = null
        
        chatId = null
        token = null
        reconnectAttempts = 0
        isConnecting = false
        messageQueue.clear()
    }
}

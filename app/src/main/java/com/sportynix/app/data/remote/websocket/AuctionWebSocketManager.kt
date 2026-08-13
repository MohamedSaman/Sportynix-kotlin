package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.dto.AuctionSessionSnapshotDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuctionWSEvent {
    data class EventReceived(val type: String, val payload: JsonObject) : AuctionWSEvent()
    object Connected : AuctionWSEvent()
    object Disconnected : AuctionWSEvent()
    data class Error(val message: String) : AuctionWSEvent()
}

@Singleton
class AuctionWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var isConnecting = false
    private var isClosedIntentionally = false
    private var currentAuctionId: String? = null
    private var pingJob: Job? = null

    private val _auctionSnapshot = MutableStateFlow<AuctionSessionSnapshotDto?>(null)
    val auctionSnapshot: StateFlow<AuctionSessionSnapshotDto?> = _auctionSnapshot.asStateFlow()

    private val _events = MutableSharedFlow<AuctionWSEvent>()
    val events: SharedFlow<AuctionWSEvent> = _events.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(auctionId: String) {
        if (currentAuctionId == auctionId && (webSocket != null || isConnecting)) return
        disconnect()

        currentAuctionId = auctionId
        isConnecting = true
        isClosedIntentionally = false

        scope.launch {
            val token = sessionManager.accessToken.firstOrNull()
            val tokenQuery = if (!token.isNull_or_empty()) "?token=${android.net.Uri.encode(token)}" else ""
            val wsUrl = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/auction/session/$auctionId/live/$tokenQuery"

            Timber.d("Connecting to Auction WebSocket at $wsUrl")

            val request = Request.Builder().url(wsUrl).build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("Auction WebSocket connected")
                    isConnecting = false
                    _isConnected.value = true
                    scope.launch { _events.emit(AuctionWSEvent.Connected) }
                    startPing()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.w(t, "Auction WebSocket failure")
                    isConnecting = false
                    _isConnected.value = false
                    stopPing()
                    this@AuctionWebSocketManager.webSocket = null
                    scope.launch { _events.emit(AuctionWSEvent.Error(t.message ?: "Connection failure")) }
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("Auction WebSocket closed: $reason")
                    isConnecting = false
                    _isConnected.value = false
                    stopPing()
                    this@AuctionWebSocketManager.webSocket = null
                    scope.launch { _events.emit(AuctionWSEvent.Disconnected) }
                    if (!isClosedIntentionally) {
                        scheduleReconnect()
                    }
                }
            })
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java) ?: return
            val type = json.get("type")?.asString ?: ""

            scope.launch {
                _events.emit(AuctionWSEvent.EventReceived(type, json))
            }

            if (type == "auction_snapshot" || json.has("session") || json.has("auction")) {
                val sessionElement = json.get("session") ?: json.get("auction") ?: json
                val snapshot = gson.fromJson(sessionElement, AuctionSessionSnapshotDto::class.java)
                if (snapshot != null) {
                    _auctionSnapshot.value = snapshot
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Auction WS message")
        }
    }

    private fun startPing() {
        stopPing()
        pingJob = scope.launch {
            while (_isConnected.value) {
                delay(30000)
                try {
                    val pingMsg = JsonObject().apply { addProperty("type", "ping") }
                    webSocket?.send(gson.toJson(pingMsg))
                } catch (e: Exception) {
                    Timber.w(e, "Auction WS ping failed")
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun scheduleReconnect() {
        val auctionId = currentAuctionId ?: return
        if (isClosedIntentionally) return

        scope.launch {
            delay(5000)
            if (!isClosedIntentionally && !_isConnected.value) {
                Timber.d("Attempting Auction WS reconnect...")
                connect(auctionId)
            }
        }
    }

    fun disconnect() {
        isClosedIntentionally = true
        stopPing()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        currentAuctionId = null
        _isConnected.value = false
        isConnecting = false
        _auctionSnapshot.value = null
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}

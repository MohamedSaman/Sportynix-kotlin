package com.sportynix.app.core.network

import com.google.gson.Gson
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebSocketState {
    object Disconnected : WebSocketState()
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    data class Error(val throwable: Throwable) : WebSocketState()
}

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    fun connect(endpointPath: String) {
        if (_connectionState.value == WebSocketState.Connected || _connectionState.value == WebSocketState.Connecting) {
            Timber.d("WebSocket already connected or connecting.")
            return
        }

        _connectionState.value = WebSocketState.Connecting

        val token = runBlocking { sessionManager.getAccessTokenSync() }
        val baseUrl = BuildConfig.WS_BASE_URL.trimEnd('/')
        val fullUrl = if (token.isNullOrEmpty()) {
            "$baseUrl/$endpointPath"
        } else {
            "$baseUrl/$endpointPath?token=$token"
        }

        Timber.d("Connecting to WebSocket: $fullUrl")

        val request = Request.Builder()
            .url(fullUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket Connected Successfully")
                _connectionState.value = WebSocketState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocket Received: $text")
                scope.launch {
                    _incomingMessages.emit(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket Failure: ${t.message}")
                _connectionState.value = WebSocketState.Error(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket Closing: $code / $reason")
                webSocket.close(code, reason)
                _connectionState.value = WebSocketState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket Closed: $code / $reason")
                _connectionState.value = WebSocketState.Disconnected
            }
        })
    }

    fun sendMessage(message: String): Boolean {
        return if (_connectionState.value == WebSocketState.Connected) {
            webSocket?.send(message) ?: false
        } else {
            Timber.w("Cannot send message, WebSocket not connected")
            false
        }
    }

    fun <T> sendJson(payload: T): Boolean {
        val json = gson.toJson(payload)
        return sendMessage(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = WebSocketState.Disconnected
    }
}

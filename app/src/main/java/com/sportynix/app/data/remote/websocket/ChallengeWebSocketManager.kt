package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A single, reconnecting challenge stream shared by the challenge screen. */
@Singleton
class ChallengeWebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    private var socket: WebSocket? = null
    private var token: String? = null
    private var baseUrl: String? = null
    private var connecting = false
    private var intentionalClose = false
    private var reconnectJob: Job? = null

    fun connect(accessToken: String, httpBaseUrl: String) {
        if (token == accessToken && (socket != null || connecting)) return
        disconnect()
        token = accessToken
        baseUrl = httpBaseUrl
        intentionalClose = false
        connecting = true
        val scheme = if (httpBaseUrl.startsWith("https://")) "wss://" else "ws://"
        val host = httpBaseUrl.removePrefix("https://").removePrefix("http://")
        val url = "$scheme$host/ws/challenges/?token=$accessToken"
        val request = Request.Builder().url(url).addHeader("Origin", httpBaseUrl).build()
        socket = client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build()
            .newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) { connecting = false }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val obj = gson.fromJson(text, JsonObject::class.java)
                        val type = obj?.get("type")?.asString.orEmpty()
                        if (type.contains("challenge", true) || type == "connected" || type == "list_update") _events.tryEmit(Unit)
                    } catch (_: Exception) { _events.tryEmit(Unit) }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    connecting = false; socket = null; scheduleReconnect()
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    connecting = false; socket = null; scheduleReconnect()
                }
            })
    }

    fun disconnect() {
        intentionalClose = true
        reconnectJob?.cancel()
        socket?.close(1000, "screen closed")
        socket = null
        connecting = false
        token = null
        baseUrl = null
    }

    private fun scheduleReconnect() {
        if (intentionalClose || reconnectJob?.isActive == true) return
        val currentToken = token ?: return
        val currentBase = baseUrl ?: return
        reconnectJob = scope.launch {
            delay(2_000)
            if (!intentionalClose) connect(currentToken, currentBase)
        }
    }
}

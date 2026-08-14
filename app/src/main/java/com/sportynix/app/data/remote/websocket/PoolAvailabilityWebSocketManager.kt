package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sportynix.app.BuildConfig
import com.sportynix.app.domain.model.PoolAvailabilitySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class PoolAvailabilityWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var currentPoolId: Int? = null
    private var currentToken: String? = null
    private var isConnecting = false
    private var isClosedIntentionally = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _availabilityState = MutableStateFlow<PoolAvailabilitySnapshot?>(null)
    val availabilityState: StateFlow<PoolAvailabilitySnapshot?> = _availabilityState.asStateFlow()

    fun connect(poolId: Int, token: String) {
        if (webSocket != null && webSocket?.request()?.url?.toString()?.contains(poolId.toString()) == true) {
            return
        }
        if (currentPoolId != poolId) {
            disconnect()
        }

        currentPoolId = poolId
        currentToken = token
        isConnecting = true
        isClosedIntentionally = false

        val wsUrl = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/pool-availability/$poolId/?token=$token"
        Timber.d("Connecting to Pool Availability WebSocket at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Pool Availability WebSocket connected")
                isConnecting = false
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "Pool Availability WebSocket failure")
                isConnecting = false
                _isConnected.value = false
                this@PoolAvailabilityWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("Pool Availability WebSocket closed: $reason")
                isConnecting = false
                _isConnected.value = false
                this@PoolAvailabilityWebSocketManager.webSocket = null
                if (!isClosedIntentionally) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun disconnect() {
        isClosedIntentionally = true
        webSocket?.close(1000, "App disconnect")
        webSocket = null
        currentPoolId = null
        currentToken = null
        _isConnected.value = false
        isConnecting = false
        _availabilityState.value = null
    }

    fun requestAvailability(occurrenceId: Int) {
        if (_isConnected.value) {
            val req = mapOf("type" to "get_availability", "occurrence_id" to occurrenceId)
            webSocket?.send(gson.toJson(req))
        }
    }

    private fun scheduleReconnect() {
        if (isClosedIntentionally || currentPoolId == null || currentToken == null) return
        scope.launch {
            delay(3000)
            if (!isClosedIntentionally && webSocket == null) {
                Timber.d("Retrying Pool Availability WebSocket connection...")
                connect(currentPoolId!!, currentToken!!)
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val element = gson.fromJson(text, JsonElement::class.java)
            if (!element.isJsonObject) return
            val json = element.asJsonObject
            val type = json.get("type")?.asString ?: return

            if (type == "availability_update") {
                val snapshot = gson.fromJson(json, PoolAvailabilitySnapshot::class.java)
                _availabilityState.value = snapshot
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Pool Availability WebSocket message")
        }
    }
}

package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.BuildConfig
import com.sportynix.app.data.remote.dto.LiveStateDto
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

sealed class CricketSocketEvent {
    data class LiveStateUpdate(val liveState: LiveStateDto) : CricketSocketEvent()
    data class BallRecorded(val ballInfo: String, val runs: Int, val isWicket: Boolean) : CricketSocketEvent()
    data class BoundaryHit(val isSix: Boolean, val runs: Int) : CricketSocketEvent()
    data class WicketFallen(val batsmanName: String, val wicketType: String) : CricketSocketEvent()
}

@Singleton
class CricketMatchWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var currentMatchId: String? = null
    private var isConnecting = false
    private var isClosedIntentionally = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _liveState = MutableStateFlow<LiveStateDto?>(null)
    val liveState: StateFlow<LiveStateDto?> = _liveState.asStateFlow()

    private val _socketEvents = MutableSharedFlow<CricketSocketEvent>(replay = 0)
    val socketEvents: SharedFlow<CricketSocketEvent> = _socketEvents.asSharedFlow()

    fun connectToMatch(matchId: String) {
        if (currentMatchId == matchId && (webSocket != null || isConnecting)) return
        disconnect()
        currentMatchId = matchId
        isConnecting = true
        isClosedIntentionally = false

        val wsUrl = BuildConfig.WS_BASE_URL.trimEnd('/') + "/ws/cricket/$matchId/"
        Timber.d("Connecting to Cricket Match WebSocket at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Cricket Match WebSocket connected for match $matchId")
                isConnecting = false
                _isConnected.value = true

                val subscribeMsg = JsonObject().apply {
                    addProperty("type", "subscribe")
                    addProperty("match_id", matchId)
                }
                webSocket.send(gson.toJson(subscribeMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "Cricket Match WebSocket failure for match $matchId")
                isConnecting = false
                _isConnected.value = false
                this@CricketMatchWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("Cricket Match WebSocket closed: $reason")
                isConnecting = false
                _isConnected.value = false
                this@CricketMatchWebSocketManager.webSocket = null
                if (!isClosedIntentionally) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun disconnect() {
        isClosedIntentionally = true
        webSocket?.close(1000, "Disconnect request")
        webSocket = null
        currentMatchId = null
        _isConnected.value = false
        _liveState.value = null
        isConnecting = false
    }

    private fun scheduleReconnect() {
        val matchId = currentMatchId ?: return
        if (isClosedIntentionally) return
        scope.launch {
            delay(4000)
            if (!isClosedIntentionally && webSocket == null && currentMatchId == matchId) {
                Timber.d("Reconnecting Cricket Match WebSocket for $matchId...")
                connectToMatch(matchId)
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val element = gson.fromJson(text, JsonElement::class.java)
            if (!element.isJsonObject) return
            val json = element.asJsonObject

            val type = json.get("type")?.asString ?: json.get("event")?.asString ?: return

            when (type) {
                "live_state", "match_update", "score_update" -> {
                    val stateData = json.get("data") ?: json
                    val stateDto = gson.fromJson(stateData, LiveStateDto::class.java)
                    if (stateDto != null) {
                        _liveState.value = stateDto
                        scope.launch {
                            _socketEvents.emit(CricketSocketEvent.LiveStateUpdate(stateDto))
                        }
                    }
                }
                "ball_recorded" -> {
                    val runs = json.get("runs")?.asInt ?: 0
                    val isWicket = json.get("is_wicket")?.asBoolean ?: false
                    val ballInfo = json.get("ball_info")?.asString ?: ""
                    scope.launch {
                        _socketEvents.emit(CricketSocketEvent.BallRecorded(ballInfo, runs, isWicket))
                    }
                }
                "boundary" -> {
                    val runs = json.get("runs")?.asInt ?: 4
                    val isSix = runs == 6
                    scope.launch {
                        _socketEvents.emit(CricketSocketEvent.BoundaryHit(isSix, runs))
                    }
                }
                "wicket" -> {
                    val batsmanName = json.get("batsman_name")?.asString ?: "Batsman"
                    val wicketType = json.get("wicket_type")?.asString ?: "out"
                    scope.launch {
                        _socketEvents.emit(CricketSocketEvent.WicketFallen(batsmanName, wicketType))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing Cricket WebSocket message: $text")
        }
    }
}

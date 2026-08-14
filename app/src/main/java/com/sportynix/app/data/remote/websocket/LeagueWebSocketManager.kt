package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sportynix.app.BuildConfig
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

sealed class LeagueSocketEvent {
    data class StandingsUpdate(val data: JsonElement) : LeagueSocketEvent()
    data class MatchScheduled(val data: JsonElement) : LeagueSocketEvent()
    data class MatchResult(val data: JsonElement) : LeagueSocketEvent()
    data class TeamRegistered(val data: JsonElement) : LeagueSocketEvent()
    data class LeagueStatusChange(val data: JsonElement) : LeagueSocketEvent()
    data class PlayerApplicationStatusUpdate(val data: JsonElement) : LeagueSocketEvent()
    data class MatchLiveScoreUpdate(val data: JsonElement) : LeagueSocketEvent()
    data class GenericUpdate(val type: String, val data: JsonElement) : LeagueSocketEvent()
}

@Singleton
class LeagueWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var currentLeagueId: String? = null
    private var currentToken: String? = null
    private var isConnecting = false
    private var isClosedIntentionally = false

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<LeagueSocketEvent>(replay = 0)
    val events: SharedFlow<LeagueSocketEvent> = _events.asSharedFlow()

    fun connect(leagueId: String, token: String) {
        if (webSocket != null && currentLeagueId == leagueId) return
        disconnect()

        currentLeagueId = leagueId
        currentToken = token
        isConnecting = true
        isClosedIntentionally = false

        val wsUrl = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/league/$leagueId/updates/?token=$token"
        Timber.d("Connecting to League Updates WebSocket at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("League Updates WebSocket connected")
                isConnecting = false
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "League Updates WebSocket failure")
                isConnecting = false
                _isConnected.value = false
                this@LeagueWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("League Updates WebSocket closed: $reason")
                isConnecting = false
                _isConnected.value = false
                this@LeagueWebSocketManager.webSocket = null
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
        currentLeagueId = null
        currentToken = null
        _isConnected.value = false
        isConnecting = false
    }

    private fun scheduleReconnect() {
        if (isClosedIntentionally || currentLeagueId == null || currentToken == null) return
        scope.launch {
            delay(3000)
            if (!isClosedIntentionally && webSocket == null) {
                Timber.d("Retrying League Updates WebSocket connection...")
                connect(currentLeagueId!!, currentToken!!)
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val element = gson.fromJson(text, JsonElement::class.java)
            if (!element.isJsonObject) return
            val json = element.asJsonObject

            val type = json.get("type")?.asString ?: return
            if (type == "ping" || type == "pong") return

            val data = json.get("data") ?: json

            val event = when (type) {
                "standings_update" -> LeagueSocketEvent.StandingsUpdate(data)
                "match_scheduled" -> LeagueSocketEvent.MatchScheduled(data)
                "match_result" -> LeagueSocketEvent.MatchResult(data)
                "team_registered" -> LeagueSocketEvent.TeamRegistered(data)
                "league_status_change" -> LeagueSocketEvent.LeagueStatusChange(data)
                "player_application_status_update" -> LeagueSocketEvent.PlayerApplicationStatusUpdate(data)
                "match_live_score_update" -> LeagueSocketEvent.MatchLiveScoreUpdate(data)
                else -> LeagueSocketEvent.GenericUpdate(type, data)
            }

            scope.launch {
                _events.emit(event)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing League WebSocket message")
        }
    }
}

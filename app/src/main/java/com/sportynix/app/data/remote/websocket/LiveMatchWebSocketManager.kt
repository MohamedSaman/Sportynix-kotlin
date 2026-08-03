package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.sportynix.app.BuildConfig
import com.sportynix.app.data.remote.dto.LiveMatchDto
import com.sportynix.app.domain.model.LiveMatchSnapshot
import com.sportynix.app.domain.model.LiveMatchTeam
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
class LiveMatchWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var isConnecting = false
    private var isClosedIntentionally = false

    private val _matchesState = MutableStateFlow<List<LiveMatchSnapshot>>(emptyList())
    val matchesState: StateFlow<List<LiveMatchSnapshot>> = _matchesState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect() {
        if (webSocket != null || isConnecting) return
        isConnecting = true
        isClosedIntentionally = false

        val wsUrl = BuildConfig.WS_BASE_URL.trimEnd('/') + "/ws/live-matches/"
        Timber.d("Connecting to Live Matches WebSocket at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Live Matches WebSocket connected")
                isConnecting = false
                _isConnected.value = true

                // Subscribe / request initial snapshot
                val filterMsg = JsonObject().apply {
                    addProperty("type", "filter")
                    addProperty("status", "all")
                }
                webSocket.send(gson.toJson(filterMsg))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "Live Matches WebSocket failure")
                isConnecting = false
                _isConnected.value = false
                this@LiveMatchWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("Live Matches WebSocket closed: $reason")
                isConnecting = false
                _isConnected.value = false
                this@LiveMatchWebSocketManager.webSocket = null
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
        _isConnected.value = false
        isConnecting = false
    }

    private fun scheduleReconnect() {
        if (isClosedIntentionally) return
        scope.launch {
            delay(5000)
            if (!isClosedIntentionally && webSocket == null) {
                Timber.d("Retrying Live Matches WebSocket connection...")
                connect()
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val jsonElement = gson.fromJson(text, JsonElement::class.java)
            if (!jsonElement.isJsonObject) return
            val jsonObject = jsonElement.asJsonObject

            val type = jsonObject.get("type")?.asString ?: return

            when (type) {
                "matches_snapshot", "matches_page" -> {
                    val dataObj = jsonObject.get("data")
                    val matchesList = parseMatchesList(dataObj)
                    if (matchesList.isNotEmpty()) {
                        _matchesState.value = matchesList
                    }
                }
                "live_match_added", "live_match_updated", "match_update", "match_live_score_update" -> {
                    val matchData = jsonObject.get("data")
                    val updatedMatch = parseMatchDto(matchData)
                    if (updatedMatch != null) {
                        val currentList = _matchesState.value.toMutableList()
                        val index = currentList.indexOfFirst { it.matchId == updatedMatch.matchId }
                        if (index != -1) {
                            currentList[index] = updatedMatch
                        } else {
                            currentList.add(0, updatedMatch)
                        }
                        _matchesState.value = currentList
                    }
                }
                "match_completed" -> {
                    val matchData = jsonObject.get("data")
                    val completedMatch = parseMatchDto(matchData)
                    if (completedMatch != null) {
                        val currentList = _matchesState.value.toMutableList()
                        val index = currentList.indexOfFirst { it.matchId == completedMatch.matchId }
                        if (index != -1) {
                            currentList[index] = completedMatch.copy(status = "completed")
                        } else {
                            currentList.add(completedMatch.copy(status = "completed"))
                        }
                        _matchesState.value = currentList
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing WebSocket message: $text")
        }
    }

    private fun parseMatchesList(element: JsonElement?): List<LiveMatchSnapshot> {
        if (element == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<LiveMatchDto>>() {}.type
            val dtos: List<LiveMatchDto> = when {
                element.isJsonArray -> gson.fromJson(element, listType)
                element.isJsonObject && element.asJsonObject.has("matches") -> gson.fromJson(element.asJsonObject.get("matches"), listType)
                else -> emptyList()
            }
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing matches list")
            emptyList()
        }
    }

    private fun parseMatchDto(element: JsonElement?): LiveMatchSnapshot? {
        if (element == null) return null
        return try {
            val dto = gson.fromJson(element, LiveMatchDto::class.java)
            dto?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing match DTO")
            null
        }
    }

    private fun LiveMatchDto.toDomain(): LiveMatchSnapshot {
        val t1 = team1
        val t2 = team2

        val formattedScore1 = formatScoreString(t1?.score, t1?.wickets, t1?.overs)
        val formattedScore2 = formatScoreString(t2?.score, t2?.wickets, t2?.overs)

        return LiveMatchSnapshot(
            matchId = matchId,
            leagueId = leagueId.orEmpty(),
            leagueName = leagueName.orEmpty(),
            competitionType = competitionType,
            sourceLabel = sourceLabel,
            sourceReference = sourceReference,
            cricketVariant = cricketVariant,
            sportType = sportType ?: "cricket",
            team1 = LiveMatchTeam(
                id = t1?.id.orEmpty(),
                name = t1?.name ?: "Team 1",
                shortName = t1?.shortName ?: "TM1",
                logo = t1?.logo,
                score = formattedScore1,
                wickets = t1?.wickets,
                overs = t1?.overs
            ),
            team2 = LiveMatchTeam(
                id = t2?.id.orEmpty(),
                name = t2?.name ?: "Team 2",
                shortName = t2?.shortName ?: "TM2",
                logo = t2?.logo,
                score = formattedScore2,
                wickets = t2?.wickets,
                overs = t2?.overs
            ),
            battingTeamId = battingTeamId,
            currentInnings = currentInnings,
            displayMessage = displayMessage,
            chaseStatus = chaseStatus,
            tossText = tossText,
            target = target,
            runsRequired = runsRequired,
            ballsRemaining = ballsRemaining,
            isBreak = isBreak ?: false,
            breakReason = breakReason,
            venue = venue,
            matchType = matchType,
            matchNumber = matchNumber,
            status = status ?: "scheduled",
            result = result,
            winnerName = winner?.name,
            margin = margin,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime
        )
    }

    private fun formatScoreString(score: Any?, wickets: Int?, overs: String?): String {
        val raw = score?.toString() ?: ""
        if (raw.contains("/")) return raw
        val runs = raw.toIntOrNull() ?: 0
        val wText = if (wickets != null) "/$wickets" else ""
        val oText = if (!overs.isNullOrBlank()) " ($overs)" else ""
        return if (runs > 0 || wickets != null || !overs.isNullOrBlank()) "$runs$wText$oText" else ""
    }
}

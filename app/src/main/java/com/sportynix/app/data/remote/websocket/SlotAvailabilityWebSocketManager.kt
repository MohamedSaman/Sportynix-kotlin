package com.sportynix.app.data.remote.websocket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.sportynix.app.data.remote.dto.PermanentSlotAvailability
import com.sportynix.app.data.remote.dto.SlotData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SlotAvailabilityUpdatePayload(
    val date: String?,
    val slots: List<SlotData>
)

data class PermanentAvailabilityUpdatePayload(
    val type: String,
    val availability: Map<String, PermanentSlotAvailability>?,
    val slots: List<SlotData>?
)

@Singleton
class SlotAvailabilityWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var currentSportId: Int? = null
    private var currentToken: String? = null
    private var pendingRequests = mutableListOf<String>()

    private val _connectedFlow = MutableSharedFlow<Unit>(replay = 1)
    val connectedFlow: SharedFlow<Unit> = _connectedFlow.asSharedFlow()

    private val _slotAvailabilityFlow = MutableSharedFlow<SlotAvailabilityUpdatePayload>()
    val slotAvailabilityFlow: SharedFlow<SlotAvailabilityUpdatePayload> = _slotAvailabilityFlow.asSharedFlow()

    private val _permanentAvailabilityFlow = MutableSharedFlow<PermanentAvailabilityUpdatePayload>()
    val permanentAvailabilityFlow: SharedFlow<PermanentAvailabilityUpdatePayload> = _permanentAvailabilityFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

    fun connect(sportId: Int, token: String, baseUrl: String) {
        if (currentSportId == sportId && isConnected) return

        if (currentSportId != sportId) {
            disconnect()
        }

        currentSportId = sportId
        currentToken = token

        val wsScheme = if (baseUrl.startsWith("https://")) "wss://" else "ws://"
        val hostPath = baseUrl.removePrefix("https://").removePrefix("http://")
        val wsUrl = "$wsScheme$hostPath/ws/slots/$sportId/?token=$token"

        Timber.d("[SlotWS] Connecting to $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Origin", baseUrl)
            .build()

        val client = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        webSocket?.close(1000, "Screen destroyed")
        webSocket = null
        isConnected = false
        currentSportId = null
        currentToken = null
        pendingRequests.clear()
    }

    fun requestSlotAvailability(date: String, venueId: Int, excludeCurrentUserHolds: Boolean = false) {
        val msg = mapOf(
            "type" to "get_availability",
            "date" to date,
            "venue_id" to venueId,
            "exclude_current_user_holds" to excludeCurrentUserHolds
        )
        sendOrQueue(msg)
    }

    fun requestPermanentAvailability(selectedDays: List<Int>) {
        val msg = mapOf(
            "type" to "get_permanent_availability",
            "selected_days" to selectedDays
        )
        sendOrQueue(msg)
    }

    private fun sendOrQueue(msgObj: Map<String, Any>) {
        val jsonStr = gson.toJson(msgObj)
        if (isConnected && webSocket != null) {
            webSocket?.send(jsonStr)
        } else {
            synchronized(pendingRequests) {
                pendingRequests.add(jsonStr)
            }
        }
    }

    private fun flushPendingRequests() {
        synchronized(pendingRequests) {
            val queued = pendingRequests.toList()
            pendingRequests.clear()
            for (json in queued) {
                webSocket?.send(json)
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Timber.d("[SlotWS] Connection opened")
                scope.launch {
                    _connectedFlow.emit(Unit)
                }
                flushPendingRequests()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Timber.e(t, "[SlotWS] Connection failure")
                scope.launch {
                    _errorFlow.emit(t.localizedMessage ?: "WebSocket connection failed")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Timber.d("[SlotWS] Connection closed: $reason")
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java) ?: return
            val type = if (json.has("type")) json.get("type").asString else ""

            when (type) {
                "connected" -> {
                    isConnected = true
                    scope.launch {
                        _connectedFlow.emit(Unit)
                    }
                    flushPendingRequests()
                }
                "availability_update", "slot_availability_update", "slot_update" -> {
                    parseSlotUpdate(json)?.let { payload ->
                        scope.launch { _slotAvailabilityFlow.emit(payload) }
                    }
                    scope.launch {
                        _permanentAvailabilityFlow.emit(PermanentAvailabilityUpdatePayload("related_slot_change", null, null))
                    }
                }
                "permanent_availability_update", "permanent_slot_availability_update" -> {
                    parsePermanentUpdate(json, type)?.let { payload ->
                        scope.launch { _permanentAvailabilityFlow.emit(payload) }
                    }
                }
                "permanent_availability_refresh" -> {
                    scope.launch {
                        _permanentAvailabilityFlow.emit(PermanentAvailabilityUpdatePayload("permanent_availability_refresh", null, null))
                    }
                }
                "error" -> {
                    val err = if (json.has("error")) json.get("error").asString else "WebSocket error"
                    scope.launch { _errorFlow.emit(err) }
                }
                else -> {
                    parseSlotUpdate(json)?.let { payload ->
                        scope.launch { _slotAvailabilityFlow.emit(payload) }
                    } ?: run {
                        if (json.has("availability")) {
                            parsePermanentUpdate(json, type)?.let { payload ->
                                scope.launch { _permanentAvailabilityFlow.emit(payload) }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[SlotWS] Error handling incoming text")
        }
    }

    private fun parseSlotUpdate(json: JsonObject): SlotAvailabilityUpdatePayload? {
        val slots = mutableListOf<SlotData>()
        if (json.has("slots") && json.get("slots").isJsonArray) {
            val type = object : TypeToken<List<SlotData>>() {}.type
            val parsed: List<SlotData> = gson.fromJson(json.get("slots"), type)
            slots.addAll(parsed)
        } else if (json.has("slot") && json.get("slot").isJsonObject) {
            val parsed: SlotData = gson.fromJson(json.get("slot"), SlotData::class.java)
            slots.add(parsed)
        }

        if (slots.isEmpty()) return null
        val normalizedSlots = slots.map { normalizeSlotData(it) }
        val date = if (json.has("date")) json.get("date").asString else null
        return SlotAvailabilityUpdatePayload(date, normalizedSlots)
    }

    private fun parsePermanentUpdate(json: JsonObject, fallbackType: String): PermanentAvailabilityUpdatePayload {
        var availabilityMap: Map<String, PermanentSlotAvailability>? = null
        if (json.has("availability") && json.get("availability").isJsonObject) {
            val type = object : TypeToken<Map<String, PermanentSlotAvailability>>() {}.type
            val rawMap: Map<String, PermanentSlotAvailability> = gson.fromJson(json.get("availability"), type)
            val normalized = mutableMapOf<String, PermanentSlotAvailability>()
            rawMap.forEach { (key, value) ->
                normalized[key.replace("-24:00", "-00:00")] = value
            }
            availabilityMap = normalized
        }

        var slots: List<SlotData>? = null
        if (json.has("slots") && json.get("slots").isJsonArray) {
            val type = object : TypeToken<List<SlotData>>() {}.type
            val parsed: List<SlotData> = gson.fromJson(json.get("slots"), type)
            slots = parsed.map { normalizeSlotData(it) }
        }

        return PermanentAvailabilityUpdatePayload(fallbackType, availabilityMap, slots)
    }

    private fun normalizeSlotData(slot: SlotData): SlotData {
        val rawStart = slot.rawStart ?: slot.startTime?.take(5)
        val rawEndCandidate = slot.rawEnd ?: slot.endTime?.take(5)
        val rawEnd = rawEndCandidate?.replace("24:00", "00:00")
        val keyCandidate = slot.slotKey ?: if (rawStart != null && rawEnd != null) "$rawStart-$rawEnd" else null
        val normalizedKey = keyCandidate?.replace("-24:00", "-00:00")

        return slot.copy(
            rawStart = rawStart,
            rawEnd = rawEnd,
            slotKey = normalizedKey
        )
    }
}

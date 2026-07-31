package com.sportynix.app.presentation.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.websocket.PermanentAvailabilityUpdatePayload
import com.sportynix.app.data.remote.websocket.SlotAvailabilityUpdatePayload
import com.sportynix.app.data.remote.websocket.SlotAvailabilityWebSocketManager
import com.sportynix.app.domain.model.TimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class BookingType { NORMAL, PERMANENT }

data class BookingUiState(
    val venueId: Int = 1,
    val sportId: Int = 1,
    val venue: VenueDto? = null,
    val sports: List<VenueSportDto> = emptyList(),
    val selectedSport: VenueSportDto? = null,
    val bookingType: BookingType = BookingType.NORMAL,
    val selectedDate: String = "",
    val selectedDays: List<String> = emptyList(), // e.g. ["Mon", "Wed"]
    val availableSlots: List<SlotData> = emptyList(),
    val selectedSlots: List<SlotData> = emptyList(),
    val permanentAvailability: Map<String, PermanentSlotAvailability> = emptyMap(),
    val isLoadingSlots: Boolean = false,
    val isHoldingSlot: Boolean = false,
    val showPhoneVerificationModal: Boolean = false,
    val userPhone: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingApiService: BookingApiService,
    private val venueApiService: VenueApiService,
    private val slotWebSocketManager: SlotAvailabilityWebSocketManager,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state by mutableStateOf(BookingUiState())
        private set

    val navVenueId: String? = savedStateHandle.get<String>("venueId")
    val navSportId: String? = savedStateHandle.get<String>("sportId")

    init {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val vId = navVenueId?.toIntOrNull() ?: 1
        val sId = navSportId?.toIntOrNull() ?: 1
        state = state.copy(venueId = vId, sportId = sId, selectedDate = todayStr)

        loadVenueAndSports(vId, sId)
        observeWebSocket()
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            slotWebSocketManager.slotAvailabilityFlow.collectLatest { payload: SlotAvailabilityUpdatePayload ->
                if (payload.date == null || payload.date == state.selectedDate) {
                    val updatedSlots = payload.slots
                    state = state.copy(availableSlots = updatedSlots)
                }
            }
        }

        viewModelScope.launch {
            slotWebSocketManager.permanentAvailabilityFlow.collectLatest { payload: PermanentAvailabilityUpdatePayload ->
                payload.availability?.let { map ->
                    state = state.copy(permanentAvailability = map)
                }
            }
        }
    }

    fun initBooking(vId: Int, sId: Int) {
        if (state.venueId == vId && state.sportId == sId && state.venue != null) return
        state = state.copy(venueId = vId, sportId = sId)
        loadVenueAndSports(vId, sId)
    }

    private fun loadVenueAndSports(vId: Int, sId: Int) {
        viewModelScope.launch {
            state = state.copy(isLoadingSlots = true)
            try {
                val venueRes = venueApiService.getVenueById(vId.toString())
                if (venueRes.isSuccessful && venueRes.body() != null) {
                    val venueData = venueRes.body()!!
                    val sportsList = venueData.sports ?: emptyList()
                    val selected = sportsList.firstOrNull { it.id == sId } ?: sportsList.firstOrNull()
                    state = state.copy(
                        venue = venueData,
                        sports = sportsList,
                        selectedSport = selected,
                        sportId = selected?.id ?: sId
                    )
                    connectWebSocket(selected?.id ?: sId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading venue/sports")
            }
            loadSlots()
        }
    }

    fun setBookingType(type: BookingType) {
        if (state.bookingType == type) return
        releaseAllHolds()
        state = state.copy(bookingType = type, selectedSlots = emptyList())
        loadSlots()
    }

    fun setSelectedSport(sport: VenueSportDto) {
        if (state.sportId == sport.id) return
        releaseAllHolds()
        state = state.copy(sportId = sport.id, selectedSport = sport, selectedSlots = emptyList())
        connectWebSocket(sport.id)
        loadSlots()
    }

    fun setSelectedDate(dateStr: String) {
        if (state.selectedDate == dateStr) return
        releaseAllHolds()
        state = state.copy(selectedDate = dateStr, selectedSlots = emptyList())
        loadSlots()
    }

    fun toggleSelectedDay(day: String) {
        releaseAllHolds()
        val current = state.selectedDays.toMutableList()
        if (current.contains(day)) current.remove(day)
        else current.add(day)
        state = state.copy(selectedDays = current, selectedSlots = emptyList())
        if (state.bookingType == BookingType.PERMANENT) {
            fetchPermanentAvailability()
        }
    }

    fun connectWebSocket(sId: Int) {
        viewModelScope.launch {
            val token = sessionManager.accessToken.firstOrNull() ?: ""
            if (token.isNotEmpty()) {
                slotWebSocketManager.connect(sId, token, "https://api.sportynix.com")
            }
        }
    }

    fun loadSlots() {
        if (state.bookingType == BookingType.PERMANENT) {
            fetchPermanentAvailability()
        } else {
            fetchNormalSlots()
        }
    }

    private fun fetchNormalSlots() {
        if (state.selectedDate.isEmpty()) return
        viewModelScope.launch {
            state = state.copy(isLoadingSlots = true)
            try {
                val res = bookingApiService.fetchAvailableSlots(
                    sportId = state.sportId,
                    venueId = state.venueId,
                    date = state.selectedDate
                )
                if (res.isSuccessful && res.body() != null) {
                    val slots = res.body()!!.availableSlots ?: res.body()!!.slots ?: emptyList()
                    state = state.copy(availableSlots = slots, isLoadingSlots = false)
                } else {
                    state = state.copy(isLoadingSlots = false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching available slots")
                state = state.copy(isLoadingSlots = false)
            }
        }
    }

    private fun fetchPermanentAvailability() {
        if (state.selectedDays.isEmpty()) return
        viewModelScope.launch {
            state = state.copy(isLoadingSlots = true)
            try {
                val dayNumbers = state.selectedDays.mapNotNull { dayToNumber(it) }
                val body = mapOf("days" to dayNumbers)
                val res = bookingApiService.fetchPermanentAvailability(state.sportId, body)
                if (res.isSuccessful && res.body() != null) {
                    val parsed = parsePermanentAvailabilityJson(res.body()!!)
                    state = state.copy(permanentAvailability = parsed, isLoadingSlots = false)
                } else {
                    state = state.copy(isLoadingSlots = false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching permanent availability")
                state = state.copy(isLoadingSlots = false)
            }
        }
    }

    fun toggleSlotSelection(slot: SlotData) {
        val slotKey = getSlotKey(slot)
        val isSelected = state.selectedSlots.any { getSlotKey(it) == slotKey }

        if (isSelected) {
            deselectSlot(slot)
        } else {
            if (state.selectedSlots.size >= 4) {
                state = state.copy(errorMessage = "Maximum 4 slots can be selected at once")
                return
            }
            selectSlotOptimistically(slot)
        }
    }

    private fun selectSlotOptimistically(slot: SlotData) {
        val currentSelected = state.selectedSlots + slot
        state = state.copy(selectedSlots = currentSelected)

        viewModelScope.launch {
            try {
                val body = mapOf(
                    "sport_id" to state.sportId,
                    "venue_id" to state.venueId,
                    "date" to state.selectedDate,
                    "start_time" to (slot.startTime ?: slot.rawStart ?: ""),
                    "end_time" to (slot.endTime ?: slot.rawEnd ?: "")
                )
                val res = bookingApiService.holdSlot(body)
                if (!res.isSuccessful) {
                    // Rollback
                    state = state.copy(
                        selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == getSlotKey(slot) },
                        errorMessage = "Slot hold failed. Please select another slot."
                    )
                }
            } catch (e: Exception) {
                state = state.copy(
                    selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == getSlotKey(slot) },
                    errorMessage = "Failed to reserve slot. Network error."
                )
            }
        }
    }

    private fun deselectSlot(slot: SlotData) {
        val slotKey = getSlotKey(slot)
        state = state.copy(selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == slotKey })

        viewModelScope.launch {
            try {
                val body = mapOf(
                    "sport_id" to state.sportId,
                    "venue_id" to state.venueId,
                    "date" to state.selectedDate,
                    "start_time" to (slot.startTime ?: slot.rawStart ?: ""),
                    "end_time" to (slot.endTime ?: slot.rawEnd ?: "")
                )
                bookingApiService.releaseSlot(body)
            } catch (e: Exception) {
                Timber.e(e, "Error releasing slot")
            }
        }
    }

    fun releaseAllHolds() {
        val selected = state.selectedSlots
        if (selected.isEmpty()) return
        val date = state.selectedDate
        val sId = state.sportId
        val vId = state.venueId

        viewModelScope.launch {
            selected.forEach { slot ->
                try {
                    val body = mapOf(
                        "sport_id" to sId,
                        "venue_id" to vId,
                        "date" to date,
                        "start_time" to (slot.startTime ?: slot.rawStart ?: ""),
                        "end_time" to (slot.endTime ?: slot.rawEnd ?: "")
                    )
                    bookingApiService.releaseSlot(body)
                } catch (_: Exception) {}
            }
        }
        state = state.copy(selectedSlots = emptyList())
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    override fun onCleared() {
        releaseAllHolds()
        slotWebSocketManager.disconnect()
        super.onCleared()
    }

    private fun getSlotKey(slot: SlotData): String {
        return slot.slotKey ?: "${slot.startTime ?: slot.rawStart}-${slot.endTime ?: slot.rawEnd}"
    }

    private fun dayToNumber(day: String): Int? {
        return when (day.take(3).lowercase()) {
            "mon" -> 0
            "tue" -> 1
            "wed" -> 2
            "thu" -> 3
            "fri" -> 4
            "sat" -> 5
            "sun" -> 6
            else -> null
        }
    }

    private fun parsePermanentAvailabilityJson(jsonElement: JsonElement): Map<String, PermanentSlotAvailability> {
        return try {
            val mapType = object : TypeToken<Map<String, PermanentSlotAvailability>>() {}.type
            if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject
                if (obj.has("availability")) gson.fromJson(obj.get("availability"), mapType)
                else gson.fromJson(jsonElement, mapType)
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

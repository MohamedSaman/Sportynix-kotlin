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
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.VenueApiService
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.websocket.PermanentAvailabilityUpdatePayload
import com.sportynix.app.data.remote.websocket.SlotAvailabilityUpdatePayload
import com.sportynix.app.data.remote.websocket.SlotAvailabilityWebSocketManager
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.model.TimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    val existingPermanentSlotKeys: Set<String> = emptySet(),
    val isLoadingSlots: Boolean = false,
    val isHoldingSlot: Boolean = false,
    val processingSlotKeys: Set<String> = emptySet(),
    val showPhoneVerificationModal: Boolean = false,
    val userPhone: String? = null,
    val phoneNumber: String = "",
    val phoneOtp: String = "",
    val phoneChallengeId: Int? = null,
    val isPhoneSending: Boolean = false,
    val isPhoneVerifying: Boolean = false,
    val phoneVerificationError: String? = null,
    val proceedToSummary: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingApiService: BookingApiService,
    private val venueApiService: VenueApiService,
    private val slotWebSocketManager: SlotAvailabilityWebSocketManager,
    private val sessionManager: SessionManager,
    private val profileRepository: ProfileRepository,
    private val gson: Gson,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val holdsInFlight = mutableSetOf<String>()
    private val releasesInFlight = mutableSetOf<String>()
    private var handedOffToSummary = false
    private var initializedVenueSport: Pair<Int, Int>? = null

    var state by mutableStateOf(BookingUiState())
        private set

    val navVenueId: String? = savedStateHandle.get<String>("venueId")
    val navSportId: String? = savedStateHandle.get<String>("sportId")

    init {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val vId = navVenueId?.toIntOrNull() ?: 1
        val sId = navSportId?.toIntOrNull() ?: 1
        state = state.copy(venueId = vId, sportId = sId, selectedDate = todayStr)
        initializedVenueSport = vId to sId

        loadVenueAndSports(vId, sId)
        observeWebSocket()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = profileRepository.currentUser.value
                ?: profileRepository.fetchProfile().getOrNull()
            state = state.copy(userPhone = user?.phoneNumber, phoneNumber = user?.phoneNumber.orEmpty())
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            slotWebSocketManager.slotAvailabilityFlow.collectLatest { payload: SlotAvailabilityUpdatePayload ->
                if (payload.date == null || payload.date == state.selectedDate) {
                    val selectedByKey = state.selectedSlots.associateBy(::getSlotKey)
                    val normalizedIncoming = payload.slots.map(::normalizeSlot)
                    val incomingByKey = normalizedIncoming.associateBy(::getSlotKey)
                    val isFullSnapshot = normalizedIncoming.size > 1 || state.availableSlots.size <= 1
                    val source = if (isFullSnapshot) normalizedIncoming else state.availableSlots.map { existing ->
                        incomingByKey[getSlotKey(existing)] ?: existing
                    }
                    val updatedSlots = source.map { incoming ->
                        selectedByKey[getSlotKey(incoming)] ?: incoming
                    }
                    state = state.copy(availableSlots = updatedSlots)
                    syncCurrentUserHolds()
                }
            }
        }

        viewModelScope.launch {
            slotWebSocketManager.permanentAvailabilityFlow.collectLatest { payload: PermanentAvailabilityUpdatePayload ->
                if (payload.type == "related_slot_change" || payload.type == "permanent_availability_refresh") {
                    fetchPermanentAvailability()
                } else payload.availability?.let { map ->
                    state = state.copy(permanentAvailability = normalizePermanentAvailability(map), isLoadingSlots = false)
                }
            }
        }
        viewModelScope.launch {
            slotWebSocketManager.connectedFlow.collectLatest { requestCurrentAvailability() }
        }
    }

    fun initBooking(vId: Int, sId: Int) {
        // The composable also supplies route arguments. The ViewModel init has
        // already started this exact request, so do not issue a second venue
        // or slot request while that first one is in flight.
        if (initializedVenueSport == (vId to sId)) return
        initializedVenueSport = vId to sId
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
        if (!isDateSelectable(dateStr)) {
            state = state.copy(errorMessage = "Bookings are available from today through ${maxBookingDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))}.")
            return
        }
        if (state.selectedDate == dateStr) return
        releaseAllHolds()
        state = state.copy(selectedDate = dateStr, selectedSlots = emptyList())
        loadSlots()
    }

    /** Swift BookingView's dateRange: today...today + one calendar month. */
    fun isDateSelectable(dateStr: String): Boolean = runCatching {
        val date = LocalDate.parse(dateStr)
        !date.isBefore(todayDate()) && !date.isAfter(maxBookingDate())
    }.getOrDefault(false)

    private fun todayDate(): LocalDate = LocalDate.now()

    private fun maxBookingDate(): LocalDate = todayDate().plusMonths(1)

    fun toggleSelectedDay(day: String) {
        releaseAllHolds()
        val updated = if (state.selectedDays.contains(day)) emptyList() else listOf(day)
        state = state.copy(selectedDays = updated, selectedSlots = emptyList(), permanentAvailability = emptyMap())
        if (state.bookingType == BookingType.PERMANENT) {
            fetchExistingPermanentBookings()
            fetchPermanentAvailability()
        }
    }

    fun connectWebSocket(sId: Int) {
        viewModelScope.launch {
            val token = sessionManager.accessToken.firstOrNull() ?: ""
            if (token.isNotEmpty()) {
                slotWebSocketManager.connect(sId, token, BuildConfig.BASE_URL.trimEnd('/'))
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
                    date = state.selectedDate,
                    excludeCurrentUserHolds = true
                )
                if (res.isSuccessful && res.body() != null) {
                    val slots = res.body()!!.availableSlots ?: res.body()!!.slots ?: emptyList()
                    state = state.copy(availableSlots = slots.map(::normalizeSlot), isLoadingSlots = false)
                    syncCurrentUserHolds()
                    requestCurrentAvailability()
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
                    state = state.copy(permanentAvailability = normalizePermanentAvailability(parsed), isLoadingSlots = false)
                    requestCurrentAvailability()
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
            // Availability events can arrive before the local selection list is
            // reconciled. A hold owned by this user is already valid on the
            // backend, so adopt it locally instead of POSTing a duplicate hold.
            if (slot.heldByCurrentUser == true) {
                val normalized = normalizeSlot(slot)
                if (state.selectedSlots.size < 4) {
                    state = state.copy(selectedSlots = state.selectedSlots + normalized, errorMessage = null)
                }
                return
            }
            if (isSlotBlockedForSelection(slot)) {
                state = state.copy(
                    errorMessage = slot.disabledReason?.takeIf { it.isNotBlank() }
                        ?: "This slot is not available right now."
                )
                return
            }
            if (state.bookingType == BookingType.PERMANENT) {
                if (state.selectedDays.isEmpty()) {
                    state = state.copy(errorMessage = "Please select at least one weekday first")
                    return
                }
                val availability = state.permanentAvailability[slotKey]
                if (availability == null) {
                    state = state.copy(errorMessage = "Still checking availability. Please try again in a moment.")
                    return
                }
                if (!availability.available || availability.daysRemaining <= 0) {
                    state = state.copy(errorMessage = "This recurring slot is not available for the selected weekdays.")
                    return
                }
            }
            if (state.selectedSlots.size >= 4) {
                state = state.copy(errorMessage = "Maximum 4 slots can be selected at once")
                return
            }
            selectSlotOptimistically(slot)
        }
    }

    private fun selectSlotOptimistically(slot: SlotData) {
        val key = getSlotKey(slot)
        if (!holdsInFlight.add(key)) return
        val normalized = normalizeSlot(slot).copy(heldByCurrentUser = true, isHeld = true)
        val currentSelected = state.selectedSlots + normalized
        state = state.copy(selectedSlots = currentSelected, processingSlotKeys = state.processingSlotKeys + key)

        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "sport_id" to state.sportId,
                    "date" to state.selectedDate,
                    "start_time" to normalized.rawStart.orEmpty(),
                    "end_time" to normalized.rawEnd.orEmpty()
                )
                if (state.bookingType == BookingType.PERMANENT) {
                    body["is_permanent"] = true
                    body["selected_days"] = state.selectedDays.mapNotNull(::dayToNumber)
                }
                val res = bookingApiService.holdSlot(body)
                if (!res.isSuccessful) {
                    // Rollback
                    state = state.copy(
                        selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == key },
                        errorMessage = parseApiError(res.errorBody()?.string())
                            ?: "Slot hold failed. Please select another slot."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Live hold request failed for $key")
                state = state.copy(
                    selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == key },
                    errorMessage = "Failed to reserve slot: ${e.localizedMessage?.takeIf { it.isNotBlank() } ?: "network error"}"
                )
            } finally {
                holdsInFlight.remove(key)
                state = state.copy(processingSlotKeys = state.processingSlotKeys - key)
                requestCurrentAvailability()
            }
        }
    }

    private fun deselectSlot(slot: SlotData) {
        val slotKey = getSlotKey(slot)
        if (!releasesInFlight.add(slotKey)) return
        val normalized = normalizeSlot(slot)
        state = state.copy(selectedSlots = state.selectedSlots.filterNot { getSlotKey(it) == slotKey })

        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "sport_id" to state.sportId,
                    "date" to state.selectedDate,
                    "start_time" to normalized.rawStart.orEmpty(),
                    "end_time" to normalized.rawEnd.orEmpty()
                )
                if (state.bookingType == BookingType.PERMANENT) {
                    body["is_permanent"] = true
                    body["selected_days"] = state.selectedDays.mapNotNull(::dayToNumber)
                }
                val response = bookingApiService.releaseSlot(body)
                if (!response.isSuccessful) {
                    state = state.copy(errorMessage = "Failed to release slot. Availability will refresh.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error releasing slot")
            } finally {
                releasesInFlight.remove(slotKey)
                requestCurrentAvailability()
            }
        }
    }

    private fun requestCurrentAvailability() {
        if (state.bookingType == BookingType.PERMANENT) {
            slotWebSocketManager.requestPermanentAvailability(state.selectedDays.mapNotNull(::dayToNumber))
        } else {
            slotWebSocketManager.requestSlotAvailability(state.selectedDate, state.venueId, excludeCurrentUserHolds = true)
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
                    val body = mutableMapOf<String, Any>(
                        "sport_id" to sId,
                        "date" to date,
                        "start_time" to (slot.rawStart ?: slot.startTime ?: ""),
                        "end_time" to (slot.rawEnd ?: slot.endTime ?: "")
                    )
                    if (state.bookingType == BookingType.PERMANENT) {
                        body["is_permanent"] = true
                        body["selected_days"] = state.selectedDays.mapNotNull(::dayToNumber)
                    }
                    bookingApiService.releaseSlot(body)
                } catch (_: Exception) {}
            }
        }
        state = state.copy(selectedSlots = emptyList())
    }

    fun clearErrorMessage() {
        state = state.copy(errorMessage = null)
    }

    fun checkPhoneVerificationAndProceed() {
        if (state.selectedSlots.isEmpty()) {
            state = state.copy(errorMessage = "Select at least one available slot to continue.")
            return
        }
        viewModelScope.launch {
            val user = profileRepository.currentUser.value
                ?: profileRepository.fetchProfile().getOrNull()
            if (user?.phoneVerifiedAt != null) {
                state = state.copy(proceedToSummary = true)
            } else {
                state = state.copy(
                    showPhoneVerificationModal = true,
                    phoneNumber = user?.phoneNumber.orEmpty().trim(),
                    phoneOtp = "",
                    phoneChallengeId = null,
                    phoneVerificationError = null
                )
            }
        }
    }

    fun consumeProceedToSummary() {
        handedOffToSummary = true
        state = state.copy(proceedToSummary = false)
    }
    fun updatePhoneNumber(value: String) {
        state = state.copy(phoneNumber = value.filter(Char::isDigit).take(10), phoneVerificationError = null)
    }
    fun updatePhoneOtp(value: String) {
        state = state.copy(phoneOtp = value.filter(Char::isDigit).take(6), phoneVerificationError = null)
    }
    fun dismissPhoneVerification() {
        if (state.isPhoneSending || state.isPhoneVerifying) return
        state = state.copy(showPhoneVerificationModal = false, phoneOtp = "", phoneChallengeId = null, phoneVerificationError = null)
    }
    fun sendPhoneOtp() {
        val phone = state.phoneNumber.trim()
        if (!phone.matches(Regex("^07\\d{8}$"))) {
            state = state.copy(phoneVerificationError = "Please enter a valid phone number in the format 07XXXXXXXX")
            return
        }
        if (state.isPhoneSending) return
        viewModelScope.launch {
            state = state.copy(isPhoneSending = true, phoneVerificationError = null)
            profileRepository.sendPhoneOtp(phone).fold(
                onSuccess = { response ->
                    val challenge = response.challengeId
                    state = if (challenge != null) state.copy(isPhoneSending = false, phoneChallengeId = challenge)
                    else state.copy(isPhoneSending = false, phoneVerificationError = response.error ?: "OTP challenge was not returned")
                },
                onFailure = { state = state.copy(isPhoneSending = false, phoneVerificationError = it.message ?: "Failed to send OTP") }
            )
        }
    }
    fun verifyPhoneOtp() {
        val challenge = state.phoneChallengeId ?: run {
            state = state.copy(phoneVerificationError = "Please send OTP first"); return
        }
        if (!state.phoneOtp.matches(Regex("^\\d{6}$"))) {
            state = state.copy(phoneVerificationError = "Please enter a valid 6-digit OTP"); return
        }
        if (state.isPhoneVerifying) return
        viewModelScope.launch {
            state = state.copy(isPhoneVerifying = true, phoneVerificationError = null)
            profileRepository.verifyPhoneOtp(challenge, state.phoneOtp).fold(
                onSuccess = {
                    state = state.copy(isPhoneVerifying = false, showPhoneVerificationModal = false,
                        phoneOtp = "", phoneChallengeId = null, proceedToSummary = true)
                },
                onFailure = { state = state.copy(isPhoneVerifying = false, phoneVerificationError = it.message ?: "Invalid OTP") }
            )
        }
    }

    override fun onCleared() {
        if (!handedOffToSummary) releaseAllHolds()
        slotWebSocketManager.disconnect()
        super.onCleared()
    }

    private fun getSlotKey(slot: SlotData): String {
        val start = normalizeApiTime(slot.rawStart ?: slot.startTime)
        val end = normalizeApiTime(slot.rawEnd ?: slot.endTime)
        return if (start != null && end != null) "$start-$end"
        else slot.slotKey.orEmpty().trim().replace("-24:00", "-00:00")
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

    private fun normalizePermanentAvailability(source: Map<String, PermanentSlotAvailability>) =
        source.mapKeys { (key, _) -> key.replace("-24:00", "-00:00") }

    private fun normalizeSlot(slot: SlotData): SlotData {
        // Preserve raw server values exactly for hold/release requests. Only
        // derive HH:mm values from a display label when raw fields are absent.
        val rawStart = slot.rawStart?.trim()?.replace("24:00", "00:00")
            ?: normalizeApiTime(slot.startTime)
        val rawEnd = slot.rawEnd?.trim()?.replace("24:00", "00:00")
            ?: normalizeApiTime(slot.endTime)
        val key = if (rawStart != null && rawEnd != null) "${normalizeApiTime(rawStart) ?: rawStart}-${normalizeApiTime(rawEnd) ?: rawEnd}"
            else slot.slotKey?.replace("-24:00", "-00:00")
        val locallyPast = runCatching {
            LocalDate.parse(state.selectedDate).isEqual(LocalDate.now()) &&
                LocalTime.parse(normalizeApiTime(rawStart) ?: rawStart).isBefore(LocalTime.now())
        }.getOrDefault(false)
        return slot.copy(rawStart = rawStart, rawEnd = rawEnd, slotKey = key,
            isPastTime = slot.isPastTime == true || locallyPast)
    }

    private fun normalizeApiTime(value: String?): String? {
        val input = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized24Hour = input.replace("24:00", "00:00")
        listOf("H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss", "h:mm a", "hh:mm a").forEach { pattern ->
            runCatching {
                val parsed = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(normalized24Hour)
                if (parsed != null) return SimpleDateFormat("HH:mm", Locale.US).format(parsed)
            }
        }
        return null
    }

    private fun availableCourtCount(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        null -> null
        else -> Regex("\\d+").find(value.toString())?.value?.toIntOrNull()
    }

    private fun isSlotBlockedForSelection(slot: SlotData): Boolean {
        if (slot.isPastTime == true || slot.isFullyBooked == true || slot.available == false) return true
        if (slot.heldByCurrentUser == true) return false
        val hasCapacity = (availableCourtCount(slot.availableCourts) ?: 0) > 0
        if (slot.available == false && !hasCapacity) return true
        return (slot.isHeld == true || slot.isPaymentReserved == true) && !hasCapacity
    }

    private fun parseApiError(body: String?): String? = runCatching {
        val json = gson.fromJson(body, JsonElement::class.java)?.asJsonObject ?: return@runCatching null
        listOf("error", "detail", "message").firstNotNullOfOrNull { key ->
            json.get(key)?.takeUnless { it.isJsonNull }?.asString?.takeIf(String::isNotBlank)
        }
    }.getOrNull()

    private fun syncCurrentUserHolds() {
        val selectedKeys = state.selectedSlots.map(::getSlotKey).toSet()
        val held = state.availableSlots.filter { it.heldByCurrentUser == true && getSlotKey(it) !in selectedKeys }
        if (held.isNotEmpty()) state = state.copy(selectedSlots = state.selectedSlots + held)
    }

    private fun fetchExistingPermanentBookings() {
        if (state.bookingType != BookingType.PERMANENT || state.selectedDays.isEmpty()) return
        val selectedDayNumbers = state.selectedDays.mapNotNull(::dayToNumber).toSet()
        viewModelScope.launch {
            runCatching { bookingApiService.getPermanentBookings(state.sportId) }.onSuccess { response ->
                if (!response.isSuccessful) return@onSuccess
                val root = response.body() ?: return@onSuccess
                val array = when {
                    root.isJsonArray -> root.asJsonArray
                    root.isJsonObject && root.asJsonObject.has("results") -> root.asJsonObject["results"].asJsonArray
                    else -> return@onSuccess
                }
                val keys = buildSet {
                    array.forEach { element ->
                        val obj = element.asJsonObject
                        val config = obj.getAsJsonObject("recurring_config") ?: return@forEach
                        val days = config.getAsJsonArray("selected_days")?.mapNotNull { it.asInt }?.toSet().orEmpty()
                        if (days.intersect(selectedDayNumbers).isNotEmpty()) {
                            val start = obj.get("start_time")?.asString ?: return@forEach
                            val end = obj.get("end_time")?.asString?.replace("24:00", "00:00") ?: return@forEach
                            add("$start-$end")
                        }
                    }
                }
                state = state.copy(existingPermanentSlotKeys = keys)
            }
        }
    }

    fun isSelectedDayClosed(): Boolean {
        val day = runCatching {
            LocalDate.parse(state.selectedDate).format(DateTimeFormatter.ofPattern("EEEE", Locale.US)).lowercase(Locale.US)
        }.getOrDefault("")
        return state.venue?.openingHours?.get(day)?.isClosed == true ||
            state.selectedSport?.openingHours?.get(day)?.isClosed == true
    }

    private fun openingRange(): IntRange {
        val days = if (state.bookingType == BookingType.PERMANENT) state.selectedDays.map { it.lowercase(Locale.US) }
        else listOf(runCatching {
            LocalDate.parse(state.selectedDate).format(DateTimeFormatter.ofPattern("EEEE", Locale.US)).lowercase(Locale.US)
        }.getOrDefault(""))
        var earliest = 24
        var latest = 0
        var anyOpenDay = false
        var anyHoursConfigured = false
        days.forEach { day ->
            val venueHours = state.venue?.openingHours?.get(day)
            val sportHours = state.selectedSport?.openingHours?.get(day)
            if (venueHours != null || sportHours != null) anyHoursConfigured = true
            if (venueHours?.isClosed == true || sportHours?.isClosed == true) return@forEach
            val effective = sportHours ?: venueHours ?: return@forEach
            val open = effective.open?.substringBefore(':')?.toIntOrNull() ?: return@forEach
            val closeRaw = effective.close?.substringBefore(':')?.toIntOrNull() ?: return@forEach
            earliest = minOf(earliest, open)
            latest = maxOf(latest, if (closeRaw == 0) 24 else closeRaw)
            anyOpenDay = true
        }
        return when {
            anyOpenDay && earliest < latest -> earliest until latest
            anyHoursConfigured -> IntRange.EMPTY
            else -> 6 until 24
        }
    }

    fun visibleSlots(): List<SlotData> {
        val range = openingRange()
        if (range.isEmpty()) return emptyList()
        if (state.bookingType == BookingType.NORMAL) {
            return state.availableSlots.map(::normalizeSlot).filter { slot ->
                slot.rawStart?.substringBefore(':')?.toIntOrNull()?.let(range::contains) == true
            }.sortedBy { it.rawStart }
        }
        if (state.selectedDays.isEmpty()) return emptyList()
        return range.mapNotNull { hour ->
            val start = String.format(Locale.US, "%02d:00", hour)
            val end = if (hour + 1 == 24) "00:00" else String.format(Locale.US, "%02d:00", hour + 1)
            val key = "$start-$end"
            val availability = state.permanentAvailability[key] ?: return@mapNotNull null
            SlotData(rawStart = start, rawEnd = end, slotKey = key,
                startTime = formatTo12Hour(start), endTime = formatTo12Hour(end),
                available = key !in state.existingPermanentSlotKeys && availability.available && availability.daysRemaining > 0,
                isFullyBooked = key in state.existingPermanentSlotKeys || !availability.available || availability.daysRemaining == 0,
                disabledReason = if (key in state.existingPermanentSlotKeys) "Already booked by you"
                else if (!availability.available || availability.daysRemaining == 0)
                    "Fully booked (${availability.bookedCount}/${availability.totalDaysChecked} days)" else null)
        }
    }

    private fun formatTo12Hour(time: String): String {
        val hour = time.substringBefore(':').toIntOrNull() ?: return time
        val minute = time.substringAfter(':', "00")
        val suffix = if (hour >= 12) "PM" else "AM"
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.US, "%02d:%s %s", h12, minute, suffix)
    }
}

package com.sportynix.app.presentation.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.*
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.ChallengeApiService
import com.sportynix.app.data.remote.dto.SlotData
import com.sportynix.app.data.remote.websocket.ChallengeWebSocketManager
import com.sportynix.app.data.remote.websocket.SlotAvailabilityWebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

enum class ChallengeTab { FIND_TEAMS, MY_CHALLENGES }
enum class ChallengeSection { INCOMING, SENT, HISTORY }
enum class ChallengeStep { MY_TEAM, OPPONENT, SPORT, VENUE, SLOT, REVIEW }
enum class ChallengeBookingType { NORMAL, PERMANENT }

data class ChallengeTeamUi(val id: Int, val name: String, val logo: String? = null, val members: Int = 0, val location: String = "", val sport: String = "", val description: String = "")
data class ChallengeSportUi(val id: Int, val name: String, val price: Double = 0.0, val image: String? = null)
data class ChallengeVenueUi(val id: Int, val name: String, val address: String = "", val city: String = "")
data class ChallengeUi(
    val id: Int, val challenger: String, val challenged: String, val challengerId: Int? = null, val challengedId: Int? = null,
    val sport: String = "", val venue: String = "", val date: String? = null, val start: String? = null, val end: String? = null,
    val status: String = "pending", val stake: Double = 0.0, val canAccept: Boolean = false, val canDecline: Boolean = false,
    val canCancel: Boolean = false, val chatId: String? = null, val raw: JsonObject? = null
)
data class ChallengeSlotUi(val key: String, val slot: SlotData, val selected: Boolean = false, val processing: Boolean = false)

data class ChallengeState(
    val tab: ChallengeTab = ChallengeTab.FIND_TEAMS, val section: ChallengeSection = ChallengeSection.INCOMING,
    val loading: Boolean = false, val refreshing: Boolean = false, val search: String = "", val myTeams: List<ChallengeTeamUi> = emptyList(),
    val opponents: List<ChallengeTeamUi> = emptyList(), val sports: List<ChallengeSportUi> = emptyList(), val venues: List<ChallengeVenueUi> = emptyList(),
    val sent: List<ChallengeUi> = emptyList(), val incoming: List<ChallengeUi> = emptyList(), val history: List<ChallengeUi> = emptyList(),
    val opponentPage: Int = 1, val hasMoreOpponents: Boolean = true, val selectedTeam: ChallengeTeamUi? = null, val selectedOpponent: ChallengeTeamUi? = null,
    val selectedSport: ChallengeSportUi? = null, val selectedVenue: ChallengeVenueUi? = null, val bookingType: ChallengeBookingType = ChallengeBookingType.NORMAL,
    val date: String = LocalDate.now().toString(), val selectedDays: List<Int> = emptyList(), val slots: List<ChallengeSlotUi> = emptyList(),
    val step: ChallengeStep = ChallengeStep.MY_TEAM, val creating: Boolean = false, val loadingSlots: Boolean = false, val submitting: Boolean = false,
    val selectedSlot: ChallengeSlotUi? = null, val selectedHoldId: String? = null, val selectedDetail: ChallengeUi? = null, val selectedTeamDetail: ChallengeTeamUi? = null,
    val message: String? = null, val error: String? = null
)

sealed interface ChallengeEvent { data class OpenChat(val conversationId: String): ChallengeEvent }

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val api: ChallengeApiService,
    private val bookingApi: BookingApiService,
    private val slotWs: SlotAvailabilityWebSocketManager,
    private val challengeWs: ChallengeWebSocketManager,
    private val session: SessionManager,
    private val gson: Gson
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<ChallengeEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    private val running = mutableSetOf<String>()
    private var searchJob: Job? = null
    private val slotBusy = AtomicBoolean(false)

    init {
        loadLists()
        viewModelScope.launch { session.getAccessTokenSync()?.let { challengeWs.connect(it, BuildConfig.BASE_URL) } }
        viewModelScope.launch {
            slotWs.slotAvailabilityFlow.collect { update ->
                val current = _state.value
                if (current.selectedSport != null && update.slots.isNotEmpty()) {
                    _state.value = current.copy(slots = update.slots.map { ChallengeSlotUi(slotKey(it), it, current.selectedSlot?.key == slotKey(it)) }, loadingSlots = false)
                }
            }
        }
        viewModelScope.launch { challengeWs.events.collect { loadLists(silent = true) } }
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null, error = null) }
    fun setTab(tab: ChallengeTab) { _state.value = _state.value.copy(tab = tab); if (tab == ChallengeTab.MY_CHALLENGES) loadChallenges() else loadOpponents() }
    fun setSection(section: ChallengeSection) { _state.value = _state.value.copy(section = section); loadChallenges() }
    fun refresh() = loadLists()
    fun search(value: String) {
        _state.value = _state.value.copy(search = value, opponentPage = 1, hasMoreOpponents = true)
        searchJob?.cancel(); searchJob = viewModelScope.launch { delay(350); loadOpponents() }
    }
    fun loadMoreOpponents() { if (_state.value.hasMoreOpponents && !running.contains("opponents")) loadOpponents(_state.value.opponentPage + 1) }

    fun openCreate() { _state.value = _state.value.copy(creating = true, step = ChallengeStep.MY_TEAM, selectedTeam = null, selectedOpponent = null, selectedSport = null, selectedVenue = null, slots = emptyList(), selectedSlot = null, selectedHoldId = null) }
    fun closeCreate() { viewModelScope.launch { releaseHold() }; _state.value = _state.value.copy(creating = false, selectedHoldId = null) }
    fun nextStep() {
        val s = _state.value
        when (s.step) {
            ChallengeStep.MY_TEAM -> if (s.selectedTeam != null) _state.value = s.copy(step = ChallengeStep.OPPONENT) else fail("Select your team")
            ChallengeStep.OPPONENT -> if (s.selectedOpponent != null) _state.value = s.copy(step = ChallengeStep.SPORT) else fail("Select an opponent")
            ChallengeStep.SPORT -> if (s.selectedSport != null) { loadVenues(); _state.value = s.copy(step = ChallengeStep.VENUE) } else fail("Select a sport")
            ChallengeStep.VENUE -> if (s.selectedVenue != null) { _state.value = s.copy(step = ChallengeStep.SLOT); loadSlots() } else fail("Select a venue")
            ChallengeStep.SLOT -> if (s.selectedSlot != null || s.bookingType == ChallengeBookingType.PERMANENT) _state.value = s.copy(step = ChallengeStep.REVIEW) else fail("Select a time slot")
            ChallengeStep.REVIEW -> submit()
        }
    }
    fun previousStep() { val step = _state.value.step; _state.value = _state.value.copy(step = when(step) { ChallengeStep.OPPONENT -> ChallengeStep.MY_TEAM; ChallengeStep.SPORT -> ChallengeStep.OPPONENT; ChallengeStep.VENUE -> ChallengeStep.SPORT; ChallengeStep.SLOT -> ChallengeStep.VENUE; ChallengeStep.REVIEW -> ChallengeStep.SLOT; else -> ChallengeStep.MY_TEAM }) }
    fun selectTeam(team: ChallengeTeamUi) { _state.value = _state.value.copy(selectedTeam = team) }
    fun selectOpponent(team: ChallengeTeamUi) { _state.value = _state.value.copy(selectedOpponent = team) }
    fun selectSport(sport: ChallengeSportUi) { _state.value = _state.value.copy(selectedSport = sport, selectedVenue = null, slots = emptyList()); loadVenues() }
    fun selectVenue(venue: ChallengeVenueUi) { _state.value = _state.value.copy(selectedVenue = venue); loadSlots() }
    fun setBookingType(type: ChallengeBookingType) { _state.value = _state.value.copy(bookingType = type); if (type == ChallengeBookingType.PERMANENT) _state.value = _state.value.copy(selectedDays = emptyList()) }
    fun setDate(date: LocalDate) { _state.value = _state.value.copy(date = date.toString()); loadSlots() }
    fun toggleDay(day: Int) { val days = _state.value.selectedDays; _state.value = _state.value.copy(selectedDays = if (day in days) days - day else days + day); if (_state.value.bookingType == ChallengeBookingType.PERMANENT) requestPermanent() }
    fun selectSlot(item: ChallengeSlotUi) {
        val s = _state.value; val slot = item.slot; val key = item.key
        if (item.processing || slot.isPastTime == true || slot.isFullyBookedForChallenge() || slot.isHeld == true || slot.available == false) return
        if (!slotBusy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                if (s.selectedSlot?.key == key) { releaseHold(); _state.value = _state.value.copy(selectedSlot = null, selectedHoldId = null, slots = s.slots.map { it.copy(selected = false) }); return@launch }
                releaseHold()
                _state.value = _state.value.copy(slots = _state.value.slots.map { it.copy(processing = it.key == key) })
                val sport = _state.value.selectedSport ?: return@launch
                val venue = _state.value.selectedVenue ?: return@launch
                val body = mapOf<String, Any>("sport_id" to sport.id, "venue_id" to venue.id, "date" to _state.value.date, "start_time" to (item.slot.rawStart ?: item.slot.startTime.orEmpty()), "end_time" to (item.slot.rawEnd ?: item.slot.endTime.orEmpty()), "for_challenge" to true)
                val response = bookingApi.holdSlot(body)
                if (!response.isSuccessful) throw IllegalStateException(response.errorBody()?.string() ?: "Unable to reserve slot")
                val holdId = response.body()?.string()?.let { parseHoldId(it) }
                val chosen = item.copy(selected = true, processing = false)
                _state.value = _state.value.copy(selectedSlot = chosen, selectedHoldId = holdId, slots = _state.value.slots.map { if (it.key == key) chosen else it.copy(selected = false, processing = false) }, error = null)
            } catch (e: Exception) { _state.value = _state.value.copy(error = e.message ?: "Failed to reserve slot", slots = _state.value.slots.map { it.copy(processing = false) }) }
            finally { slotBusy.set(false) }
        }
    }
    fun openDetails(challenge: ChallengeUi) { _state.value = _state.value.copy(selectedDetail = challenge) }
    fun openChat(challenge: ChallengeUi) { challenge.chatId?.let { _events.tryEmit(ChallengeEvent.OpenChat(it)) } }
    fun closeDetails() { _state.value = _state.value.copy(selectedDetail = null) }
    fun openTeam(team: ChallengeTeamUi) { _state.value = _state.value.copy(selectedTeamDetail = team) }
    fun closeTeam() { _state.value = _state.value.copy(selectedTeamDetail = null) }
    fun accept(challenge: ChallengeUi) = action("accept-${challenge.id}") { val response = api.accept(challenge.id); ensure(response); val id = extractChatId(response.body()); _state.value = _state.value.copy(message = "Challenge accepted", selectedDetail = null); loadChallenges(silent = true); id?.let { _events.tryEmit(ChallengeEvent.OpenChat(it)) } }
    fun decline(challenge: ChallengeUi) = action("decline-${challenge.id}") { ensure(api.decline(challenge.id)); _state.value = _state.value.copy(message = "Challenge declined", selectedDetail = null); loadChallenges(silent = true) }
    fun cancel(challenge: ChallengeUi) = action("cancel-${challenge.id}") { ensure(api.cancel(challenge.id)); _state.value = _state.value.copy(message = "Challenge cancelled", selectedDetail = null); loadChallenges(silent = true) }

    private fun submit() = action("submit") {
        val s = _state.value; val team = s.selectedTeam ?: return@action fail("Select your team"); val opponent = s.selectedOpponent ?: return@action fail("Select an opponent"); val sport = s.selectedSport ?: return@action fail("Select a sport")
        val body = JsonObject().apply { addProperty("challenger_id", team.id); addProperty("challenged_id", opponent.id); addProperty("sport_id", sport.id); addProperty("stake", sport.price)
            s.selectedVenue?.let { addProperty("venue_id", it.id) }; if (s.selectedSlot != null) { addProperty("date", s.date); addProperty("start_time", s.selectedSlot.slot.rawStart ?: s.selectedSlot.slot.startTime); addProperty("end_time", s.selectedSlot.slot.rawEnd ?: s.selectedSlot.slot.endTime) }; addProperty("booking_type", s.bookingType.name.lowercase()); if (s.selectedDays.isNotEmpty()) add("selected_days", gson.toJsonTree(s.selectedDays)); s.selectedHoldId?.let { addProperty("hold_id", it) } }
        ensure(api.create(body)); releaseHold(); _state.value = _state.value.copy(creating = false, message = "Challenge sent successfully", selectedHoldId = null); loadChallenges(silent = true)
    }
    private fun loadLists(silent: Boolean = false) { loadTeams(); loadOpponents(); loadSports(); loadChallenges(silent) }
    private fun loadTeams() = request("teams") { val r = api.myTeams(); ensure(r); _state.value = _state.value.copy(myTeams = parseTeams(r.body()!!), loading = false) }
    private fun loadOpponents(page: Int = 1) = request("opponents") { val r = api.opponents(page, 10, _state.value.search.ifBlank { null }); ensure(r); val list = parseTeams(r.body()!!); val merged = if (page == 1) list else (_state.value.opponents + list).distinctBy { it.id }; _state.value = _state.value.copy(opponents = merged, opponentPage = page, hasMoreOpponents = list.size >= 10, loading = false) }
    private fun loadChallenges(silent: Boolean = false) = request("challenges", silent) { val sent = api.sent(); val incoming = api.incoming(); val history = api.history(); _state.value = _state.value.copy(sent = if (sent.isSuccessful) parseChallenges(sent.body()) else emptyList(), incoming = if (incoming.isSuccessful) parseChallenges(incoming.body()) else emptyList(), history = if (history.isSuccessful) parseChallenges(history.body()) else emptyList(), loading = false, refreshing = false) }
    private fun loadVenues() = request("venues") { val sport = _state.value.selectedSport ?: return@request; val r = api.venues(sport.id); if (r.isSuccessful) _state.value = _state.value.copy(venues = parseVenues(r.body()!!)) }
    private fun loadSports() = request("sports", silent = true) { val r = api.sports(); ensure(r); _state.value = _state.value.copy(sports = array(r.body()!!).mapNotNull { e -> if (!e.isJsonObject) null else { val o = e.asJsonObject; ChallengeSportUi(o.int("id") ?: return@mapNotNull null, o.string("name") ?: "Sport", o.double("price") ?: o.double("hourly_rate") ?: 0.0, o.string("image") ?: o.string("image_url")) } }) }
    private fun loadSlots() = request("slots") { val sport = _state.value.selectedSport ?: return@request; val venue = _state.value.selectedVenue ?: return@request; _state.value = _state.value.copy(loadingSlots = true, selectedSlot = null, selectedHoldId = null); val r = bookingApi.fetchAvailableSlots(sport.id, venue.id, _state.value.date, false); if (!r.isSuccessful) throw IllegalStateException("Unable to load slots"); val items = (r.body()?.availableSlots.orEmpty().ifEmpty { r.body()?.slots.orEmpty() }).map { ChallengeSlotUi(slotKey(it), it) }; _state.value = _state.value.copy(slots = items, loadingSlots = false); session.getAccessTokenSync()?.let { slotWs.connect(sport.id, it, BuildConfig.BASE_URL); slotWs.requestSlotAvailability(_state.value.date, venue.id) } }
    private fun requestPermanent() { val sport = _state.value.selectedSport ?: return; if (_state.value.selectedDays.isEmpty()) return; viewModelScope.launch { val r = bookingApi.fetchPermanentAvailability(sport.id, mapOf("selected_days" to _state.value.selectedDays)); if (r.isSuccessful) { val arr = array(r.body()!!); _state.value = _state.value.copy(slots = arr.mapNotNull { runCatching { ChallengeSlotUi(slotKey(gson.fromJson(it, SlotData::class.java)), gson.fromJson(it, SlotData::class.java)) }.getOrNull() }, loadingSlots = false) } } }
    private suspend fun releaseHold() { val s = _state.value; val slot = s.selectedSlot?.slot ?: return; val sport = s.selectedSport ?: return; val venue = s.selectedVenue ?: return; val body = mapOf<String, Any>("sport_id" to sport.id, "venue_id" to venue.id, "date" to s.date, "start_time" to (slot.rawStart ?: slot.startTime.orEmpty()), "end_time" to (slot.rawEnd ?: slot.endTime.orEmpty())); runCatching { bookingApi.releaseSlot(body) } }
    private fun <T> action(key: String, block: suspend () -> T) { if (!running.add(key)) return; viewModelScope.launch { try { block() } catch (e: Exception) { fail(e.message ?: "Request failed") } finally { running.remove(key) } } }
    private fun request(key: String, silent: Boolean = false, block: suspend () -> Unit) { if (!running.add(key)) return; if (!silent) _state.value = _state.value.copy(loading = true); viewModelScope.launch { try { block() } catch (e: Exception) { _state.value = _state.value.copy(loading = false, refreshing = false, error = e.message ?: "Request failed") } finally { running.remove(key) } } }
    private fun ensure(response: retrofit2.Response<*>) { if (!response.isSuccessful) throw IllegalStateException(response.errorBody()?.string() ?: "Request failed (${response.code()})") }
    private fun fail(message: String) { _state.value = _state.value.copy(error = message) }

    private fun parseTeams(root: JsonElement): List<ChallengeTeamUi> = array(root).mapNotNull { e -> if (!e.isJsonObject) null else { val o = e.asJsonObject; ChallengeTeamUi(o.int("id") ?: return@mapNotNull null, o.string("name") ?: "Team", o.string("logo") ?: o.string("logo_url") ?: o.string("image"), o.int("members_count") ?: o.int("member_count") ?: 0, o.string("location") ?: o.string("city") ?: "", o.string("sport_name") ?: "", o.string("description") ?: "") } }
    private fun parseVenues(root: JsonElement): List<ChallengeVenueUi> = array(root).mapNotNull { e -> if (!e.isJsonObject) null else { val o = e.asJsonObject; ChallengeVenueUi(o.int("id") ?: return@mapNotNull null, o.string("name") ?: "Venue", o.string("address") ?: "", o.string("city") ?: o.string("location") ?: "") } }
    private fun parseChallenges(root: JsonElement?): List<ChallengeUi> = root?.let { array(it).mapNotNull { e -> if (!e.isJsonObject) null else parseChallenge(e.asJsonObject) } }.orEmpty()
    private fun parseChallenge(o: JsonObject): ChallengeUi { val challenger = o.obj("challenger"); val challenged = o.obj("challenged"); val sport = o.obj("sport"); val venue = o.obj("venue"); val status = o.string("status") ?: "pending"; return ChallengeUi(o.int("id") ?: 0, challenger?.string("name") ?: o.string("challenger_name") ?: "My team", challenged?.string("name") ?: o.string("challenged_name") ?: "Opponent", challenger?.int("id") ?: o.int("challenger_id"), challenged?.int("id") ?: o.int("challenged_id"), sport?.string("name") ?: o.string("sport_name") ?: o.string("sport") ?: "", venue?.string("name") ?: o.string("venue_name") ?: o.string("venue") ?: "", o.string("match_date") ?: o.string("date"), o.string("match_time") ?: o.string("start_time"), o.string("end_time"), status, o.double("stake") ?: 0.0, o.bool("can_accept") ?: status == "pending", o.bool("can_decline") ?: status == "pending", o.bool("can_cancel") ?: status == "pending", extractChatId(o), o) }
    private fun extractChatId(e: JsonElement?): String? { val o = e?.takeIf { it.isJsonObject }?.asJsonObject ?: return null; return o.string("chat_id") ?: o.obj("chat")?.string("id") ?: o.obj("rivalry_chat")?.string("id") }
    private fun parseHoldId(text: String): String? = runCatching { gson.fromJson(text, JsonObject::class.java).string("hold_id") ?: gson.fromJson(text, JsonObject::class.java).string("id") }.getOrNull()
    private fun array(e: JsonElement): List<JsonElement> = when { e.isJsonArray -> e.asJsonArray.toList(); e.isJsonObject && e.asJsonObject.get("results")?.isJsonArray == true -> e.asJsonObject.getAsJsonArray("results").toList(); e.isJsonObject && e.asJsonObject.get("data")?.isJsonArray == true -> e.asJsonObject.getAsJsonArray("data").toList(); else -> emptyList() }
    private fun slotKey(s: SlotData): String = s.slotKey ?: "${s.rawStart ?: s.startTime}-${s.rawEnd ?: s.endTime}"
    private fun SlotData.isFullyBookedForChallenge(): Boolean = isFullyBooked == true || (availableCourts is Number && totalCourts is Number && (availableCourts as Number).toInt() <= 0)
    private fun JsonObject.string(name: String): String? = get(name)?.takeIf { !it.isJsonNull }?.asString
    private fun JsonObject.int(name: String): Int? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
    private fun JsonObject.double(name: String): Double? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }
    private fun JsonObject.bool(name: String): Boolean? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asBoolean }.getOrNull() }
    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
    override fun onCleared() { challengeWs.disconnect(); slotWs.disconnect(); super.onCleared() }
}

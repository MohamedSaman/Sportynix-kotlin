package com.sportynix.app.presentation.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.sportynix.app.BuildConfig
import com.sportynix.app.core.datastore.SessionManager
import com.sportynix.app.data.remote.api.BookingApiService
import com.sportynix.app.data.remote.api.ChallengeApiService
import com.sportynix.app.data.remote.api.TeamApiService
import com.sportynix.app.data.remote.api.ChatApiService
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
import timber.log.Timber

enum class ChallengeTab { FIND_TEAMS, MY_CHALLENGES }
enum class ChallengeSection { INCOMING, SENT, HISTORY }
enum class ChallengeStep { MY_TEAM, OPPONENT, SPORT, REVIEW }
enum class ChallengeBookingType { NORMAL, PERMANENT }

data class ChallengeTeamUi(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val members: Int = 0,
    val location: String = "",
    val sport: String = "",
    val description: String = ""
)

data class ChallengeSportUi(
    val id: Int,
    val name: String,
    val price: Double = 0.0,
    val image: String? = null
)

data class ChallengeVenueUi(
    val id: Int,
    val name: String,
    val address: String = "",
    val city: String = ""
)

data class ChallengeUi(
    val id: Int,
    val challenger: String,
    val challenged: String,
    val challengerId: Int? = null,
    val challengedId: Int? = null,
    val sport: String = "",
    val venue: String = "",
    val date: String? = null,
    val start: String? = null,
    val end: String? = null,
    val status: String = "pending",
    val stake: Double = 0.0,
    val canAccept: Boolean = false,
    val canDecline: Boolean = false,
    val canCancel: Boolean = false,
    val chatId: String? = null,
    val raw: JsonObject? = null
)

data class ChallengeSlotUi(
    val key: String,
    val slot: SlotData,
    val selected: Boolean = false,
    val processing: Boolean = false
)

data class ChallengeStatsUi(
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val noResults: Int = 0,
    val winPercentage: Double = 0.0
)

data class MatchScoreUi(
    val runs: Int = 0,
    val wickets: Int = 0,
    val overs: String = ""
)

data class RecentMatchUi(
    val id: String,
    val opponentId: Int?,
    val opponentName: String,
    val opponentLogo: String?,
    val result: String, // 'win' | 'loss' | 'no_result'
    val matchDate: String?,
    val venue: String?,
    val margin: String?,
    val teamScore: MatchScoreUi?,
    val opponentScore: MatchScoreUi?
)

data class TeamDetailUi(
    val id: Int,
    val name: String,
    val description: String = "",
    val membersCount: Int = 0,
    val maxMembers: Int = 0,
    val logo: String? = null,
    val coverImage: String? = null,
    val location: String? = null,
    val teamType: String? = null,
    val skillLevel: String? = null,
    val isPublic: Boolean = true,
    val challengeStats: ChallengeStatsUi? = null
)

data class ChallengeState(
    val tab: ChallengeTab = ChallengeTab.FIND_TEAMS,
    val section: ChallengeSection = ChallengeSection.INCOMING,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val search: String = "",
    val myTeams: List<ChallengeTeamUi> = emptyList(),
    val opponents: List<ChallengeTeamUi> = emptyList(),
    val sports: List<ChallengeSportUi> = emptyList(),
    val venues: List<ChallengeVenueUi> = emptyList(),
    val sent: List<ChallengeUi> = emptyList(),
    val incoming: List<ChallengeUi> = emptyList(),
    val history: List<ChallengeUi> = emptyList(),
    val myTeamsPage: Int = 1,
    val hasMoreMyTeams: Boolean = true,
    val loadingMyTeams: Boolean = false,
    val opponentPage: Int = 1,
    val hasMoreOpponents: Boolean = true,
    val loadingMoreOpponents: Boolean = false,
    val relationships: Map<String, String> = emptyMap(),
    val selectedTeam: ChallengeTeamUi? = null,
    val selectedOpponent: ChallengeTeamUi? = null,
    val selectedSport: ChallengeSportUi? = null,
    val selectedVenue: ChallengeVenueUi? = null,
    val bookingType: ChallengeBookingType = ChallengeBookingType.NORMAL,
    val date: String = LocalDate.now().toString(),
    val selectedDays: List<Int> = emptyList(),
    val slots: List<ChallengeSlotUi> = emptyList(),
    val step: ChallengeStep = ChallengeStep.MY_TEAM,
    val creating: Boolean = false,
    val loadingSlots: Boolean = false,
    val submitting: Boolean = false,
    val selectedSlot: ChallengeSlotUi? = null,
    val selectedHoldId: String? = null,
    val selectedDetail: ChallengeUi? = null,
    
    // Team Preview Modal fields
    val selectedTeamDetail: ChallengeTeamUi? = null,
    val loadingPreviewTeam: Boolean = false,
    val previewTeamDetail: TeamDetailUi? = null,
    val previewRecentMatches: List<RecentMatchUi> = emptyList(),
    val previewRecentMatchesPage: Int = 1,
    val previewRecentMatchesHasMore: Boolean = false,
    val previewJoinStatus: String = "none",
    val loadingMorePreviewMatches: Boolean = false,

    // Loading indicator overrides for actions
    val acceptingChallengeId: Int? = null,
    val decliningChallengeId: Int? = null,
    val cancellingChallengeId: Int? = null,

    val stake: String = "",
    val message: String? = null,
    val error: String? = null
)

sealed interface ChallengeEvent {
    data class OpenChat(val conversationId: String) : ChallengeEvent
}

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val api: ChallengeApiService,
    private val bookingApi: BookingApiService,
    private val teamApi: TeamApiService,
    private val chatApi: ChatApiService,
    private val slotWs: SlotAvailabilityWebSocketManager,
    private val challengeWs: ChallengeWebSocketManager,
    private val session: SessionManager,
    private val gson: Gson
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChallengeState())
    val state = _state.asStateFlow()
    
    private val _events = MutableSharedFlow<ChallengeEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    
    private val running = mutableSetOf<String>()
    private var searchJob: Job? = null
    private val slotBusy = AtomicBoolean(false)

    init {
        loadLists()
        viewModelScope.launch {
            session.getAccessTokenSync()?.let {
                challengeWs.connect(it, BuildConfig.BASE_URL)
            }
        }
        viewModelScope.launch {
            slotWs.slotAvailabilityFlow.collect { update ->
                val current = _state.value
                if (current.selectedSport != null && update.slots.isNotEmpty()) {
                    _state.value = current.copy(
                        slots = update.slots.map {
                            ChallengeSlotUi(
                                key = slotKey(it),
                                slot = it,
                                selected = current.selectedSlot?.key == slotKey(it)
                            )
                        },
                        loadingSlots = false
                    )
                }
            }
        }
        viewModelScope.launch {
            challengeWs.events.collect {
                loadLists(silent = true)
            }
        }
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun setTab(tab: ChallengeTab) {
        _state.value = _state.value.copy(tab = tab)
        if (tab == ChallengeTab.MY_CHALLENGES) {
            loadChallenges()
        } else {
            loadOpponents(reset = true)
        }
    }

    fun setSection(section: ChallengeSection) {
        _state.value = _state.value.copy(section = section)
        loadChallenges()
    }

    fun refresh() {
        _state.value = _state.value.copy(refreshing = true)
        loadLists()
    }

    fun search(value: String) {
        _state.value = _state.value.copy(search = value, opponentPage = 1, hasMoreOpponents = true)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadOpponents(page = 1, reset = true)
        }
    }

    fun loadMoreOpponents() {
        if (_state.value.hasMoreOpponents && !running.contains("opponents")) {
            loadOpponents(page = _state.value.opponentPage + 1)
        }
    }

    fun loadMoreMyTeams() {
        if (_state.value.hasMoreMyTeams && !running.contains("teams")) {
            loadTeams(page = _state.value.myTeamsPage + 1)
        }
    }

    fun openCreate() {
        _state.value = _state.value.copy(
            creating = true,
            step = ChallengeStep.MY_TEAM,
            selectedTeam = null,
            selectedOpponent = null,
            selectedSport = null,
            selectedVenue = null,
            slots = emptyList(),
            selectedSlot = null,
            selectedHoldId = null,
            stake = ""
        )
    }

    fun closeCreate() {
        viewModelScope.launch { releaseHold() }
        _state.value = _state.value.copy(creating = false, selectedHoldId = null)
    }

    fun selectOpponentFromPreview(team: ChallengeTeamUi) {
        _state.value = _state.value.copy(
            creating = true,
            step = ChallengeStep.MY_TEAM,
            selectedOpponent = team,
            selectedTeam = null,
            selectedSport = null,
            selectedVenue = null,
            slots = emptyList(),
            selectedSlot = null,
            selectedHoldId = null,
            stake = ""
        )
    }

    fun nextStep() {
        val s = _state.value
        when (s.step) {
            ChallengeStep.MY_TEAM -> {
                if (s.selectedTeam != null) {
                    _state.value = s.copy(step = ChallengeStep.OPPONENT)
                } else {
                    fail("Please select your team")
                }
            }
            ChallengeStep.OPPONENT -> {
                if (s.selectedOpponent != null) {
                    val blockReason = getChallengeBlockReason(s.selectedTeam?.id, s.selectedOpponent.id)
                    if (blockReason != null) {
                        val helperMsg = if (blockReason == "existing") "Already in challenge" else "Pending challenge already exists"
                        fail(helperMsg)
                    } else {
                        _state.value = s.copy(step = ChallengeStep.SPORT)
                    }
                } else {
                    fail("Please select an opponent team")
                }
            }
            ChallengeStep.SPORT -> {
                if (s.selectedSport != null) {
                    _state.value = s.copy(step = ChallengeStep.REVIEW)
                } else {
                    fail("Please select a sport")
                }
            }
            ChallengeStep.REVIEW -> submit()
        }
    }

    fun previousStep() {
        val s = _state.value
        _state.value = s.copy(
            step = when (s.step) {
                ChallengeStep.OPPONENT -> ChallengeStep.MY_TEAM
                ChallengeStep.SPORT -> ChallengeStep.OPPONENT
                ChallengeStep.REVIEW -> ChallengeStep.SPORT
                else -> ChallengeStep.MY_TEAM
            }
        )
    }

    fun selectTeam(team: ChallengeTeamUi) {
        _state.value = _state.value.copy(selectedTeam = team)
    }

    fun selectOpponent(team: ChallengeTeamUi) {
        _state.value = _state.value.copy(selectedOpponent = team)
    }

    fun selectSport(sport: ChallengeSportUi) {
        _state.value = _state.value.copy(selectedSport = sport, selectedVenue = null, slots = emptyList())
    }

    fun setStake(stake: String) {
        _state.value = _state.value.copy(stake = stake)
    }

    fun selectVenue(venue: ChallengeVenueUi) {
        _state.value = _state.value.copy(selectedVenue = venue)
        loadSlots()
    }

    fun setBookingType(type: ChallengeBookingType) {
        _state.value = _state.value.copy(bookingType = type)
        if (type == ChallengeBookingType.PERMANENT) {
            _state.value = _state.value.copy(selectedDays = emptyList())
        }
    }

    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date.toString())
        loadSlots()
    }

    fun toggleDay(day: Int) {
        val days = _state.value.selectedDays
        val updatedDays = if (day in days) days - day else days + day
        if (updatedDays.size > 4) {
            _state.value = _state.value.copy(error = "Maximum 4 selected days allowed for permanent booking")
            return
        }
        _state.value = _state.value.copy(selectedDays = updatedDays)
        if (_state.value.bookingType == ChallengeBookingType.PERMANENT) {
            requestPermanent()
        }
    }

    fun selectSlot(item: ChallengeSlotUi) {
        val s = _state.value
        val slot = item.slot
        val key = item.key
        if (item.processing || slot.isPastTime == true || slot.isFullyBookedForChallenge() || slot.isHeld == true || slot.available == false) return
        if (!slotBusy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                if (s.selectedSlot?.key == key) {
                    releaseHold()
                    _state.value = _state.value.copy(
                        selectedSlot = null,
                        selectedHoldId = null,
                        slots = s.slots.map { it.copy(selected = false) }
                    )
                    return@launch
                }
                releaseHold()
                _state.value = _state.value.copy(slots = _state.value.slots.map { it.copy(processing = it.key == key) })
                val sport = _state.value.selectedSport ?: return@launch
                val venue = _state.value.selectedVenue ?: return@launch
                val body = mapOf<String, Any>(
                    "sport_id" to sport.id,
                    "venue_id" to venue.id,
                    "date" to _state.value.date,
                    "start_time" to (item.slot.rawStart ?: item.slot.startTime.orEmpty()),
                    "end_time" to (item.slot.rawEnd ?: item.slot.endTime.orEmpty()),
                    "for_challenge" to true
                )
                val response = bookingApi.holdSlot(body)
                if (!response.isSuccessful) throw IllegalStateException(response.errorBody()?.string() ?: "Unable to reserve slot")
                val holdId = response.body()?.string()?.let { parseHoldId(it) }
                val chosen = item.copy(selected = true, processing = false)
                _state.value = _state.value.copy(
                    selectedSlot = chosen,
                    selectedHoldId = holdId,
                    slots = _state.value.slots.map { if (it.key == key) chosen else it.copy(selected = false, processing = false) },
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to reserve slot",
                    slots = _state.value.slots.map { it.copy(processing = false) }
                )
            } finally {
                slotBusy.set(false)
            }
        }
    }

    fun openDetails(challenge: ChallengeUi) {
        _state.value = _state.value.copy(selectedDetail = challenge)
    }

    fun openChat(challenge: ChallengeUi) {
        challenge.chatId?.let { _events.tryEmit(ChallengeEvent.OpenChat(it)) }
    }

    fun closeDetails() {
        _state.value = _state.value.copy(selectedDetail = null)
    }

    // ── Team Preview Modal Functions ──

    fun openTeam(team: ChallengeTeamUi) {
        _state.value = _state.value.copy(
            selectedTeamDetail = team,
            loadingPreviewTeam = true,
            previewTeamDetail = null,
            previewRecentMatches = emptyList(),
            previewRecentMatchesPage = 1,
            previewRecentMatchesHasMore = false,
            previewJoinStatus = "none"
        )
        fetchTeamPreviewData(team.id)
    }

    fun closeTeam() {
        _state.value = _state.value.copy(
            selectedTeamDetail = null,
            previewTeamDetail = null,
            previewRecentMatches = emptyList()
        )
    }

    private fun fetchTeamPreviewData(teamId: Int) {
        viewModelScope.launch {
            try {
                // Fetch team details
                val detailResp = teamApi.details(teamId)
                if (detailResp.isSuccessful && detailResp.body() != null) {
                    val detailObj = detailResp.body()!!.asJsonObject
                    val detailUi = parseTeamDetail(detailObj)
                    _state.value = _state.value.copy(
                        previewTeamDetail = detailUi
                    )
                }

                // Fetch Join status
                val joinResp = teamApi.joinStatus(teamId)
                if (joinResp.isSuccessful && joinResp.body() != null) {
                    val joinObj = joinResp.body()!!.asJsonObject
                    val status = joinObj.string("status") ?: "none"
                    _state.value = _state.value.copy(
                        previewJoinStatus = status
                    )
                }

                // Fetch challenge matches (page 1)
                val matchesResp = teamApi.challengeMatches(teamId, 1, 5)
                if (matchesResp.isSuccessful && matchesResp.body() != null) {
                    val bodyObj = matchesResp.body()!!
                    val matches = parseRecentMatches(bodyObj)
                    val count = if (bodyObj.isJsonObject && bodyObj.asJsonObject.has("count")) {
                        bodyObj.asJsonObject.int("count") ?: 0
                    } else matches.size
                    
                    _state.value = _state.value.copy(
                        previewRecentMatches = matches,
                        previewRecentMatchesPage = 1,
                        previewRecentMatchesHasMore = matches.size < count
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading team preview details")
            } finally {
                _state.value = _state.value.copy(loadingPreviewTeam = false)
            }
        }
    }

    fun loadMorePreviewMatches() {
        val s = _state.value
        val teamId = s.selectedTeamDetail?.id ?: return
        if (s.loadingMorePreviewMatches || !s.previewRecentMatchesHasMore) return
        
        _state.value = s.copy(loadingMorePreviewMatches = true)
        viewModelScope.launch {
            try {
                val nextPage = s.previewRecentMatchesPage + 1
                val matchesResp = teamApi.challengeMatches(teamId, nextPage, 5)
                if (matchesResp.isSuccessful && matchesResp.body() != null) {
                    val bodyObj = matchesResp.body()!!
                    val matches = parseRecentMatches(bodyObj)
                    val count = if (bodyObj.isJsonObject && bodyObj.asJsonObject.has("count")) {
                        bodyObj.asJsonObject.int("count") ?: 0
                    } else matches.size
                    
                    val merged = (s.previewRecentMatches + matches).distinctBy { it.id }
                    _state.value = _state.value.copy(
                        previewRecentMatches = merged,
                        previewRecentMatchesPage = nextPage,
                        previewRecentMatchesHasMore = merged.size < count
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading more preview matches")
            } finally {
                _state.value = _state.value.copy(loadingMorePreviewMatches = false)
            }
        }
    }

    fun requestJoinTeam() {
        val s = _state.value
        val teamId = s.selectedTeamDetail?.id ?: return
        if (s.previewJoinStatus != "none") return
        
        viewModelScope.launch {
            try {
                val res = teamApi.requestJoin(teamId)
                if (res.isSuccessful) {
                    _state.value = _state.value.copy(
                        previewJoinStatus = "requested",
                        message = "Join request submitted"
                    )
                } else {
                    fail("Failed to submit join request")
                }
            } catch (e: Exception) {
                fail("Failed to submit join request")
            }
        }
    }

    fun openTeamChat() {
        val s = _state.value
        val teamId = s.selectedTeamDetail?.id ?: return
        
        viewModelScope.launch {
            try {
                val res = chatApi.getTeamChat(teamId.toLong())
                if (res.isSuccessful && res.body() != null) {
                    val chat = res.body()!!
                    val id = chat.id.toString()
                    _events.tryEmit(ChallengeEvent.OpenChat(id))
                    closeTeam()
                } else {
                    fail("Unable to open team chat")
                }
            } catch (e: Exception) {
                fail("Unable to open team chat")
            }
        }
    }

    // ── Challenge Actions ──

    fun accept(challenge: ChallengeUi) {
        _state.value = _state.value.copy(acceptingChallengeId = challenge.id)
        action("accept-${challenge.id}") {
            val response = api.accept(challenge.id)
            ensure(response)
            
            // Extract chat id from accept response
            var chatId = extractChatId(response.body())
            
            // Fallback 1: Fetch challenge details
            if (chatId == null) {
                runCatching {
                    val detail = api.details(challenge.id)
                    if (detail.isSuccessful) {
                        chatId = extractChatId(detail.body())
                    }
                }
            }
            
            // Fallback 2: Search user chats for rivalry matches
            if (chatId == null) {
                runCatching {
                    val chatsResp = chatApi.getMyChats()
                    if (chatsResp.isSuccessful && chatsResp.body() != null) {
                        val arr = array(chatsResp.body()!!)
                        for (chatElem in arr) {
                            if (!chatElem.isJsonObject) continue
                            val chat = chatElem.asJsonObject
                            val type = chat.string("chat_type") ?: ""
                            if (type == "rivalry" || type == "challenge") {
                                val teamObj = chat.obj("team")
                                val teamId = teamObj?.int("id")
                                if (teamId == challenge.challengerId || teamId == challenge.challengedId) {
                                    chatId = chat.string("id") ?: chat.int("id")?.toString()
                                    break
                                }
                            }
                        }
                    }
                }
            }

            _state.value = _state.value.copy(
                message = "Challenge accepted! 🎉",
                selectedDetail = null,
                acceptingChallengeId = null
            )
            loadChallenges(silent = true)
            chatId?.let { _events.tryEmit(ChallengeEvent.OpenChat(it)) }
        }
    }

    fun decline(challenge: ChallengeUi) {
        _state.value = _state.value.copy(decliningChallengeId = challenge.id)
        action("decline-${challenge.id}") {
            ensure(api.decline(challenge.id))
            _state.value = _state.value.copy(
                message = "Challenge declined",
                selectedDetail = null,
                decliningChallengeId = null
            )
            loadChallenges(silent = true)
        }
    }

    fun cancel(challenge: ChallengeUi) {
        _state.value = _state.value.copy(cancellingChallengeId = challenge.id)
        action("cancel-${challenge.id}") {
            ensure(api.cancel(challenge.id))
            _state.value = _state.value.copy(
                message = "Challenge cancelled",
                selectedDetail = null,
                cancellingChallengeId = null
            )
            loadChallenges(silent = true)
        }
    }

    private fun submit() = action("submit") {
        val s = _state.value
        val team = s.selectedTeam ?: return@action fail("Select your team")
        val opponent = s.selectedOpponent ?: return@action fail("Select an opponent")
        val sport = s.selectedSport ?: return@action fail("Select a sport")
        
        val body = JsonObject().apply {
            addProperty("challenger_id", team.id)
            addProperty("challenged_id", opponent.id)
            addProperty("sport_id", sport.id)
            if (s.stake.isNotBlank()) {
                addProperty("stake", s.stake.toDoubleOrNull() ?: 0.0)
            }
        }
        
        ensure(api.create(body))
        releaseHold()
        _state.value = _state.value.copy(
            creating = false,
            message = "Challenge sent successfully",
            selectedHoldId = null
        )
        loadChallenges(silent = true)
    }

    private fun loadLists(silent: Boolean = false) {
        loadTeams(page = 1)
        loadOpponents(page = 1, reset = true)
        loadSports()
        loadChallenges(silent)
        loadRelationships()
    }

    private fun loadTeams(page: Int = 1) {
        _state.value = _state.value.copy(loadingMyTeams = true)
        viewModelScope.launch {
            try {
                val r = api.myTeams(page)
                if (r.isSuccessful && r.body() != null) {
                    val teams = parseTeams(r.body()!!)
                    val nextUrl = r.body()!!.takeIf { it.isJsonObject }?.asJsonObject?.string("next")
                    
                    val merged = if (page == 1) teams else (_state.value.myTeams + teams).distinctBy { it.id }
                    _state.value = _state.value.copy(
                        myTeams = merged,
                        myTeamsPage = page,
                        hasMoreMyTeams = !nextUrl.isNullOrBlank(),
                        loadingMyTeams = false
                    )
                } else {
                    _state.value = _state.value.copy(loadingMyTeams = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingMyTeams = false)
            }
        }
    }

    private fun loadOpponents(page: Int = 1, reset: Boolean = false) {
        val searchVal = _state.value.search.ifBlank { null }
        if (reset) {
            _state.value = _state.value.copy(loading = true)
        } else {
            _state.value = _state.value.copy(loadingMoreOpponents = true)
        }
        
        viewModelScope.launch {
            try {
                val r = api.opponents(page = page, search = searchVal)
                if (r.isSuccessful && r.body() != null) {
                    val list = parseTeams(r.body()!!)
                    val nextUrl = r.body()!!.takeIf { it.isJsonObject }?.asJsonObject?.string("next")
                    
                    val merged = if (page == 1 || reset) list else (_state.value.opponents + list).distinctBy { it.id }
                    _state.value = _state.value.copy(
                        opponents = merged,
                        opponentPage = page,
                        hasMoreOpponents = !nextUrl.isNullOrBlank(),
                        loading = false,
                        loadingMoreOpponents = false
                    )
                } else {
                    _state.value = _state.value.copy(loading = false, loadingMoreOpponents = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, loadingMoreOpponents = false)
            }
        }
    }

    private fun loadChallenges(silent: Boolean = false) = request("challenges", silent) {
        val sent = api.sent()
        val incoming = api.incoming()
        val history = api.history()
        
        _state.value = _state.value.copy(
            sent = if (sent.isSuccessful) parseChallenges(sent.body()) else emptyList(),
            incoming = if (incoming.isSuccessful) parseChallenges(incoming.body()) else emptyList(),
            history = if (history.isSuccessful) parseChallenges(history.body()) else emptyList(),
            loading = false,
            refreshing = false
        )
    }

    private fun loadRelationships() {
        viewModelScope.launch {
            try {
                val res = api.relationships()
                if (res.isSuccessful && res.body() != null) {
                    val json = res.body()!!
                    val relObj = if (json.isJsonObject) json.asJsonObject.get("relationships") else null
                    val map = mutableMapOf<String, String>()
                    if (relObj != null && relObj.isJsonArray) {
                        for (item in relObj.asJsonArray) {
                            if (!item.isJsonObject) continue
                            val obj = item.asJsonObject
                            val tA = obj.int("team_a_id") ?: continue
                            val tB = obj.int("team_b_id") ?: continue
                            val reason = obj.string("reason") ?: "pending"
                            map[challengePairKey(tA, tB)] = reason
                        }
                    }
                    _state.value = _state.value.copy(relationships = map)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load challenge relationships")
            }
        }
    }

    private fun loadVenues() = request("venues") {
        val sport = _state.value.selectedSport ?: return@request
        val r = api.venues(sport.id)
        if (r.isSuccessful) {
            _state.value = _state.value.copy(venues = parseVenues(r.body()!!))
        }
    }

    private fun loadSports() = request("sports", silent = true) {
        val r = api.sports()
        ensure(r)
        _state.value = _state.value.copy(
            sports = array(r.body()!!).mapNotNull { e ->
                if (!e.isJsonObject) null else {
                    val o = e.asJsonObject
                    ChallengeSportUi(
                        id = o.int("id") ?: return@mapNotNull null,
                        name = o.string("name") ?: "Sport",
                        price = o.double("price") ?: o.double("hourly_rate") ?: 0.0,
                        image = o.string("image") ?: o.string("image_url")
                    )
                }
            }
        )
    }

    private fun loadSlots() = request("slots") {
        val sport = _state.value.selectedSport ?: return@request
        val venue = _state.value.selectedVenue ?: return@request
        _state.value = _state.value.copy(loadingSlots = true, selectedSlot = null, selectedHoldId = null)
        val r = bookingApi.fetchAvailableSlots(sport.id, venue.id, _state.value.date, false)
        if (!r.isSuccessful) throw IllegalStateException("Unable to load slots")
        val items = (r.body()?.availableSlots.orEmpty().ifEmpty { r.body()?.slots.orEmpty() }).map {
            ChallengeSlotUi(slotKey(it), it)
        }
        _state.value = _state.value.copy(slots = items, loadingSlots = false)
        session.getAccessTokenSync()?.let {
            slotWs.connect(sport.id, it, BuildConfig.BASE_URL)
            slotWs.requestSlotAvailability(_state.value.date, venue.id)
        }
    }

    private fun requestPermanent() {
        val sport = _state.value.selectedSport ?: return
        if (_state.value.selectedDays.isEmpty()) return
        viewModelScope.launch {
            val r = bookingApi.fetchPermanentAvailability(sport.id, mapOf("selected_days" to _state.value.selectedDays))
            if (r.isSuccessful) {
                val arr = array(r.body()!!)
                _state.value = _state.value.copy(
                    slots = arr.mapNotNull {
                        runCatching {
                            ChallengeSlotUi(
                                key = slotKey(gson.fromJson(it, SlotData::class.java)),
                                slot = gson.fromJson(it, SlotData::class.java)
                            )
                        }.getOrNull()
                    },
                    loadingSlots = false
                )
            }
        }
    }

    private suspend fun releaseHold() {
        val s = _state.value
        val slot = s.selectedSlot?.slot ?: return
        val sport = s.selectedSport ?: return
        val venue = s.selectedVenue ?: return
        val body = mapOf<String, Any>(
            "sport_id" to sport.id,
            "venue_id" to venue.id,
            "date" to s.date,
            "start_time" to (slot.rawStart ?: slot.startTime.orEmpty()),
            "end_time" to (slot.rawEnd ?: slot.endTime.orEmpty())
        )
        runCatching { bookingApi.releaseSlot(body) }
    }

    private fun <T> action(key: String, block: suspend () -> T) {
        if (!running.add(key)) return
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                fail(e.message ?: "Request failed")
            } finally {
                running.remove(key)
                _state.value = _state.value.copy(
                    acceptingChallengeId = null,
                    decliningChallengeId = null,
                    cancellingChallengeId = null
                )
            }
        }
    }

    private fun request(key: String, silent: Boolean = false, block: suspend () -> Unit) {
        if (!running.add(key)) return
        if (!silent) _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message ?: "Request failed"
                )
            } finally {
                running.remove(key)
            }
        }
    }

    private fun ensure(response: retrofit2.Response<*>) {
        if (!response.isSuccessful) {
            val errJson = response.errorBody()?.string() ?: ""
            val errMsg = runCatching {
                gson.fromJson(errJson, JsonObject::class.java).string("error")
            }.getOrNull() ?: runCatching {
                gson.fromJson(errJson, JsonObject::class.java).string("detail")
            }.getOrNull() ?: "Request failed (${response.code()})"
            throw IllegalStateException(errMsg)
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    private fun getChallengeBlockReason(teamAId: Int?, teamBId: Int?): String? {
        if (teamAId == null || teamBId == null) return null
        val pair = challengePairKey(teamAId, teamBId)
        val rel = _state.value.relationships[pair]
        if (rel != null) return rel
        
        // Check local challenges as fallback
        val matching = (_state.value.incoming + _state.value.sent + _state.value.history).filter { c ->
            (c.challengerId == teamAId && c.challengedId == teamBId) ||
            (c.challengerId == teamBId && c.challengedId == teamAId)
        }
        
        val alreadyActive = matching.some { c ->
            c.chatId != null || c.status == "accepted" || c.status == "completed"
        }
        if (alreadyActive) return "existing"
        
        return if (matching.any { it.status == "pending" }) "pending" else null
    }

    private fun challengePairKey(tA: Int, tB: Int): String =
        listOf(tA, tB).sorted().joinToString(":")

    private fun parseTeams(root: JsonElement): List<ChallengeTeamUi> =
        array(root).mapNotNull { e ->
            if (!e.isJsonObject) null else {
                val o = e.asJsonObject
                ChallengeTeamUi(
                    id = o.int("id") ?: return@mapNotNull null,
                    name = o.string("name") ?: "Team",
                    logo = o.string("logo") ?: o.string("logo_url") ?: o.string("image"),
                    members = o.int("members_count") ?: o.int("member_count") ?: 0,
                    location = o.string("location") ?: o.string("city") ?: "",
                    sport = o.string("sport_name") ?: "",
                    description = o.string("description") ?: ""
                )
            }
        }

    private fun parseVenues(root: JsonElement): List<ChallengeVenueUi> =
        array(root).mapNotNull { e ->
            if (!e.isJsonObject) null else {
                val o = e.asJsonObject
                ChallengeVenueUi(
                    id = o.int("id") ?: return@mapNotNull null,
                    name = o.string("name") ?: "Venue",
                    address = o.string("address") ?: "",
                    city = o.string("city") ?: o.string("location") ?: ""
                )
            }
        }

    private fun parseChallenges(root: JsonElement?): List<ChallengeUi> =
        root?.let { array(it).mapNotNull { e -> if (!e.isJsonObject) null else parseChallenge(e.asJsonObject) } }.orEmpty()

    private fun parseChallenge(o: JsonObject): ChallengeUi {
        val challenger = o.obj("challenger")
        val challenged = o.obj("challenged")
        val sport = o.obj("sport")
        val venue = o.obj("venue")
        val status = o.string("status") ?: "pending"
        return ChallengeUi(
            id = o.int("id") ?: 0,
            challenger = challenger?.string("name") ?: o.string("challenger_name") ?: "My team",
            challenged = challenged?.string("name") ?: o.string("challenged_name") ?: "Opponent",
            challengerId = challenger?.int("id") ?: o.int("challenger_id"),
            challengedId = challenged?.int("id") ?: o.int("challenged_id"),
            sport = sport?.string("name") ?: o.string("sport_name") ?: o.string("sport") ?: "",
            venue = venue?.string("name") ?: o.string("venue_name") ?: o.string("venue") ?: "",
            date = o.string("match_date") ?: o.string("date"),
            start = o.string("match_time") ?: o.string("start_time"),
            end = o.string("end_time"),
            status = status,
            stake = o.double("stake") ?: 0.0,
            canAccept = o.bool("can_accept") ?: (status == "pending"),
            canDecline = o.bool("can_decline") ?: (status == "pending"),
            canCancel = o.bool("can_cancel") ?: (status == "pending"),
            chatId = extractChatId(o),
            raw = o
        )
    }

    private fun parseTeamDetail(o: JsonObject): TeamDetailUi {
        val statsObj = o.obj("challenge_stats")
        val stats = statsObj?.let {
            ChallengeStatsUi(
                totalMatches = it.int("total_matches") ?: 0,
                wins = it.int("wins") ?: 0,
                losses = it.int("losses") ?: 0,
                noResults = it.int("no_results") ?: 0,
                winPercentage = it.double("win_percentage") ?: 0.0
            )
        }
        return TeamDetailUi(
            id = o.int("id") ?: 0,
            name = o.string("name") ?: "Team",
            description = o.string("description") ?: "",
            membersCount = o.int("members_count") ?: 0,
            maxMembers = o.int("max_members") ?: 0,
            logo = o.string("logo") ?: o.string("logo_url") ?: o.string("image"),
            coverImage = o.string("cover_image") ?: o.string("cover_image_url"),
            location = o.string("location") ?: o.string("city") ?: "",
            teamType = o.string("team_type") ?: "",
            skillLevel = o.string("skill_level") ?: "",
            isPublic = o.bool("is_public") ?: true,
            challengeStats = stats
        )
    }

    private fun parseRecentMatches(root: JsonElement): List<RecentMatchUi> {
        return array(root).mapNotNull { e ->
            if (!e.isJsonObject) null else {
                val o = e.asJsonObject
                val opponentObj = o.obj("opponent")
                val teamScoreObj = o.obj("team_score")
                val opponentScoreObj = o.obj("opponent_score")
                RecentMatchUi(
                    id = o.string("id") ?: o.string("match_id") ?: "",
                    opponentId = opponentObj?.int("id"),
                    opponentName = opponentObj?.string("name") ?: "Opponent",
                    opponentLogo = opponentObj?.string("logo") ?: opponentObj?.string("logo_url"),
                    result = o.string("result") ?: "no_result",
                    matchDate = o.string("match_date") ?: o.string("completed_at"),
                    venue = o.string("venue") ?: o.string("venue_name"),
                    margin = o.string("margin") ?: "",
                    teamScore = teamScoreObj?.let {
                        MatchScoreUi(
                            runs = it.int("runs") ?: 0,
                            wickets = it.int("wickets") ?: 0,
                            overs = it.string("overs") ?: ""
                        )
                    },
                    opponentScore = opponentScoreObj?.let {
                        MatchScoreUi(
                            runs = it.int("runs") ?: 0,
                            wickets = it.int("wickets") ?: 0,
                            overs = it.string("overs") ?: ""
                        )
                    }
                )
            }
        }
    }

    private fun extractChatId(e: JsonElement?): String? {
        val o = e?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        return o.string("chat_id") ?: o.obj("chat")?.string("id") ?: o.obj("rivalry_chat")?.string("id") ?: o.int("chat_id")?.toString()
    }

    private fun parseHoldId(text: String): String? = runCatching {
        gson.fromJson(text, JsonObject::class.java).string("hold_id") ?: gson.fromJson(text, JsonObject::class.java).string("id") ?: gson.fromJson(text, JsonObject::class.java).int("id")?.toString()
    }.getOrNull()

    private fun array(e: JsonElement): List<JsonElement> = when {
        e.isJsonArray -> e.asJsonArray.toList()
        e.isJsonObject && e.asJsonObject.get("results")?.isJsonArray == true -> e.asJsonObject.getAsJsonArray("results").toList()
        e.isJsonObject && e.asJsonObject.get("data")?.isJsonArray == true -> e.asJsonObject.getAsJsonArray("data").toList()
        else -> emptyList()
    }

    private fun slotKey(s: SlotData): String = s.slotKey ?: "${s.rawStart ?: s.startTime}-${s.rawEnd ?: s.endTime}"
    private fun SlotData.isFullyBookedForChallenge(): Boolean = isFullyBooked == true || (availableCourts is Number && totalCourts is Number && (availableCourts as Number).toInt() <= 0)

    private fun JsonObject.string(name: String): String? = get(name)?.takeIf { !it.isJsonNull }?.asString
    private fun JsonObject.int(name: String): Int? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
    private fun JsonObject.double(name: String): Double? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asDouble }.getOrNull() }
    private fun JsonObject.bool(name: String): Boolean? = get(name)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asBoolean }.getOrNull() }
    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun <T> List<T>.some(predicate: (T) -> Boolean): Boolean {
        for (item in this) {
            if (predicate(item)) return true
        }
        return false
    }

    override fun onCleared() {
        challengeWs.disconnect()
        slotWs.disconnect()
        super.onCleared()
    }
}

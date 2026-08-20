package com.sportynix.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.BuildConfig
import com.sportynix.app.data.remote.api.TeamApiService
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.dto.LocationCityDto
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.repository.LocationRepository
import com.sportynix.app.domain.model.location.LocationCity
import com.sportynix.app.domain.model.location.LocationDistrict
import com.sportynix.app.domain.model.location.LocationProvince
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

enum class TeamTab { MY_TEAMS, JOIN, INVITATIONS }
enum class InvitationsSubTab { RECEIVED, SENT }
enum class JoinStatus { NONE, REQUESTED, APPROVED, REJECTED, MEMBER }

data class TeamImagePart(val part: MultipartBody.Part)

data class TeamFormState(
    val name: String = "",
    val description: String = "",
    val teamType: String = "friends",
    val location: String = "",
    val cityId: Int? = null,
    val city: String = "",
    val district: String = "",
    val province: String = "",
    val maxMembers: String = "20",
    val expiryDays: String = "14",
    val expiryHours: String = "0",
    val isPublic: Boolean = true,
    val preferredSports: List<String> = emptyList(),
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val removeLogo: Boolean = false,
    val removeCover: Boolean = false
)

data class TeamMemberUi(
    val id: Int,
    val name: String,
    val username: String = "",
    val role: String = "Member",
    val email: String = "",
    val avatar: String? = null
)

data class TeamMembershipUi(
    val id: Int,
    val teamId: Int,
    val teamName: String,
    val userId: Int,
    val name: String,
    val username: String = "",
    val status: String = "requested",
    val requestedAt: String = "",
    val invitedBy: String = "",
    val avatar: String? = null,
    val teamLogo: String? = null
)

data class ChallengeStatsUi(
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val noResults: Int = 0,
    val winPercentage: Float = 0f
)

data class MatchScoreUi(
    val runs: Int = 0,
    val wickets: Int = 0,
    val overs: String = "0.0"
)

data class RecentMatchUi(
    val id: String,
    val opponentName: String,
    val opponentLogo: String? = null,
    val result: String = "no_result",
    val status: String = "",
    val matchDate: String = "",
    val venue: String = "",
    val margin: String = "",
    val teamScore: MatchScoreUi? = null,
    val opponentScore: MatchScoreUi? = null
)

data class TeamUi(
    val id: Int,
    val name: String,
    val description: String = "",
    val membersCount: Int = 0,
    val role: String? = null,
    val logo: String? = null,
    val cover: String? = null,
    val isPublic: Boolean = true,
    val maxMembers: Int = 20,
    val location: String = "",
    val cityId: Int? = null,
    val city: String = "",
    val district: String = "",
    val province: String = "",
    val type: String = "friends",
    val sports: List<String> = emptyList(),
    val joinStatus: JoinStatus = JoinStatus.NONE,
    val challengeStats: ChallengeStatsUi? = null,
    val recentMatches: List<RecentMatchUi> = emptyList(),
    val recentMatchesCount: Int = 0,
    val recentMatchesPage: Int = 1
)

data class TeamState(
    val tab: TeamTab = TeamTab.MY_TEAMS,
    val invitationsTab: InvitationsSubTab = InvitationsSubTab.RECEIVED,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val saving: Boolean = false,
    val loadingMoreMatches: Boolean = false,
    val searchQuery: String = "",
    val inviteSearchQuery: String = "",
    val teams: List<TeamUi> = emptyList(),
    val discover: List<TeamUi> = emptyList(),
    val received: List<TeamMembershipUi> = emptyList(),
    val sent: List<TeamMembershipUi> = emptyList(),
    val selected: TeamUi? = null,
    val members: List<TeamMemberUi> = emptyList(),
    val pending: List<TeamMembershipUi> = emptyList(),
    val searchedUsers: List<TeamMemberUi> = emptyList(),
    val cities: List<LocationCityDto> = emptyList(),
    val form: TeamFormState = TeamFormState(),
    val editing: Boolean = false,
    val showForm: Boolean = false,
    val showDetails: Boolean = false,
    val showInviteSearch: Boolean = false,
    val inviteToken: String? = null,
    val invitePreview: TeamUi? = null,
    val inviteRequiresAuth: Boolean = false,
    val previewImageUrl: String? = null,
    val provinces: List<LocationProvince> = emptyList(),
    val districts: List<LocationDistrict> = emptyList(),
    val locationCities: List<LocationCity> = emptyList(),
    val selectedProvinceId: Int? = null,
    val selectedDistrictId: Int? = null,
    val locationSearchQuery: String = "",
    val loadingProvinces: Boolean = false,
    val loadingDistricts: Boolean = false,
    val loadingCities: Boolean = false,
    val showLocationPickerSheet: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

sealed interface TeamEvent {
    data class OpenChat(val conversationId: String) : TeamEvent
    data class ShareInvite(val link: String) : TeamEvent
}

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val api: TeamApiService,
    private val userApi: UserApiService,
    private val locationRepository: LocationRepository,
    private val gson: Gson
) : ViewModel() {
    private val _state = MutableStateFlow(TeamState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<TeamEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    private val running = mutableSetOf<String>()
    private var searchDebounceJob: Job? = null

    init {
        loadMyTeams()
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun openImagePreview(url: String) {
        _state.value = _state.value.copy(previewImageUrl = url)
    }

    fun closeImagePreview() {
        _state.value = _state.value.copy(previewImageUrl = null)
    }

    fun selectTab(tab: TeamTab) {
        val current = _state.value
        if (current.tab == tab && !current.loading && when (tab) {
                TeamTab.MY_TEAMS -> current.teams.isNotEmpty()
                TeamTab.JOIN -> current.discover.isNotEmpty()
                TeamTab.INVITATIONS -> current.received.isNotEmpty() || current.sent.isNotEmpty()
            }) return
        _state.value = _state.value.copy(tab = tab, error = null)
        when (tab) {
            TeamTab.MY_TEAMS -> loadMyTeams()
            TeamTab.JOIN -> loadDiscover()
            TeamTab.INVITATIONS -> loadInvitations()
        }
    }

    fun refreshCurrentTab() {
        _state.value = _state.value.copy(refreshing = true)
        when (_state.value.tab) {
            TeamTab.MY_TEAMS -> loadMyTeams(isRefresh = true)
            TeamTab.JOIN -> loadDiscover(isRefresh = true)
            TeamTab.INVITATIONS -> loadInvitations(isRefresh = true)
        }
    }

    fun selectInvitationsSubTab(subTab: InvitationsSubTab) {
        _state.value = _state.value.copy(invitationsTab = subTab)
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun openCreate() {
        _state.value = _state.value.copy(
            showForm = true,
            editing = false,
            form = TeamFormState(),
            selectedProvinceId = null,
            selectedDistrictId = null,
            locationSearchQuery = "",
            locationCities = emptyList()
        )
        loadCities("")
    }

    fun openEdit(team: TeamUi) {
        _state.value = _state.value.copy(
            showForm = true,
            editing = true,
            selected = team,
            form = team.toForm(),
            selectedProvinceId = null,
            selectedDistrictId = null,
            locationSearchQuery = "",
            locationCities = emptyList()
        )
        loadCities(team.city)
    }

    fun openLocationPicker() {
        _state.value = _state.value.copy(showLocationPickerSheet = true)
        if (_state.value.provinces.isEmpty()) {
            loadProvinces()
        }
        val currentDistrictId = _state.value.selectedDistrictId
        val currentQuery = _state.value.locationSearchQuery
        if (currentDistrictId != null || currentQuery.isNotBlank()) {
            searchLocationCities()
        }
    }

    fun closeLocationPicker() {
        _state.value = _state.value.copy(showLocationPickerSheet = false)
    }

    fun loadProvinces() {
        _state.value = _state.value.copy(loadingProvinces = true)
        viewModelScope.launch {
            val provinces = locationRepository.getProvinces().getOrNull() ?: emptyList()
            _state.value = _state.value.copy(
                provinces = provinces,
                loadingProvinces = false
            )
        }
    }

    fun selectProvince(provinceId: Int) {
        val current = _state.value
        if (current.selectedProvinceId == provinceId) {
            _state.value = current.copy(
                selectedProvinceId = null,
                selectedDistrictId = null,
                districts = emptyList(),
                locationSearchQuery = "",
                locationCities = emptyList()
            )
        } else {
            _state.value = current.copy(
                selectedProvinceId = provinceId,
                selectedDistrictId = null,
                districts = emptyList(),
                locationSearchQuery = "",
                locationCities = emptyList(),
                loadingDistricts = true
            )
            viewModelScope.launch {
                val districts = locationRepository.getDistricts(provinceId).getOrNull() ?: emptyList()
                _state.value = _state.value.copy(
                    districts = districts,
                    loadingDistricts = false
                )
            }
        }
    }

    fun selectDistrict(districtId: Int) {
        val current = _state.value
        if (current.selectedDistrictId == districtId) {
            _state.value = current.copy(
                selectedDistrictId = null,
                locationSearchQuery = ""
            )
        } else {
            _state.value = current.copy(
                selectedDistrictId = districtId,
                locationSearchQuery = ""
            )
        }
        searchLocationCities()
    }

    fun onLocationSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(locationSearchQuery = query)
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(250)
            searchLocationCities()
        }
    }

    fun searchLocationCities() {
        val current = _state.value
        val trimmed = current.locationSearchQuery.trim()
        if (current.selectedDistrictId == null && trimmed.isEmpty()) {
            _state.value = current.copy(locationCities = emptyList(), loadingCities = false)
            return
        }

        _state.value = _state.value.copy(loadingCities = true)
        viewModelScope.launch {
            val fetched = locationRepository.getCities(
                districtId = current.selectedDistrictId,
                search = trimmed.ifEmpty { null },
                pageSize = 100
            ).getOrNull() ?: emptyList()
            val filtered = if (current.selectedProvinceId != null) {
                fetched.filter { it.provinceId == current.selectedProvinceId }
            } else fetched
            _state.value = _state.value.copy(
                locationCities = filtered,
                loadingCities = false
            )
        }
    }

    fun onLocationCitySelected(city: LocationCity) {
        val cityName = city.nameEn
        val districtName = city.districtName
        val provinceName = city.provinceName
        updateForm {
            it.copy(
                cityId = city.id,
                city = cityName,
                location = "$cityName, $districtName",
                district = districtName,
                province = provinceName
            )
        }
        _state.value = _state.value.copy(showLocationPickerSheet = false)
    }

    fun closeForm() {
        _state.value = _state.value.copy(showForm = false, editing = false)
    }

    fun updateForm(form: TeamFormState) {
        _state.value = _state.value.copy(form = form)
    }

    fun updateForm(transform: (TeamFormState) -> TeamFormState) {
        updateForm(transform(_state.value.form))
    }

    fun loadMyTeams(isRefresh: Boolean = false) = request("my-teams", isRefresh) {
        val response = api.myTeams()
        ensureOk(response)
        val all = parseTeams(response.body()!!)
        val myTeamsList = all.map { team ->
            if (team.role == null) team.copy(role = "Member") else team
        }
        _state.value = _state.value.copy(teams = myTeamsList, loading = false, refreshing = false)
    }

    fun loadDiscover(isRefresh: Boolean = false) = request("discover", isRefresh) {
        val response = api.discoverTeams()
        ensureOk(response)
        val mine = _state.value.teams.map { it.id }.toSet()
        val discoverList = parseTeams(response.body()!!).filter { it.id !in mine && it.joinStatus !in setOf(JoinStatus.MEMBER, JoinStatus.APPROVED) }
        _state.value = _state.value.copy(discover = discoverList, loading = false, refreshing = false)
    }

    fun loadInvitations(isRefresh: Boolean = false) = request("invitations", isRefresh) {
        val received = api.receivedInvitations()
        val sent = api.sentInvitations()
        val recvList = if (received.code() == 404) emptyList() else { ensureOk(received); parseMemberships(received.body()!!) }
        val sentList = if (sent.code() == 404) emptyList() else { ensureOk(sent); parseMemberships(sent.body()!!) }
        _state.value = _state.value.copy(received = recvList, sent = sentList, loading = false, refreshing = false)
    }

    fun openDetails(team: TeamUi) = request("details-${team.id}") {
        val response = api.details(team.id)
        ensureOk(response)
        val root = response.body()!!
        val details = parseTeam(root, team)

        val joinStatusRes = runCatching { api.joinStatus(team.id) }.getOrNull()
        val finalStatus = if (joinStatusRes?.isSuccessful == true) {
            parseJoinStatus(joinStatusRes.body()?.asJsonObject?.string("status"))
        } else details.joinStatus

        val updatedTeam = details.copy(joinStatus = finalStatus)
        val members = root.asJsonObject.obj("members")?.let { parseMembers(it) }
            ?: parseMembers(root)
        _state.value = _state.value.copy(
            selected = updatedTeam,
            members = members,
            showDetails = true,
            loading = false
        )
        if (updatedTeam.role == "Captain" || updatedTeam.role == "Admin") {
            loadPending(team.id)
        }
    }

    fun loadMoreMatches(teamId: Int) {
        val selected = _state.value.selected ?: return
        if (_state.value.loadingMoreMatches || selected.recentMatches.size >= selected.recentMatchesCount) return
        val nextPage = selected.recentMatchesPage + 1
        request("matches-$teamId-p$nextPage") {
            _state.value = _state.value.copy(loadingMoreMatches = true)
            val response = api.challengeMatches(teamId, page = nextPage, pageSize = 5)
            if (response.isSuccessful && response.body() != null) {
                val newMatches = parseRecentMatches(response.body()!!)
                val currentMatches = selected.recentMatches
                val combined = (currentMatches + newMatches).distinctBy { it.id }
                val updatedSelected = selected.copy(
                    recentMatches = combined,
                    recentMatchesPage = nextPage
                )
                _state.value = _state.value.copy(
                    selected = updatedSelected,
                    loadingMoreMatches = false,
                    loading = false
                )
            } else {
                _state.value = _state.value.copy(loadingMoreMatches = false, loading = false)
            }
        }
    }

    fun closeDetails() {
        _state.value = _state.value.copy(showDetails = false, selected = null, searchedUsers = emptyList(), inviteSearchQuery = "")
    }

    fun loadPending(teamId: Int) = request("pending-$teamId") {
        val response = api.pendingMembers(teamId)
        ensureOk(response)
        _state.value = _state.value.copy(pending = parseMemberships(response.body()!!), loading = false)
    }

    fun onInviteQueryChanged(query: String) {
        _state.value = _state.value.copy(inviteSearchQuery = query)
        searchDebounceJob?.cancel()
        if (query.trim().isEmpty()) {
            _state.value = _state.value.copy(searchedUsers = emptyList())
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            searchMembers(query)
        }
    }

    fun searchMembers(query: String) {
        val id = _state.value.selected?.id ?: return
        if (query.trim().isEmpty()) {
            _state.value = _state.value.copy(searchedUsers = emptyList())
            return
        }
        request("search-$id") {
            val response = api.searchMembers(id, query.trim())
            ensureOk(response)
            _state.value = _state.value.copy(searchedUsers = parseMembers(response.body()!!), loading = false)
        }
    }

    fun saveTeam(logo: TeamImagePart? = null, cover: TeamImagePart? = null, removeLogo: Boolean = false, removeCover: Boolean = false) = action("save") {
        val form = _state.value.form
        val validation = validate(form)
        if (validation != null) {
            fail(validation)
            return@action
        }
        val fields = mutableMapOf<String, okhttp3.RequestBody>()
        fun field(key: String, value: String) { fields[key] = value.toRequestBody() }
        field("name", form.name.trim())
        field("description", form.description.trim())
        field("team_type", form.teamType)
        field("location", form.city.ifBlank { form.location.trim() })
        field("city", form.city)
        field("district", form.district)
        field("province_name", form.province)
        form.cityId?.let { field("city_id", it.toString()) }
        field("max_members", (form.maxMembers.toIntOrNull() ?: 20).toString())
        val days = form.expiryDays.toIntOrNull() ?: 14
        val hours = form.expiryHours.toIntOrNull() ?: 0
        field("invite_link_expiry_hours", (days * 24 + hours).coerceAtLeast(1).toString())
        field("is_public", form.isPublic.toString())
        field("preferred_sports", gson.toJson(form.preferredSports))

        if (_state.value.editing) {
            if (removeLogo) field("remove_logo", "true")
            if (removeCover) field("remove_cover_image", "true")
        }

        val result = if (_state.value.editing && _state.value.selected != null) {
            api.update(_state.value.selected!!.id, fields, logo?.part, cover?.part)
        } else {
            api.create(fields, logo?.part, cover?.part)
        }
        ensureOk(result)

        val createdOrUpdatedTeam = result.body()?.let { parseTeam(it, _state.value.selected) }

        _state.value = _state.value.copy(
            showForm = false,
            editing = false,
            saving = false,
            message = if (_state.value.editing) "Team updated successfully" else "Team created successfully",
            form = TeamFormState()
        )
        loadMyTeams()

        if (createdOrUpdatedTeam != null) {
            openDetails(createdOrUpdatedTeam)
        }
    }

    fun requestJoin(team: TeamUi) = action("join-${team.id}") {
        val response = api.requestJoin(team.id)
        ensureOk(response)
        _state.value = _state.value.copy(
            discover = _state.value.discover.map { if (it.id == team.id) it.copy(joinStatus = JoinStatus.REQUESTED) else it },
            selected = if (_state.value.selected?.id == team.id) _state.value.selected?.copy(joinStatus = JoinStatus.REQUESTED) else _state.value.selected,
            message = "Join request sent"
        )
    }

    fun cancelJoin(team: TeamUi) = action("cancel-join-${team.id}") {
        ensureOk(api.cancelJoin(team.id))
        _state.value = _state.value.copy(
            discover = _state.value.discover.map { if (it.id == team.id) it.copy(joinStatus = JoinStatus.NONE) else it },
            selected = if (_state.value.selected?.id == team.id) _state.value.selected?.copy(joinStatus = JoinStatus.NONE) else _state.value.selected,
            message = "Join request cancelled"
        )
    }

    fun acceptInvitation(item: TeamMembershipUi) = action("accept-${item.id}") {
        ensureOk(api.acceptInvitation(item.teamId, obj("membership_id", item.id)))
        _state.value = _state.value.copy(message = "Invitation accepted")
        loadInvitations()
        loadMyTeams()
    }

    fun rejectInvitation(item: TeamMembershipUi) = action("reject-inv-${item.id}") {
        ensureOk(api.rejectInvitation(item.teamId, obj("membership_id", item.id)))
        _state.value = _state.value.copy(message = "Invitation rejected")
        loadInvitations()
    }

    fun cancelSentInvitation(item: TeamMembershipUi) = action("cancel-inv-${item.id}") {
        ensureOk(api.cancelInvitation(item.teamId, obj("membership_id", item.id)))
        _state.value = _state.value.copy(message = "Invitation cancelled")
        loadInvitations()
    }

    fun invite(user: TeamMemberUi) = action("invite-${user.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        val response = api.invite(teamId, obj("user_id", user.id))
        ensureOk(response)
        val status = response.body()?.string("membership_status") ?: response.body()?.string("status")
        _state.value = _state.value.copy(
            message = if (status == "approved") "Member added to team" else "Invitation sent",
            searchedUsers = emptyList(),
            inviteSearchQuery = ""
        )
        openDetails(_state.value.selected!!)
    }

    fun approve(item: TeamMembershipUi) = action("approve-${item.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.approveMembership(teamId, obj("membership_id", item.id)))
        _state.value = _state.value.copy(message = "Membership approved")
        openDetails(_state.value.selected!!)
    }

    fun reject(item: TeamMembershipUi) = action("reject-${item.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.rejectMembership(teamId, obj("membership_id", item.id)))
        _state.value = _state.value.copy(message = "Membership rejected")
        loadPending(teamId)
    }

    fun addAdmin(member: TeamMemberUi) = action("add-admin-${member.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.addAdmin(teamId, obj("user_id", member.id)))
        _state.value = _state.value.copy(message = "${member.name} is now an Admin")
        openDetails(_state.value.selected!!)
    }

    fun removeAdmin(member: TeamMemberUi) = action("remove-admin-${member.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.removeAdmin(teamId, obj("user_id", member.id)))
        _state.value = _state.value.copy(message = "Admin permissions removed")
        openDetails(_state.value.selected!!)
    }

    fun removeMember(member: TeamMemberUi) = action("remove-${member.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.removeMember(teamId, obj("user_id", member.id)))
        _state.value = _state.value.copy(message = "Member removed")
        openDetails(_state.value.selected!!)
    }

    fun leave(team: TeamUi) = action("leave-${team.id}") {
        ensureOk(api.leave(team.id))
        _state.value = _state.value.copy(showDetails = false, selected = null, message = "You left the team")
        loadMyTeams()
    }

    fun delete(team: TeamUi, force: Boolean = false) = action("delete-${team.id}") {
        val response = if (force) api.forceDelete(team.id) else api.delete(team.id)
        if (!response.isSuccessful && response.code() == 400 && !force) {
            fail("This team has active members. Remove them first or use force delete.")
            return@action
        }
        ensureOk(response)
        _state.value = _state.value.copy(showDetails = false, selected = null, message = "Team deleted")
        loadMyTeams()
    }

    fun clearTeamChat(teamId: Int, chatType: String = "team_group") = action("clear-chat-$teamId") {
        val response = api.clearChat(teamId, obj("chat_type", chatType))
        ensureOk(response)
        _state.value = _state.value.copy(message = if (chatType == "channel") "Channel announcements cleared" else "Group chat cleared")
    }

    fun generateInvite(team: TeamUi) = action("link-${team.id}") {
        val response = api.generateInviteLink(team.id)
        ensureOk(response)
        val link = response.body()?.string("invite_link") ?: response.body()?.string("url")
        if (link != null) {
            _events.tryEmit(TeamEvent.ShareInvite(link))
        } else {
            fail("Invite link was not returned")
        }
    }

    fun resolveInvite(token: String) = request("resolve-invite") {
        val response = api.resolveInvite(token)
        ensureOk(response)
        val root = response.body()!!
        val team = parseTeam(root.asJsonObject.obj("team") ?: root, null)
        _state.value = _state.value.copy(
            tab = TeamTab.JOIN,
            inviteToken = token,
            invitePreview = team,
            inviteRequiresAuth = root.asJsonObject.bool("requires_authentication") == true,
            loading = false
        )
    }

    fun requestInviteJoin() = action("invite-join") {
        val token = _state.value.inviteToken ?: return@action
        ensureOk(api.requestInviteJoin(obj("token", token)))
        _state.value = _state.value.copy(
            invitePreview = _state.value.invitePreview?.copy(joinStatus = JoinStatus.REQUESTED),
            message = "Join request sent"
        )
    }

    fun loadCities(query: String) = viewModelScope.launch {
        try {
            val result = userApi.getLocationCities(search = query.trim().ifBlank { null })
            if (result.isSuccessful) {
                val parsed = parseCities(result.body())
                _state.value = _state.value.copy(cities = parsed)
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }

    private fun parseCities(element: JsonElement?): List<LocationCityDto> {
        if (element == null || element.isJsonNull) return emptyList()
        return try {
            val array = when {
                element.isJsonArray -> element.asJsonArray
                element.isJsonObject && element.asJsonObject.has("results") && element.asJsonObject.get("results").isJsonArray ->
                    element.asJsonObject.getAsJsonArray("results")
                else -> null
            }
            if (array != null) {
                array.mapNotNull {
                    try {
                        gson.fromJson(it, LocationCityDto::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun citySelected(city: LocationCityDto) {
        val cityName = city.nameEn.orEmpty()
        val districtName = city.districtName.orEmpty()
        val provinceName = city.provinceName.orEmpty()
        updateForm {
            it.copy(
                cityId = city.id,
                city = cityName,
                location = cityName,
                district = districtName,
                province = provinceName
            )
        }
    }

    fun openChat(team: TeamUi) = action("chat-${team.id}") {
        val response = api.teamChat(team.id)
        ensureOk(response)
        val conversationId = response.body()?.string("id")
            ?: response.body()?.string("chat_id")
            ?: response.body()?.string("conversation_id")
        if (conversationId != null) {
            _events.tryEmit(TeamEvent.OpenChat(conversationId))
        } else {
            fail("Unable to open team chat")
        }
    }

    private fun validate(form: TeamFormState): String? {
        val days = form.expiryDays.toIntOrNull() ?: 14
        val hours = form.expiryHours.toIntOrNull() ?: 0
        val totalHours = days * 24 + hours

        return when {
            form.name.trim().isEmpty() -> "Team name is required"
            form.name.trim().length < 3 -> "Team name must be at least 3 characters"
            (form.maxMembers.toIntOrNull() ?: 0) < 1 -> "Max members must be at least 1"
            form.cityId == null -> "Please search and select a valid city"
            totalHours < 1 -> "Invite link expiry must be at least 1 hour"
            else -> null
        }
    }

    private fun request(key: String, isRefresh: Boolean = false, block: suspend () -> Unit) {
        if (!running.add(key)) return
        if (!isRefresh) _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                fail(e.message ?: "Network request failed")
            } finally {
                running.remove(key)
                if (_state.value.loading || _state.value.refreshing) {
                    _state.value = _state.value.copy(loading = false, refreshing = false)
                }
            }
        }
    }

    private fun action(key: String, block: suspend () -> Unit) {
        if (!running.add(key)) return
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                fail(e.message ?: "Request failed")
            } finally {
                running.remove(key)
                _state.value = _state.value.copy(saving = false)
            }
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(error = message, loading = false, saving = false, refreshing = false)
    }

    private fun ensureOk(response: retrofit2.Response<JsonElement>) {
        if (!response.isSuccessful) {
            throw IllegalStateException(response.errorBody()?.string()?.take(240) ?: "Request failed (${response.code()})")
        }
    }

    private fun obj(key: String, value: Any): JsonObject = JsonObject().also { it.addProperty(key, value.toString()) }

    private fun makeFullUrl(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        if (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("data:")) return uri
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = if (uri.startsWith("/")) uri else "/$uri"
        return "$base$path"
    }

    private fun parseTeams(element: JsonElement): List<TeamUi> =
        array(element).mapNotNull { if (it.isJsonObject) parseTeam(it, null) else null }

    private fun parseMemberships(element: JsonElement): List<TeamMembershipUi> =
        array(element).mapNotNull { if (it.isJsonObject) membership(it.asJsonObject) else null }

    private fun parseMembers(element: JsonElement): List<TeamMemberUi> =
        array(element).mapNotNull { if (it.isJsonObject) member(it.asJsonObject) else null }

    private fun array(element: JsonElement?): List<JsonElement> {
        if (element == null || element.isJsonNull) return emptyList()
        if (element.isJsonArray) return element.asJsonArray.toList()
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val keys = listOf("results", "data", "teams", "my_teams", "items")
            for (key in keys) {
                if (obj.has(key) && obj.get(key).isJsonArray) {
                    return obj.getAsJsonArray(key).toList()
                }
            }
        }
        return emptyList()
    }

    private fun parseTeam(element: JsonElement, fallback: TeamUi?): TeamUi {
        val o = if (element.isJsonObject) element.asJsonObject else JsonObject()
        val id = o.int("id") ?: fallback?.id ?: 0

        val statsObj = o.obj("challenge_stats")
        val challengeStats = statsObj?.let {
            ChallengeStatsUi(
                totalMatches = it.int("total_matches") ?: 0,
                wins = it.int("wins") ?: 0,
                losses = it.int("losses") ?: 0,
                noResults = it.int("no_results") ?: 0,
                winPercentage = it.float("win_percentage") ?: 0f
            )
        } ?: fallback?.challengeStats

        val recentMatchesList = o.obj("recent_matches")?.let { parseRecentMatches(it) }
            ?: o.get("recent_matches")?.takeIf { it.isJsonArray }?.let { parseRecentMatches(it) }
            ?: fallback?.recentMatches ?: emptyList()

        val recentMatchesCount = o.int("recent_matches_count") ?: fallback?.recentMatchesCount ?: recentMatchesList.size

        val extractedRole = o.string("role", "user_role", "membership_role", "my_role")
            ?: o.obj("membership")?.string("role")
            ?: if (o.bool("is_captain") == true) "Captain"
               else if (o.bool("is_admin") == true) "Admin"
               else if (o.bool("is_member") == true) "Member"
               else fallback?.role

        return TeamUi(
            id = id,
            name = o.string("name") ?: fallback?.name ?: "Team",
            description = o.string("description") ?: fallback?.description ?: "",
            membersCount = o.int("members_count", "member_count") ?: fallback?.membersCount ?: 0,
            role = extractedRole,
            logo = makeFullUrl(o.string("logo", "team_logo") ?: fallback?.logo),
            cover = makeFullUrl(o.string("cover_image", "cover") ?: fallback?.cover),
            isPublic = o.bool("is_public") ?: fallback?.isPublic ?: true,
            maxMembers = o.int("max_members") ?: fallback?.maxMembers ?: 20,
            location = o.string("location") ?: fallback?.location ?: "",
            cityId = o.int("city_id") ?: fallback?.cityId,
            city = o.string("city") ?: fallback?.city ?: "",
            district = o.string("district") ?: fallback?.district ?: "",
            province = o.string("province_name", "province") ?: fallback?.province ?: "",
            type = o.string("team_type", "type") ?: fallback?.type ?: "friends",
            sports = o.strings("preferred_sports", "sports").ifEmpty { fallback?.sports ?: emptyList() },
            joinStatus = parseJoinStatus(o.string("join_status")),
            challengeStats = challengeStats,
            recentMatches = recentMatchesList,
            recentMatchesCount = recentMatchesCount,
            recentMatchesPage = fallback?.recentMatchesPage ?: 1
        )
    }

    private fun parseRecentMatches(element: JsonElement): List<RecentMatchUi> =
        array(element).mapNotNull { if (it.isJsonObject) matchItem(it.asJsonObject) else null }

    private fun matchItem(o: JsonObject): RecentMatchUi {
        val opponentObj = o.obj("opponent")
        val opponentName = opponentObj?.string("name") ?: o.string("opponent_name") ?: "Opponent"
        val opponentLogo = makeFullUrl(opponentObj?.string("logo") ?: o.string("opponent_logo"))

        val teamScoreObj = o.obj("team_score")
        val teamScore = teamScoreObj?.let {
            MatchScoreUi(it.int("runs") ?: 0, it.int("wickets") ?: 0, it.string("overs") ?: "0.0")
        }

        val oppScoreObj = o.obj("opponent_score")
        val oppScore = oppScoreObj?.let {
            MatchScoreUi(it.int("runs") ?: 0, it.int("wickets") ?: 0, it.string("overs") ?: "0.0")
        }

        return RecentMatchUi(
            id = o.string("id") ?: System.currentTimeMillis().toString(),
            opponentName = opponentName,
            opponentLogo = opponentLogo,
            result = o.string("result") ?: "no_result",
            status = o.string("status") ?: "",
            matchDate = o.string("match_date", "completed_at") ?: "",
            venue = o.string("venue") ?: "",
            margin = o.string("margin") ?: "",
            teamScore = teamScore,
            opponentScore = oppScore
        )
    }

    private fun member(o: JsonObject) = TeamMemberUi(
        id = o.int("id", "user_id") ?: 0,
        name = o.string("full_name", "name") ?: "Member",
        username = o.string("username") ?: "",
        role = o.string("role") ?: "Member",
        email = o.string("email") ?: "",
        avatar = makeFullUrl(o.string("avatar", "profile_picture", "user_avatar"))
    )

    private fun membership(o: JsonObject) = TeamMembershipUi(
        id = o.int("id") ?: 0,
        teamId = o.int("team", "team_id") ?: 0,
        teamName = o.string("team_name") ?: "Team",
        userId = o.int("user", "user_id") ?: 0,
        name = o.string("full_name", "name") ?: "Member",
        username = o.string("username") ?: "",
        status = o.string("status") ?: "requested",
        requestedAt = o.string("requested_at") ?: "",
        invitedBy = o.string("invited_by_name", "admin_name") ?: "",
        avatar = makeFullUrl(o.string("user_avatar", "profile_picture")),
        teamLogo = makeFullUrl(o.string("team_logo"))
    )

    private fun TeamUi.toForm() = TeamFormState(
        name = name,
        description = description,
        teamType = type,
        location = location,
        cityId = cityId,
        city = city,
        district = district,
        province = province,
        maxMembers = maxMembers.toString(),
        expiryDays = "14",
        expiryHours = "0",
        isPublic = isPublic,
        preferredSports = sports,
        logoUrl = logo,
        coverUrl = cover
    )

    private fun JsonObject.string(vararg names: String): String? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.asString }
    private fun JsonObject.int(vararg names: String): Int? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() } }
    private fun JsonObject.float(vararg names: String): Float? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asFloat }.getOrNull() } }
    private fun JsonObject.bool(vararg names: String): Boolean? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asBoolean }.getOrNull() } }
    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.strings(vararg names: String): List<String> = names.asSequence().mapNotNull { get(it) }.firstOrNull()?.let { e -> if (e.isJsonArray) e.asJsonArray.mapNotNull { it.takeIf { x -> x.isJsonPrimitive }?.asString } else listOf(e.asString) } ?: emptyList()
    private fun JsonElement.string(name: String): String? = takeIf { isJsonObject }?.asJsonObject?.string(name)
    private fun parseJoinStatus(value: String?): JoinStatus = when (value?.lowercase()) {
        "requested" -> JoinStatus.REQUESTED
        "approved" -> JoinStatus.APPROVED
        "rejected" -> JoinStatus.REJECTED
        "member" -> JoinStatus.MEMBER
        else -> JoinStatus.NONE
    }
}

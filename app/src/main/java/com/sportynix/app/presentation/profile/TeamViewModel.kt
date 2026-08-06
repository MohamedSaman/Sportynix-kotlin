package com.sportynix.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sportynix.app.data.remote.api.TeamApiService
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.dto.LocationCityDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

enum class TeamTab { MY_TEAMS, JOIN, INVITATIONS }
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
    val coverUrl: String? = null
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
    val joinStatus: JoinStatus = JoinStatus.NONE
)

data class TeamState(
    val tab: TeamTab = TeamTab.MY_TEAMS,
    val loading: Boolean = false,
    val saving: Boolean = false,
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
    private val gson: Gson
) : ViewModel() {
    private val _state = MutableStateFlow(TeamState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<TeamEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    private val running = mutableSetOf<String>()

    init { loadMyTeams() }

    fun dismissMessage() { _state.value = _state.value.copy(message = null, error = null) }
    fun selectTab(tab: TeamTab) {
        val current = _state.value
        if (current.tab == tab && !current.loading && when (tab) {
                TeamTab.MY_TEAMS -> current.teams.isNotEmpty()
                TeamTab.JOIN -> current.discover.isNotEmpty()
                TeamTab.INVITATIONS -> current.received.isNotEmpty() || current.sent.isNotEmpty()
            }) return
        _state.value = _state.value.copy(tab = tab, error = null)
        when (tab) { TeamTab.MY_TEAMS -> loadMyTeams(); TeamTab.JOIN -> loadDiscover(); TeamTab.INVITATIONS -> loadInvitations() }
    }
    fun openCreate() { _state.value = _state.value.copy(showForm = true, editing = false, form = TeamFormState()) }
    fun openEdit(team: TeamUi) { _state.value = _state.value.copy(showForm = true, editing = true, form = team.toForm()) }
    fun closeForm() { _state.value = _state.value.copy(showForm = false, editing = false) }
    fun updateForm(form: TeamFormState) { _state.value = _state.value.copy(form = form) }
    fun updateForm(transform: (TeamFormState) -> TeamFormState) { updateForm(transform(_state.value.form)) }

    fun loadMyTeams() = request("my-teams") {
        val response = api.myTeams(); ensureOk(response)
        val all = parseTeams(response.body()!!)
        _state.value = _state.value.copy(teams = all.filter { it.role != null }, loading = false)
    }
    fun loadDiscover() = request("discover") {
        val response = api.discoverTeams(); ensureOk(response)
        val mine = _state.value.teams.map { it.id }.toSet()
        _state.value = _state.value.copy(discover = parseTeams(response.body()!!).filter { it.id !in mine && it.joinStatus !in setOf(JoinStatus.MEMBER, JoinStatus.APPROVED) }, loading = false)
    }
    fun loadInvitations() = request("invitations") {
        val received = api.receivedInvitations(); val sent = api.sentInvitations()
        val recvList = if (received.code() == 404) emptyList() else { ensureOk(received); parseMemberships(received.body()!!) }
        val sentList = if (sent.code() == 404) emptyList() else { ensureOk(sent); parseMemberships(sent.body()!!) }
        _state.value = _state.value.copy(received = recvList, sent = sentList, loading = false)
    }
    fun openDetails(team: TeamUi) = request("details-${team.id}") {
        val response = api.details(team.id); ensureOk(response)
        val root = response.body()!!
        val details = parseTeam(root, team)
        val members = root.asJsonObject.obj("members")?.let { parseMembers(it) } ?: emptyList()
        _state.value = _state.value.copy(selected = details, members = members, showDetails = true, loading = false)
        if (details.role == "Captain" || details.role == "Admin") loadPending(team.id)
    }
    fun closeDetails() { _state.value = _state.value.copy(showDetails = false, selected = null, searchedUsers = emptyList()) }
    fun loadPending(teamId: Int) = request("pending-$teamId") {
        val response = api.pendingMembers(teamId); ensureOk(response)
        _state.value = _state.value.copy(pending = parseMemberships(response.body()!!), loading = false)
    }
    fun searchMembers(query: String) {
        val id = _state.value.selected?.id ?: return
        if (query.trim().isEmpty()) { _state.value = _state.value.copy(searchedUsers = emptyList()); return }
        request("search-$id") {
            val response = api.searchMembers(id, query.trim()); ensureOk(response)
            _state.value = _state.value.copy(searchedUsers = parseMembers(response.body()!!), loading = false)
        }
    }

    fun saveTeam(logo: TeamImagePart? = null, cover: TeamImagePart? = null) = action("save") {
        val form = _state.value.form
        val validation = validate(form)
        if (validation != null) { fail(validation); return@action }
        val fields = mutableMapOf<String, okhttp3.RequestBody>()
        fun field(key: String, value: String) { fields[key] = value.toRequestBody() }
        field("name", form.name.trim()); field("description", form.description.trim()); field("team_type", form.teamType)
        field("location", form.city.ifBlank { form.location.trim() }); field("city", form.city); field("district", form.district); field("province_name", form.province)
        form.cityId?.let { field("city_id", it.toString()) }
        field("max_members", (form.maxMembers.toIntOrNull() ?: 20).toString())
        field("invite_link_expiry_hours", ((form.expiryDays.toIntOrNull() ?: 14) * 24 + (form.expiryHours.toIntOrNull() ?: 0)).coerceAtLeast(1).toString())
        field("is_public", form.isPublic.toString()); field("preferred_sports", gson.toJson(form.preferredSports))
        val result = if (_state.value.editing) api.update(_state.value.selected!!.id, fields, logo?.part, cover?.part) else api.create(fields, logo?.part, cover?.part)
        ensureOk(result)
        _state.value = _state.value.copy(showForm = false, editing = false, saving = false, message = if (_state.value.editing) "Team updated successfully" else "Team created successfully", form = TeamFormState())
        loadMyTeams()
    }
    fun requestJoin(team: TeamUi) = action("join-${team.id}") {
        val response = api.requestJoin(team.id); ensureOk(response)
        _state.value = _state.value.copy(discover = _state.value.discover.map { if (it.id == team.id) it.copy(joinStatus = JoinStatus.REQUESTED) else it }, message = "Join request sent")
    }
    fun cancelJoin(team: TeamUi) = action("cancel-join-${team.id}") {
        ensureOk(api.cancelJoin(team.id)); _state.value = _state.value.copy(discover = _state.value.discover.map { if (it.id == team.id) it.copy(joinStatus = JoinStatus.NONE) else it }, message = "Join request cancelled")
    }
    fun acceptInvitation(item: TeamMembershipUi) = action("accept-${item.id}") {
        ensureOk(api.acceptInvitation(item.teamId, obj("membership_id", item.id))); _state.value = _state.value.copy(message = "Invitation accepted"); loadInvitations(); loadMyTeams()
    }
    fun rejectInvitation(item: TeamMembershipUi) = action("reject-inv-${item.id}") {
        ensureOk(api.rejectInvitation(item.teamId, obj("membership_id", item.id))); _state.value = _state.value.copy(message = "Invitation rejected"); loadInvitations()
    }
    fun invite(user: TeamMemberUi) = action("invite-${user.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        val response = api.invite(teamId, obj("user_id", user.id)); ensureOk(response)
        _state.value = _state.value.copy(message = if (response.body()?.string("membership_status") == "approved") "Member added to team" else "Invitation sent", searchedUsers = emptyList())
        openDetails(_state.value.selected!!)
    }
    fun approve(item: TeamMembershipUi) = action("approve-${item.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.approveMembership(teamId, obj("membership_id", item.id))); _state.value = _state.value.copy(message = "Membership approved"); openDetails(_state.value.selected!!)
    }
    fun reject(item: TeamMembershipUi) = action("reject-${item.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.rejectMembership(teamId, obj("membership_id", item.id))); _state.value = _state.value.copy(message = "Membership rejected"); loadPending(teamId)
    }
    fun removeMember(member: TeamMemberUi) = action("remove-${member.id}") {
        val teamId = _state.value.selected?.id ?: return@action
        ensureOk(api.removeMember(teamId, obj("user_id", member.id))); _state.value = _state.value.copy(message = "Member removed"); openDetails(_state.value.selected!!)
    }
    fun leave(team: TeamUi) = action("leave-${team.id}") { ensureOk(api.leave(team.id)); _state.value = _state.value.copy(showDetails = false, message = "You left the team"); loadMyTeams() }
    fun delete(team: TeamUi, force: Boolean = false) = action("delete-${team.id}") {
        val response = if (force) api.forceDelete(team.id) else api.delete(team.id)
        if (!response.isSuccessful && response.code() == 400 && !force) { fail("This team has active members. Remove them first or use force delete."); return@action }
        ensureOk(response); _state.value = _state.value.copy(showDetails = false, message = "Team deleted"); loadMyTeams()
    }
    fun generateInvite(team: TeamUi) = action("link-${team.id}") { val response = api.generateInviteLink(team.id); ensureOk(response); response.body()?.string("invite_link")?.let { _events.tryEmit(TeamEvent.ShareInvite(it)) } ?: fail("Invite link was not returned") }
    fun resolveInvite(token: String) = request("resolve-invite") {
        val response = api.resolveInvite(token); ensureOk(response); val root = response.body()!!; val team = parseTeam(root.asJsonObject.obj("team") ?: root, null)
        _state.value = _state.value.copy(tab = TeamTab.JOIN, inviteToken = token, invitePreview = team, inviteRequiresAuth = root.asJsonObject.bool("requires_authentication") == true)
    }
    fun requestInviteJoin() = action("invite-join") {
        val token = _state.value.inviteToken ?: return@action
        ensureOk(api.requestInviteJoin(obj("token", token))); _state.value = _state.value.copy(invitePreview = _state.value.invitePreview?.copy(joinStatus = JoinStatus.REQUESTED), message = "Join request sent")
    }
    fun loadCities(query: String) = viewModelScope.launch {
        val result = userApi.getLocationCities(search = query.trim().ifBlank { null })
        if (result.isSuccessful) _state.value = _state.value.copy(cities = result.body().orEmpty())
    }
    fun citySelected(city: LocationCityDto) { updateForm { it.copy(cityId = city.id, city = city.nameEn, location = city.nameEn, district = city.districtName, province = city.provinceName) } }
    fun openChat(team: TeamUi) = action("chat-${team.id}") { val response = api.teamChat(team.id); ensureOk(response); response.body()?.string("id")?.let { _events.tryEmit(TeamEvent.OpenChat(it)) } ?: fail("Unable to open team chat") }

    private fun validate(form: TeamFormState): String? = when {
        form.name.trim().isEmpty() -> "Team name is required"
        form.name.trim().length < 3 -> "Team name must be at least 3 characters"
        (form.maxMembers.toIntOrNull() ?: 0) < 1 -> "Max members must be at least 1"
        form.cityId == null -> "Please search and select a valid city"
        else -> null
    }
    private fun request(key: String, block: suspend () -> Unit) { if (!running.add(key)) return; _state.value = _state.value.copy(loading = true, error = null); viewModelScope.launch { try { block() } catch (e: Exception) { fail(e.message ?: "Network request failed") } finally { running.remove(key); if (_state.value.loading) _state.value = _state.value.copy(loading = false) } } }
    private fun action(key: String, block: suspend () -> Unit) { if (!running.add(key)) return; _state.value = _state.value.copy(saving = true, error = null); viewModelScope.launch { try { block() } catch (e: Exception) { fail(e.message ?: "Request failed") } finally { running.remove(key); _state.value = _state.value.copy(saving = false) } } }
    private fun fail(message: String) { _state.value = _state.value.copy(error = message, loading = false, saving = false) }
    private fun ensureOk(response: retrofit2.Response<JsonElement>) { if (!response.isSuccessful) throw IllegalStateException(response.errorBody()?.string()?.take(240) ?: "Request failed (${response.code()})") }
    private fun obj(key: String, value: Any): JsonObject = JsonObject().also { it.addProperty(key, value.toString()) }

    private fun parseTeams(element: JsonElement): List<TeamUi> = array(element).mapNotNull { if (it.isJsonObject) parseTeam(it, null) else null }
    private fun parseMemberships(element: JsonElement): List<TeamMembershipUi> = array(element).mapNotNull { if (!it.isJsonObject) null else membership(it.asJsonObject) }
    private fun parseMembers(element: JsonElement): List<TeamMemberUi> = array(element).mapNotNull { if (!it.isJsonObject) null else member(it.asJsonObject) }
    private fun array(element: JsonElement): List<JsonElement> = when { element.isJsonArray -> element.asJsonArray.toList(); element.isJsonObject && element.asJsonObject.obj("results") != null -> element.asJsonObject.obj("results")!!.asJsonArray.toList(); else -> emptyList() }
    private fun parseTeam(element: JsonElement, fallback: TeamUi?): TeamUi {
        val o = if (element.isJsonObject) element.asJsonObject else JsonObject(); val id = o.int("id") ?: fallback?.id ?: 0
        return TeamUi(id, o.string("name") ?: fallback?.name ?: "Team", o.string("description") ?: "", o.int("members_count", "member_count") ?: fallback?.membersCount ?: 0, o.string("role") ?: fallback?.role, o.string("logo", "team_logo"), o.string("cover_image", "cover"), o.bool("is_public") ?: true, o.int("max_members") ?: 20, o.string("location") ?: "", o.int("city_id"), o.string("city") ?: "", o.string("district") ?: "", o.string("province_name", "province") ?: "", o.string("team_type", "type") ?: "friends", o.strings("preferred_sports", "sports"), parseJoinStatus(o.string("join_status")))
    }
    private fun member(o: JsonObject) = TeamMemberUi(o.int("id", "user_id") ?: 0, o.string("full_name", "name") ?: "Member", o.string("username") ?: "", o.string("role") ?: "Member", o.string("email") ?: "", o.string("avatar", "profile_picture", "user_avatar"))
    private fun membership(o: JsonObject) = TeamMembershipUi(o.int("id") ?: 0, o.int("team", "team_id") ?: 0, o.string("team_name") ?: "Team", o.int("user", "user_id") ?: 0, o.string("full_name", "name") ?: "Member", o.string("username") ?: "", o.string("status") ?: "requested", o.string("requested_at") ?: "", o.string("invited_by_name", "admin_name") ?: "", o.string("user_avatar", "profile_picture", "team_logo"))
    private fun TeamUi.toForm() = TeamFormState(name, description, type, location, cityId, city, district, province, maxMembers.toString(), "14", "0", isPublic, sports, logo, cover)
    private fun JsonObject.string(vararg names: String): String? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.asString }
    private fun JsonObject.int(vararg names: String): Int? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() } }
    private fun JsonObject.bool(vararg names: String): Boolean? = names.firstNotNullOfOrNull { n -> get(n)?.takeIf { !it.isJsonNull }?.let { runCatching { it.asBoolean }.getOrNull() } }
    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.strings(vararg names: String): List<String> = names.asSequence().mapNotNull { get(it) }.firstOrNull()?.let { e -> if (e.isJsonArray) e.asJsonArray.mapNotNull { it.takeIf { x -> x.isJsonPrimitive }?.asString } else listOf(e.asString) } ?: emptyList()
    private fun JsonElement.string(name: String): String? = takeIf { isJsonObject }?.asJsonObject?.string(name)
    private fun parseJoinStatus(value: String?): JoinStatus = when (value?.lowercase()) { "requested" -> JoinStatus.REQUESTED; "approved" -> JoinStatus.APPROVED; "rejected" -> JoinStatus.REJECTED; "member" -> JoinStatus.MEMBER; else -> JoinStatus.NONE }
}

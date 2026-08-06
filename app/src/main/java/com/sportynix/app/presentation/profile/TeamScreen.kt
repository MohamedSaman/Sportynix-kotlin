package com.sportynix.app.presentation.profile

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    onNavigateBack: () -> Unit,
    initialTab: Int = 0,
    inviteToken: String? = null,
    teamId: Int? = null,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: TeamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isDark = LocalThemeController.current.isDark
    val green = if (isDark) NeonGreen else SportynixGreenPrimary
    var imageTarget by remember { mutableStateOf<String?>(null) }
    var logoPart by remember { mutableStateOf<TeamImagePart?>(null) }
    var coverPart by remember { mutableStateOf<TeamImagePart?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val part = uri.toMultipart(context, imageTarget ?: "image")
            if (imageTarget == "logo") logoPart = part else coverPart = part
        }
    }

    LaunchedEffect(initialTab) { viewModel.selectTab(TeamTab.values().getOrElse(initialTab.coerceIn(0, 2)) { TeamTab.MY_TEAMS }) }
    LaunchedEffect(inviteToken) { if (!inviteToken.isNullOrBlank()) viewModel.resolveInvite(inviteToken) }
    LaunchedEffect(teamId, state.teams) {
        if (teamId != null && teamId > 0) {
            state.teams.firstOrNull { it.id == teamId }?.let { viewModel.openDetails(it) }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TeamEvent.OpenChat -> onNavigateToChat(event.conversationId)
                is TeamEvent.ShareInvite -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, event.link) }, "Share invite link"))
            }
        }
    }

    val error = state.error
    val message = state.message
    Scaffold(containerColor = if (isDark) DarkBackground else LightBackground) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground) }
                Column(Modifier.weight(1f)) { Text("My Teams", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Build your squad and play together", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                IconButton(onClick = viewModel::openCreate) { Icon(Icons.Default.Add, "Create team", tint = green) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).background(if (isDark) Color(0x99162238) else Color.White).border(1.dp, green.copy(.25f), RoundedCornerShape(20.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TeamTab.values().forEach { tab ->
                    val selected = tab == state.tab
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (selected) Brush.horizontalGradient(listOf(Color(0xFF00B86B), Color(0xFF00E58A))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))).clickable { viewModel.selectTab(tab) }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                        Text(tab.label, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
                    }
                }
            }
            if (state.loading && state.teams.isEmpty() && state.discover.isEmpty() && state.received.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = green) }
            else when (state.tab) {
                TeamTab.MY_TEAMS -> MyTeams(state, green, viewModel)
                TeamTab.JOIN -> DiscoverTeams(state, green, viewModel)
                TeamTab.INVITATIONS -> Invitations(state, green, viewModel)
            }
        }
    }

    if (state.showForm) {
        TeamFormDialog(state, green, viewModel, onPickLogo = { imageTarget = "logo"; picker.launch("image/*") }, onPickCover = { imageTarget = "cover"; picker.launch("image/*") }, logoPart, coverPart)
    }
    if (state.showDetails && state.selected != null) TeamDetailsSheet(state, green, viewModel)
    if (!error.isNullOrBlank() || !message.isNullOrBlank()) AlertDialog(onDismissRequest = viewModel::dismissMessage, title = { Text(if (error != null) "Team" else "Success") }, text = { Text(error ?: message.orEmpty()) }, confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("OK", color = green) } })
}

@Composable private fun MyTeams(state: TeamState, green: Color, vm: TeamViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (state.teams.isEmpty()) item { EmptyTeams(green, vm) }
        items(state.teams, key = { it.id }) { team -> TeamCard(team, green, onClick = { vm.openDetails(team) }, onEdit = { vm.openEdit(team) }) }
    }
}

@Composable private fun DiscoverTeams(state: TeamState, green: Color, vm: TeamViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (state.invitePreview != null) item { InvitePreview(state.invitePreview, state.inviteRequiresAuth, green, vm) }
        if (state.discover.isEmpty() && state.invitePreview == null) item { EmptyText("No public teams available to join") }
        items(state.discover, key = { it.id }) { team -> TeamCard(team, green, onClick = { vm.openDetails(team) }, joinAction = { if (team.joinStatus == JoinStatus.REQUESTED) vm.cancelJoin(team) else vm.requestJoin(team) }) }
    }
}

@Composable private fun Invitations(state: TeamState, green: Color, vm: TeamViewModel) {
    var sent by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Received", "Sent").forEachIndexed { i, label -> OutlinedButton(onClick = { sent = i == 1 }, colors = ButtonDefaults.outlinedButtonColors(containerColor = if (sent == (i == 1)) green.copy(.16f) else Color.Transparent, contentColor = if (sent == (i == 1)) green else MaterialTheme.colorScheme.onSurfaceVariant), border = BorderStroke(1.dp, green.copy(.35f))) { Text(label) } } }
        val list = if (sent) state.sent else state.received
        if (list.isEmpty()) EmptyText(if (sent) "No sent invitations" else "No received invitations") else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(list, key = { it.id }) { item -> InvitationCard(item, sent, green, vm) } }
    }
}

@Composable private fun EmptyTeams(green: Color, vm: TeamViewModel) { GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 3.dp) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Groups, null, tint = green, modifier = Modifier.size(42.dp)); Text("You haven't joined a team yet", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Create a team to invite friends and organize matches", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); Spacer(Modifier.height(14.dp)); Button(onClick = vm::openCreate, colors = ButtonDefaults.buttonColors(containerColor = green)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Create a Team") } } } }
@Composable private fun EmptyText(text: String) { Box(Modifier.fillMaxWidth().padding(42.dp), Alignment.Center) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun TeamCard(team: TeamUi, green: Color, onClick: () -> Unit, onEdit: (() -> Unit)? = null, joinAction: (() -> Unit)? = null) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), elevation = 6.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(112.dp)) { if (!team.cover.isNullOrBlank()) AsyncImage(team.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0C6B49), Color(0xFF00B86B)))))
                if (team.role != null) Surface(Modifier.align(Alignment.TopEnd).padding(10.dp), shape = RoundedCornerShape(10.dp), color = Color.Black.copy(.6f)) { Text(team.role, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!team.logo.isNullOrBlank()) AsyncImage(team.logo, team.name, Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Box(Modifier.size(50.dp).clip(CircleShape).background(green), Alignment.Center) { Text(team.name.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(team.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(team.type.replaceFirstChar { it.uppercase() }, color = green, fontSize = 12.sp, fontWeight = FontWeight.Bold); if (team.location.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = green, modifier = Modifier.size(14.dp)); Text(team.location, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("${team.membersCount}/${team.maxMembers} members", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Default.MoreVert, "Team actions") }
            }
            if (joinAction != null) { Button(onClick = joinAction, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (team.joinStatus == JoinStatus.REQUESTED) Color.Transparent else green), border = if (team.joinStatus == JoinStatus.REQUESTED) BorderStroke(1.dp, green) else null) { Text(if (team.joinStatus == JoinStatus.REQUESTED) "Cancel request" else "Request to join", color = if (team.joinStatus == JoinStatus.REQUESTED) green else Color.White) } ; Spacer(Modifier.height(14.dp)) }
        }
    }
}

@Composable private fun InvitationCard(item: TeamMembershipUi, sent: Boolean, green: Color, vm: TeamViewModel) { GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = 3.dp) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { if (!item.teamLogo.isNullOrBlank()) AsyncImage(item.teamLogo, null, Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Box(Modifier.size(46.dp).clip(CircleShape).background(green), Alignment.Center) { Icon(Icons.Default.Group, null, tint = Color.White) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(item.teamName, fontWeight = FontWeight.Bold); Text(if (sent) "Invitation to ${item.name}" else "Invited by ${item.invitedBy.ifBlank { "team admin" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(item.status.replaceFirstChar { it.uppercase() }, color = if (item.status == "approved") green else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; if (!sent && item.status == "invited") { IconButton(onClick = { vm.acceptInvitation(item) }) { Icon(Icons.Default.Check, "Accept", tint = green) }; IconButton(onClick = { vm.rejectInvitation(item) }) { Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error) } } } } }

@Composable private fun InvitePreview(team: TeamUi, requiresAuth: Boolean, green: Color, vm: TeamViewModel) { GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 5.dp) { Column(Modifier.padding(18.dp)) { Text("Team invite", color = green, fontWeight = FontWeight.Bold); Text(team.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("${team.membersCount}/${team.maxMembers} members", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); Button(onClick = vm::requestInviteJoin, enabled = !requiresAuth && team.joinStatus == JoinStatus.NONE, colors = ButtonDefaults.buttonColors(containerColor = green)) { Text(if (requiresAuth) "Sign in to join" else if (team.joinStatus == JoinStatus.REQUESTED) "Request sent" else "Request to join") } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TeamDetailsSheet(state: TeamState, green: Color, vm: TeamViewModel) {
    val team = state.selected ?: return
    var inviteQuery by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = vm::closeDetails, containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 18.dp), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text(team.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = { vm.openChat(team) }) { Icon(Icons.Default.ChatBubble, "Open team chat", tint = green) }; IconButton(onClick = { vm.generateInvite(team) }) { Icon(Icons.Default.Share, "Share invite", tint = green) } } }
            item { Text(team.description.ifBlank { "No team description" }, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${team.membersCount}/${team.maxMembers} members • ${team.type}", color = green, fontWeight = FontWeight.SemiBold) }
            if (team.role == "Captain" || team.role == "Admin") item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { vm.openEdit(team) }, border = BorderStroke(1.dp, green), colors = ButtonDefaults.outlinedButtonColors(contentColor = green)) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit") }; OutlinedButton(onClick = { vm.delete(team) }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete") } } }
            if (team.role != "Captain" && team.role != "Admin") item { OutlinedButton(onClick = { vm.leave(team) }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Leave team") } }
            item { Divider(); Text("Members", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            items(state.members, key = { it.id }) { member -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(CircleShape).background(green), Alignment.Center) { Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(member.name, fontWeight = FontWeight.SemiBold); Text("${member.role}${if (member.username.isBlank()) "" else " • @${member.username}"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; if ((team.role == "Captain" || team.role == "Admin") && member.role != "Captain") IconButton(onClick = { vm.removeMember(member) }) { Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error) } } }
            if (team.role == "Captain" || team.role == "Admin") item {
                Text("Pending requests", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                state.pending.forEach { request -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(request.name, Modifier.weight(1f)); IconButton(onClick = { vm.approve(request) }) { Icon(Icons.Default.Check, "Approve", tint = green) }; IconButton(onClick = { vm.reject(request) }) { Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error) } } }
                OutlinedTextField(value = inviteQuery, onValueChange = { inviteQuery = it; vm.searchMembers(it) }, label = { Text("Search people to invite") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                state.searchedUsers.forEach { user -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(user.name, Modifier.weight(1f)); TextButton(onClick = { vm.invite(user) }) { Text("Invite", color = green) } } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TeamFormDialog(state: TeamState, green: Color, vm: TeamViewModel, onPickLogo: () -> Unit, onPickCover: () -> Unit, logoPart: TeamImagePart?, coverPart: TeamImagePart?) {
    val form = state.form
    var cityMenu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = vm::closeForm, title = { Text(if (state.editing) "Edit Team" else "Create Team") }, text = {
        Column(Modifier.fillMaxWidth().height(560.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item { OutlinedTextField(form.name, { value -> vm.updateForm { current -> current.copy(name = value) } }, label = { Text("Team name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(form.description, { value -> vm.updateForm { current -> current.copy(description = value) } }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("friends", "community", "competitive").forEach { type -> FilterChip(selected = form.teamType == type, onClick = { vm.updateForm { current -> current.copy(teamType = type) } }, label = { Text(type.replaceFirstChar { it.uppercase() }) }) } } }
                item { OutlinedTextField(form.maxMembers, { value -> vm.updateForm { current -> current.copy(maxMembers = value.filter { it.isDigit() }) } }, label = { Text("Maximum members") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { ExposedDropdownMenuBox(expanded = cityMenu, onExpandedChange = { cityMenu = !cityMenu }) { OutlinedTextField(form.city, { value -> vm.updateForm { current -> current.copy(city = value, cityId = null) }; vm.loadCities(value) }, label = { Text("City (select a result)") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cityMenu) }, modifier = Modifier.fillMaxWidth(), singleLine = true); ExposedDropdownMenu(expanded = cityMenu && state.cities.isNotEmpty(), onDismissRequest = { cityMenu = false }) { state.cities.take(8).forEach { city -> androidx.compose.material3.DropdownMenuItem(text = { Text("${city.nameEn}, ${city.districtName}") }, onClick = { vm.citySelected(city); cityMenu = false }) } } } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Public team", Modifier.weight(1f)); Switch(checked = form.isPublic, onCheckedChange = { value -> vm.updateForm { current -> current.copy(isPublic = value) } }) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onPickLogo, border = BorderStroke(1.dp, green), colors = ButtonDefaults.outlinedButtonColors(contentColor = green)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(4.dp)); Text(if (logoPart == null) "Team logo" else "Logo selected") }; OutlinedButton(onClick = onPickCover, border = BorderStroke(1.dp, green), colors = ButtonDefaults.outlinedButtonColors(contentColor = green)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(4.dp)); Text(if (coverPart == null) "Cover image" else "Cover selected") } } }
            }
        }
    }, confirmButton = { Button(onClick = { vm.saveTeam(logoPart, coverPart) }, enabled = !state.saving, colors = ButtonDefaults.buttonColors(containerColor = green)) { if (state.saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (state.editing) "Save changes" else "Create team") } }, dismissButton = { TextButton(onClick = vm::closeForm) { Text("Cancel") } })
}

private fun Uri.toMultipart(context: android.content.Context, field: String): TeamImagePart? = runCatching {
    val resolver = context.contentResolver; val bytes = resolver.openInputStream(this)?.use { it.readBytes() } ?: return null
    val type = resolver.getType(this) ?: "image/jpeg"; val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: "jpg"
    TeamImagePart(MultipartBody.Part.createFormData(field, "team_${field}_${System.currentTimeMillis()}.$ext", bytes.toRequestBody(type.toMediaType())))
}.getOrNull()

private val TeamTab.label: String get() = when (this) { TeamTab.MY_TEAMS -> "My Teams"; TeamTab.JOIN -> "Join"; TeamTab.INVITATIONS -> "Invitations" }

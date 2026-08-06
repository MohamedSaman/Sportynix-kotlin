package com.sportynix.app.presentation.challenge

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ChallengeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    vm: ChallengeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme
    LaunchedEffect(Unit) { vm.events.collectLatest { event -> if (event is ChallengeEvent.OpenChat) onNavigateToChat(event.conversationId) } }
    Surface(color = bg) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                ChallengeHeader(state, green, onNavigateBack, vm)
                if (state.tab == ChallengeTab.FIND_TEAMS) FindTeams(state, green, vm) else MyChallenges(state, green, vm)
            }
            state.error?.let { msg -> AlertDialog(onDismissRequest = vm::dismissMessage, title = { Text("Challenge") }, text = { Text(msg) }, confirmButton = { TextButton(onClick = vm::dismissMessage) { Text("OK", color = green) } }) }
            state.message?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(2200); vm.dismissMessage() } }
            if (state.creating) CreateChallengeSheet(state, green, vm)
            state.selectedDetail?.let { ChallengeDetailDialog(it, green, vm) }
            state.selectedTeamDetail?.let { TeamPreviewDialog(it, green, vm) }
        }
    }
}

@Composable private fun ChallengeHeader(state: ChallengeState, green: Color, back: () -> Unit, vm: ChallengeViewModel) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) { Text("Challenges", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("Find your next rivalry", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, "Refresh", tint = green) }
            FilledIconButton(onClick = vm::openCreate, colors = IconButtonDefaults.filledIconButtonColors(containerColor = green)) { Icon(Icons.Default.Add, "Create") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(if (LocalThemeController.current.isDark) DarkSurface else LightSurface).padding(4.dp)) {
            Segment("Find Teams", state.tab == ChallengeTab.FIND_TEAMS, green) { vm.setTab(ChallengeTab.FIND_TEAMS) }
            Segment("My Challenges", state.tab == ChallengeTab.MY_CHALLENGES, green) { vm.setTab(ChallengeTab.MY_CHALLENGES) }
        }
    }
}
@Composable private fun RowScope.Segment(text: String, selected: Boolean, green: Color, onClick: () -> Unit) { Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (selected) green else Color.Transparent).clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } }

@Composable private fun FindTeams(state: ChallengeState, green: Color, vm: ChallengeViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { OutlinedTextField(value = state.search, onValueChange = vm::search, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search teams") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
        if (state.loading && state.opponents.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = green) } }
        if (!state.loading && state.opponents.isEmpty()) item { EmptyMessage("No opponent teams found", green) }
        items(state.opponents, key = { it.id }) { team -> TeamChallengeCard(team, green, onPreview = { vm.openTeam(team) }, onChallenge = { vm.openCreate(); vm.selectOpponent(team); vm.nextStep() }) }
        if (state.hasMoreOpponents && state.opponents.isNotEmpty()) item { OutlinedButton(onClick = vm::loadMoreOpponents, modifier = Modifier.fillMaxWidth()) { Text("Load more") } }
    }
}

@Composable private fun MyChallenges(state: ChallengeState, green: Color, vm: ChallengeViewModel) {
    val list = when (state.section) { ChallengeSection.INCOMING -> state.incoming; ChallengeSection.SENT -> state.sent; ChallengeSection.HISTORY -> state.history }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = state.section.ordinal, edgePadding = 18.dp, containerColor = Color.Transparent, divider = {}) { ChallengeSection.entries.forEach { section -> Tab(selected = state.section == section, onClick = { vm.setSection(section) }, text = { Text(section.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        if (state.loading && list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = green) }
        else if (list.isEmpty()) EmptyMessage("No ${state.section.name.lowercase()} challenges yet", green)
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(list, key = { it.id }) { ChallengeCard(it, green, vm) } }
    }
}

@Composable private fun TeamChallengeCard(team: ChallengeTeamUi, green: Color, onPreview: () -> Unit, onChallenge: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onPreview), shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { if (!team.logo.isNullOrBlank()) AsyncImage(team.logo, null, Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))) else Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(green), Alignment.Center) { Icon(Icons.Default.Groups, null, tint = Color.White) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(team.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("${team.members} members${if (team.location.isNotBlank()) " • ${team.location}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); if (team.sport.isNotBlank()) Text(team.sport, color = green, fontSize = 12.sp) }; Button(onClick = onChallenge, colors = ButtonDefaults.buttonColors(containerColor = green), shape = RoundedCornerShape(14.dp)) { Text("Challenge") } } }
}

@Composable private fun ChallengeCard(c: ChallengeUi, green: Color, vm: ChallengeViewModel) {
    val statusColor = when (c.status.lowercase()) { "accepted" -> green; "declined", "cancelled", "expired" -> MaterialTheme.colorScheme.error; else -> StatusWarning }
    GlassCard(Modifier.fillMaxWidth().clickable { vm.openDetails(c) }, shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(c.challenger, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f)); Text(c.status.replaceFirstChar { it.uppercase() }, color = statusColor, fontWeight = FontWeight.Bold) }; Text("vs  ${c.challenged}", color = MaterialTheme.colorScheme.onSurfaceVariant); if (c.sport.isNotBlank()) Text("${c.sport}${if (c.venue.isNotBlank()) " • ${c.venue}" else ""}", color = green, fontSize = 13.sp); c.date?.let { Text("${it}${c.start?.let { s -> "  $s${c.end?.let { e -> " - $e" } ?: ""}" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }; Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (c.canAccept) Button(onClick = { vm.accept(c) }, colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("Accept") }; if (c.canDecline) OutlinedButton(onClick = { vm.decline(c) }) { Text("Decline") }; if (c.canCancel) OutlinedButton(onClick = { vm.cancel(c) }) { Text("Cancel") }; if (c.status == "accepted" && c.chatId != null) TextButton(onClick = { vm.openChat(c) }) { Text("Open chat", color = green) } } } }
}

@Composable private fun EmptyMessage(text: String, green: Color) { Box(Modifier.fillMaxWidth().padding(45.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Sports, null, tint = green, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun CreateChallengeSheet(state: ChallengeState, green: Color, vm: ChallengeViewModel) {
    ModalBottomSheet(onDismissRequest = vm::closeCreate, containerColor = if (LocalThemeController.current.isDark) DarkSurface else LightSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Create Challenge", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)); IconButton(onClick = vm::closeCreate) { Icon(Icons.Default.Close, "Close") } }
            Text("Step ${state.step.ordinal + 1} of ${ChallengeStep.entries.size}", color = green, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            when (state.step) {
                ChallengeStep.MY_TEAM -> SelectionList("Select my team", state.myTeams, state.selectedTeam?.id, green, { it.id }, { it.name }, vm::selectTeam)
                ChallengeStep.OPPONENT -> SelectionList("Select opponent", state.opponents, state.selectedOpponent?.id, green, { it.id }, { it.name }, vm::selectOpponent)
                ChallengeStep.SPORT -> SelectionList("Select sport", state.sports, state.selectedSport?.id, green, { it.id }, { it.name }, vm::selectSport)
                ChallengeStep.VENUE -> { ToggleBookingType(state, green, vm); SelectionList("Select venue", state.venues, state.selectedVenue?.id, green, { it.id }, { it.name }, vm::selectVenue) }
                ChallengeStep.SLOT -> SlotStep(state, green, vm)
                ChallengeStep.REVIEW -> ReviewStep(state, green)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { if (state.step != ChallengeStep.MY_TEAM) OutlinedButton(onClick = vm::previousStep, modifier = Modifier.weight(1f)) { Text("Back") }; Button(onClick = vm::nextStep, enabled = !state.submitting, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = green)) { if (state.submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White) else Text(if (state.step == ChallengeStep.REVIEW) "Send challenge" else "Continue") } }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable private fun <T> SelectionList(title: String, values: List<T>, selected: Int?, green: Color, id: (T) -> Int, label: (T) -> String, onSelect: (T) -> Unit) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); if (values.isEmpty()) Text("No options available", color = MaterialTheme.colorScheme.onSurfaceVariant); else LazyColumn(Modifier.heightIn(max = 270.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(values) { value -> val active = selected == id(value); GlassCard(Modifier.fillMaxWidth().clickable { onSelect(value) }, shape = RoundedCornerShape(16.dp), backgroundColor = if (active) green.copy(alpha = .2f) else null, borderColor = if (active) green else null) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(label(value), modifier = Modifier.weight(1f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal); if (active) Icon(Icons.Default.CheckCircle, null, tint = green) } } } } }

@Composable private fun ToggleBookingType(state: ChallengeState, green: Color, vm: ChallengeViewModel) { Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ChallengeBookingType.entries.forEach { type -> FilterChip(selected = state.bookingType == type, onClick = { vm.setBookingType(type) }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }, leadingIcon = if (state.bookingType == type) ({ Icon(Icons.Default.Check, null) }) else null) } } }

@Composable private fun SlotStep(state: ChallengeState, green: Color, vm: ChallengeViewModel) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) { Text("Select date & slot", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = { val date = LocalDate.parse(state.date); DatePickerDialog(context, { _, y, m, d -> vm.setDate(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.minDate = System.currentTimeMillis(); datePicker.maxDate = System.currentTimeMillis() + 31L * 24 * 60 * 60 * 1000 }.show() }) { Text(LocalDate.parse(state.date).format(DateTimeFormatter.ofPattern("dd MMM yyyy")), color = green) } }
    Spacer(Modifier.height(8.dp)); if (state.loadingSlots) Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) { CircularProgressIndicator(color = green) } else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightIn(max = 330.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.slots, key = { it.key }) { item -> SlotCard(item, green) { vm.selectSlot(item) } } }
}
@Composable private fun SlotCard(item: ChallengeSlotUi, green: Color, onClick: () -> Unit) { val s = item.slot; val disabled = s.isPastTime == true || s.isHeld == true || s.isFullyBooked == true || s.available == false; val color = when { item.selected -> green; s.isFullyBooked == true -> StatusError; s.isHeld == true -> StatusWarning; disabled -> MaterialTheme.colorScheme.onSurfaceVariant; else -> green }; GlassCard(Modifier.fillMaxWidth().clickable(enabled = !disabled || item.selected, onClick = onClick), shape = RoundedCornerShape(16.dp), backgroundColor = if (item.selected) green else null, borderColor = color.copy(alpha = .65f)) { Column(Modifier.padding(13.dp)) { if (item.processing) CircularProgressIndicator(Modifier.size(18.dp), color = green) else { Text("${s.startTime ?: s.rawStart ?: ""} - ${s.endTime ?: s.rawEnd ?: ""}", fontWeight = FontWeight.Bold, color = if (item.selected) Color.White else MaterialTheme.colorScheme.onSurface); Text(if (item.selected) "✓ Selected" else if (s.isPastTime == true) "Time passed" else if (s.isHeld == true) "Held" else if (s.isFullyBooked == true) "Fully booked" else "Available", color = if (item.selected) Color.White else color, fontSize = 12.sp); Text("Rs. ${"%.2f".format(s.price ?: 0.0)}", color = if (item.selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } } }
@Composable private fun ReviewStep(state: ChallengeState, green: Color) { Text("Review challenge", fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("${state.selectedTeam?.name}  vs  ${state.selectedOpponent?.name}", fontWeight = FontWeight.Bold); Text(state.selectedSport?.name.orEmpty(), color = green); Text(state.selectedVenue?.name.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant); state.selectedSlot?.let { Text("${it.slot.startTime} - ${it.slot.endTime}") }; Text("Stake: Rs. ${"%.2f".format(state.selectedSport?.price ?: 0.0)}", color = green, fontWeight = FontWeight.Bold) } } }

@Composable private fun ChallengeDetailDialog(c: ChallengeUi, green: Color, vm: ChallengeViewModel) {
    AlertDialog(
        onDismissRequest = vm::closeDetails,
        title = { Text("Challenge details") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("${c.challenger}  vs  ${c.challenged}", fontWeight = FontWeight.Bold); Text("Status: ${c.status}"); if (c.sport.isNotBlank()) Text("Sport: ${c.sport}"); if (c.venue.isNotBlank()) Text("Venue: ${c.venue}"); c.date?.let { Text("Date: $it") } } },
        confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { if (c.canAccept) TextButton(onClick = { vm.accept(c) }) { Text("Accept", color = green) }; if (c.canDecline) TextButton(onClick = { vm.decline(c) }) { Text("Decline") }; if (c.canCancel) TextButton(onClick = { vm.cancel(c) }) { Text("Cancel") }; TextButton(onClick = vm::closeDetails) { Text("Close") } } }
    )
}
@Composable private fun TeamPreviewDialog(t: ChallengeTeamUi, green: Color, vm: ChallengeViewModel) { AlertDialog(onDismissRequest = vm::closeTeam, title = { Text(t.name) }, text = { Column { Text("${t.members} members"); if (t.location.isNotBlank()) Text(t.location); if (t.description.isNotBlank()) Text(t.description) } }, confirmButton = { TextButton(onClick = { vm.closeTeam(); vm.openCreate(); vm.selectOpponent(t); vm.nextStep() }) { Text("Challenge", color = green) } }, dismissButton = { TextButton(onClick = vm::closeTeam) { Text("Close") } }) }

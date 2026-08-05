package com.sportynix.app.presentation.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

private val historyFilters = listOf("All", "Upcoming", "Completed", "Cancelled", "No-Show")

@Composable
fun BookingHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Booking) -> Unit,
    onNavigateToCancel: (Booking) -> Unit = {},
    onNavigateToNewBooking: () -> Unit = {},
    viewModel: BookingHistoryViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val green = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val bg = if (isDark) Color(0xFF070C16) else Color(0xFFF7FAF8)
    val card = if (isDark) Color(0xFF141E30) else Color.White
    val text = if (isDark) Color(0xFFF8FAFC) else Color(0xFF14201A)
    val secondary = if (isDark) Color(0xFF9AA9BC) else Color(0xFF66736C)
    val border = if (isDark) Color(0xFF2B405D) else Color(0xFFDCEAE2)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Booking", fontSize = 30.sp, fontWeight = FontWeight.Black, color = text)
                    Text("History & Upcoming", fontSize = 16.sp, color = secondary)
                }
                HeaderAction(Icons.Default.SwapVert, "Sort", green, card) { viewModel.setShowSortSheet(true) }
                Spacer(Modifier.width(10.dp))
                HeaderAction(Icons.Default.Add, "New booking", Color.White, green, onNavigateToNewBooking)
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                BookingTypeSwitch(state, green, card, border, text, secondary, viewModel::setBookingType)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyFilters) { filter ->
                        val selected = filter == state.selectedFilter
                        Surface(
                            shape = CircleShape,
                            color = if (selected) green else card,
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, border),
                            modifier = Modifier.clickable { viewModel.setFilter(filter) }
                        ) {
                            Text("$filter (${state.filterCounts[filter] ?: 0})", fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = if (selected) Color.White else secondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                        }
                    }
                }

                AnimatedContent(targetState = Triple(state.isLoading, state.errorMessage, state.bookings.isEmpty()), label = "historyState") { contentState ->
                    when {
                        contentState.first -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = green)
                        }
                        contentState.second != null && state.allBookings.isEmpty() -> HistoryError(contentState.second!!, green, text, secondary, viewModel::refresh)
                        contentState.third -> HistoryEmpty(state.selectedFilter, green, text, secondary)
                        else -> LazyColumn(
                            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.bookings, key = { it.bookingId }) { booking ->
                                BookingHistoryCard(
                                    booking, state.currentUserId, state.cancellingBookingId, state.assigningBookingId,
                                    green, card, border, text, secondary,
                                    onClick = { onNavigateToDetail(booking) },
                                    onQr = { viewModel.openQRModal(booking) },
                                    onCancel = { viewModel.requestCancellation(booking) },
                                    onAssign = { viewModel.openTeamSheet(booking) },
                                    onRemoveTeam = { viewModel.removeTeam(booking) },
                                    onMap = {
                                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(booking.location)}")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                                        else context.startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(booking.location)}")))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.showSortSheet) SortSheet(state.sortOption, card, text, secondary, green,
            onDismiss = { viewModel.setShowSortSheet(false) }, onSelect = viewModel::setSortOption)
        if (state.showQRModal && state.selectedBookingForQR != null) QrSheet(
            state.selectedBookingForQR, state.qrCodeUrl, state.isLoadingQR, state.qrError,
            card, text, secondary, green, viewModel::dismissQRModal)
        if (state.showTeamSheet) TeamSheet(state, card, text, secondary, green,
            viewModel::dismissTeamSheet, viewModel::assignTeam)
        if (state.showCancelAlert && state.bookingToCancel != null) AlertDialog(
            onDismissRequest = viewModel::dismissCancellation,
            title = { Text(if (state.bookingToCancel.isPermanent) "Cancel permanent series?" else "Cancel booking?") },
            text = { Text(if (state.bookingToCancel.isPermanent) "This will cancel every active booking in this permanent series." else "This booking will be cancelled and the slot released.") },
            confirmButton = { Button(onClick = viewModel::confirmCancellation,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Cancel booking") } },
            dismissButton = { TextButton(onClick = viewModel::dismissCancellation) { Text("Keep booking") } }
        )
    }
}

@Composable private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, background: Color, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(background).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
    }
}

@Composable private fun BookingTypeSwitch(state: BookingHistoryUiState, green: Color, card: Color, border: Color, text: Color, secondary: Color, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp).clip(RoundedCornerShape(22.dp))
        .background(card).border(1.dp, border, RoundedCornerShape(22.dp)).padding(5.dp)) {
        listOf(Triple(0, "Normal\nBookings", state.normalCount), Triple(1, "Permanent\nBookings", state.permanentCount)).forEach { (type, label, count) ->
            val selected = state.selectedBookingType == type
            Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (selected) green else Color.Transparent)
                .clickable { onSelect(type) }.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (type == 0) Icons.Default.CalendarMonth else Icons.Default.Sync, null,
                    tint = if (selected) Color.White else secondary, modifier = Modifier.size(20.dp))
                Text(label, Modifier.padding(start = 10.dp).weight(1f), color = if (selected) Color.White else secondary,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (selected) Color.White.copy(.18f) else border), contentAlignment = Alignment.Center) {
                    Text(count.toString(), color = if (selected) Color.White else secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun BookingHistoryCard(
    booking: Booking, currentUserId: Int?, cancellingId: Int?, assigningId: Int?,
    green: Color, card: Color, border: Color, text: Color, secondary: Color,
    onClick: () -> Unit, onQr: () -> Unit, onCancel: () -> Unit, onAssign: () -> Unit,
    onRemoveTeam: () -> Unit, onMap: () -> Unit
) {
    val status = normalizedHistoryStatus(booking.status)
    val statusColor = statusColor(status, green)
    val active = status == "Upcoming" || status == "Ongoing"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(card)
        .border(1.dp, border, RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(if (booking.isPermanent) "Permanent · ${booking.slotCount} slots" else "One-time Booking", green)
            if (booking.isChallengeBooking) { Spacer(Modifier.width(7.dp)); Badge("Challenge Match", Color(0xFFEF4444)) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.CalendarMonth, null, tint = secondary, modifier = Modifier.size(14.dp))
            Text(" ${booking.bookedDate}", color = secondary, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(model = booking.imageURL, contentDescription = booking.complexName,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize().background(green.copy(.1f))) },
                error = { Box(Modifier.fillMaxSize().background(green.copy(.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Sports, null, tint = green) } })
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(booking.complexName, color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${booking.sport} · ${booking.courtName}", color = secondary, fontSize = 13.sp)
            }
            Badge(status, statusColor)
        }
        if (booking.teamId != null) InfoLine(Icons.Default.Groups, "${booking.teamName} (${booking.memberCount} members)", if (booking.isChallengeBooking) green else text)
        if (booking.isChallengeBooking && booking.opponentTeamName != null) {
            InfoLine(Icons.Default.Shield, "VS ${booking.opponentTeamName} (${booking.opponentMemberCount ?: 0} members)", Color(0xFFEF4444))
            val total = booking.memberCount + (booking.opponentMemberCount ?: 0)
            if (total > 0) InfoLine(Icons.Default.Groups, "Total: $total players", secondary)
        }
        InfoLine(Icons.Default.CalendarMonth, "Play Date: ${booking.playDateStart} - ${booking.playDateEnd}", secondary)
        InfoLine(Icons.Default.Schedule, booking.timeSlot, secondary)
        InfoLine(Icons.Default.Timer, booking.duration, secondary)
        InfoLine(Icons.Default.LocationOn, booking.location, secondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(booking.price, color = green, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (booking.isPermanent) Text("${booking.slotCount} slots", color = secondary, fontSize = 12.sp)
            }
            if (active && booking.qrCode) ActionButton("Show QR", Icons.Default.QrCode, green, false, onQr)
            if (active && booking.canCancel) {
                Spacer(Modifier.width(8.dp)); ActionButton("Cancel", Icons.Default.Cancel, Color(0xFFEF4444), cancellingId == booking.bookingId, onCancel)
            }
        }
        if (!booking.isChallengeBooking && active && booking.userId == currentUserId) {
            HorizontalDivider(color = border)
            if (booking.teamId == null) WideAction("Assign Team", Icons.Default.GroupAdd, green, assigningId == booking.bookingId, onAssign)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Assigned to: ${booking.teamName} (${booking.memberCount} members)", color = green, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onRemoveTeam, enabled = assigningId == null) { Text("Remove", color = Color(0xFFEF4444)) }
            }
        }
        HorizontalDivider(color = border)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Booking ID: ${booking.bookingId}", color = secondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onMap) { Icon(Icons.Default.LocationOn, "Open map", tint = green) }
        }
    }
}

@Composable private fun Badge(label: String, color: Color) = Surface(shape = CircleShape, color = color.copy(.12f)) {
    Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp))
}
@Composable private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) = Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, null, tint = color.copy(.8f), modifier = Modifier.size(17.dp)); Text(label, color = color, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp), maxLines = 2)
}
@Composable private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, busy: Boolean, onClick: () -> Unit) = OutlinedButton(
    onClick = onClick, enabled = !busy, shape = RoundedCornerShape(13.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(.35f)), contentPadding = PaddingValues(horizontal = 13.dp)) {
    if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = color) else Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
    Text(" $label", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
@Composable private fun WideAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, busy: Boolean, onClick: () -> Unit) = Button(
    onClick = onClick, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = color.copy(.12f), contentColor = color)) {
    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = color) else Icon(icon, null, modifier = Modifier.size(18.dp)); Text(" $label", fontWeight = FontWeight.Bold)
}

@Composable private fun SortSheet(selected: BookingSortOption, card: Color, text: Color, secondary: Color, green: Color, onDismiss: () -> Unit, onSelect: (BookingSortOption) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = card) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sort Bookings", color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Choose how to organize your bookings", color = secondary, fontSize = 13.sp)
            BookingSortOption.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (selected == option) green.copy(.09f) else Color.Transparent)
                    .clickable { onSelect(option) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == option, onClick = { onSelect(option) }, colors = RadioButtonDefaults.colors(selectedColor = green))
                    Column(Modifier.padding(start = 8.dp)) { Text(option.label, color = text, fontWeight = FontWeight.Bold); Text(option.description, color = secondary, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable private fun QrSheet(booking: Booking, qrUrl: String?, loading: Boolean, error: String?, card: Color, text: Color, secondary: Color, green: Color, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = card) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Booking QR Code", color = text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("${booking.complexName} - ${booking.sport}", color = secondary)
            Text("Date: ${booking.playDateStart}", color = green, fontWeight = FontWeight.Bold)
            Text(booking.timeSlot, color = secondary); Badge(normalizedHistoryStatus(booking.status), statusColor(normalizedHistoryStatus(booking.status), green))
            Box(Modifier.size(260.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                when { loading -> CircularProgressIndicator(color = green)
                    !qrUrl.isNullOrBlank() -> SubcomposeAsyncImage(model = qrUrl, contentDescription = "Booking QR", modifier = Modifier.fillMaxSize().padding(20.dp), contentScale = ContentScale.Fit,
                        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = green) } },
                        error = { Text("Failed to load QR code", color = Color.DarkGray) })
                    else -> Text(error ?: "QR code unavailable", color = Color.DarkGray)
                }
            }
            Text("Booking ID: #${booking.bookingId}", color = secondary, fontWeight = FontWeight.Bold)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("Close") }
        }
    }
}

@Composable private fun TeamSheet(state: BookingHistoryUiState, card: Color, text: Color, secondary: Color, green: Color, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = card) {
        Column(Modifier.fillMaxWidth().padding(22.dp).padding(bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Team", color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            state.userTeams.forEach { team -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(team.id) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(team.name ?: "Team", color = text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${team.memberCount ?: 0} members", color = secondary, fontSize = 13.sp); Icon(Icons.Default.ChevronRight, null, tint = green)
            } }
        }
    }
}

@Composable private fun HistoryError(message: String, green: Color, text: Color, secondary: Color, retry: () -> Unit) = Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Icon(Icons.Default.CloudOff, null, tint = green, modifier = Modifier.size(58.dp)); Text("Couldn't load bookings", color = text, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(message, color = secondary); Button(onClick = retry, colors = ButtonDefaults.buttonColors(containerColor = green), modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
}
@Composable private fun HistoryEmpty(filter: String, green: Color, text: Color, secondary: Color) = Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Icon(Icons.Default.EventBusy, null, tint = green.copy(.55f), modifier = Modifier.size(64.dp)); Text("No bookings found", color = text, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text("You don't have any ${filter.lowercase()} bookings yet.", color = secondary)
}

private fun normalizedHistoryStatus(value: String): String = when (value.lowercase().trim()) {
    "playing", "ongoing" -> "Ongoing"; "confirmed", "upcoming", "pending" -> "Upcoming"; "completed" -> "Completed"; "no-show", "no_show", "noshow" -> "No-Show"; else -> "Cancelled"
}
private fun statusColor(status: String, green: Color) = when (status) { "Ongoing", "Upcoming" -> green; "Completed" -> Color.Gray; "No-Show" -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }

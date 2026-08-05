package com.sportynix.app.presentation.booking

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    initialBooking: Booking? = null,
    bookingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCancel: (Booking, String) -> Unit,
    onNavigateToCheckout: (PaymentCheckoutResponseDto) -> Unit,
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)

    fun statusColor(status: String): Color = when (status.lowercase()) {
        "completed" -> Color(0xFF22C55E)
        "cancelled", "canceled" -> Color(0xFFEF4444)
        "no-show", "noshow" -> Color(0xFFF59E0B)
        "ongoing", "playing" -> Color(0xFFF59E0B)
        else -> primaryGreen
    }

    LaunchedEffect(initialBooking, bookingId) {
        viewModel.initBooking(initialBooking, bookingId)
    }

    val booking = state.booking

    val permanentTabs = listOf("All", "Upcoming", "Completed", "Cancelled", "No-Show")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Booking Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                            .clickable {
                                booking?.let { b ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out my court booking at ${b.complexName} on ${b.playDateStart} at ${b.timeSlot}! Reference: #${b.bookingId}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Booking"))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = primaryGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        bottomBar = {
            booking?.let { b ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .background(cardBg)
                        .border(1.dp, borderClr)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (b.qrCode && b.status.lowercase() !in setOf("cancelled", "canceled", "no-show", "noshow")) {
                            OutlinedButton(
                                onClick = { viewModel.openQRModal() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("QR Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }

                        if (b.canCancel && b.status.lowercase() in setOf("upcoming", "confirmed", "pending", "ongoing", "playing")) {
                            Button(
                                onClick = { onNavigateToCancel(b, if (b.isPermanent) "series" else "single") },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (b.isPermanent) "Cancel Series" else "Cancel Booking", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else if (booking == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Booking details not found.", fontSize = 14.sp, color = textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner Image
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncImage(
                            model = booking.imageURL,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(booking.complexName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(booking.location, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                    .background(statusColor(booking.status))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(booking.status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Actions — mirrors the Swift detail view and keeps every action live.
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(cardBg).border(1.dp, borderClr, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        if (booking.status.lowercase() in setOf("upcoming", "confirmed", "pending", "ongoing", "playing") && booking.qrCode) {
                            OutlinedButton(
                                onClick = { viewModel.openQRModal() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode, null, tint = Color(0xFF3B82F6))
                                Spacer(Modifier.width(10.dp))
                                Text("View QR Code", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Sportynix booking at ${booking.complexName} on ${booking.playDateStart} at ${booking.timeSlot}. Reference #${booking.bookingId}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Booking"))
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color(0xFF3B82F6))
                            Spacer(Modifier.width(10.dp))
                            Text("Share Booking", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Booking Information Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Booking Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sport / Court", fontSize = 14.sp, color = textSecondary)
                            Text("${booking.sport} · ${booking.courtName}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Play Date", fontSize = 14.sp, color = textSecondary)
                            Text(booking.playDateStart, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Time Slot", fontSize = 14.sp, color = textSecondary)
                            Text(booking.timeSlot, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duration", fontSize = 14.sp, color = textSecondary)
                            Text(booking.duration, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Price", fontSize = 14.sp, color = textSecondary)
                            Text(booking.price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reference #", fontSize = 14.sp, color = textSecondary)
                            Text("#${booking.bookingId}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        }
                    }
                }

                // Swift-style booking details and team information.
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(cardBg).border(1.dp, borderClr, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Booking Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        DetailLine("Booked by", "Account holder", textPrimary, textSecondary)
                        DetailLine("Team", if (booking.teamId != null) booking.teamName else "No team assigned", textPrimary, textSecondary)
                        if (booking.teamId != null) DetailLine("Members", "${booking.memberCount} members", textPrimary, textSecondary)
                    }
                }

                // Challenge Match Banner or Team Card
                item {
                    if (booking.isChallengeBooking) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(primaryGreen.copy(alpha = 0.1f))
                                .border(1.5.dp, primaryGreen, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Challenge Match", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(booking.teamName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("${booking.memberCount} players", fontSize = 12.sp, color = textSecondary)
                                }
                                Text("VS", fontSize = 18.sp, fontWeight = FontWeight.Black, color = primaryGreen)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(booking.opponentTeamName ?: "Opponent", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("${booking.opponentMemberCount ?: 0} players", fontSize = 12.sp, color = textSecondary)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Assigned Team", fontSize = 14.sp, color = textSecondary)
                                Text(booking.teamName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }

                            TextButton(onClick = { viewModel.openTeamSheet() }) {
                                Text("Change Team", color = primaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Permanent Series Generated Slots Section
                if (booking.isPermanent || state.permanentSlots.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Permanent Series Occurrences (${state.permanentSlots.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(permanentTabs) { tab ->
                                    val isSelected = state.activePermanentTab.equals(tab, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) primaryGreen else Color.Transparent)
                                            .border(1.dp, if (isSelected) Color.Transparent else borderClr, CircleShape)
                                            .clickable { viewModel.setPermanentTab(tab) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(tab, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
                                    }
                                }
                            }

                            val filteredSlots = state.permanentSlots.filter { slot ->
                                state.activePermanentTab == "All" || slot.status.equals(state.activePermanentTab, ignoreCase = true) ||
                                    (state.activePermanentTab == "No-Show" && slot.status.equals("no_show", ignoreCase = true))
                            }
                            val visibleSlots = if (state.showAllPermanentSlots) filteredSlots else filteredSlots.take(3)
                            visibleSlots.forEach { slot ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(slot.playDateStart, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                            Text(slot.timeSlot, fontSize = 12.sp, color = textSecondary)
                                        }
                                        Text(slot.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor(slot.status))
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.openQRModal(slot.bookingId) },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode, null, modifier = Modifier.size(15.dp), tint = Color(0xFF3B82F6))
                                            Spacer(Modifier.width(4.dp))
                                            Text("View QR", fontSize = 11.sp, color = Color(0xFF3B82F6))
                                        }
                                        if (slot.status.lowercase() in setOf("upcoming", "confirmed", "pending")) {
                                            OutlinedButton(
                                                onClick = { onNavigateToCancel(slot, "single") },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                            ) {
                                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Cancel Slot", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            if (filteredSlots.size > 3) {
                                TextButton(
                                    onClick = { viewModel.setShowAllPermanentSlots(!state.showAllPermanentSlots) },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(if (state.showAllPermanentSlots) "Show Less" else "Show All ${filteredSlots.size} Slots", color = primaryGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!booking.isPermanent) {
                    item {
                        DetailCard(title = "Schedule", cardBg = cardBg, borderClr = borderClr, textPrimary = textPrimary) {
                            DetailLine("Play date", booking.playDateStart, textPrimary, textSecondary)
                            DetailLine("Time", booking.timeSlot, primaryGreen, textSecondary)
                            DetailLine("Duration", booking.duration, textPrimary, textSecondary)
                        }
                    }
                }

                item {
                    DetailCard(title = "Location", cardBg = cardBg, borderClr = borderClr, textPrimary = textPrimary) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = .05f) else Color(0xFFF1F5F9))
                                .clickable {
                                    val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(booking.location)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF3B82F6))
                            Text(booking.location.ifBlank { "Location unavailable" }, modifier = Modifier.weight(1f), color = textPrimary, fontSize = 14.sp)
                            Icon(Icons.Default.ChevronRight, null, tint = textSecondary)
                        }
                    }
                }

                item {
                    DetailCard(title = "Payment Summary", cardBg = cardBg, borderClr = borderClr, textPrimary = textPrimary) {
                        DetailLine("Total amount", booking.price, textPrimary, textSecondary)
                        DetailLine("Payment status", booking.status.replaceFirstChar { it.uppercase() }, statusColor(booking.status), textSecondary)
                    }
                }

                item {
                    DetailCard(title = "Booking Information", cardBg = cardBg, borderClr = borderClr, textPrimary = textPrimary) {
                        DetailLine("Booking ID", "#${booking.bookingId}", textPrimary, textSecondary)
                        HorizontalDivider(color = borderClr)
                        DetailLine("Booked date", booking.bookedDate.ifBlank { "N/A" }, textPrimary, textSecondary)
                        DetailLine("Created", booking.createdAt ?: "N/A", textPrimary, textSecondary)
                        DetailLine("Status", booking.status.replaceFirstChar { it.uppercase() }, statusColor(booking.status), textSecondary)
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }

        // QR Code Modal
        if (state.showQRModal) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissQRModal() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissQRModal() }) {
                        Text("Close", color = primaryGreen, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Court Entry QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        val qrUrl = state.qrCodeUrl ?: "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${booking?.bookingId ?: 1}"
                        AsyncImage(
                            model = qrUrl,
                            contentDescription = "QR",
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text("Scan at venue counter to check in", fontSize = 12.sp, color = textSecondary)
                    }
                }
            )
        }

        // Team Assignment Sheet
        if (state.showTeamSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissTeamSheet() },
                containerColor = cardBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Team to Assign", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    if (state.userTeams.isEmpty()) {
                        Text("No teams available. Create a team from your Profile first.", fontSize = 14.sp, color = textSecondary)
                    } else {
                        state.userTeams.forEach { (tId, tName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.assignTeam(tId) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: Color, labelColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, modifier = Modifier.width(100.dp), fontSize = 14.sp, color = labelColor)
        Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun DetailCard(
    title: String,
    cardBg: Color,
    borderClr: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(cardBg).border(1.dp, borderClr, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        content()
    }
}

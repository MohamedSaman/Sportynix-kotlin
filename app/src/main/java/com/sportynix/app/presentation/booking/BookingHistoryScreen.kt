package com.sportynix.app.presentation.booking

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit = {},
    onNavigateToNewBooking: () -> Unit = {},
    viewModel: BookingHistoryViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    val filters = remember { listOf("All", "Upcoming", "Completed", "Cancelled", "No-Show") }

    val filteredBookings = remember(state.bookings, state.selectedBookingType, state.selectedFilter, state.sortOption) {
        val typeFiltered = if (state.selectedBookingType == 0) {
            state.bookings.filter { !it.isPermanent }
        } else {
            state.bookings.filter { it.isPermanent }
        }

        val statusFiltered = if (state.selectedFilter == "All") {
            typeFiltered
        } else {
            typeFiltered.filter { it.status.equals(state.selectedFilter, ignoreCase = true) }
        }

        statusFiltered.sortedWith { a, b ->
            when (state.sortOption) {
                BookingSortOption.PLAY_DATE_NEWEST -> viewModel.parseDate(b.playDateStart).compareTo(viewModel.parseDate(a.playDateStart))
                BookingSortOption.PLAY_DATE_OLDEST -> viewModel.parseDate(a.playDateStart).compareTo(viewModel.parseDate(b.playDateStart))
                BookingSortOption.BOOKED_DATE_LATEST -> viewModel.parseDate(b.bookedDate).compareTo(viewModel.parseDate(a.bookedDate))
                BookingSortOption.BOOKED_DATE_OLDEST -> viewModel.parseDate(a.bookedDate).compareTo(viewModel.parseDate(b.bookedDate))
            }
        }
    }

    val normalCount = remember(state.bookings) { state.bookings.count { !it.isPermanent } }
    val permanentCount = remember(state.bookings) { state.bookings.count { it.isPermanent } }

    fun filterCount(f: String): Int {
        val typeFiltered = if (state.selectedBookingType == 0) state.bookings.filter { !it.isPermanent } else state.bookings.filter { it.isPermanent }
        if (f == "All") return typeFiltered.size
        return typeFiltered.count { it.status.equals(f, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E262C) else Color(0xFFE2E8F0))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Booking",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = "History & Upcoming",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setShowSortSheet(true) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = primaryGreen
                            )
                        }

                        IconButton(
                            onClick = { onNavigateToNewBooking() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(primaryGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Booking",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. NORMAL VS PERMANENT SEGMENTED TOGGLE
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isNormalSelected = state.selectedBookingType == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isNormalSelected) primaryGreen else cardBg)
                            .border(1.dp, if (isNormalSelected) primaryGreen else borderClr, RoundedCornerShape(20.dp))
                            .clickable { viewModel.setBookingType(0) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Normal Bookings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isNormalSelected) Color.White else textPrimary)
                                Text("$normalCount bookings", fontSize = 11.sp, color = if (isNormalSelected) Color.White.copy(alpha = 0.8f) else textSecondary)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (isNormalSelected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    val isPermSelected = state.selectedBookingType == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isPermSelected) primaryGreen else cardBg)
                            .border(1.dp, if (isPermSelected) primaryGreen else borderClr, RoundedCornerShape(20.dp))
                            .clickable { viewModel.setBookingType(1) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Permanent Bookings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isPermSelected) Color.White else textPrimary)
                                Text("$permanentCount series", fontSize = 11.sp, color = if (isPermSelected) Color.White.copy(alpha = 0.8f) else textSecondary)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (isPermSelected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // 2. FILTER CHIPS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSel = state.selectedFilter == filter
                        val cnt = filterCount(filter)
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(if (isSel) primaryGreen else cardBg)
                                .border(1.dp, if (isSel) primaryGreen else borderClr, CapsuleShape)
                                .clickable { viewModel.setFilter(filter) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "$filter ($cnt)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSel) Color.White else textPrimary
                            )
                        }
                    }
                }
            }

            // 3. BOOKING CARDS LIST
            if (state.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = primaryGreen)
                        Text("Loading bookings...", fontSize = 14.sp, color = textSecondary)
                    }
                }
            } else if (filteredBookings.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.EventBusy, contentDescription = null, tint = textSecondary, modifier = Modifier.size(48.dp))
                        Text("No bookings found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("You have no ${state.selectedFilter.lowercase()} bookings.", fontSize = 13.sp, color = textSecondary)
                    }
                }
            } else {
                items(filteredBookings, key = { it.id }) { booking ->
                    BookingHistoryCard(
                        booking = booking,
                        cardBg = cardBg,
                        borderClr = borderClr,
                        primaryGreen = primaryGreen,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onCardClick = { onNavigateToDetail(booking.bookingId) },
                        onQRClick = { viewModel.openQRModal(booking) },
                        onShareClick = {
                            val shareText = "Booking Details at ${booking.complexName}\nDate: ${booking.playDateStart}\nTime: ${booking.timeSlot}\nPrice: ${booking.price}"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Booking"))
                        },
                        onCancelClick = { viewModel.promptCancelBooking(booking) },
                        onTeamClick = { viewModel.openTeamSheet(booking) }
                    )
                }
            }
        }
    }

    // Sort Sheet Modal
    if (state.showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowSortSheet(false) },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, tint = primaryGreen)
                    }
                    Column {
                        Text("Sort Bookings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Choose how to organize your bookings", fontSize = 13.sp, color = textSecondary)
                    }
                }

                BookingSortOption.values().forEach { option ->
                    val isSel = state.sortOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) primaryGreen.copy(alpha = 0.08f) else Color.Transparent)
                            .border(1.dp, if (isSel) primaryGreen.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setSortOption(option) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { viewModel.setSortOption(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(option.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text(option.description, fontSize = 12.sp, color = textSecondary)
                        }
                    }
                }
            }
        }
    }

    // QR Modal
    if (state.showQRModal && state.selectedBookingForQR != null) {
        val booking = state.selectedBookingForQR
        Dialog(onDismissRequest = { viewModel.dismissQRModal() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Booking QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        IconButton(onClick = { viewModel.dismissQRModal() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoadingQR) {
                            CircularProgressIndicator(color = primaryGreen)
                        } else if (!state.qrCodeUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = state.qrCodeUrl,
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("QR Code unavailable", fontSize = 13.sp, color = Color.Red)
                        }

                        // Overlay for non-upcoming bookings
                        if (booking.status.equals("Completed", ignoreCase = true)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                                Text("EXPIRED", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else if (booking.status.equals("Cancelled", ignoreCase = true)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                                Text("CANCELLED", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else if (booking.status.equals("No-Show", ignoreCase = true)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEA580C).copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                                Text("NO-SHOW", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(booking.complexName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("${booking.sport} • ${booking.courtName}", fontSize = 13.sp, color = textSecondary)
                        Text("${booking.playDateStart} (${booking.timeSlot})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
                        Text("Booking ID: #${booking.bookingId}", fontSize = 12.sp, color = textSecondary)
                    }

                    Button(
                        onClick = { viewModel.dismissQRModal() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Cancel Alert
    if (state.showCancelAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelAlert() },
            title = { Text("Cancel Booking", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel this booking? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmCancelBooking() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Cancel Booking", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCancelAlert() }) {
                    Text("Keep Booking", color = textSecondary)
                }
            }
        )
    }

    // Team Selection Sheet Modal
    if (state.showTeamSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissTeamSheet() },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select Team for Booking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                if (state.isLoadingTeams) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = primaryGreen)
                } else if (state.userTeams.isEmpty()) {
                    Text("No teams found. Create a team first in Teams module.", fontSize = 14.sp, color = textSecondary)
                } else {
                    state.userTeams.forEach { (tId, tName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFFF1F5F9))
                                .clickable { viewModel.assignTeam(tId) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary)
                        }
                    }
                }

                TextButton(onClick = { viewModel.dismissTeamSheet() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancel", color = textSecondary)
                }
            }
        }
    }
}

@Composable
private fun BookingHistoryCard(
    booking: Booking,
    cardBg: Color,
    borderClr: Color,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    onCardClick: () -> Unit,
    onQRClick: () -> Unit,
    onShareClick: () -> Unit,
    onCancelClick: () -> Unit,
    onTeamClick: () -> Unit
) {
    val statusColor = when (booking.status.lowercase()) {
        "ongoing", "upcoming", "confirmed" -> primaryGreen
        "completed" -> Color.Gray
        "no-show", "noshow" -> Color(0xFFEA580C)
        else -> Color.Red
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(20.dp))
            .clickable { onCardClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Badges & Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(if (booking.isPermanent) Color(0xFF9333EA).copy(alpha = 0.12f) else primaryGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (booking.isPermanent) "Permanent Series" else "One-time Booking",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.isPermanent) Color(0xFF9333EA) else primaryGreen
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(CapsuleShape)
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = booking.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = booking.imageURL,
                    contentDescription = null,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(booking.complexName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${booking.sport} • ${booking.courtName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                    Text("${booking.playDateStart} • ${booking.timeSlot}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
                }
            }

            HorizontalDivider(color = borderClr)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(booking.price, fontSize = 16.sp, fontWeight = FontWeight.Black, color = textPrimary)
                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.qrCode || booking.status.equals("Upcoming", ignoreCase = true)) {
                        IconButton(onClick = onQRClick, modifier = Modifier.size(36.dp).clip(CircleShape).background(primaryGreen.copy(alpha = 0.1f))) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR Code", tint = primaryGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp).clip(CircleShape).background(textSecondary.copy(alpha = 0.1f))) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = textSecondary, modifier = Modifier.size(18.dp))
                    }

                    if (booking.canCancel && (booking.status.equals("Upcoming", ignoreCase = true) || booking.status.equals("Confirmed", ignoreCase = true))) {
                        IconButton(onClick = onCancelClick, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

val CapsuleShape = RoundedCornerShape(50.dp)

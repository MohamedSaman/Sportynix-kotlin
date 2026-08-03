package com.sportynix.app.presentation.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.dto.SlotData
import com.sportynix.app.data.remote.dto.VenueSportDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    venueId: Int,
    sportId: Int,
    sportName: String = "",
    sportPrice: String = "",
    sportImageURL: String = "",
    complexName: String = "",
    complexLocation: String = "",
    complexRating: Double = 5.0,
    complexReviews: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (BookingPayload) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    LaunchedEffect(venueId, sportId) {
        viewModel.initBooking(venueId, sportId)
    }

    // 14-day date list generator
    val dateList = remember {
        val list = mutableListOf<Triple<String, String, String>>() // (fullDate, dayName, dayNum)
        val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val sdfNum = SimpleDateFormat("dd", Locale.US)
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            val date = cal.time
            list.add(Triple(sdfFull.format(date), sdfDay.format(date), sdfNum.format(date)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val totalAmount = remember(state.selectedSlots, state.selectedSport) {
        val unitPrice = state.selectedSport?.price?.replace("Rs. ", "")?.toDoubleOrNull() ?: 500.0
        state.selectedSlots.sumOf { slot ->
            slot.price ?: (unitPrice * ((slot.duration ?: 60) / 60.0))
        }
    }

    val weekdaysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
                    Text("Book a Court", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(cardBg)
                    .border(1.dp, borderClr)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Amount", fontSize = 12.sp, color = textSecondary)
                        Text("LKR ${"%.2f".format(totalAmount)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(primaryGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${state.selectedSlots.size} slots selected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }
                }

                Button(
                    onClick = {
                        if (state.selectedSlots.isNotEmpty()) {
                            val slotInfos = state.selectedSlots.map { slot ->
                                val s = slot.startTime ?: slot.rawStart ?: "00:00"
                                val e = slot.endTime ?: slot.rawEnd ?: "01:00"
                                BookingSlotInfo(
                                    startTime = s,
                                    endTime = e,
                                    displayStart = s,
                                    displayEnd = e,
                                    duration = slot.duration ?: 60,
                                    price = slot.price ?: 500.0
                                )
                            }
                            val payload = BookingPayload(
                                sportId = state.sportId,
                                sportName = state.selectedSport?.name ?: sportName,
                                sportPrice = state.selectedSport?.price ?: sportPrice,
                                sportImageURL = state.selectedSport?.imageSecure ?: state.selectedSport?.image ?: sportImageURL,
                                venueId = state.venueId,
                                venueName = state.venue?.name ?: complexName,
                                venueAddress = state.venue?.formattedLocationLine ?: complexLocation,
                                bookingType = if (state.bookingType == BookingType.PERMANENT) "Permanent" else "Normal",
                                bookingDate = state.selectedDate,
                                selectedDays = state.selectedDays,
                                slots = slotInfos,
                                totalPrice = totalAmount
                            )
                            onNavigateToSummary(payload)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    enabled = state.selectedSlots.isNotEmpty()
                ) {
                    Text("Continue to Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle Capsule
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (state.bookingType == BookingType.NORMAL) primaryGreen else Color.Transparent)
                            .clickable { viewModel.setBookingType(BookingType.NORMAL) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Single Booking", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.bookingType == BookingType.NORMAL) Color.White else textSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (state.bookingType == BookingType.PERMANENT) primaryGreen else Color.Transparent)
                            .clickable { viewModel.setBookingType(BookingType.PERMANENT) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Permanent (1-Month)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.bookingType == BookingType.PERMANENT) Color.White else textSecondary)
                    }
                }
            }

            // Sport Selector Row
            if (state.sports.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select Sport", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.sports) { sport ->
                                val isSelected = state.sportId == sport.id
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) primaryGreen else cardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderClr, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setSelectedSport(sport) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsyncImage(
                                        model = sport.imageSecure ?: sport.image ?: "",
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(sport.name ?: "Sport", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Date Selector (Normal) or Weekdays Selector (Permanent)
            if (state.bookingType == BookingType.NORMAL) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select Date", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(dateList) { (fullDate, dayName, dayNum) ->
                                val isSelected = state.selectedDate == fullDate
                                Column(
                                    modifier = Modifier
                                        .width(65.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) primaryGreen else cardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderClr, RoundedCornerShape(14.dp))
                                        .clickable { viewModel.setSelectedDate(fullDate) }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(dayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White.copy(alpha = 0.8f) else textSecondary)
                                    Text(dayNum, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select Recurring Weekdays", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Text("Slots will be reserved for 1 full month on chosen days", fontSize = 12.sp, color = textSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(weekdaysList) { day ->
                                val isSelected = state.selectedDays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) primaryGreen else cardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderClr, CircleShape)
                                        .clickable { viewModel.toggleSelectedDay(day) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(day, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Live Slot Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available Time Slots", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        if (state.isLoadingSlots) {
                            CircularProgressIndicator(color = primaryGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }

                    if (state.availableSlots.isEmpty() && !state.isLoadingSlots) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No available time slots for selected options.", fontSize = 14.sp, color = textSecondary)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 2000.dp)
                        ) {
                            items(state.availableSlots) { slot ->
                                SlotCardItem(
                                    slot = slot,
                                    isSelected = state.selectedSlots.any { (it.startTime ?: it.rawStart) == (slot.startTime ?: slot.rawStart) },
                                    primaryGreen = primaryGreen,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    cardBg = cardBg,
                                    borderClr = borderClr,
                                    onClick = { viewModel.toggleSlotSelection(slot) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SlotCardItem(
    slot: SlotData,
    isSelected: Boolean,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    onClick: () -> Unit
) {
    val isPast = slot.isPastTime == true || slot.isFullyBooked == true
    val isHeldByMe = slot.heldByCurrentUser == true
    val isHeldByOthers = slot.isHeld == true && !isHeldByMe

    val bg = when {
        isSelected || isHeldByMe -> primaryGreen
        isHeldByOthers -> Color(0xFFF59E0B).copy(alpha = 0.12f)
        isPast -> Color.Gray.copy(alpha = 0.12f)
        else -> cardBg
    }

    val border = when {
        isSelected || isHeldByMe -> Color.Transparent
        isHeldByOthers -> Color(0xFFF59E0B)
        isPast -> Color.Transparent
        else -> borderClr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isPast) { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${slot.startTime ?: slot.rawStart} - ${slot.endTime ?: slot.rawEnd}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected || isHeldByMe) Color.White else if (isPast) textSecondary else textPrimary
            )
            if (isSelected) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (slot.price != null) "Rs. ${slot.price}" else "Standard",
                fontSize = 12.sp,
                color = if (isSelected || isHeldByMe) Color.White.copy(alpha = 0.85f) else textSecondary
            )

            if (isHeldByOthers) {
                Text("Held", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            } else if (isPast) {
                Text("Unavailable", fontSize = 10.sp, color = textSecondary)
            }
        }
    }
}

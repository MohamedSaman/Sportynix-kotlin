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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(venueId, sportId) {
        viewModel.initBooking(venueId, sportId)
    }

    // Swift BookingView allows today through the same calendar date next month.
    val dateList = remember {
        val list = mutableListOf<Triple<String, String, String>>() // (fullDate, dayName, dayNum)
        val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val sdfNum = SimpleDateFormat("dd", Locale.US)
        val cal = Calendar.getInstance()
        val end = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        while (!cal.after(end)) {
            val date = cal.time
            list.add(Triple(sdfFull.format(date), sdfDay.format(date), sdfNum.format(date)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val visibleSlots = viewModel.visibleSlots()
    val totalAmount = remember(state.selectedSlots, state.selectedSport, state.bookingType, state.selectedDays) {
        val unitPrice = state.selectedSport?.price?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
        val base = state.selectedSlots.sumOf { slot ->
            slot.price ?: (unitPrice * ((slot.duration ?: 60) / 60.0))
        }
        if (state.bookingType == BookingType.PERMANENT) base * state.selectedDays.size * 4.0 else base
    }

    val weekdaysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun createPayload(): BookingPayload {
        val sport = state.selectedSport
        val unitPrice = sport?.price?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
        return BookingPayload(
            sportId = state.sportId, sportName = sport?.name ?: sportName,
            sportPrice = sport?.price ?: sportPrice,
            sportImageURL = sport?.imageSecure ?: sport?.image ?: sportImageURL,
            venueId = state.venueId, venueName = state.venue?.name ?: complexName,
            venueAddress = state.venue?.formattedLocationLine ?: complexLocation,
            bookingType = if (state.bookingType == BookingType.PERMANENT) "Permanent" else "Normal",
            bookingDate = state.selectedDate, selectedDays = state.selectedDays,
            slots = state.selectedSlots.map { slot ->
                val start = slot.rawStart ?: slot.startTime.orEmpty().take(5)
                val end = (slot.rawEnd ?: slot.endTime.orEmpty().take(5)).replace("24:00", "00:00")
                BookingSlotInfo(start, end, slot.startTime ?: start, slot.endTime ?: end,
                    slot.duration ?: 60, slot.price ?: unitPrice)
            }, totalPrice = totalAmount)
    }

    LaunchedEffect(state.proceedToSummary) {
        if (state.proceedToSummary) {
            viewModel.consumeProceedToSummary()
            onNavigateToSummary(createPayload())
        }
    }

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
            if (state.selectedSlots.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .background(cardBg)
                        .border(1.dp, borderClr)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (state.selectedSlots.size == 1) "1 slot selected" else "${state.selectedSlots.size} slots selected",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textSecondary
                            )
                            Text("LKR ${"%.2f".format(totalAmount)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                        Button(
                            onClick = viewModel::checkPhoneVerificationAndProceed,
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                        ) {
                            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle Capsule
            item {
                BookingSectionCard("Booking Type", Icons.Default.Event, cardBg, borderClr, primaryGreen, textPrimary) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)).padding(4.dp)
                    ) {
                        listOf(BookingType.NORMAL to "Normal", BookingType.PERMANENT to "Permanent").forEach { (type, label) ->
                            Box(
                                modifier = Modifier.weight(1f).clip(CircleShape)
                                    .background(if (state.bookingType == type) primaryGreen else Color.Transparent)
                                    .clickable { viewModel.setBookingType(type) }.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    color = if (state.bookingType == type) Color.White else textSecondary)
                            }
                        }
                    }
                }
            }

            // Sport Selector Row
            if (state.sports.isNotEmpty()) {
                item {
                    BookingSectionCard("Select Sport", Icons.Default.Sports, cardBg, borderClr, primaryGreen, textPrimary) {
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
                        state.selectedSport?.let { sport ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                AsyncImage(model = sport.imageSecure ?: sport.image,
                                    contentDescription = sport.name,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(sport.name.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("Rs. ${sport.price.orEmpty()}/hour", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                                    Text(state.venue?.name.orEmpty(), fontSize = 13.sp, color = textSecondary)
                                    Text(state.venue?.formattedLocationLine.orEmpty(), fontSize = 12.sp, color = textSecondary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                                        Text(" ${sport.rating ?: state.venue?.rating ?: 0f} (${sport.reviews ?: 0} reviews)",
                                            fontSize = 12.sp, color = textSecondary)
                                    }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, borderClr, RoundedCornerShape(14.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedLabel = runCatching {
                                SimpleDateFormat("d MMMM yyyy", Locale.US).format(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(state.selectedDate)!!
                                )
                            }.getOrDefault(state.selectedDate)
                            Text(selectedLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select date", tint = textSecondary)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(dateList) { (fullDate, dayName, dayNum) ->
                                val isSelected = state.selectedDate == fullDate
                                Column(
                                    modifier = Modifier
                                        .width(65.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) primaryGreen else cardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderClr, RoundedCornerShape(14.dp))
                                        .clickable(enabled = viewModel.isDateSelectable(fullDate)) { viewModel.setSelectedDate(fullDate) }
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
                                    Text(day.take(3), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
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

                    if (state.bookingType == BookingType.NORMAL && viewModel.isSelectedDayClosed()) {
                        Text("Venue is closed on the selected day. Please select a different date.",
                            color = Color(0xFFDC2626), fontSize = 14.sp, modifier = Modifier.padding(vertical = 24.dp))
                    } else if (visibleSlots.isEmpty() && !state.isLoadingSlots) {
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
                            items(visibleSlots) { slot ->
                                SlotCardItem(
                                    slot = slot,
                                    isSelected = state.selectedSlots.any { selected ->
                                        normalizedSlotKey(selected) == normalizedSlotKey(slot)
                                    },
                                    primaryGreen = primaryGreen,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    cardBg = cardBg,
                                    borderClr = borderClr,
                                    defaultPrice = state.selectedSport?.price
                                        ?.filter { it.isDigit() || it == '.' }
                                        ?.toDoubleOrNull(),
                                    isProcessing = state.processingSlotKeys.contains(normalizedSlotKey(slot)),
                                    permanentAvailability = state.permanentAvailability[normalizedSlotKey(slot)],
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

    if (state.showPhoneVerificationModal) {
        PhoneVerificationDialog(
            phone = state.phoneNumber,
            otp = state.phoneOtp,
            challengeSent = state.phoneChallengeId != null,
            sending = state.isPhoneSending,
            verifying = state.isPhoneVerifying,
            error = state.phoneVerificationError,
            primaryGreen = primaryGreen,
            onPhoneChange = viewModel::updatePhoneNumber,
            onOtpChange = viewModel::updatePhoneOtp,
            onSend = viewModel::sendPhoneOtp,
            onVerify = viewModel::verifyPhoneOtp,
            onDismiss = viewModel::dismissPhoneVerification
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearErrorMessage,
            title = { Text("Slot selection") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearErrorMessage) { Text("OK") } }
        )
    }

    if (showDatePicker) {
        val pickerUtc = remember { TimeZone.getTimeZone("UTC") }
        val todayMillis = remember {
            Calendar.getInstance(pickerUtc).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val maxMillis = remember {
            Calendar.getInstance(pickerUtc).apply {
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
        val selectedMillis = remember(state.selectedDate) {
            runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = pickerUtc }.parse(state.selectedDate)?.time }.getOrNull() ?: todayMillis
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in todayMillis..maxMillis
            }
        )
        ModalBottomSheet(onDismissRequest = { showDatePicker = false }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                Text("Select Date", fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = pickerUtc }.format(Date(millis))
                        if (viewModel.isDateSelectable(selected)) viewModel.setSelectedDate(selected)
                    }
                    showDatePicker = false
                }) { Text("Done", color = primaryGreen, fontWeight = FontWeight.Bold) }
            }
            DatePicker(state = pickerState, showModeToggle = false)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun BookingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    border: Color,
    accent: Color,
    textColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(background).border(1.dp, border, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = .12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        content()
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
    defaultPrice: Double?,
    isProcessing: Boolean,
    permanentAvailability: PermanentSlotAvailability?,
    onClick: () -> Unit
) {
    val availableCourtsCount = (slot.availableCourts as? Number)?.toInt()
        ?: slot.availableCourts?.toString()?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }
    val hasCapacity = availableCourtsCount != null && availableCourtsCount > 0
    val isPast = slot.isPastTime == true
    val isHeldByMe = slot.heldByCurrentUser == true
    val isFullyBooked = !isHeldByMe && (slot.isFullyBooked == true || slot.available == false || (availableCourtsCount != null && !hasCapacity))
    // A live hold belongs to another user even when the venue reports another
    // court as free; it must never be selectable from this device.
    val isHeldByOthers = (slot.isHeld == true || slot.isPaymentReserved == true) && !isHeldByMe

    val pastBg = if (cardBg == Color.White) Color(0xFFFFF5F5) else Color(0xFF2A1D2A)
    val bg = when {
        isSelected || isHeldByMe -> primaryGreen
        isHeldByOthers -> Color(0xFFF59E0B).copy(alpha = 0.12f)
        isFullyBooked -> Color(0xFFDC2626).copy(alpha = 0.10f)
        isPast -> pastBg
        else -> cardBg
    }

    val border = when {
        isSelected || isHeldByMe -> Color.Transparent
        isHeldByOthers -> Color(0xFFF59E0B)
        isFullyBooked -> Color(0xFFFCA5A5)
        isPast -> Color(0xFFF59AA5).copy(alpha = 0.75f)
        else -> borderClr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(if (isSelected || isHeldByMe) 0.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable(enabled = !isPast && !isFullyBooked && !isHeldByOthers && !isProcessing) { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${slot.startTime ?: slot.rawStart} - ${slot.endTime ?: slot.rawEnd}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected || isHeldByMe) Color.White else if (isPast) textSecondary else textPrimary
            )
            if (isProcessing) CircularProgressIndicator(
                modifier = Modifier.padding(start = 8.dp).size(18.dp),
                color = if (isSelected || isHeldByMe) Color.White else primaryGreen,
                strokeWidth = 2.dp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayPrice = slot.price ?: defaultPrice
            Text(
                text = displayPrice?.let { "Rs. ${formatSlotPrice(it)}" } ?: "Price unavailable",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected || isHeldByMe) Color.White.copy(alpha = 0.9f) else textSecondary
            )

            val totalCourts = (slot.totalCourts as? Number)?.toInt()
                ?: slot.totalCourts?.toString()?.toIntOrNull()
            if (isSelected || isHeldByMe) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("Selected", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else if (totalCourts != null && totalCourts > 1) {
                Text("${availableCourtsCount ?: 0}/$totalCourts courts", fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, color = primaryGreen)
            } else if (isHeldByOthers) {
                Text("Held", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            } else if (isFullyBooked) {
                Text(slot.disabledReason ?: "Fully booked", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
            } else if (isPast) {
                Text("Time passed", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE07A85))
            } else if (permanentAvailability != null) {
                Text("${permanentAvailability.daysRemaining}/${permanentAvailability.totalDaysChecked} days available",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium, color = primaryGreen)
            }
        }
    }
}

private fun formatSlotPrice(price: Double): String = String.format(Locale.US, "%.2f", price)

private fun normalizedSlotKey(slot: SlotData): String {
    fun apiTime(value: String?): String? {
        val input = value?.trim()?.replace("24:00", "00:00") ?: return null
        val patterns = listOf("H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss", "h:mm a", "hh:mm a")
        patterns.forEach { pattern ->
            runCatching {
                val parsed = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply { isLenient = false }.parse(input)
                if (parsed != null) return java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(parsed)
            }
        }
        return null
    }
    val start = apiTime(slot.rawStart ?: slot.startTime)
    val end = apiTime(slot.rawEnd ?: slot.endTime)
    return if (start != null && end != null) "$start-$end"
    else slot.slotKey.orEmpty().replace("-24:00", "-00:00")
}

@Composable
private fun PhoneVerificationDialog(
    phone: String,
    otp: String,
    challengeSent: Boolean,
    sending: Boolean,
    verifying: Boolean,
    error: String?,
    primaryGreen: Color,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSend: () -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(Modifier.size(72.dp).clip(CircleShape).background(primaryGreen.copy(alpha = .12f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Phone, null, tint = primaryGreen, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text("Phone Verification Required", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Please verify your phone number to continue with booking.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                OutlinedTextField(
                    value = phone, onValueChange = onPhoneChange, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone number") }, placeholder = { Text("07XXXXXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = primaryGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !challengeSent && !sending && !verifying,
                    supportingText = { Text("${phone.length}/10") }, singleLine = true
                )
                if (challengeSent) {
                    OutlinedTextField(
                        value = otp, onValueChange = onOtpChange, modifier = Modifier.fillMaxWidth(),
                        label = { Text("6-digit OTP") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        supportingText = { Text("OTP: ${otp.length}/6") }, enabled = !verifying, singleLine = true
                    )
                }
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(10.dp)).padding(10.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = if (challengeSent) onVerify else onSend,
                enabled = if (challengeSent) otp.length == 6 && !verifying else phone.length == 10 && !sending,
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)) {
                if (sending || verifying) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (challengeSent) "Verify & Continue" else "Send OTP")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !sending && !verifying) { Text("Cancel") } }
    )
}

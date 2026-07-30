package com.sportynix.app.presentation.booking

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.BookingPayload
import com.sportynix.app.data.remote.dto.OpeningHourEntryDto
import com.sportynix.app.data.remote.dto.PermanentSlotAvailability
import com.sportynix.app.data.remote.dto.SlotData
import com.sportynix.app.data.remote.dto.VenueSportDto
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

enum class TimeSlotStatus {
    AVAILABLE,
    FULLY_BOOKED,
    TIME_PASSED,
    HELD,
    SELECTED,
    PROCESSING,
    ALREADY_BOOKED
}

data class TimeSlotUiModel(
    val slotKey: String,
    val startTime: String,
    val endTime: String,
    val rawStart: String,
    val rawEnd: String,
    val duration: Int,
    val status: TimeSlotStatus,
    val disabledReason: String?,
    val availableCourts: Int?,
    val totalCourts: Int?
)

@Composable
fun BookingScreen(
    venueId: Int,
    sportId: Int,
    sportName: String,
    sportPrice: String,
    sportImageURL: String,
    complexName: String,
    complexLocation: String,
    complexRating: Double,
    complexReviews: Int,
    sportOpeningHours: Map<String, OpeningHourEntryDto>? = null,
    venueOpeningHours: Map<String, OpeningHourEntryDto>? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (BookingPayload) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
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

    val daysOfWeek = remember { listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday") }

    LaunchedEffect(sportId, venueId) {
        viewModel.initBooking(
            sportId = sportId,
            venueId = venueId,
            sportName = sportName,
            sportPrice = sportPrice,
            sportImageURL = sportImageURL,
            complexName = complexName,
            complexLocation = complexLocation,
            complexRating = complexRating,
            complexReviews = complexReviews,
            sportOpeningHours = sportOpeningHours,
            venueOpeningHours = venueOpeningHours
        )
    }

    LaunchedEffect(state.navigateToSummary, state.payload) {
        if (state.navigateToSummary && state.payload != null) {
            onNavigateToSummary(state.payload)
            viewModel.clearNavigation()
        }
    }

    val pricePerHour = remember(state.sportPrice) {
        var cleaned = state.sportPrice.replace("Rs.", "").replace("Rs", "").replace("/hour", "").replace(",", "").trim()
        cleaned.toDoubleOrNull() ?: cleaned.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 500.0
    }

    val totalPrice = remember(state.selectedSlots, state.bookingType, state.selectedDays, pricePerHour) {
        val base = state.selectedSlots.size * pricePerHour
        if (state.bookingType == 1) base * state.selectedDays.size * 4.0 else base
    }

    // Opening Hours Logic
    val isSelectedDayClosed = remember(state.selectedDate, state.venueOpeningHours, state.sportOpeningHours) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = try { sdf.parse(state.selectedDate) ?: Date() } catch (e: Exception) { Date() }
        val dayName = SimpleDateFormat("EEEE", Locale.US).format(d).lowercase()

        state.venueOpeningHours?.get(dayName)?.isClosed == true || state.sportOpeningHours?.get(dayName)?.isClosed == true
    }

    val selectedDayName = remember(state.selectedDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = try { sdf.parse(state.selectedDate) ?: Date() } catch (e: Exception) { Date() }
        SimpleDateFormat("EEEE", Locale.US).format(d)
    }

    val openingHourRange = remember(state.selectedDate, state.bookingType, state.selectedDays, state.venueOpeningHours, state.sportOpeningHours) {
        val defaultStart = 6
        val defaultEnd = 24
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d = try { sdf.parse(state.selectedDate) ?: Date() } catch (e: Exception) { Date() }

        val relevantDays = if (state.bookingType == 1 && state.selectedDays.isNotEmpty()) {
            state.selectedDays.map { it.lowercase() }
        } else {
            listOf(SimpleDateFormat("EEEE", Locale.US).format(d).lowercase())
        }

        var earliestOpen = 24
        var latestClose = 0

        fun parseHour(timeStr: String?): Int? {
            if (timeStr.isNullOrEmpty()) return null
            return timeStr.split(":").firstOrNull()?.toIntOrNull()
        }

        for (day in relevantDays) {
            if (state.venueOpeningHours?.get(day)?.isClosed == true) continue
            if (state.sportOpeningHours?.get(day)?.isClosed == true) continue

            val sportEntry = state.sportOpeningHours?.get(day)
            if (sportEntry != null && !sportEntry.isClosed) {
                parseHour(sportEntry.open)?.let { earliestOpen = kotlin.math.min(earliestOpen, it) }
                parseHour(sportEntry.close)?.let {
                    val eff = if (it == 0) 24 else it
                    latestClose = kotlin.math.max(latestClose, eff)
                }
            } else {
                val venueEntry = state.venueOpeningHours?.get(day)
                if (venueEntry != null && !venueEntry.isClosed) {
                    parseHour(venueEntry.open)?.let { earliestOpen = kotlin.math.min(earliestOpen, it) }
                    parseHour(venueEntry.close)?.let {
                        val eff = if (it == 0) 24 else it
                        latestClose = kotlin.math.max(latestClose, eff)
                    }
                }
            }
        }

        if (earliestOpen >= latestClose) Pair(defaultStart, defaultEnd) else Pair(earliestOpen, latestClose)
    }

    fun formatTo12Hour(time24: String): String {
        val parts = time24.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return time24
        val h = parts[0]
        val m = parts[1]
        val period = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format("%02d:%02d %s", h12, m, period)
    }

    fun normalizedSlotKey(slot: SlotData): String {
        slot.slotKey?.let { if (it.isNotEmpty()) return it.replace("-24:00", "-00:00") }
        val rs = slot.rawStart ?: slot.startTime ?: ""
        val reRaw = slot.rawEnd ?: slot.endTime ?: ""
        val re = reRaw.replace("24:00", "00:00")
        return "$rs-$re"
    }

    val timeSlots: List<TimeSlotUiModel> = remember(state.bookingType, isSelectedDayClosed, state.apiSlots, state.selectedSlots, state.processingSlotKeys, state.permanentAvailability, state.selectedDays, openingHourRange) {
        if (state.bookingType == 1) {
            val range = openingHourRange
            val result = mutableListOf<TimeSlotUiModel>()
            for (hour in range.first until range.second) {
                val rawStart = String.format(Locale.US, "%02d:00", hour)
                val rawEnd = if (hour + 1 == 24) "00:00" else String.format(Locale.US, "%02d:00", hour + 1)
                val slotKey = "$rawStart-$rawEnd"
                val displayStart = formatTo12Hour(rawStart)
                val displayEnd = formatTo12Hour(rawEnd)
                val isSelected = state.selectedSlots.any { normalizedSlotKey(it) == slotKey }
                val isProcessing = state.processingSlotKeys.contains(slotKey)
                val hasExisting = state.userExistingPermanentBookings.contains(slotKey)
                val avail = state.permanentAvailability[slotKey]

                var status = TimeSlotStatus.AVAILABLE
                var reason: String? = null

                if (isProcessing) {
                    status = TimeSlotStatus.PROCESSING
                } else if (isSelected) {
                    status = TimeSlotStatus.SELECTED
                } else if (hasExisting) {
                    status = TimeSlotStatus.ALREADY_BOOKED
                    reason = "Already booked by you"
                } else if (avail == null && state.selectedDays.isNotEmpty() && state.loadingPermanentAvailability) {
                    status = TimeSlotStatus.PROCESSING
                    reason = "Checking..."
                } else if (avail != null) {
                    if (!avail.available || avail.daysRemaining == 0) {
                        status = TimeSlotStatus.FULLY_BOOKED
                        reason = "Fully booked (${avail.bookedCount}/${avail.totalDaysChecked} days)"
                    }
                } else if (state.selectedDays.isNotEmpty() && !state.loadingPermanentAvailability) {
                    continue
                }

                result.add(
                    TimeSlotUiModel(
                        slotKey = slotKey,
                        startTime = displayStart,
                        endTime = displayEnd,
                        rawStart = rawStart,
                        rawEnd = rawEnd,
                        duration = 60,
                        status = status,
                        disabledReason = reason,
                        availableCourts = null,
                        totalCourts = null
                    )
                )
            }
            result
        } else {
            if (isSelectedDayClosed) emptyList()
            else {
                val range = openingHourRange
                val apiLookup = state.apiSlots.associateBy { normalizedSlotKey(it) }
                val result = mutableListOf<TimeSlotUiModel>()

                for (hour in range.first until range.second) {
                    val rawStart = String.format(Locale.US, "%02d:00", hour)
                    val rawEnd = if (hour + 1 == 24) "00:00" else String.format(Locale.US, "%02d:00", hour + 1)
                    val slotKey = "$rawStart-$rawEnd"
                    val displayStart = formatTo12Hour(rawStart)
                    val displayEnd = formatTo12Hour(rawEnd)

                    val isSelected = state.selectedSlots.any { normalizedSlotKey(it) == slotKey }
                    val isProcessing = state.processingSlotKeys.contains(slotKey)

                    var status = TimeSlotStatus.AVAILABLE
                    var reason: String? = null

                    val apiSlot = apiLookup[slotKey]
                    if (apiSlot != null) {
                        val isHeldByCurrentUser = apiSlot.heldByCurrentUser == true
                        val reasonText = apiSlot.disabledReason?.lowercase() ?: ""
                        val isHeldReason = reasonText.contains("held")

                        if (isProcessing) status = TimeSlotStatus.PROCESSING
                        else if (isSelected || isHeldByCurrentUser) status = TimeSlotStatus.SELECTED
                        else if (apiSlot.isPastTime == true) status = TimeSlotStatus.TIME_PASSED
                        else if (apiSlot.isHeld == true || isHeldReason) status = TimeSlotStatus.HELD
                        else if (apiSlot.isFullyBooked == true || apiSlot.available == false) status = TimeSlotStatus.FULLY_BOOKED

                        reason = if (!isSelected && !isHeldByCurrentUser && !isProcessing) apiSlot.disabledReason else null
                    } else if (state.isLoadingSlots) {
                        status = TimeSlotStatus.PROCESSING
                        reason = "Loading..."
                    } else {
                        continue
                    }

                    result.add(
                        TimeSlotUiModel(
                            slotKey = slotKey,
                            startTime = displayStart,
                            endTime = displayEnd,
                            rawStart = rawStart,
                            rawEnd = rawEnd,
                            duration = 60,
                            status = status,
                            disabledReason = reason,
                            availableCourts = apiSlot?.availableCourts,
                            totalCourts = apiSlot?.totalCourts
                        )
                    )
                }
                result
            }
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
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Book Slot",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = state.complexName,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.selectedSlots.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Price (${state.selectedSlots.size} slots)",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = "LKR %.2f".format(totalPrice),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryGreen
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.checkPhoneVerificationAndProceed()
                                },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                            ) {
                                Text("Continue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BOOKING TYPE SECTION
            SectionCard(icon = Icons.Default.CalendarToday, title = "Booking Type", cardBg = cardBg, borderClr = borderClr, primaryGreen = primaryGreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    listOf("Normal", "Permanent").forEachIndexed { idx, typeName ->
                        val isSel = state.bookingType == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(if (isSel) primaryGreen else Color.Transparent)
                                .clickable { viewModel.setBookingType(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSel) Color.White else textSecondary
                            )
                        }
                    }
                }
            }

            // 2. SELECT SPORT SECTION
            SectionCard(icon = Icons.Default.SportsCricket, title = "Select Sport", cardBg = cardBg, borderClr = borderClr, primaryGreen = primaryGreen) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.White)
                            .border(1.dp, borderClr, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setShowSportPicker(true) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.sportName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = textSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = state.sportImageURL,
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(state.sportName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(if (state.sportPrice.contains("/")) state.sportPrice else "${state.sportPrice}/hour", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                                Text("%.1f".format(state.complexRating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("(${state.complexReviews} reviews)", fontSize = 12.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }

            // 3. DATE & DAYS SELECTOR
            if (state.bookingType == 0) {
                SectionCard(icon = Icons.Default.DateRange, title = "Select Date & Time", cardBg = cardBg, borderClr = borderClr, primaryGreen = primaryGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.White)
                                .border(1.dp, borderClr, RoundedCornerShape(12.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, yr, mo, dy ->
                                            val c = Calendar.getInstance()
                                            c.set(yr, mo, dy)
                                            val sel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
                                            viewModel.selectDate(sel)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = try {
                                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(state.selectedDate) ?: Date()
                                    SimpleDateFormat("d MMMM yyyy", Locale.US).format(d)
                                } catch (e: Exception) { state.selectedDate },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = textSecondary)
                        }

                        // Horizontal Date Strip (30 days)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (offset in 0 until 30) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, offset)
                                val dateObj = cal.time
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dateObj)
                                val isSel = dateStr == state.selectedDate

                                val dayName = SimpleDateFormat("EEE", Locale.US).format(dateObj).uppercase()
                                val dayNum = SimpleDateFormat("d", Locale.US).format(dateObj)
                                val monthName = SimpleDateFormat("MMM", Locale.US).format(dateObj).uppercase()

                                Box(
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) primaryGreen else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White))
                                        .border(1.dp, if (isSel) primaryGreen else borderClr, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.selectDate(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(dayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else textSecondary)
                                        Text(dayNum, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isSel) Color.White else textPrimary)
                                        Text(monthName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White.copy(alpha = 0.8f) else textSecondary)
                                    }
                                }
                            }
                        }

                        Text("Available Time Slots", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                        if (state.isLoadingSlots) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(20.dp), color = primaryGreen)
                        } else if (isSelectedDayClosed) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                Text("Venue is closed on $selectedDayName", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
                                Text("Please select a different date", fontSize = 13.sp, color = textSecondary)
                            }
                        } else if (timeSlots.isEmpty()) {
                            Text("No available slots for this date", fontSize = 14.sp, color = textSecondary, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), textAlign = TextAlign.Center)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.heightIn(max = 600.dp)
                            ) {
                                items(timeSlots, key = { it.slotKey }) { slot ->
                                    TimeSlotCell(slot = slot, primaryGreen = primaryGreen, textPrimary = textPrimary, textSecondary = textSecondary, cardBg = cardBg, borderClr = borderClr, isDark = isDark) {
                                        viewModel.handleSlotTap(slot.slotKey, slot.rawStart, slot.rawEnd, slot.startTime, slot.endTime, slot.duration)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Permanent Booking Days Selector
                SectionCard(icon = Icons.Default.EventRepeat, title = "Select Days", cardBg = cardBg, borderClr = borderClr, primaryGreen = primaryGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select 1 day (for 1 month)", fontSize = 13.sp, color = textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            daysOfWeek.forEach { day ->
                                val isSel = state.selectedDays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) primaryGreen else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White))
                                        .border(1.dp, if (isSel) primaryGreen else borderClr, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.toggleDaySelection(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(day.take(3), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) Color.White else textPrimary)
                                }
                            }
                        }
                    }
                }

                SectionCard(icon = Icons.Default.AccessTime, title = "Select Time Slots", cardBg = cardBg, borderClr = borderClr, primaryGreen = primaryGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Available Time Slots", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        if (state.selectedDays.isEmpty()) {
                            Text("Please select days first", fontSize = 14.sp, color = textSecondary, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), textAlign = TextAlign.Center)
                        } else if (state.loadingPermanentAvailability && state.permanentAvailability.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(20.dp), color = primaryGreen)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.heightIn(max = 600.dp)
                            ) {
                                items(timeSlots, key = { it.slotKey }) { slot ->
                                    TimeSlotCell(slot = slot, primaryGreen = primaryGreen, textPrimary = textPrimary, textSecondary = textSecondary, cardBg = cardBg, borderClr = borderClr, isDark = isDark, permanentAvail = state.permanentAvailability[slot.slotKey]) {
                                        viewModel.handleSlotTap(slot.slotKey, slot.rawStart, slot.rawEnd, slot.startTime, slot.endTime, slot.duration)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Max slots alert
    if (state.showMaxSlotsAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowMaxSlotsAlert(false) },
            title = { Text("Maximum Slots Reached", fontWeight = FontWeight.Bold) },
            text = { Text("You can book up to 4 slots at once.") },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowMaxSlotsAlert(false) }) {
                    Text("OK", color = primaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Phone Verification Sheet Modal
    if (state.showPhoneVerificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissPhoneVerificationSheet() },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(primaryGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(32.dp))
                }

                Text("Phone Verification Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Please verify your phone number to continue with booking.", fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center)

                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = { viewModel.updatePhoneNumber(it) },
                    label = { Text("07XXXXXXXX") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = primaryGreen) },
                    enabled = !state.isPhoneSending && !state.isPhoneVerifying && state.phoneChallengeId == null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.phoneChallengeId != null) {
                    OutlinedTextField(
                        value = state.phoneOTP,
                        onValueChange = { viewModel.updatePhoneOTP(it) },
                        label = { Text("6-digit OTP") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = primaryGreen) },
                        enabled = !state.isPhoneVerifying,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.phoneVerificationError?.let { err ->
                    Text(err, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (state.phoneChallengeId == null) {
                    Button(
                        onClick = { viewModel.sendPhoneOTP() },
                        enabled = state.phoneNumber.length == 10 && !state.isPhoneSending,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isPhoneSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("Send OTP", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.verifyPhoneOTP() },
                        enabled = state.phoneOTP.length == 6 && !state.isPhoneVerifying,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isPhoneVerifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("Verify & Continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                TextButton(onClick = { viewModel.dismissPhoneVerificationSheet() }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        }
    }
}

@Composable
private fun TimeSlotCell(
    slot: TimeSlotUiModel,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    isDark: Boolean,
    permanentAvail: PermanentSlotAvailability? = null,
    onTap: () -> Unit
) {
    val isDisabled = (slot.status == TimeSlotStatus.TIME_PASSED || slot.status == TimeSlotStatus.FULLY_BOOKED || slot.status == TimeSlotStatus.HELD || slot.status == TimeSlotStatus.ALREADY_BOOKED || slot.status == TimeSlotStatus.PROCESSING) && slot.status != TimeSlotStatus.SELECTED

    val bg = when (slot.status) {
        TimeSlotStatus.SELECTED -> primaryGreen
        TimeSlotStatus.FULLY_BOOKED -> Color(0xFFFEE2E2)
        TimeSlotStatus.HELD -> Color(0xFFFEF3C7)
        TimeSlotStatus.ALREADY_BOOKED -> Color(0xFFF3E8FF)
        TimeSlotStatus.TIME_PASSED -> if (isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFF1F5F9)
        else -> if (isDark) Color.White.copy(alpha = 0.04f) else Color.White
    }

    val textClr = when (slot.status) {
        TimeSlotStatus.SELECTED -> Color.White
        TimeSlotStatus.FULLY_BOOKED -> Color(0xFFDC2626)
        TimeSlotStatus.HELD -> Color(0xFFD97706)
        TimeSlotStatus.ALREADY_BOOKED -> Color(0xFF7E22CE)
        TimeSlotStatus.TIME_PASSED -> textSecondary.copy(alpha = 0.6f)
        else -> textPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.5.dp, if (slot.status == TimeSlotStatus.SELECTED) primaryGreen else borderClr, RoundedCornerShape(10.dp))
            .clickable(enabled = !isDisabled) { onTap() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (slot.status == TimeSlotStatus.PROCESSING) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryGreen, strokeWidth = 2.dp)
            } else {
                Text("${slot.startTime} - ${slot.endTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textClr, maxLines = 1)

                slot.totalCourts?.let { tot ->
                    if (tot > 1) {
                        val avail = slot.availableCourts ?: 0
                        Text(
                            text = "$avail/$tot courts",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (slot.status == TimeSlotStatus.SELECTED) Color.White else if (avail > 0) primaryGreen else textClr
                        )
                    }
                }

                when (slot.status) {
                    TimeSlotStatus.SELECTED -> Text("✓ Selected", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    TimeSlotStatus.TIME_PASSED -> Text("Time passed", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textClr)
                    TimeSlotStatus.FULLY_BOOKED -> Text(slot.disabledReason ?: "Fully booked", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textClr)
                    TimeSlotStatus.HELD -> Text("Temporarily held", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textClr)
                    TimeSlotStatus.ALREADY_BOOKED -> Text("Already booked by you", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textClr)
                    else -> slot.disabledReason?.let { Text(it, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textClr) }
                }

                permanentAvail?.let { pa ->
                    if (slot.status == TimeSlotStatus.AVAILABLE) {
                        Text(
                            text = "${pa.daysRemaining}/${pa.totalDaysChecked} days available",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (pa.daysRemaining < pa.totalDaysChecked / 2) Color.Red else primaryGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    cardBg: Color,
    borderClr: Color,
    primaryGreen: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(16.dp))
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        content()
    }
}

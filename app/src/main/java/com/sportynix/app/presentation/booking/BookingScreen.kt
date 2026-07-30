package com.sportynix.app.presentation.booking

import android.app.DatePickerDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    venueId: String,
    sportId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (venueId: String, sportId: String, date: String, slotIds: String, bookingType: String, selectedDays: String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var selectedBookingType by remember { mutableStateOf("Normal") } // "Normal" or "Permanent"
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var selectedSlots by remember { mutableStateOf(setOf<TimeSlot>()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1000
    }

    LaunchedEffect(venueId, sportId) {
        viewModel.loadVenueAndSlots(venueId, sportId, selectedDate)
    }

    val availableSlots = remember {
        listOf(
            TimeSlot("slot_1", "07:00 AM", "08:00 AM", 400.0, true),
            TimeSlot("slot_2", "08:00 AM", "09:00 AM", 400.0, true),
            TimeSlot("slot_3", "09:00 AM", "10:00 AM", 400.0, true),
            TimeSlot("slot_4", "10:00 AM", "11:00 AM", 400.0, false),
            TimeSlot("slot_5", "04:00 PM", "05:00 PM", 400.0, true),
            TimeSlot("slot_6", "05:00 PM", "06:00 PM", 400.0, true),
            TimeSlot("slot_7", "06:00 PM", "07:00 PM", 400.0, true),
            TimeSlot("slot_8", "07:00 PM", "08:00 PM", 400.0, true)
        )
    }

    val totalAmount = selectedSlots.sumOf { it.price }

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
                    Text(
                        text = "Book Slot",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        bottomBar = {
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
                                text = "Total Price (${selectedSlots.size} slots)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "LKR %.2f".format(totalAmount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryGreen
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedSlots.isEmpty()) {
                                    validationError = "Please select at least one available slot"
                                    return@Button
                                }
                                if (selectedBookingType == "Permanent" && selectedDays.isEmpty()) {
                                    validationError = "Please select at least one day of the week"
                                    return@Button
                                }
                                validationError = null
                                val slotKeys = selectedSlots.joinToString(",") { it.id }
                                val daysStr = selectedDays.joinToString(",")
                                onNavigateToSummary(venueId, sportId, selectedDate, slotKeys, selectedBookingType, daysStr)
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── 1. BOOKING TYPE SWITCHER (Normal vs Permanent) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isNormal = selectedBookingType == "Normal"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isNormal) primaryGreen else if (isDark) Color(0xFF1B2228) else Color(0xFFE2E8F0))
                        .clickable { selectedBookingType = "Normal" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Normal Booking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isNormal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val isPermanent = selectedBookingType == "Permanent"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isPermanent) primaryGreen else if (isDark) Color(0xFF1B2228) else Color(0xFFE2E8F0))
                        .clickable { selectedBookingType = "Permanent" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Permanent Booking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isPermanent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── 2. DATE OR WEEKDAY SELECTOR ──
            if (selectedBookingType == "Normal") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clickable { datePickerDialog.show() },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Select Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    Text("Select Recurring Days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        days.forEach { day ->
                            val isSel = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) primaryGreen else if (isDark) Color(0xFF1E242B) else Color(0xFFE2E8F0))
                                    .clickable {
                                        selectedDays = if (isSel) selectedDays - day else selectedDays + day
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(day.take(3), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Validation Error Alert
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // ── 3. TIME SLOT GRID ──
            Text("Available Time Slots", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableSlots.chunked(2).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val isSelected = selectedSlots.contains(slot)
                            val isAvail = slot.isAvailable

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            isSelected -> primaryGreen
                                            !isAvail -> if (isDark) Color(0xFF2D1B1B) else Color(0xFFFEE2E2)
                                            else -> if (isDark) Color(0xFF1E242B) else Color(0xFFF1F5F9)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) primaryGreen else if (!isAvail) Color(0xFFEF4444).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = isAvail) {
                                        selectedSlots = if (isSelected) selectedSlots - slot else selectedSlots + slot
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${slot.startTime} - ${slot.endTime}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSelected -> Color.White
                                            !isAvail -> Color(0xFFEF4444)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = if (!isAvail) "Booked Out" else "LKR %.0f".format(slot.price),
                                        fontSize = 10.sp,
                                        color = when {
                                            isSelected -> Color.White.copy(alpha = 0.8f)
                                            !isAvail -> Color(0xFFEF4444).copy(alpha = 0.8f)
                                            else -> primaryGreen
                                        }
                                    )
                                }
                            }
                        }
                        if (rowSlots.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 4. PAYMENT POLICY SUMMARY CARD ──
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Payment Policy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Advance or full online payment option is supported at this venue.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

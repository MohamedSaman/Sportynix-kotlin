package com.sportynix.app.presentation.booking

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.*

data class BookingCardItem(
    val id: String,
    val bookingType: String, // "One-time Booking", "Permanent Booking"
    val createdDate: String,
    val venueName: String,
    val sportCourt: String,
    val playDate: String,
    val timeSlot: String,
    val duration: String,
    val address: String,
    val amount: Double,
    val status: String, // "Upcoming", "Completed", "No-Show", "Cancelled"
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var selectedBookingType by remember { mutableIntStateOf(0) } // 0: Normal Bookings, 1: Permanent Bookings
    var selectedFilterChip by remember { mutableStateOf("All") }
    var showQrModal by remember { mutableStateOf(false) }
    var selectedQrId by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    val sampleBookings = remember {
        listOf(
            BookingCardItem(
                id = "436",
                bookingType = "One-time Booking",
                createdDate = "2026-07-23",
                venueName = "Sportynix sport's com...",
                sportCourt = "Badminton • 1",
                playDate = "2026-07-23 - 2026-07-23",
                timeSlot = "12:00 PM - 01:00 PM",
                duration = "60 min",
                address = "Warana Rd, Kalagedihena, Gampaha",
                amount = 400.0,
                status = "Upcoming",
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=500"
            ),
            BookingCardItem(
                id = "425",
                bookingType = "One-time Booking",
                createdDate = "2026-07-21",
                venueName = "Sportynix sport's com...",
                sportCourt = "Badminton • 1",
                playDate = "2026-07-22 - 2026-07-22",
                timeSlot = "08:00 AM - 09:00 AM",
                duration = "60 min",
                address = "Warana Rd, Kalagedihena, Gampaha",
                amount = 400.0,
                status = "No-Show",
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=500"
            )
        )
    }

    val filteredBookings = remember(selectedFilterChip) {
        when (selectedFilterChip) {
            "Upcoming" -> sampleBookings.filter { it.status == "Upcoming" }
            "Completed" -> sampleBookings.filter { it.status == "Completed" }
            "Cancelled" -> sampleBookings.filter { it.status == "Cancelled" }
            else -> sampleBookings
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Booking",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "History & Upcoming",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort Button
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = SportynixGreenPrimary
                            )
                        }

                        // Add Booking FAB Header Icon
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) NeonGreen else SportynixGreenPrimary)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── 1. NORMAL VS PERMANENT BOOKINGS TYPE SELECTOR ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Normal Bookings Card
                    val isNormalSelected = selectedBookingType == 0
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { selectedBookingType = 0 },
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isNormalSelected) {
                            if (isDark) NeonGreen.copy(alpha = 0.85f) else SportynixGreenPrimary
                        } else Color.Transparent,
                        elevation = if (isNormalSelected) 6.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isNormalSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Normal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isNormalSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    Text("Bookings", fontSize = 11.sp, color = if (isNormalSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("58", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    // Permanent Bookings Card
                    val isPermSelected = selectedBookingType == 1
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { selectedBookingType = 1 },
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isPermSelected) {
                            if (isDark) NeonGreen.copy(alpha = 0.85f) else SportynixGreenPrimary
                        } else Color.Transparent,
                        elevation = if (isPermSelected) 6.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (isPermSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Permanent", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isPermSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    Text("Bookings", fontSize = 11.sp, color = if (isPermSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("73", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── 2. HORIZONTAL FILTER CHIPS ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterChips = listOf("All (58)", "Upcoming (1)", "Completed (3)", "Cancelled (0)")
                    filterChips.forEach { chipText ->
                        val chipKey = chipText.split(" ")[0]
                        val isSelected = selectedFilterChip == chipKey

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        if (isDark) NeonGreen else SportynixGreenPrimary
                                    } else (if (isDark) GlassCardDark else GlassCardLight)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) (if (isDark) NeonGreen else SportynixGreenPrimary) else GlassBorderDark.copy(alpha = 0.2f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilterChip = chipKey }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = chipText,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── 3. BOOKING CARDS LIST ──
            items(filteredBookings) { booking ->
                PaddingValues(horizontal = 16.dp, vertical = 8.dp).let {
                    Box(modifier = Modifier.padding(it)) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(booking.id) },
                            shape = RoundedCornerShape(24.dp),
                            elevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header Badge Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SportynixGreenPrimary.copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "📅 ${booking.bookingType}",
                                            color = SportynixGreenPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "📅 ${booking.createdDate}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Venue Thumbnail & Details Row
                                Row(verticalAlignment = Alignment.Top) {
                                    AsyncImage(
                                        model = booking.imageUrl,
                                        contentDescription = booking.venueName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = booking.venueName,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Status Badge Chip
                                            val statusBg = if (booking.status == "Upcoming") Color(0x3310B981) else Color(0x33F59E0B)
                                            val statusTextColor = if (booking.status == "Upcoming") Color(0xFF10B981) else Color(0xFFF59E0B)

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(statusBg)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = booking.status,
                                                    color = statusTextColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = booking.sportCourt,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(text = "📅 Play Date: ${booking.playDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "🕒 ${booking.timeSlot}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "🕒 ${booking.duration}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "📍 ${booking.address}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Price & Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LKR %.2f".format(booking.amount),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) NeonGreen else SportynixGreenPrimary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedQrId = booking.id
                                                showQrModal = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SportynixGreenPrimary.copy(alpha = 0.5f)),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Show QR", color = SportynixGreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { showCancelDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444)),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x22DC2626)),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Assign Team Button
                                OutlinedButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SportynixGreenPrimary.copy(alpha = 0.5f))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, tint = SportynixGreenPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Assign Team", color = SportynixGreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Card Footer: Booking ID
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Booking ID: ${booking.id}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = "Share",
                                        tint = SportynixGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── QR CODE MODAL ──
        if (showQrModal) {
            Dialog(onDismissRequest = { showQrModal = false }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Booking QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(160.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Booking ID: #${selectedQrId.ifEmpty { "436" }}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showQrModal = false },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }

        // ── CANCEL DIALOG ──
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Booking", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to cancel this booking?") },
                confirmButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Cancel Booking", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Booking")
                    }
                }
            )
        }
    }
}

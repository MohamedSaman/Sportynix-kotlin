package com.sportynix.app.presentation.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Booking) -> Unit,
    onNavigateToCancel: (Booking) -> Unit,
    viewModel: BookingHistoryViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    val normalFilters = listOf("All", "Upcoming", "Pending", "Completed", "Cancelled", "No-Show")
    val permanentFilters = listOf("All", "Upcoming", "Confirmed", "Pending", "Playing", "Completed", "Cancelled", "No-Show")
    val currentFilters = if (state.selectedBookingType == 0) normalFilters else permanentFilters

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
                    Text("Booking History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                            .clickable { viewModel.setShowSortSheet(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = primaryGreen, modifier = Modifier.size(20.dp))
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
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -30f && state.selectedBookingType == 0) {
                            viewModel.setBookingType(1)
                        } else if (dragAmount > 30f && state.selectedBookingType == 1) {
                            viewModel.setBookingType(0)
                        }
                    }
                }
        ) {
            // Mode Toggle Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (state.selectedBookingType == 0) primaryGreen else Color.Transparent)
                        .clickable { viewModel.setBookingType(0) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Normal Bookings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.selectedBookingType == 0) Color.White else textSecondary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (state.selectedBookingType == 1) primaryGreen else Color.Transparent)
                        .clickable { viewModel.setBookingType(1) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Permanent Series", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.selectedBookingType == 1) Color.White else textSecondary)
                }
            }

            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentFilters) { filter ->
                    val isSelected = state.selectedFilter.equals(filter, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) primaryGreen else cardBg)
                            .border(1.dp, if (isSelected) Color.Transparent else borderClr, CircleShape)
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(filter, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textPrimary)
                    }
                }
            }

            // Booking List
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryGreen)
                }
            } else if (state.bookings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No bookings found in history.", fontSize = 14.sp, color = textSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.bookings) { booking ->
                        BookingCardItem(
                            booking = booking,
                            primaryGreen = primaryGreen,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            cardBg = cardBg,
                            borderClr = borderClr,
                            onCardClick = { onNavigateToDetail(booking) },
                            onQRClick = { viewModel.openQRModal(booking) },
                            onTeamClick = { viewModel.openTeamSheet(booking) },
                            onCancelClick = { onNavigateToCancel(booking) }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sort Bookings By", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    BookingSortOption.values().forEach { option ->
                        val isSelected = state.sortOption == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { viewModel.setSortOption(option) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(option.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(option.description, fontSize = 12.sp, color = textSecondary)
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primaryGreen)
                            }
                        }
                    }
                }
            }
        }

        // QR Code Modal
        if (state.showQRModal && state.selectedBookingForQR != null) {
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
                        val qrUrl = state.qrCodeUrl ?: "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${state.selectedBookingForQR!!.bookingId}"
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
    }
}

@Composable
private fun BookingCardItem(
    booking: Booking,
    primaryGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    borderClr: Color,
    onCardClick: () -> Unit,
    onQRClick: () -> Unit,
    onTeamClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primaryGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(if (booking.isPermanent) "Permanent Series" else "Single Booking", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primaryGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(booking.status.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = booking.imageURL,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(booking.complexName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("${booking.sport} · ${booking.courtName}", fontSize = 13.sp, color = textSecondary)
                Text("${booking.playDateStart} · ${booking.timeSlot}", fontSize = 12.sp, color = primaryGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = borderClr)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(booking.price, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryGreen)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (booking.qrCode) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(primaryGreen.copy(alpha = 0.12f))
                            .clickable { onQRClick() }
                            .padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR", tint = primaryGreen, modifier = Modifier.size(18.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(primaryGreen.copy(alpha = 0.12f))
                        .clickable { onTeamClick() }
                        .padding(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = "Team", tint = primaryGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

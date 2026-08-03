package com.sportynix.app.presentation.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingCancellationReviewScreen(
    bookingId: Int,
    cancellationMode: String = "single",
    onNavigateBack: () -> Unit,
    onNavigateToBookingDetail: (Int) -> Unit,
    onNavigateToBookingHistory: () -> Unit,
    viewModel: BookingCancellationViewModel = hiltViewModel()
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    val isSeries = cancellationMode == "series"

    LaunchedEffect(bookingId) {
        viewModel.loadBooking(bookingId)
    }

    val booking = viewModel.booking
    val policy = booking?.refundPolicy
    val isRefundEligible = policy?.eligible == true
    val refundAmount = policy?.refundAmount ?: booking?.onlinePaidAmount ?: booking?.amountPaid ?: booking?.advanceAmount ?: 0.0

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
                    Text(if (isSeries) "Cancel Permanent Series" else "Cancel Booking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
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
                Button(
                    onClick = {
                        viewModel.cancelBooking(bookingId, isSeries) {
                            if (isSeries) onNavigateToBookingHistory() else onNavigateToBookingDetail(bookingId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !viewModel.isCancelling && !viewModel.isLoadingPolicy
                ) {
                    if (viewModel.isCancelling) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isSeries) "Confirm Permanent Series Cancellation" else "Confirm Cancellation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Go Back", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }
        },
        containerColor = bgClr
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(26.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("This action cannot be undone", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    Text(
                        text = if (isSeries) "Cancelling releases all active generated slots for other users. Eligible refunds are queued per slot for Sportynix review."
                        else "Cancelling releases this court slot for other users. Refund approval depends on the policy below.",
                        fontSize = 13.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 18.sp
                    )
                }
            }

            // Booking Details Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(booking?.venueName ?: booking?.complexName ?: booking?.venue ?: "Venue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("${booking?.sportName ?: booking?.sport ?: "Sport"} · ${booking?.date ?: ""}", fontSize = 14.sp, color = textSecondary)
                Text(booking?.time ?: "${booking?.startTime ?: ""} - ${booking?.endTime ?: ""}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
            }

            // Refund Policy Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Refund Policy & Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                if (viewModel.isLoadingPolicy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = primaryGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Fetching latest refund policy...", fontSize = 13.sp, color = textSecondary)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Refund eligibility", fontSize = 14.sp, color = textSecondary)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isRefundEligible) primaryGreen.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (isRefundEligible) "Eligible for review" else "Not refundable", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isRefundEligible) primaryGreen else Color.Red)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Refund amount", fontSize = 14.sp, color = textSecondary)
                        Text("LKR ${"%.2f".format(refundAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    policy?.deadline?.let { deadline ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Refund deadline", fontSize = 14.sp, color = textSecondary)
                            Text(deadline, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                        }
                    }
                }
            }

            viewModel.errorMessage?.let { err ->
                Text(err, fontSize = 13.sp, color = Color.Red, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

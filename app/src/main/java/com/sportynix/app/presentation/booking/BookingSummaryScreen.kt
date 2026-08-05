package com.sportynix.app.presentation.booking

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingSummaryScreen(
    payload: BookingPayload,
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: (PaymentCheckoutResponseDto) -> Unit,
    viewModel: BookingSummaryViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)

    LaunchedEffect(payload) {
        viewModel.initSummary(payload)
    }

    val quote = state.quote
    val bookingTotal = quote?.bookingTotal ?: "%.2f".format(payload.totalPrice)
    val advanceAmount = quote?.advanceAmount ?: "%.2f".format(payload.totalPrice * 0.5)
    val gatewayAmount = quote?.gatewayAmount ?: if (state.selectedPaymentOption == "advance") advanceAmount else bookingTotal
    val remainingBalance = quote?.remainingBalance ?: "0.00"
    val paymentRequired = quote?.paymentRequired ?: true
    val allowedPaymentOptions = quote?.allowedPaymentOptions.orEmpty()
    val paymentMode = quote?.paymentMode.orEmpty().lowercase()
    val canPayAdvance = allowedPaymentOptions.isEmpty() && paymentMode != "full_only" ||
        allowedPaymentOptions.any { it.equals("advance", ignoreCase = true) }
    val canPayFull = allowedPaymentOptions.isEmpty() && paymentMode != "advance_only" ||
        allowedPaymentOptions.any { it.equals("full", ignoreCase = true) }

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
                    Text("Booking Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(cardBg)
                    .border(1.dp, borderClr)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.confirmBooking { checkoutResp ->
                            onNavigateToCheckout(checkoutResp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    enabled = !state.isSubmittingBooking && !state.isLoadingQuote
                ) {
                    if (state.isSubmittingBooking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                !paymentRequired -> "Confirm Booking"
                                state.selectedPaymentOption == "full" -> "Pay Full Amount"
                                else -> "Pay Advance"
                            }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
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
            // Premium sport hero
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .height(140.dp)
                        .border(1.dp, borderClr, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    if (payload.sportImageURL.isNotBlank()) {
                        AsyncImage(
                            model = payload.sportImageURL,
                            contentDescription = payload.sportName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(cardBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SportsSoccer, null, tint = primaryGreen, modifier = Modifier.size(54.dp))
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(74.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f))))
                    )
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sports, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(payload.sportName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                SummaryCard(cardBg, borderClr, primaryGreen, textPrimary, textSecondary, "Booking Details", Icons.Default.EventNote) {
                    SummaryRow("Venue", payload.venueName, textPrimary, textSecondary)
                    SummaryRow("Sport", payload.sportName, textPrimary, textSecondary)
                    SummaryRow("Booking type", payload.bookingType, primaryGreen, textSecondary)
                    if (payload.bookingType.equals("Normal", ignoreCase = true)) {
                        SummaryRow("Date", formatBookingDate(payload.bookingDate), textPrimary, textSecondary)
                    } else {
                        SummaryRow("Selected days", payload.selectedDays.joinToString(", ") { it.take(3) }, textPrimary, textSecondary)
                        SummaryRow("Duration", "1 Month", textPrimary, textSecondary)
                    }
                    SummaryRow("Booked by", state.userName, textPrimary, textSecondary)
                    SummaryRow("Email", state.userEmail, textPrimary, textSecondary)
                    SummaryRow("Contact no", state.userPhone, textPrimary, textSecondary)
                    if (payload.venueAddress.isNotBlank()) SummaryRow("Location", payload.venueAddress, textPrimary, textSecondary)
                }
            }

            // Selected Slots Section
            item {
                SummaryCard(cardBg, borderClr, primaryGreen, textPrimary, textSecondary, "Selected Slots (${payload.slots.size})", Icons.Default.Schedule) {
                    payload.slots.forEach { slot ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${slot.startTime} - ${slot.endTime}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text("Rs. ${formatBookingMoney(slot.price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                    }
                    HorizontalDivider(color = borderClr)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Rs. ${formatBookingMoney(bookingTotal.toDoubleOrNull() ?: payload.totalPrice)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }
                }
            }

            // Payment Option Selector (Advance vs. Full)
            if (paymentRequired) item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payment Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("Choose how much to pay now", fontSize = 13.sp, color = textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (canPayAdvance) Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (state.selectedPaymentOption == "advance") primaryGreen.copy(alpha = 0.12f) else cardBg)
                                .border(if (state.selectedPaymentOption == "advance") 1.5.dp else 1.dp, if (state.selectedPaymentOption == "advance") primaryGreen else borderClr, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setPaymentOption("advance") }
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pay Advance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.selectedPaymentOption == "advance") Color.White else textPrimary)
                                Text("LKR $advanceAmount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (state.selectedPaymentOption == "advance") Color.White else primaryGreen)
                            }
                        }

                        if (canPayFull) Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (state.selectedPaymentOption == "full") primaryGreen.copy(alpha = 0.12f) else cardBg)
                                .border(if (state.selectedPaymentOption == "full") 1.5.dp else 1.dp, if (state.selectedPaymentOption == "full") primaryGreen else borderClr, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setPaymentOption("full") }
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pay Full", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (state.selectedPaymentOption == "full") Color.White else textPrimary)
                                Text("LKR $bookingTotal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (state.selectedPaymentOption == "full") Color.White else primaryGreen)
                            }
                        }
                    }
                    SummaryCard(cardBg, borderClr, primaryGreen, textPrimary, textSecondary, "Payment Breakdown", Icons.Default.CreditCard) {
                        SummaryRow("Booking total", "LKR $bookingTotal", textPrimary, textSecondary)
                        SummaryRow("Payable now", "LKR $gatewayAmount", primaryGreen, textSecondary)
                        if (remainingBalance != "0.00") SummaryRow("Remaining balance", "LKR $remainingBalance", Color(0xFFF59E0B), textSecondary)
                    }
                }
            }

            item {
                SummaryCard(cardBg, borderClr, primaryGreen, textPrimary, textSecondary, "Terms & Conditions", Icons.Default.Info) {
                    Text("• Refund eligibility follows the cancellation deadline.", fontSize = 13.sp, color = textSecondary)
                    Text("• Online payments are confirmed through the secure gateway.", fontSize = 13.sp, color = textSecondary)
                    Text("• Arrive 15 minutes before your slot.", fontSize = 13.sp, color = textSecondary)
                    Text("• Venue rules must be followed.", fontSize = 13.sp, color = textSecondary)
                }
            }

            // Saved Cards Option
            if (state.savedCards.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select Saved Card", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.savedCards) { card ->
                                val isSelected = state.selectedSavedCard?.id == card.id
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) primaryGreen.copy(alpha = 0.12f) else cardBg)
                                        .border(1.dp, if (isSelected) primaryGreen else borderClr, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setSelectedSavedCard(card) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(20.dp))
                                    Text("•••• ${card.last4 ?: "4242"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                }
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

    state.errorMessage?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearErrorMessage,
            title = { Text("Booking payment") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearErrorMessage) { Text("OK") } })
    }
}

@Composable
private fun SummaryCard(
    background: Color, border: Color, accent: Color, textPrimary: Color, textSecondary: Color,
    title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(background)
        .border(1.dp, border, RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        }
        content()
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color, labelColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.width(112.dp), fontSize = 14.sp, color = labelColor)
        Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor,
            textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatBookingMoney(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
private fun formatBookingDate(value: String): String = runCatching {
    java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(value)!!
    )
}.getOrDefault(value)

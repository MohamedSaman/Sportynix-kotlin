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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    LaunchedEffect(payload) {
        viewModel.initSummary(payload)
    }

    val quote = state.quote
    val bookingTotal = quote?.bookingTotal ?: "%.2f".format(payload.totalPrice)
    val advanceAmount = quote?.advanceAmount ?: "%.2f".format(payload.totalPrice * 0.5)
    val gatewayAmount = quote?.gatewayAmount ?: if (state.selectedPaymentOption == "advance") advanceAmount else bookingTotal
    val remainingBalance = quote?.remainingBalance ?: "0.00"

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
                        Text("Confirm & Pay LKR $gatewayAmount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            // Header Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = payload.sportImageURL,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(payload.sportName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(primaryGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(payload.bookingType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                            }
                        }

                        Text(payload.venueName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text(payload.bookingDate, fontSize = 12.sp, color = textSecondary)
                    }
                }
            }

            // Selected Slots Section
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
                    Text("Selected Slots (${payload.slots.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    payload.slots.forEach { slot ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(16.dp))
                                Text("${slot.startTime} - ${slot.endTime}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            }
                            Text("LKR ${"%.2f".format(slot.price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                    }
                }
            }

            // Payment Option Selector (Advance vs. Full)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Payment Option", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (state.selectedPaymentOption == "advance") primaryGreen.copy(alpha = 0.12f) else cardBg)
                                .border(if (state.selectedPaymentOption == "advance") 1.5.dp else 1.dp, if (state.selectedPaymentOption == "advance") primaryGreen else borderClr, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setPaymentOption("advance") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = state.selectedPaymentOption == "advance",
                                onClick = { viewModel.setPaymentOption("advance") },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                            )
                            Column {
                                Text("Pay Advance Only", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("LKR $advanceAmount", fontSize = 12.sp, color = primaryGreen)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (state.selectedPaymentOption == "full") primaryGreen.copy(alpha = 0.12f) else cardBg)
                                .border(if (state.selectedPaymentOption == "full") 1.5.dp else 1.dp, if (state.selectedPaymentOption == "full") primaryGreen else borderClr, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setPaymentOption("full") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = state.selectedPaymentOption == "full",
                                onClick = { viewModel.setPaymentOption("full") },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                            )
                            Column {
                                Text("Pay Full Amount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("LKR $bookingTotal", fontSize = 12.sp, color = primaryGreen)
                            }
                        }
                    }
                }
            }

            // Payment Breakdown Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderClr, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Payment Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 14.sp, color = textSecondary)
                        Text("LKR $bookingTotal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    }

                    if (quote?.discountAmount != null && quote.discountAmount != "0.00") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", fontSize = 14.sp, color = textSecondary)
                            Text("-LKR ${quote.discountAmount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                    }

                    HorizontalDivider(color = borderClr)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Booking Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("LKR $bookingTotal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payable Now", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        Text("LKR $gatewayAmount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }

                    if (remainingBalance != "0.00") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Balance (Pay at venue)", fontSize = 13.sp, color = textSecondary)
                            Text("LKR $remainingBalance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                        }
                    }
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
}

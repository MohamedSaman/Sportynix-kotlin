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
import com.sportynix.app.data.remote.dto.QuoteResponseDto
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingSummaryScreen(
    venueId: String,
    sportId: String,
    date: String,
    slotIds: String,
    bookingType: String = "Normal",
    selectedDays: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToPaymentPreview: (checkoutUrl: String, orderId: String, amount: Double) -> Unit,
    onNavigateToConfirmation: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var selectedPaymentOption by remember { mutableStateOf("advance") } // "advance" or "full"
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(venueId, sportId, date, slotIds, selectedPaymentOption) {
        viewModel.fetchQuote(venueId, sportId, date, slotIds, bookingType, selectedDays, selectedPaymentOption)
    }

    val quote = state.quoteResponse ?: QuoteResponseDto(
        bookingTotal = "800.00",
        paymentRequired = true,
        advanceRequired = true,
        advanceAmount = "400.00",
        gatewayAmount = "400.00",
        remainingBalance = "400.00",
        pointsDiscount = "0.00",
        acceptedPoints = 0,
        paymentOption = selectedPaymentOption,
        paymentMode = "advance_or_full",
        allowedPaymentOptions = listOf("advance", "full")
    )

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
                        text = "Booking Summary",
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
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.confirmBookingOrCheckout(
                            venueId = venueId,
                            sportId = sportId,
                            date = date,
                            slotIds = slotIds,
                            bookingType = bookingType,
                            selectedDays = selectedDays,
                            paymentOption = selectedPaymentOption,
                            onPaymentCheckoutReady = { url, orderId, amt ->
                                isSubmitting = false
                                onNavigateToPaymentPreview(url, orderId, amt)
                            },
                            onDirectConfirmationReady = {
                                isSubmitting = false
                                onNavigateToConfirmation()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = if (quote.paymentRequired == true) "Proceed to Payment" else "Confirm Booking",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. BOOKING OVERVIEW CARD ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Booking Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Booking Type", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(bookingType, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }
                    if (selectedDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recurring Days", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedDays, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // ── 2. PAYMENT BREAKDOWN CARD ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Price & Quote Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Booking Price", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("LKR ${quote.bookingTotal ?: "0.00"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Advance Required", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("LKR ${quote.advanceAmount ?: "0.00"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining Balance (at Venue)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("LKR ${quote.remainingBalance ?: "0.00"}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Online Gateway Amount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("LKR ${quote.gatewayAmount ?: "0.00"}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = primaryGreen)
                    }
                }
            }

            // ── 3. PAYMENT OPTION SELECTOR ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Payment Option",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedPaymentOption == "advance") primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (selectedPaymentOption == "advance") primaryGreen else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedPaymentOption = "advance" }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentOption == "advance",
                            onClick = { selectedPaymentOption = "advance" },
                            colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pay Advance Only", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Pay LKR ${quote.advanceAmount ?: "400.00"} now online, balance at venue.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedPaymentOption == "full") primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (selectedPaymentOption == "full") primaryGreen else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedPaymentOption = "full" }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentOption == "full",
                            onClick = { selectedPaymentOption = "full" },
                            colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pay Full Amount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Pay LKR ${quote.bookingTotal ?: "800.00"} now online.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

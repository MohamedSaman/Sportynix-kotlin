package com.sportynix.app.presentation.booking

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingAdvancePaymentPreviewScreen(
    checkoutResponse: PaymentCheckoutResponseDto?,
    bookingType: String = "Normal",
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (List<ConfirmedBookingDto>, String) -> Unit,
    viewModel: BookingPaymentViewModel = hiltViewModel()
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val bgClr = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)

    var isWebViewOpen by remember { mutableStateOf(false) }
    var showExitPrompt by remember { mutableStateOf(false) }

    val checkoutUrl = remember(checkoutResponse) {
        val raw = checkoutResponse?.checkout?.url ?: ""
        when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("//") -> "https:$raw"
            raw.isNotEmpty() -> "https://$raw"
            else -> ""
        }
    }

    val orderId = remember(checkoutResponse) {
        checkoutResponse?.payment?.orderId ?: ""
    }

    fun requestExit() {
        if (viewModel.step == PaymentStep.SUCCESS || viewModel.step == PaymentStep.FAILED || viewModel.step == PaymentStep.EXPIRED) {
            onNavigateBack()
        } else {
            showExitPrompt = true
        }
    }

    BackHandler(enabled = !isWebViewOpen) { requestExit() }

    Scaffold(
        containerColor = bgClr
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)).clickable { requestExit() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (bookingType.equals("Permanent", true)) "Secure permanent payment" else "Secure advance payment",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("Sportynix card gateway", fontSize = 12.sp, color = textSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(primaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (viewModel.step) {
                            PaymentStep.SUCCESS -> Icons.Default.CheckCircle
                            PaymentStep.FAILED, PaymentStep.EXPIRED -> Icons.Default.Error
                            else -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = when (viewModel.step) {
                            PaymentStep.SUCCESS -> primaryGreen
                            PaymentStep.FAILED, PaymentStep.EXPIRED -> Color.Red
                            else -> primaryGreen
                        },
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = when (viewModel.step) {
                        PaymentStep.SUCCESS -> "Payment confirmed"
                        PaymentStep.FAILED -> "Payment incomplete"
                        PaymentStep.EXPIRED -> "Reservation expired"
                        PaymentStep.VERIFYING -> "Verifying payment"
                        else -> "Complete your payment"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = viewModel.statusMessage,
                    fontSize = 14.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (isDark) Color(0xFF151C1B) else Color.White)
                        .border(1.dp, primaryGreen.copy(alpha = .28f), RoundedCornerShape(22.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sportynix Booking", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("${bookingType.replaceFirstChar { it.uppercase() }} booking", fontSize = 14.sp, color = textSecondary)
                    HorizontalDivider(color = textSecondary.copy(alpha = .16f))
                    PaymentInfoRow("Pay now", checkoutResponse?.payment?.amount?.let { "LKR ${"%.2f".format(it)}" } ?: "LKR —", primaryGreen, textSecondary)
                    PaymentInfoRow("Order", orderId.ifBlank { "Pending" }, textPrimary, textSecondary)
                    checkoutResponse?.reservationExpiresAt?.let { expires ->
                        PaymentInfoRow("Reserved until", expires, textPrimary, textSecondary)
                    }
                }

                if (viewModel.step == PaymentStep.READY || viewModel.step == PaymentStep.WAITING) {
                    Button(
                        onClick = {
                            if (checkoutUrl.isNotEmpty()) {
                                isWebViewOpen = true
                            } else {
                                viewModel.pollStatus(orderId) { list ->
                                    onNavigateToConfirmation(list, bookingType)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                    ) {
                        Icon(Icons.Default.CreditCard, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(if (viewModel.step == PaymentStep.READY) "Continue to card payment" else "Open card payment", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.pollStatus(orderId) { list ->
                                onNavigateToConfirmation(list, bookingType)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Check payment status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                } else if (viewModel.step == PaymentStep.VERIFYING) {
                    CircularProgressIndicator(color = primaryGreen)
                } else if (viewModel.step == PaymentStep.SUCCESS) {
                    Button(onClick = { onNavigateToConfirmation(viewModel.verifiedBookings, bookingType) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)) {
                        Icon(Icons.Default.QrCode2, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("View booking QR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)) {
                        Text("Select slots again", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Text("Sportynix never receives or stores your card number. Payment confirmation comes directly from the secure gateway.",
                    fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            // Hosted Checkout WebView Dialog
            if (isWebViewOpen && checkoutUrl.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = {
                        isWebViewOpen = false
                        viewModel.pollStatus(orderId) { list ->
                            onNavigateToConfirmation(list, bookingType)
                        }
                    },
                    confirmButton = {},
                    text = {
                        Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                if (url?.contains("/return/") == true || url?.contains("/status/") == true) {
                                                    isWebViewOpen = false
                                                    viewModel.pollStatus(orderId) { list ->
                                                        onNavigateToConfirmation(list, bookingType)
                                                    }
                                                }
                                            }
                                        }
                                        loadUrl(checkoutUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                )
            }
        }
    }

    if (showExitPrompt) {
        AlertDialog(
            onDismissRequest = { showExitPrompt = false },
            title = { Text("Exit payment?") },
            text = { Text("Your slot remains reserved until the pending-payment timer expires. Do you want to leave this payment screen?") },
            confirmButton = { TextButton(onClick = { showExitPrompt = false; onNavigateBack() }) { Text("Exit", color = Color(0xFFDC2626)) } },
            dismissButton = { TextButton(onClick = { showExitPrompt = false }) { Text("Stay") } }
        )
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String, valueColor: Color, labelColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = labelColor)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
    }
}

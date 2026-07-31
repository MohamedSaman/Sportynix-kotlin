package com.sportynix.app.presentation.booking

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingAdvancePaymentPreviewScreen(
    checkoutResponse: PaymentCheckoutResponseDto?,
    onNavigateBack: () -> Unit,
    onNavigateToConfirmation: (List<ConfirmedBookingDto>, String) -> Unit,
    viewModel: BookingPaymentViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val bgClr = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    var isWebViewOpen by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = bgClr
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(primaryGreen.copy(alpha = 0.12f)),
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
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when (viewModel.step) {
                        PaymentStep.SUCCESS -> "Payment Confirmed!"
                        PaymentStep.FAILED -> "Payment Failed"
                        PaymentStep.VERIFYING -> "Verifying Payment"
                        else -> "Secure Payment"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = viewModel.statusMessage,
                    fontSize = 14.sp,
                    color = textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                if (viewModel.step == PaymentStep.READY || viewModel.step == PaymentStep.WAITING) {
                    Button(
                        onClick = {
                            if (checkoutUrl.isNotEmpty()) {
                                isWebViewOpen = true
                            } else {
                                viewModel.pollStatus(orderId) { list ->
                                    onNavigateToConfirmation(list, "Normal")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                    ) {
                        Text("Open Hosted Payment Gateway", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.pollStatus(orderId) { list ->
                                onNavigateToConfirmation(list, "Normal")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Check Payment Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                } else if (viewModel.step == PaymentStep.VERIFYING) {
                    CircularProgressIndicator(color = primaryGreen)
                }
            }

            // Hosted Checkout WebView Dialog
            if (isWebViewOpen && checkoutUrl.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = {
                        isWebViewOpen = false
                        viewModel.pollStatus(orderId) { list ->
                            onNavigateToConfirmation(list, "Normal")
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
                                                        onNavigateToConfirmation(list, "Normal")
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
}

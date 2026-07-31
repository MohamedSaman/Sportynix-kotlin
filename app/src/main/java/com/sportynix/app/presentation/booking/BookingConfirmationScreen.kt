package com.sportynix.app.presentation.booking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto

@Composable
fun BookingConfirmationScreen(
    bookings: List<ConfirmedBookingDto>,
    bookingType: String = "Normal",
    onNavigateToHome: () -> Unit,
    onNavigateToBookingDetail: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Prevent back navigation into completed booking flow
    BackHandler {
        onNavigateToHome()
    }

    Scaffold(
        containerColor = if (isDark) Color(0xFF090B18) else Color(0xFFF8FAFC)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            BookingConfirmationStack(
                bookings = if (bookings.isNotEmpty()) bookings else listOf(
                    ConfirmedBookingDto(
                        id = "1",
                        qrCode = "SPX-DEMO-QR",
                        startTime = "18:00",
                        endTime = "19:00",
                        price = 500.0,
                        duration = 60,
                        bookingDate = "2026-07-31",
                        bookingReference = "REF12345",
                        paymentStatus = "Confirmed",
                        paymentAmount = 500.0,
                        paymentCurrency = "LKR",
                        receiptNumber = "REC-001",
                        receiptDownloadUrl = null
                    )
                ),
                bookingType = bookingType,
                onAssignTeam = {
                    val firstId = bookings.firstOrNull()?.id ?: "1"
                    onNavigateToBookingDetail(firstId)
                },
                onHome = onNavigateToHome
            )
        }
    }
}

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
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark

    // Prevent back navigation into completed booking flow
    BackHandler {
        onNavigateToHome()
    }

    Scaffold(
        containerColor = if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            BookingConfirmationStack(
                bookings = bookings,
                bookingType = bookingType,
                onAssignTeam = {
                    bookings.firstOrNull()?.id?.let(onNavigateToBookingDetail)
                },
                onHome = onNavigateToHome
            )
        }
    }
}

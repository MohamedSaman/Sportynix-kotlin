package com.sportynix.app.presentation.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.components.SportynixTopBar
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BookingSummaryScreen(
    venueId: String,
    slotId: String,
    date: String,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (bookingId: String, amount: Double) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state = viewModel.state

    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookingUiEffect.NavigateToPayment -> onNavigateToPayment(effect.bookingId, effect.amount)
            }
        }
    }

    Scaffold(
        topBar = {
            SportynixTopBar(
                title = "Booking Summary",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Booking Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryRow("Date", date)
                    SummaryRow("Time Slot", "07:00 AM - 08:00 AM")
                    SummaryRow("Court Fee", "$35.00")
                    SummaryRow("Service Fee", "$2.50")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row {
                        Text(
                            text = "Total Payable",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$37.50",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SportynixGreenPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Confirm & Proceed to Pay",
                onClick = { viewModel.confirmBooking(venueId, slotId, date) },
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

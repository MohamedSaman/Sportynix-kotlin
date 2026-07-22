package com.sportynix.app.presentation.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.components.SportynixTopBar
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun PaymentScreen(
    bookingId: String,
    amount: Double,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CREDIT_CARD") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SportynixTopBar(
                title = "Payment",
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
            Text(
                text = "Total Amount: $$amount",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SportynixGreenPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select Payment Method",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            PaymentMethodOption(
                title = "Credit / Debit Card",
                selected = selectedMethod == "CREDIT_CARD",
                onClick = { selectedMethod = "CREDIT_CARD" }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PaymentMethodOption(
                title = "Google Pay",
                selected = selectedMethod == "GOOGLE_PAY",
                onClick = { selectedMethod = "GOOGLE_PAY" }
            )
            Spacer(modifier = Modifier.height(8.dp))
            PaymentMethodOption(
                title = "Apple Pay / UPI",
                selected = selectedMethod == "UPI",
                onClick = { selectedMethod = "UPI" }
            )

            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Pay $$amount Now",
                onClick = {
                    isProcessing = true
                    onPaymentSuccess()
                },
                isLoading = isProcessing
            )
        }
    }
}

@Composable
private fun PaymentMethodOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable fun EmailVerificationScreen(email: String, onVerified: () -> Unit, onBack: () -> Unit, viewModel: EmailVerificationViewModel = hiltViewModel()) {
    val state = viewModel.state; var countdown by remember { mutableIntStateOf(60) }; var success by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.effects.collectLatest { when (it) { EmailVerificationEffect.Verified -> { success = "Email verified successfully"; delay(700); onVerified() }; EmailVerificationEffect.Resent -> { success = "A new code was sent to your email"; countdown = 60 } } } }
    LaunchedEffect(countdown) { if (countdown > 0) { delay(1000); countdown-- } }
    AuthBackground { Column(Modifier.fillMaxSize().imePadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } }; Spacer(Modifier.weight(.25f)); AuthHeader("Verify your email", "We've sent a six-digit code to\n$email", Icons.Default.MarkEmailRead)
        Spacer(Modifier.height(28.dp)); AuthCard(Modifier.fillMaxWidth()) { OtpInput(state.otp, viewModel::onOtpChanged, !state.loading); Spacer(Modifier.height(12.dp)); AuthMessage(state.error); AuthMessage(success, true); Spacer(Modifier.height(16.dp)); PremiumButton("Verify Email", { viewModel.verify(email) }, state.otp.length == 6, state.loading); Spacer(Modifier.height(10.dp)); if (countdown > 0) Text("Resend in ${countdown}s", Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant) else TextButton({ success = null; viewModel.resend(email) }, Modifier.align(Alignment.CenterHorizontally), enabled = !state.resending) { Text(if (state.resending) "Sending..." else "Resend", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.weight(1f))
    } }
}

package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable fun ForgotPasswordOtpScreen(email: String, onNavigateBack: () -> Unit, onNavigateToReset: (String, String) -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state = viewModel.state; var countdown by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) { viewModel.onEmailChanged(email); viewModel.effect.collectLatest { when (it) { is ForgotPasswordUiEffect.NavigateToReset -> onNavigateToReset(it.email, it.otpCode); is ForgotPasswordUiEffect.NavigateToOtp -> countdown = 60; else -> Unit } } }
    LaunchedEffect(countdown) { if (countdown > 0) { delay(1000); countdown-- } }
    AuthBackground { Column(Modifier.fillMaxSize().imePadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }; Spacer(Modifier.weight(.25f)); AuthHeader("Check your email", "Enter the six-digit code sent to\n$email", Icons.Default.Password)
        Spacer(Modifier.height(28.dp)); AuthCard(Modifier.fillMaxWidth()) { OtpInput(state.otpInput, viewModel::onOtpChanged, !state.isLoading); Spacer(Modifier.height(14.dp)); AuthMessage(state.errorMessage); Spacer(Modifier.height(16.dp)); PremiumButton("Verify Code", { viewModel.verifyResetOtp(email) }, state.otpInput.length == 6, state.isLoading); Spacer(Modifier.height(10.dp)); if (countdown > 0) Text("Resend in ${countdown}s", Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant) else TextButton(viewModel::sendResetLink, Modifier.align(Alignment.CenterHorizontally)) { Text("Resend code", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.weight(1f))
    } }
}

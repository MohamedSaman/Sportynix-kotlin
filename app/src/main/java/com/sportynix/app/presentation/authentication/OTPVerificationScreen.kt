package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable fun OTPVerificationScreen(sessionId: String, phoneNumber: String, email: String, onNavigateToHome: () -> Unit, onNavigateBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state = viewModel.state; var countdown by remember { mutableIntStateOf(60) }; var submitted by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.effect.collectLatest { when (it) { is AuthUiEffect.NavigateToHome -> onNavigateToHome(); is AuthUiEffect.ShowToast -> countdown = 60; else -> Unit } } }
    LaunchedEffect(countdown) { if (countdown > 0) { delay(1000); countdown-- } }
    LaunchedEffect(state.otpInput, state.isLoading) { if (state.otpInput.length == 6 && !state.isLoading && submitted != state.otpInput) { submitted = state.otpInput; viewModel.verifyOtp(state.sessionId ?: sessionId, state.otpInput) } }
    LaunchedEffect(state.errorMessage) { if (state.errorMessage != null) { viewModel.onOtpChanged(""); submitted = null } }
    AuthBackground { Column(Modifier.fillMaxSize().imePadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }; Spacer(Modifier.weight(.2f)); AuthHeader("Verify your account", "We sent a six-digit code to\n$phoneNumber and $email", Icons.Default.VerifiedUser)
        Spacer(Modifier.height(28.dp)); AuthCard(Modifier.fillMaxWidth()) { OtpInput(state.otpInput, viewModel::onOtpChanged, !state.isLoading); Spacer(Modifier.height(14.dp)); AuthMessage(state.errorMessage); Spacer(Modifier.height(16.dp)); PremiumButton("Verify OTP & Continue", { submitted = state.otpInput; viewModel.verifyOtp(state.sessionId ?: sessionId) }, state.otpInput.length == 6, state.isLoading); Spacer(Modifier.height(10.dp)); if (countdown > 0) Text("Resend OTP in ${countdown}s", Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant) else TextButton({ viewModel.resendOtp(state.sessionId ?: sessionId) }, Modifier.align(Alignment.CenterHorizontally)) { Text("Resend OTP", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.weight(1f))
    } }
}

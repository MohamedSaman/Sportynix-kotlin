package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OTPVerificationScreen(
    sessionId: String,
    phoneNumber: String,
    email: String,
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val dark = LocalThemeController.current.isDark
    var countdown by remember { mutableIntStateOf(60) }
    var submitted by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest {
            when (it) {
                is AuthUiEffect.NavigateToHome -> onNavigateToHome()
                is AuthUiEffect.ShowToast -> countdown = 60
                else -> Unit
            }
        }
    }

    // 60-second countdown timer for Resend OTP
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    // Auto-verify when 6 digits entered
    LaunchedEffect(state.otpInput, state.isLoading) {
        if (state.otpInput.length == 6 && !state.isLoading && submitted != state.otpInput) {
            submitted = state.otpInput
            viewModel.verifyOtp(state.sessionId ?: sessionId, state.otpInput)
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            viewModel.onOtpChanged("")
            submitted = null
        }
    }

    AuthBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (dark) Color.White else Color.Black
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AuthHeader(
                title = "Verify your account",
                subtitle = "We sent a 6-digit code to\n${phoneNumber.ifBlank { email }}",
                icon = Icons.Default.VerifiedUser
            )

            Spacer(Modifier.height(28.dp))

            AuthCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "ENTER 6-DIGIT CODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OtpInput(
                    value = state.otpInput,
                    onValueChange = viewModel::onOtpChanged,
                    enabled = !state.isLoading
                )

                Spacer(Modifier.height(14.dp))

                AuthMessage(state.errorMessage)

                Spacer(Modifier.height(16.dp))

                PremiumButton(
                    text = "Verify OTP & Continue",
                    onClick = {
                        submitted = state.otpInput
                        viewModel.verifyOtp(state.sessionId ?: sessionId)
                    },
                    enabled = state.otpInput.length == 6,
                    loading = state.isLoading
                )

                Spacer(Modifier.height(16.dp))

                if (countdown > 0) {
                    Text(
                        text = "Resend code in ${countdown}s",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    TextButton(
                        onClick = {
                            countdown = 60
                            viewModel.resendOtp(state.sessionId ?: sessionId)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Resend OTP",
                            color = SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

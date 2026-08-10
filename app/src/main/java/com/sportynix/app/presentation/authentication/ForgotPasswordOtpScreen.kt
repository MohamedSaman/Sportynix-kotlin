package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordOtpScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToReset: (String, String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val dark = LocalThemeController.current.isDark
    var countdown by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        viewModel.onEmailChanged(email)
        viewModel.effect.collectLatest {
            when (it) {
                is ForgotPasswordUiEffect.NavigateToReset -> onNavigateToReset(it.email, it.otpCode)
                is ForgotPasswordUiEffect.NavigateToOtp -> countdown = 60
                else -> Unit
            }
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AuthBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Back Button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularBackButton(onClick = onNavigateBack)
            }

            Spacer(Modifier.height(10.dp))

            // Concentric Glowing Icon Container
            ConcentricGlowIcon(icon = Icons.Default.Dialpad)

            Spacer(Modifier.height(10.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enter Verification Code",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = if (dark) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Enter the 6-digit code sent to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SportynixGreenPrimary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // Notice Banner for Google Users
            if (state.isSocialUser || state.canSetPassword) {
                NoticeBannerCard(
                    text = "This account was created via Google. After verifying, you can set a local password."
                )
                Spacer(Modifier.height(20.dp))
            }

            // OTP Input Box
            OtpInput(
                value = state.otpInput,
                onValueChange = viewModel::onOtpChanged,
                enabled = !state.isLoading
            )

            Spacer(Modifier.height(16.dp))

            AuthMessage(state.errorMessage)

            Spacer(Modifier.height(16.dp))

            PremiumButton(
                text = "Verify Code",
                onClick = { viewModel.verifyResetOtp(email) },
                enabled = state.otpInput.length == 6,
                loading = state.isLoading
            )

            Spacer(Modifier.height(20.dp))

            // Resend Countdown Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Didn't receive the code? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (countdown > 0) {
                    Text(
                        text = "Resend in ${countdown}s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    TextButton(
                        onClick = {
                            countdown = 60
                            viewModel.sendResetLink()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Resend",
                            color = SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Helper Info Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Check your spam folder if you don't see the email",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

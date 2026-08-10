package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
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
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val dark = LocalThemeController.current.isDark
    var countdown by remember { mutableIntStateOf(60) }
    var success by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest {
            when (it) {
                EmailVerificationEffect.Verified -> {
                    success = "Email verified successfully!"
                    delay(700)
                    onVerified()
                }
                EmailVerificationEffect.Resent -> {
                    success = "A new code was sent to your email"
                    countdown = 60
                }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (dark) Color.White else Color.Black
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AuthHeader(
                title = "Verify your email",
                subtitle = "We've sent a 6-digit code to\n$email",
                icon = Icons.Default.MarkEmailRead
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
                    value = state.otp,
                    onValueChange = viewModel::onOtpChanged,
                    enabled = !state.loading
                )

                Spacer(Modifier.height(14.dp))

                AuthMessage(state.error)
                AuthMessage(success, success = true)

                Spacer(Modifier.height(16.dp))

                PremiumButton(
                    text = "Verify Email",
                    onClick = { viewModel.verify(email) },
                    enabled = state.otp.length == 6,
                    loading = state.loading
                )

                Spacer(Modifier.height(16.dp))

                if (countdown > 0) {
                    Text(
                        text = "Resend in ${countdown}s",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    TextButton(
                        onClick = {
                            success = null
                            viewModel.resend(email)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = !state.resending
                    ) {
                        Text(
                            text = if (state.resending) "Sending..." else "Resend Code",
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

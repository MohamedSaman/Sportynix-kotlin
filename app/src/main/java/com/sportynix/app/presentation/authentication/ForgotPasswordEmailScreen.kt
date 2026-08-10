package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordEmailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResetOtp: (String, String, Boolean, Boolean) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val dark = LocalThemeController.current.isDark

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest {
            if (it is ForgotPasswordUiEffect.NavigateToOtp) {
                onNavigateToResetOtp(it.sessionId, it.email, it.isSocialUser, it.canSetPassword)
            }
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
            // Top Left Circular Back Button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularBackButton(onClick = onNavigateBack)
            }

            Spacer(Modifier.height(10.dp))

            // Concentric Glowing Icon Container
            ConcentricGlowIcon(icon = Icons.Default.LockOpen)

            Spacer(Modifier.height(10.dp))

            AuthHeader(
                title = "Reset Password",
                subtitle = "Enter your email address to receive a verification code"
            )

            Spacer(Modifier.height(28.dp))

            // Form
            Column(Modifier.fillMaxWidth()) {
                PremiumAuthField(
                    value = state.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = "Enter your email",
                    icon = Icons.Default.Mail,
                    keyboardType = KeyboardType.Email
                )

                Spacer(Modifier.height(14.dp))

                AuthMessage(state.errorMessage)
                AuthMessage(state.successMessage, success = true)

                Spacer(Modifier.height(16.dp))

                PremiumButton(
                    text = "Send Reset Code",
                    onClick = viewModel::sendResetLink,
                    enabled = state.emailInput.isNotBlank(),
                    loading = state.isLoading
                )

                Spacer(Modifier.height(16.dp))

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
                        text = "We'll send a 6-digit code to your email to reset your password",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Tip Card
                TipCard(
                    title = "Tip",
                    body = "Use the same email you used to sign up"
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.errorMessage?.contains("social", true) == true || state.errorMessage?.contains("Google sign-in", true) == true) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("Social Login Account") },
            text = { Text(state.errorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("Use Google Sign-In")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text("OK")
                }
            }
        )
    }
}

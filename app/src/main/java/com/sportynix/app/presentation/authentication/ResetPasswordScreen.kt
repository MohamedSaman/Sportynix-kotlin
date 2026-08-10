package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.LocalThemeController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ResetPasswordScreen(
    email: String,
    otpCode: String,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    isSocialUser: Boolean = false,
    canSetPassword: Boolean = false,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val dark = LocalThemeController.current.isDark
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEmailChanged(email)
        viewModel.onOtpChanged(otpCode)
        viewModel.effect.collectLatest {
            if (it is ForgotPasswordUiEffect.NavigateToLogin) {
                onNavigateToLogin()
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
            // Circular Back Button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularBackButton(onClick = onNavigateBack)
            }

            Spacer(Modifier.height(10.dp))

            // Glowing Concentric Key Icon
            ConcentricGlowIcon(icon = Icons.Default.Key)

            Spacer(Modifier.height(10.dp))

            AuthHeader(
                title = "Create New Password",
                subtitle = "Enter your new password below"
            )

            Spacer(Modifier.height(20.dp))

            // Notice Banner
            if (isSocialUser || canSetPassword || state.isSocialUser || state.canSetPassword) {
                NoticeBannerCard(
                    text = "Setting a password will allow signing in with email and password."
                )
                Spacer(Modifier.height(18.dp))
            }

            // Input 1: New Password
            PremiumAuthField(
                value = state.newPasswordInput,
                onValueChange = viewModel::onNewPasswordChanged,
                placeholder = "New Password",
                icon = Icons.Default.Lock,
                visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showNew = !showNew }) {
                        Icon(
                            imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )

            Spacer(Modifier.height(14.dp))

            // Input 2: Confirm New Password
            PremiumAuthField(
                value = state.confirmPasswordInput,
                onValueChange = viewModel::onConfirmPasswordChanged,
                placeholder = "Confirm New Password",
                icon = Icons.Default.Shield,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )

            Spacer(Modifier.height(18.dp))

            // Password Requirement Card
            PasswordRequirementCard(
                password = state.newPasswordInput,
                confirmPassword = state.confirmPasswordInput
            )

            Spacer(Modifier.height(14.dp))

            AuthMessage(state.errorMessage)

            Spacer(Modifier.height(18.dp))

            val canSubmit = state.newPasswordInput.length >= 8 &&
                    state.newPasswordInput.any(Char::isUpperCase) &&
                    state.newPasswordInput.any(Char::isLowerCase) &&
                    state.newPasswordInput.any(Char::isDigit) &&
                    state.newPasswordInput == state.confirmPasswordInput

            PremiumButton(
                text = "Reset Password",
                onClick = viewModel::submitResetPassword,
                enabled = canSubmit,
                loading = state.isLoading
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

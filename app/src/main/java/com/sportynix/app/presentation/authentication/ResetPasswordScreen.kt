package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable fun ResetPasswordScreen(email: String, otpCode: String, onNavigateToLogin: () -> Unit, onNavigateBack: () -> Unit, isSocialUser: Boolean = false, canSetPassword: Boolean = false, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state = viewModel.state; var showNew by remember { mutableStateOf(false) }; var showConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.onEmailChanged(email); viewModel.onOtpChanged(otpCode); viewModel.effect.collectLatest { if (it is ForgotPasswordUiEffect.NavigateToLogin) onNavigateToLogin() } }
    AuthBackground { Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }; AuthHeader("Create new password", "Choose a strong password you haven't used before.", Icons.Default.LockReset)
        Spacer(Modifier.height(22.dp)); AuthCard(Modifier.fillMaxWidth()) {
            if (isSocialUser || canSetPassword) { Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) { Text("Setting a password lets you sign in using email and password as well as Google.", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(14.dp)) }
            AuthTextField(state.newPasswordInput, viewModel::onNewPasswordChanged, "New password", Icons.Default.Lock, visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(), trailing = { IconButton({ showNew = !showNew }) { Icon(Icons.Default.Visibility, "Toggle") } }); Spacer(Modifier.height(8.dp)); PasswordChecklist(state.newPasswordInput); Spacer(Modifier.height(14.dp))
            AuthTextField(state.confirmPasswordInput, viewModel::onConfirmPasswordChanged, "Confirm password", Icons.Default.Lock, visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(), trailing = { IconButton({ showConfirm = !showConfirm }) { Icon(Icons.Default.Visibility, "Toggle") } }); Spacer(Modifier.height(12.dp)); AuthMessage(state.errorMessage); Spacer(Modifier.height(16.dp)); PremiumButton("Reset Password", viewModel::submitResetPassword, loading = state.isLoading)
        }
    } }
}

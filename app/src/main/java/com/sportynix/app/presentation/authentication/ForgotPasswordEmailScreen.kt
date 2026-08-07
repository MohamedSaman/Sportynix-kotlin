package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable fun ForgotPasswordEmailScreen(onNavigateBack: () -> Unit, onNavigateToResetOtp: (String, String, Boolean, Boolean) -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state = viewModel.state
    LaunchedEffect(Unit) { viewModel.effect.collectLatest { if (it is ForgotPasswordUiEffect.NavigateToOtp) onNavigateToResetOtp(it.sessionId, it.email, it.isSocialUser, it.canSetPassword) } }
    AuthBackground { Column(Modifier.fillMaxSize().imePadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { IconButton(onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
        Spacer(Modifier.weight(.35f)); AuthHeader("Forgot password?", "Enter your registered email and we'll send a secure six-digit reset code.", Icons.Default.MarkEmailUnread)
        Spacer(Modifier.height(26.dp)); AuthCard(Modifier.fillMaxWidth()) { AuthTextField(state.emailInput, viewModel::onEmailChanged, "Email address", Icons.Default.MarkEmailUnread, keyboardType = KeyboardType.Email); Spacer(Modifier.height(12.dp)); AuthMessage(state.errorMessage); AuthMessage(state.successMessage, true); Spacer(Modifier.height(16.dp)); PremiumButton("Send Reset Code", viewModel::sendResetLink, loading = state.isLoading) }
        Spacer(Modifier.weight(1f))
    } }
    if (state.errorMessage?.contains("social", true) == true || state.errorMessage?.contains("Google sign-in", true) == true) AlertDialog(onDismissRequest = viewModel::clearMessage, title = { Text("Social Login Account") }, text = { Text(state.errorMessage.orEmpty()) }, confirmButton = { TextButton(onNavigateBack) { Text("Use Google Sign-In") } }, dismissButton = { TextButton(viewModel::clearMessage) { Text("OK") } })
}

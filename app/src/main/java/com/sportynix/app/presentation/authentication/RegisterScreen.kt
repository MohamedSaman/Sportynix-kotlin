package com.sportynix.app.presentation.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToOtp: (sessionId: String, phone: String, email: String) -> Unit = { _, _, _ -> },
    viewModel: AuthViewModel = hiltViewModel()
) {
    SignUpScreen(
        onNavigateToHome = onNavigateToHome,
        onNavigateToSignIn = onNavigateToSignIn,
        onNavigateToOtp = onNavigateToOtp,
        viewModel = viewModel
    )
}

package com.sportynix.app.presentation.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    SignInScreen(
        onNavigateToHome = onNavigateToHome,
        onNavigateToSignUp = onNavigateToRegister,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        viewModel = viewModel
    )
}

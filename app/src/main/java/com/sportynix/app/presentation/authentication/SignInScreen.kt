package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.R
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignInScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val dark = LocalThemeController.current.isDark
    var showPassword by remember { mutableStateOf(false) }
    var googleDob by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { if (it is AuthUiEffect.NavigateToHome) onNavigateToHome() }
    }

    AuthBackground {
        Column(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // Logo with concentric glow rings
            ConcentricGlowIcon(logoRes = if (dark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light)

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome to ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = if (dark) Color.White else Color(0xFF101B2C)
                )
                Text(
                    text = "Sportynix",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = SportynixGreenPrimary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Sign in to continue your journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 15.sp
            )

            Spacer(Modifier.height(28.dp))

            AuthCard(Modifier.fillMaxWidth()) {

            // Username or Email Field
            PremiumAuthField(
                value = state.emailInput,
                onValueChange = viewModel::onEmailChanged,
                placeholder = "Username or Email",
                icon = Icons.Default.Email
            )

            Spacer(Modifier.height(16.dp))

            // Password Field
            PremiumAuthField(
                value = state.passwordInput,
                onValueChange = viewModel::onPasswordChanged,
                placeholder = "Password",
                icon = Icons.Default.Lock,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            // Forgot Password Link
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = SportynixGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Error Message banner
            AuthMessage(state.errorMessage)

            Spacer(Modifier.height(14.dp))

            // Sign In Button
            val canLogin = state.emailInput.isNotBlank() && state.passwordInput.isNotBlank()
            PremiumButton(
                text = "Sign In",
                onClick = viewModel::login,
                enabled = canLogin,
                loading = state.isLoading
            )

            // Divider
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "or",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            // Google Sign In (Only Google, no Apple)
            GoogleAuthButton(
                onClick = { viewModel.googleSignIn(context) },
                loading = state.isSocialLoading
            )
            }

            Spacer(Modifier.height(24.dp))

            // Footer navigation link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToSignUp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Sign up",
                        color = SportynixGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.showGoogleDobDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        icon = { Icon(Icons.Default.CalendarMonth, null, tint = SportynixGreenPrimary) },
        title = { Text("Date of birth required") },
        text = {
            Column {
                Text("Choose your date of birth to finish Google Sign-In. You must be at least 13 years old.")
                Spacer(Modifier.height(12.dp))
                PremiumAuthField(value = googleDob, onValueChange = { googleDob = it }, placeholder = "YYYY-MM-DD", icon = Icons.Default.CalendarMonth)
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.completeGoogleDob(googleDob) }) { Text("Continue", color = SportynixGreenPrimary) } },
        dismissButton = { TextButton(onClick = viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
    if (state.showGoogleTermsDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Terms & Conditions") },
        text = { Text("Accept the Terms & Conditions to finish setting up your Google account.") },
        confirmButton = { TextButton(onClick = viewModel::acceptGoogleTerms) { Text("Accept & continue", color = SportynixGreenPrimary) } },
        dismissButton = { TextButton(onClick = viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
}

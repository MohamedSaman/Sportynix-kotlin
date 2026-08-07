package com.sportynix.app.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.R
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import android.widget.Toast
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
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo
            Image(
                painter = painterResource(if (dark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light),
                contentDescription = "Sportynix Logo",
                modifier = Modifier.size(68.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = if (dark) Color.White else Color.Black
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Sign in to continue your sports journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(28.dp))

            // Card
            AuthCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "EMAIL / USERNAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                PremiumAuthField(
                    value = state.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = "Username or email",
                    icon = Icons.Default.Person
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "PASSWORD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                PremiumAuthField(
                    value = state.passwordInput,
                    onValueChange = viewModel::onPasswordChanged,
                    placeholder = "Password",
                    icon = Icons.Default.Lock,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton({ showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Forgot Password Link
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Forgot password?",
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
            }

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
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Social Buttons
            GoogleAuthButton(
                onClick = { viewModel.googleSignIn(context) },
                loading = state.isSocialLoading
            )

            Spacer(Modifier.height(10.dp))

            AppleAuthButton(
                onClick = {
                    Toast.makeText(context, "Apple Sign-In is not supported on Android. Please use Google Sign-In or Email.", Toast.LENGTH_LONG).show()
                },
                loading = false
            )

            Spacer(Modifier.height(24.dp))

            // Footer navigation link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New to Sportynix? ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToSignUp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Create account",
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
        icon = { Icon(Icons.Default.Cake, null) },
        title = { Text("Date of birth required") },
        text = {
            Column {
                Text("Choose your date of birth to finish Google Sign-In. You must be at least 13 years old.")
                Spacer(Modifier.height(12.dp))
                AuthTextField(googleDob, { googleDob = it }, "YYYY-MM-DD", Icons.Default.CalendarMonth, error = state.errorMessage)
            }
        },
        confirmButton = { TextButton({ viewModel.completeGoogleDob(googleDob) }) { Text("Continue") } },
        dismissButton = { TextButton(viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
    if (state.showGoogleTermsDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Terms & Conditions") },
        text = { Text("Accept the Terms & Conditions to finish setting up your Google account.") },
        confirmButton = { TextButton(viewModel::acceptGoogleTerms) { Text("Accept & continue") } },
        dismissButton = { TextButton(viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
}

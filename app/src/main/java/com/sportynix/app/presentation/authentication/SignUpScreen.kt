package com.sportynix.app.presentation.authentication

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.R
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToOtp: (String, String, String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val dark = LocalThemeController.current.isDark
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var googleDob by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest {
            when (it) {
                is AuthUiEffect.NavigateToHome -> onNavigateToHome()
                is AuthUiEffect.NavigateToOtp -> onNavigateToOtp(it.sessionId, it.phone, it.email)
                else -> Unit
            }
        }
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // Date Picker Config
    val calendar = Calendar.getInstance()
    calendar.set(2000, 0, 1)
    val maxDateCalendar = Calendar.getInstance()
    maxDateCalendar.add(Calendar.YEAR, -13)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.onDobChanged(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = maxDateCalendar.timeInMillis
    }

    AuthBackground {
        Column(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
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

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = if (dark) Color.White else Color.Black
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Join Sportynix and start booking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 15.sp
            )

            Spacer(Modifier.height(20.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StepNumberBadge(number = "1", label = "Basics", isActive = state.currentStep >= 1)

                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    thickness = 2.dp,
                    color = if (state.currentStep > 1) SportynixGreenPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                StepNumberBadge(number = "2", label = "Setup", isActive = state.currentStep == 2)
            }

            Spacer(Modifier.height(24.dp))

            AuthCard(Modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "registrationStep"
            ) { currentStep ->
            if (currentStep == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // First Name & Last Name Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PremiumAuthField(
                            value = state.firstNameInput,
                            onValueChange = viewModel::onFirstNameChanged,
                            placeholder = "First Name",
                            icon = Icons.Default.Person
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        PremiumAuthField(
                            value = state.lastNameInput,
                            onValueChange = viewModel::onLastNameChanged,
                            placeholder = "Last Name",
                            icon = Icons.Default.Person
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Email
                PremiumAuthField(
                    value = state.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = "Email",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                Spacer(Modifier.height(14.dp))

                // Phone
                PremiumAuthField(
                    value = state.phoneInput,
                    onValueChange = viewModel::onPhoneChanged,
                    placeholder = "Phone Number",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )

                Spacer(Modifier.height(14.dp))

                // Date of Birth Dropdown
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { datePickerDialog.show() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (dark) Color(0xFF181A1E) else Color(0xFFF3F7F5),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        SportynixGreenPrimary.copy(alpha = 0.22f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 6.dp)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SportynixGreenPrimary.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, null, tint = SportynixGreenPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = state.dobInput.ifBlank { "Date of Birth" },
                            color = if (state.dobInput.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else (if (dark) Color.White else Color.Black),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                AuthMessage(state.errorMessage)
                Spacer(Modifier.height(10.dp))

                val canProceed = state.firstNameInput.isNotBlank() &&
                        state.lastNameInput.isNotBlank() &&
                        state.emailInput.isNotBlank() &&
                        state.phoneInput.isNotBlank() &&
                        state.dobInput.isNotBlank()

                PremiumButton(
                    text = "Next",
                    onClick = viewModel::continueSignup,
                    enabled = canProceed
                )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Step 2 Setup
                Text("USERNAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                PremiumAuthField(state.usernameInput, viewModel::onUsernameChanged, "Username", Icons.Default.AlternateEmail)
                if (state.isCheckingUsername) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                } else if (state.usernameAvailable == true) {
                    Text("Username is available", color = SportynixGreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                } else if (state.usernameAvailable == false) {
                    Text("Username is taken or invalid", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                Spacer(Modifier.height(14.dp))

                Text("PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(12.dp))
                PasswordRequirementCard(password = state.passwordInput, confirmPassword = state.confirmPasswordInput)
                Spacer(Modifier.height(14.dp))

                Text("CONFIRM PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                PremiumAuthField(
                    value = state.confirmPasswordInput,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    placeholder = "Confirm Password",
                    icon = Icons.Default.Lock,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle confirm password visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))

                Text("REFERRAL CODE (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                PremiumAuthField(state.referralCodeInput, { viewModel.onReferralChanged(it.uppercase()) }, "Referral Code", Icons.Default.Redeem)
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.agreeToTerms,
                        onCheckedChange = viewModel::onTermsToggled,
                        colors = CheckboxDefaults.colors(checkedColor = SportynixGreenPrimary)
                    )
                    Column {
                        Text("I agree to the ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            TextButton(
                                onClick = { openUrl("https://sportynix.com/terms") },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Terms", color = SportynixGreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Text(" and ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(
                                onClick = { openUrl("https://sportynix.com/privacy") },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Privacy Policy", color = SportynixGreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                AuthMessage(state.errorMessage)
                Spacer(Modifier.height(12.dp))

                val canSubmit = state.usernameInput.isNotBlank() &&
                        state.passwordInput.length >= 8 &&
                        state.passwordInput == state.confirmPasswordInput &&
                        state.agreeToTerms

                PremiumButton(
                    text = "Create Account",
                    onClick = viewModel::signUp,
                    enabled = canSubmit,
                    loading = state.isLoading
                )
                }
            }
            }

            // Divider
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    Modifier.weight(1f).padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "or",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    Modifier.weight(1f).padding(horizontal = 14.dp),
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

            // Footer link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                TextButton(
                    onClick = onNavigateToSignIn,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Sign in", color = SportynixGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (state.showGoogleDobDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Date of birth required") },
        text = { PremiumAuthField(value = googleDob, onValueChange = { googleDob = it }, placeholder = "YYYY-MM-DD", icon = Icons.Default.CalendarMonth) },
        confirmButton = { TextButton(onClick = { viewModel.completeGoogleDob(googleDob) }) { Text("Continue", color = SportynixGreenPrimary) } },
        dismissButton = { TextButton(onClick = viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
    if (state.showGoogleTermsDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Terms & Conditions") },
        text = { Text("Accept the Terms & Conditions to complete Google registration.") },
        confirmButton = { TextButton(onClick = viewModel::acceptGoogleTerms) { Text("Accept", color = SportynixGreenPrimary) } },
        dismissButton = { TextButton(onClick = viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
}

package com.sportynix.app.presentation.authentication

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    calendar.set(2000, 0, 1) // default to 2000-01-01
    val maxDateCalendar = Calendar.getInstance()
    maxDateCalendar.add(Calendar.YEAR, -13) // user must be at least 13 years old

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
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Header Section with Back Arrow
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (state.currentStep == 1) {
                            onNavigateToSignIn()
                        } else {
                            viewModel.setStep(1)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (dark) Color.White else Color.Black
                    )
                }

                Spacer(Modifier.weight(1f))

                // Step indicators matching iOS step badge
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepBadge(title = "Info", isActive = state.currentStep == 1, isDone = state.currentStep > 1)
                    StepBadge(title = "Auth", isActive = state.currentStep == 2, isDone = false)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Logo
            Image(
                painter = painterResource(if (dark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light),
                contentDescription = "Sportynix Logo",
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.height(12.dp))

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
                text = if (state.currentStep == 1) "Step 1: Your Information" else "Step 2: Authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(24.dp))

            // Card Form
            AuthCard(Modifier.fillMaxWidth()) {
                if (state.currentStep == 1) {
                    // First Name
                    Text("FIRST NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.firstNameInput, viewModel::onFirstNameChanged, "First Name", Icons.Default.Person)
                    Spacer(Modifier.height(14.dp))

                    // Last Name
                    Text("LAST NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.lastNameInput, viewModel::onLastNameChanged, "Last Name", Icons.Default.Person)
                    Spacer(Modifier.height(14.dp))

                    // Email
                    Text("EMAIL ADDRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.emailInput, viewModel::onEmailChanged, "your@email.com", Icons.Default.Email, keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(14.dp))

                    // Phone
                    Text("MOBILE NUMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.phoneInput, viewModel::onPhoneChanged, "+15551234567", Icons.Default.Phone, keyboardType = KeyboardType.Phone)
                    Spacer(Modifier.height(14.dp))

                    // DOB
                    Text("DATE OF BIRTH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, SportynixGreenPrimary.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, tint = SportynixGreenPrimary)
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = state.dobInput.ifBlank { "Select Date of Birth" },
                                    color = if (state.dobInput.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                } else {
                    // Username
                    Text("USERNAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.usernameInput, viewModel::onUsernameChanged, "your_username", Icons.Default.AlternateEmail)
                    if (state.isCheckingUsername) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                    } else if (state.usernameAvailable == true) {
                        Text("Username is available", color = SportynixGreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    } else if (state.usernameAvailable == false) {
                        Text("Username is taken or invalid", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(14.dp))

                    // Password
                    Text("PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(
                        value = state.passwordInput,
                        onValueChange = viewModel::onPasswordChanged,
                        placeholder = "Min 8 characters",
                        icon = Icons.Default.Lock,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton({ showPassword = !showPassword }) {
                                Icon(Icons.Default.Visibility, "Toggle password")
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    PasswordChecklist(state.passwordInput)
                    Spacer(Modifier.height(14.dp))

                    // Confirm Password
                    Text("CONFIRM PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(
                        value = state.confirmPasswordInput,
                        onValueChange = viewModel::onConfirmPasswordChanged,
                        placeholder = "Repeat password",
                        icon = Icons.Default.Lock,
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton({ showConfirmPassword = !showConfirmPassword }) {
                                Icon(Icons.Default.Visibility, "Toggle confirm password")
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))

                    // Referral
                    Text("REFERRAL CODE (OPTIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PremiumAuthField(state.referralCodeInput, { viewModel.onReferralChanged(it.uppercase()) }, "Referral Code", Icons.Default.Redeem)
                    Spacer(Modifier.height(14.dp))

                    // Terms Checklist
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

            // Divider
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f).padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("OR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f).padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Google sign-in option
            GoogleAuthButton(
                onClick = { viewModel.googleSignIn(context) },
                loading = state.isSocialLoading
            )

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
                    Text("Sign In", color = SportynixGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (state.showGoogleDobDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Date of birth required") },
        text = { AuthTextField(googleDob, { googleDob = it }, "YYYY-MM-DD", Icons.Default.CalendarMonth, error = state.errorMessage) },
        confirmButton = { TextButton({ viewModel.completeGoogleDob(googleDob) }) { Text("Continue") } },
        dismissButton = { TextButton(viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
    if (state.showGoogleTermsDialog) AlertDialog(
        onDismissRequest = viewModel::dismissGoogleCompletion,
        title = { Text("Terms & Conditions") },
        text = { Text("Accept the Terms & Conditions to complete Google registration.") },
        confirmButton = { TextButton(viewModel::acceptGoogleTerms) { Text("Accept") } },
        dismissButton = { TextButton(viewModel::dismissGoogleCompletion) { Text("Cancel") } }
    )
}

@Composable
fun StepBadge(title: String, isActive: Boolean, isDone: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isActive) SportynixGreenPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                1.dp,
                if (isActive) SportynixGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = if (isDone || isActive) SportynixGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


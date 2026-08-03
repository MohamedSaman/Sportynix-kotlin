package com.sportynix.app.presentation.authentication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.components.SportynixTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordOtpScreen(
    email: String,
    onNavigateBack: () -> Unit,
    onNavigateToReset: (email: String, otp: String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var countdown by remember { mutableIntStateOf(60) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onEmailChanged(email)
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ForgotPasswordUiEffect.NavigateToReset -> onNavigateToReset(effect.email, effect.otpCode)
                is ForgotPasswordUiEffect.NavigateToOtp -> {
                    countdown = 60
                    Toast.makeText(context, "Verification code sent", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(countdown) {
        if (countdown > 0) { delay(1000); countdown-- }
    }

    Scaffold(topBar = { SportynixTopBar("Enter Verification Code", onNavigateBack) }) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Enter Verification Code", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Enter the 6-digit code sent to\n$email", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            BasicTextField(
                value = state.otpInput,
                onValueChange = { viewModel.onOtpChanged(it.filter(Char::isDigit).take(6)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { index ->
                            val value = state.otpInput.getOrNull(index)?.toString().orEmpty()
                            val active = index == state.otpInput.length
                            Box(
                                Modifier.weight(1f).height(56.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .border(if (active) 2.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            )
            state.errorMessage?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Verify Code", { viewModel.verifyResetOtp(email) }, Modifier.fillMaxWidth(), state.otpInput.length == 6, state.isLoading)
            Spacer(Modifier.height(16.dp))
            if (countdown > 0) Text("Resend code in ${countdown}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else TextButton(onClick = { viewModel.sendResetLink() }, enabled = !state.isLoading) { Text("Resend Verification Code") }
        }
    }
}

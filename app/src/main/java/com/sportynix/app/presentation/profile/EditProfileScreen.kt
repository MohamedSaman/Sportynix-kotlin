package com.sportynix.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val accentGreen = if (isDark) NeonGreen else SportynixGreenPrimary
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditProfileEffect.NavigateBack -> onNavigateBack()
                is EditProfileEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0D1B2A) else Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Update your info", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", color = accentGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.05f)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(accentGreen.copy(0.15f))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!state.user?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = state.user?.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null,
                            tint = accentGreen, modifier = Modifier.size(48.dp))
                    }
                    if (state.isUploadingAvatar) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = accentGreen,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentGreen)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change photo",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to change photo", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            // Form Section
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Personal Information", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = accentGreen)

                    ProfileTextField(
                        value = state.firstName,
                        onValueChange = { viewModel.onFirstNameChanged(it) },
                        label = "First Name",
                        leadingIcon = Icons.Default.Person,
                        accentGreen = accentGreen,
                        isDark = isDark
                    )
                    ProfileTextField(
                        value = state.lastName,
                        onValueChange = { viewModel.onLastNameChanged(it) },
                        label = "Last Name",
                        leadingIcon = Icons.Default.Person,
                        accentGreen = accentGreen,
                        isDark = isDark
                    )
                    ProfileTextField(
                        value = state.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = "Username",
                        leadingIcon = Icons.Default.AlternateEmail,
                        accentGreen = accentGreen,
                        isDark = isDark
                    )
                    ProfileTextField(
                        value = state.bio,
                        onValueChange = { viewModel.onBioChanged(it) },
                        label = "Bio",
                        leadingIcon = Icons.Default.Info,
                        singleLine = false,
                        minLines = 2,
                        accentGreen = accentGreen,
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Contact & Location", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = accentGreen)

                    // Phone field + verify button
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileTextField(
                            value = state.phone,
                            onValueChange = { viewModel.onPhoneChanged(it) },
                            label = "Phone Number",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            accentGreen = accentGreen,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentGreen.copy(0.15f))
                                .clickable { viewModel.requestPhoneVerification() }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isVerifyingPhone) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                    color = accentGreen, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Verified, contentDescription = "Verify",
                                    tint = accentGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    ProfileTextField(
                        value = state.location,
                        onValueChange = { viewModel.onLocationChanged(it) },
                        label = "Location / City",
                        leadingIcon = Icons.Default.LocationOn,
                        accentGreen = accentGreen,
                        isDark = isDark
                    )
                    Text(
                        text = "Email: ${state.user?.email ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save Changes",
                onClick = { viewModel.saveProfile() },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Phone OTP Verification Bottom Sheet
    if (state.showPhoneVerifySheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissPhoneVerifySheet() },
            containerColor = if (isDark) Color(0xFF1A1A2E) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(accentGreen.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null,
                        tint = accentGreen, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Verify Phone Number", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Enter the OTP sent to ${state.phone}", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = state.phoneOtp,
                    onValueChange = { viewModel.onPhoneOtpChanged(it) },
                    label = { Text("OTP Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentGreen,
                        unfocusedBorderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.1f),
                        cursorColor = accentGreen
                    )
                )

                AnimatedVisibility(state.phoneVerifyError != null, enter = fadeIn(), exit = fadeOut()) {
                    Text(state.phoneVerifyError ?: "", fontSize = 12.sp, color = Color.Red,
                        modifier = Modifier.padding(top = 6.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = if (state.isVerifyingPhone) "Verifying..." else "Verify OTP",
                    onClick = { viewModel.verifyPhoneOtp() },
                    enabled = !state.isVerifyingPhone && state.phoneOtp.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    accentGreen: Color,
    isDark: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null,
                tint = accentGreen, modifier = Modifier.size(20.dp))
        },
        singleLine = singleLine,
        minLines = minLines,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentGreen,
            unfocusedBorderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.1f),
            focusedContainerColor = accentGreen.copy(0.04f),
            unfocusedContainerColor = Color.Transparent,
            cursorColor = accentGreen
        )
    )
}

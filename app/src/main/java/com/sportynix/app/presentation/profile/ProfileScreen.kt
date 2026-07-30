package com.sportynix.app.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.User
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBookingHistory: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTeam: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToPoints: () -> Unit = {},
    onNavigateToReferrals: () -> Unit = {},
    onNavigateToAboutUs: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var showLogoutDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme() || state.isDarkTheme
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 350),
        label = "ThemeRotation"
    )

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileUiEffect.NavigateToLogin -> onLogout()
                is ProfileUiEffect.NavigateTo -> {}
                is ProfileUiEffect.ShowToast -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    // Light / Dark Theme Button
                    IconButton(
                        onClick = {
                            rotationAngle += 180f
                            viewModel.toggleTheme()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cardColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Theme",
                            tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF6366F1),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(animatedRotation)
                        )
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.user == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = accentGreen
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val user = state.user

                    // ── AVATAR HEADER ──
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(accentGreen, Color(0xFF0B7A44))
                                    )
                                )
                                .clickable { viewModel.setImageModalVisible(true) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.avatarUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = when {
                                    !user?.firstName.isNullOrBlank() || !user?.lastName.isNullOrBlank() ->
                                        "${user.firstName.take(1)}${user.lastName.take(1)}".uppercase()
                                    !user?.name.isNullOrBlank() ->
                                        user.name.split(" ").mapNotNull { it.take(1) }.take(2).joinToString("").uppercase()
                                    else -> "MN"
                                }
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Verified Checkmark Badge
                        if (user?.isFullyVerified == true || user?.isEmailUnverified == false) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFF00B2FE), CircleShape)
                                    .border(2.5.dp, backgroundColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Verified",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Name
                    Text(
                        text = user?.displayName ?: "Muhammed Nashan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Username
                    if (!user?.username.isNullOrBlank()) {
                        Text(
                            text = "@${user.username}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Email + Unverified Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user?.email.takeIf { !it.isNullOrBlank() } ?: "mnashan.dev@gmail.com",
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                        if (user?.isEmailUnverified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Unverified",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF97316)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bio
                    if (!user?.bio.isNullOrBlank()) {
                        Text(
                            text = user.bio,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }

                    // Phone verification warning box
                    if (user?.mustVerifyPhone == true && !user.isPhoneVerified) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x33EF4444) else Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Phone verification required.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Verify Now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentGreen,
                                    modifier = Modifier.clickable { viewModel.setPhoneVerifyModalVisible(true) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Edit Profile Button
                    OutlinedButton(
                        onClick = { onNavigateToEditProfile() },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accentGreen.copy(alpha = 0.4f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = accentGreen.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = accentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Edit Profile",
                                color = accentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // GENERAL Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GENERAL",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // Menu Cards
                    val menuItems = listOf(
                        ProfileMenuItemData("Booking History", Icons.Default.DateRange, onNavigateToBookingHistory),
                        ProfileMenuItemData("Payments", Icons.Default.CreditCard, onNavigateToPayments),
                        ProfileMenuItemData("Favourites", Icons.Default.Favorite, onNavigateToFavorites),
                        ProfileMenuItemData("Team", Icons.Default.Group, onNavigateToTeam),
                        ProfileMenuItemData("Your Points", Icons.Default.MonetizationOn, onNavigateToPoints),
                        ProfileMenuItemData("Your Referrals", Icons.Default.LocalOffer, onNavigateToReferrals),
                        ProfileMenuItemData("Settings", Icons.Default.Settings, onNavigateToSettings),
                        ProfileMenuItemData("About Us", Icons.Default.Info, onNavigateToAboutUs)
                    )

                    menuItems.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { item.onClick() },
                            color = cardColor,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(accentGreen.copy(alpha = if (isDark) 0.2f else 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = accentGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Text(
                                        text = item.title,
                                        color = textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // LOGOUT BUTTON
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showLogoutDialog = true },
                        color = cardColor,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EF4444))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Logout",
                                color = Color(0xFFEF4444),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // PROFILE PHOTO MODAL
            if (state.showImageModal) {
                ProfilePhotoModal(
                    user = state.user,
                    accentGreen = accentGreen,
                    isDark = isDark,
                    onDismiss = { viewModel.setImageModalVisible(false) }
                )
            }

            // PHONE VERIFICATION SHEET
            if (state.showPhoneVerifyModal) {
                PhoneVerificationSheet(
                    state = state,
                    onPhoneChange = viewModel::updatePhoneInput,
                    onOtpChange = viewModel::updatePhoneOtp,
                    onSendOtp = viewModel::sendPhoneOtp,
                    onVerifyOtp = viewModel::verifyPhoneOtp,
                    onDismiss = { viewModel.setPhoneVerifyModalVisible(false) }
                )
            }

            // LOGOUT CONFIRMATION DIALOG
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(text = "Logout", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to logout?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout()
                            }
                        ) {
                            Text("Logout", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

private data class ProfileMenuItemData(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {}
)

@Composable
private fun ProfilePhotoModal(
    user: User?,
    accentGreen: Color,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val backdrop = if (isDark) DarkBackground else LightBackground
        val cardBg = if (isDark) DarkSurface else LightSurface
        val modalPrimaryText = if (isDark) TextPrimaryDark else TextPrimaryLight
        val modalSecondaryText = if (isDark) TextSecondaryDark else TextSecondaryLight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdrop)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color.White)
                            .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x11000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = modalPrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Profile Photo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = modalPrimaryText
                    )

                    Spacer(modifier = Modifier.size(42.dp))
                }

                // Center Circle Avatar
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, accentGreen.copy(alpha = 0.4f), CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(accentGreen, Color(0xFF0B7A44))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.avatarUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initials = when {
                            !user?.firstName.isNullOrBlank() || !user?.lastName.isNullOrBlank() ->
                                "${user.firstName.take(1)}${user.lastName.take(1)}".uppercase()
                            !user?.name.isNullOrBlank() ->
                                user.name.split(" ").mapNotNull { it.take(1) }.take(2).joinToString("").uppercase()
                            else -> "MN"
                        }
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x11000000))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.displayName ?: "Muhammed Nashan",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = modalPrimaryText
                        )

                        if (!user?.username.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@${user.username}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = user?.email.takeIf { !it.isNullOrBlank() } ?: "mnashan.dev@gmail.com",
                            fontSize = 14.sp,
                            color = modalSecondaryText
                        )

                        if (!user?.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = user.bio,
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = modalSecondaryText,
                                textAlign = TextAlign.Center,
                                maxLines = 3
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                        )

                        // Points Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentGreen.copy(alpha = 0.15f))
                                .border(1.dp, accentGreen.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = accentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${user?.points ?: 700} pts",
                                    color = accentGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x1AFFFFFF) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x11000000))
                ) {
                    Text("Close", color = modalPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PhoneVerificationSheet(
    state: ProfileUiState,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Verify Phone Number",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter a new phone number and verify it with OTP.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.phoneInput,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number (+94XXXXXXXXX)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.phoneOtpCode,
                onValueChange = onOtpChange,
                label = { Text("6-Digit OTP Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (!state.phoneVerifyError.isNullOrBlank()) {
                Text(
                    text = state.phoneVerifyError,
                    color = Color.Red,
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSendOtp,
                    enabled = !state.isPhoneSending && state.phoneInput.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isPhoneSending) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Send OTP")
                }

                Button(
                    onClick = onVerifyOtp,
                    enabled = !state.isPhoneVerifying && state.phoneChallengeId != null && state.phoneOtpCode.length == 6,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isPhoneVerifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Verify OTP")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

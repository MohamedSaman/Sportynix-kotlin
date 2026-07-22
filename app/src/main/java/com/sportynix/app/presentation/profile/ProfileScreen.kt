package com.sportynix.app.presentation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.sportynix.app.presentation.components.SportynixTopBar
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookingHistory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var showLogoutDialog by remember { mutableStateOf(false) }

    val isDark = state.isDarkTheme

    val backgroundColor = if (isDark) Color(0xFF121215) else Color(0xFFF8FAFC)
    val cardBgColor = if (isDark) Color(0xFF1E1E22) else Color.White
    val cardBorderColor = if (isDark) Color(0x1F22C55E) else Color(0x1F0D8A4F)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val accentGreen = if (isDark) Color(0xFF22C55E) else Color(0xFF0D8A4F)

    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileUiEffect.NavigateToLogin -> onLogout()
                is ProfileUiEffect.NavigateTo -> {
                    when (effect.route) {
                        "Settings" -> onNavigateToSettings()
                        "BookingHistory" -> onNavigateToBookingHistory()
                    }
                }
                is ProfileUiEffect.ShowToast -> {
                    // Handled if snackbar is added
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SportynixTopBar(
                title = "My Profile",
                onBackClick = onNavigateBack
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = accentGreen
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val user = state.user

                    // ── Premium Profile Card ──
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 8.dp else 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = if (isDark) Color.Black else Color(0x20000000)
                            )
                            .border(
                                width = 1.dp,
                                color = cardBorderColor,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // ── LEFT COLUMN: Avatar + Points Pill ──
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .border(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                listOf(accentGreen, accentGreen.copy(alpha = 0.5f))
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.setImageModalVisible(true) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!user?.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user?.avatarUrl,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        listOf(accentGreen, Color(0xFF16A34A))
                                                    ),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                                color = Color.White,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (user?.isEmailVerified == true && user.isPhoneVerified) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(Color(0xFF10D191), CircleShape)
                                                .border(2.dp, cardBgColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Verified",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Points Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = accentGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        accentGreen.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = "Points",
                                            tint = accentGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${user?.points ?: 0} pts",
                                            color = accentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // ── RIGHT COLUMN: User Info + Action Icons ──
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user?.name ?: "User",
                                            color = primaryTextColor,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!user?.username.isNullOrBlank()) {
                                            Text(
                                                text = "@${user?.username}",
                                                color = accentGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Action Buttons (Edit + Theme toggle)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onNavigateToSettings() },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    accentGreen.copy(alpha = 0.15f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = accentGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleTheme() },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    if (isDark) Color(0xFF785500).copy(alpha = 0.5f) else Color(0xFF6366F1).copy(alpha = 0.12f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                                contentDescription = "Toggle Theme",
                                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF6366F1),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Email & Unverified Tag
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user?.email ?: "No email",
                                        color = secondaryTextColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    if (user?.isEmailVerified == false && !user.email.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDark) Color(0x88785000) else Color(0x22F59E0B),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isDark) Color(0x77CD961E) else Color(0x55F59E0B)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Unverified",
                                                    color = if (isDark) Color(0xFFF5C842) else Color(0xFFB45309),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                if (user?.mustVerifyPhone == true) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0x22EF4444),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44EF4444))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Phone verification required",
                                                color = Color(0xFFF87171),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bio
                                Text(
                                    text = if (!user?.bio.isNullOrBlank()) user.bio else "Add a bio to your profile...",
                                    color = secondaryTextColor,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── MENU SECTION ──
                    Text(
                        text = "MENU",
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    val menuItems = listOf(
                        ProfileMenuItemData("Booking History", Icons.Default.CalendarToday, onNavigateToBookingHistory),
                        ProfileMenuItemData("Payments", Icons.Default.AccountBalanceWallet) {},
                        ProfileMenuItemData("Favourites", Icons.Default.Favorite) {},
                        ProfileMenuItemData("Team", Icons.Default.Group) {},
                        ProfileMenuItemData("Your Points", Icons.Default.MonetizationOn) {},
                        ProfileMenuItemData("Your Referrals", Icons.Default.LocalOffer) {},
                        ProfileMenuItemData("Settings", Icons.Default.Settings, onNavigateToSettings),
                        ProfileMenuItemData("About Us", Icons.Default.Info) {}
                    )

                    menuItems.forEach { menuItem ->
                        ProfileMenuCard(
                            item = menuItem,
                            cardBgColor = cardBgColor,
                            cardBorderColor = cardBorderColor,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            accentGreen = accentGreen,
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── LOGOUT BUTTON ──
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x22DC2626) else Color(0x11EF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0x44F87171) else Color(0x33EF4444)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log Out",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Logout",
                                color = Color(0xFFEF4444),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // ── PROFILE PHOTO MODAL ──
            if (state.showImageModal) {
                ProfilePhotoModal(
                    user = state.user,
                    accentGreen = accentGreen,
                    onDismiss = { viewModel.setImageModalVisible(false) },
                    onEditPhoto = {
                        viewModel.setImageModalVisible(false)
                        onNavigateToSettings()
                    }
                )
            }

            // ── LOGOUT CONFIRMATION DIALOG ──
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(text = "Logout", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to logout from your account?") },
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
private fun ProfileMenuCard(
    item: ProfileMenuItemData,
    cardBgColor: Color,
    cardBorderColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    accentGreen: Color,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        shadowElevation = if (isDark) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
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
                    color = primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProfilePhotoModal(
    user: User?,
    accentGreen: Color,
    onDismiss: () -> Unit,
    onEditPhoto: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
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
                            .background(Color(0x1AFFFFFF), CircleShape)
                            .border(1.dp, Color(0x20FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Profile Photo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(42.dp))
                }

                // Middle Photo + Details Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Double Glowing Ring Avatar Container
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(1.5.dp, accentGreen.copy(alpha = 0.3f), CircleShape)
                            .padding(8.dp)
                            .border(3.dp, accentGreen, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = "Profile Photo Large",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(accentGreen, Color(0xFF16A34A))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Glass Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0x0EFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = user?.name ?: "User",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (!user?.username.isNullOrBlank()) {
                                Text(
                                    text = "@${user?.username}",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!user?.email.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user?.email ?: "",
                                    color = Color(0x66FFFFFF),
                                    fontSize = 12.sp
                                )
                            }

                            if (!user?.bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = user?.bio ?: "",
                                    color = Color(0xB3FFFFFF),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.width(180.dp))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Points Pill
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0x1F22C55E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${user?.points ?: 0} pts",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEditPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGreen
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Change Photo",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0x88FFFFFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x20FFFFFF))
                    ) {
                        Text(
                            text = "Close",
                            color = Color(0x88FFFFFF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

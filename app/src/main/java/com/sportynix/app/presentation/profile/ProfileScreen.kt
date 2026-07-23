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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.sportynix.app.presentation.components.CustomGlassHeader
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookingHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTeam: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPointsModal by remember { mutableStateOf(false) }
    var showReferralsModal by remember { mutableStateOf(false) }
    var showAboutUsModal by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background
    val accentGreen = if (isDark) NeonGreen else SportynixGreenPrimary

    LaunchedEffect(key1 = true) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileUiEffect.NavigateToLogin -> onLogout()
                is ProfileUiEffect.NavigateTo -> {
                    when (effect.route) {
                        "Settings" -> onNavigateToSettings()
                        "BookingHistory" -> onNavigateToBookingHistory()
                        "Favorites" -> onNavigateToFavorites()
                        "Team" -> onNavigateToTeam()
                        "EditProfile" -> onNavigateToEditProfile()
                    }
                }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Theme Toggle Icon Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x33F59E0B) else Color(0x226366F1))
                            .border(1.dp, if (isDark) Color(0x66F59E0B) else Color(0x446366F1), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Theme",
                            tint = if (isDark) AccentGold else Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val user = state.user

                    // ── AVATAR CIRCLE ──
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(100.dp)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    listOf(accentGreen, accentGreen.copy(alpha = 0.4f))
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
                                            listOf(accentGreen, Color(0xFF059669))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.take(2)?.uppercase() ?: "MN",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Verified Checkmark Badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFF00B2FE), CircleShape)
                                .border(2.dp, backgroundColor, CircleShape),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── USER NAME & USERNAME & EMAIL & BIO ──
                    Text(
                        text = user?.name ?: "Muhammed Nashan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "@${user?.username ?: "mnashandev"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentGreen
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user?.email ?: "mnashan.dev@gmail.com",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!user?.bio.isNullOrBlank()) user.bio else "Dfdhccc\nXbcvxgfg\nDggg...",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── EDIT PROFILE BUTTON ──
                    OutlinedButton(
                        onClick = { onNavigateToEditProfile() },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) GlassBorderDark else GlassBorderLight
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent
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

                    // ── GENERAL SECTION HEADER ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "GENERAL",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // ── MENU ITEMS ──
                    val menuItems = listOf(
                        ProfileMenuItemData("Booking History", Icons.Default.DateRange, onNavigateToBookingHistory),
                        ProfileMenuItemData("Payments", Icons.Default.CreditCard, onNavigateToPayments),
                        ProfileMenuItemData("Favourites", Icons.Default.Favorite, onNavigateToFavorites),
                        ProfileMenuItemData("Team", Icons.Default.Group, onNavigateToTeam),
                        ProfileMenuItemData("Your Points", Icons.Default.MonetizationOn) { showPointsModal = true },
                        ProfileMenuItemData("Your Referrals", Icons.Default.LocalOffer) { showReferralsModal = true },
                        ProfileMenuItemData("Settings", Icons.Default.Settings, onNavigateToSettings),
                        ProfileMenuItemData("About Us", Icons.Default.Info) { showAboutUsModal = true }
                    )

                    menuItems.forEach { item ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { item.onClick() },
                            shape = RoundedCornerShape(20.dp),
                            elevation = 4.dp
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(accentGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = accentGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── LOGOUT BUTTON ──
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isDark) Color(0x22DC2626) else Color(0x11EF4444),
                        borderColor = Color(0x44EF4444),
                        elevation = 4.dp
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
                                modifier = Modifier.size(22.dp)
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

            // ── PROFILE PHOTO / DETAILS MODAL ──
            if (state.showImageModal) {
                ProfilePhotoModal(
                    user = state.user,
                    accentGreen = accentGreen,
                    onDismiss = { viewModel.setImageModalVisible(false) }
                )
            }

            // ── POINTS MODAL ──
            if (showPointsModal) {
                InfoDialog(
                    title = "Your Points",
                    description = "You currently have 150 Sportynix Reward Points! Use points to get discounts on venue slot bookings.",
                    onDismiss = { showPointsModal = false }
                )
            }

            // ── REFERRALS MODAL ──
            if (showReferralsModal) {
                InfoDialog(
                    title = "Your Referrals",
                    description = "Share your code MNASHAN2026 with friends and earn 50 bonus points for every friend who signs up!",
                    onDismiss = { showReferralsModal = false }
                )
            }

            // ── ABOUT US MODAL ──
            if (showAboutUsModal) {
                InfoDialog(
                    title = "About Sportynix",
                    description = "Sportynix v2.5.0\nThe ultimate next-generation sports venue booking and community app.",
                    onDismiss = { showAboutUsModal = false }
                )
            }

            // ── LOGOUT DIALOG ──
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
private fun ProfilePhotoModal(
    user: User?,
    accentGreen: Color,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large Avatar Circle
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(accentGreen, Color(0xFF059669))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.name?.take(2)?.uppercase() ?: "MN",
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.name ?: "Muhammed Nashan",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "@${user?.username ?: "mnashandev"}",
                            color = accentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = user?.email ?: "mnashan.dev@gmail.com",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (!user?.bio.isNullOrBlank()) user.bio else "Dfdhccc\nXbcvxgfg\nDggg...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Points Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentGreen.copy(alpha = 0.2f))
                                .border(1.dp, accentGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "★ ${user?.points ?: 150} pts",
                                color = accentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = description, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = SportynixGreenPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

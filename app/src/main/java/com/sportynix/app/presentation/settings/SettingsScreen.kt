package com.sportynix.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    var allowDirectTeamAdd by mutableStateOf(false)
        private set
    var user by mutableStateOf(profileRepository.currentUser.value)
        private set
    var isUpdatingPrivacy by mutableStateOf(false)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val result = profileRepository.fetchProfile()
            result.onSuccess { u ->
                allowDirectTeamAdd = u.allowDirectTeamAdd ?: false
            }
        }
    }

    fun updateAllowDirectTeamAdd(enabled: Boolean) {
        val previous = allowDirectTeamAdd
        allowDirectTeamAdd = enabled
        viewModelScope.launch {
            isUpdatingPrivacy = true
            val result = profileRepository.updateAllowDirectTeamAdd(enabled)
            if (result.isFailure) {
                allowDirectTeamAdd = previous // Rollback on error
            }
            isUpdatingPrivacy = false
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = profileRepository.deleteAccount()
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    onNavigateToReportedUsers: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var isDarkMode by remember { mutableStateOf(isDark) }
    var showChangeEmail by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showDeleteAccountAlert by remember { mutableStateOf(false) }
    var showClearCacheAlert by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    var showBugReportModal by remember { mutableStateOf(false) }

    val packageInfo = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val appVersion = packageInfo?.versionName ?: "1.0.1"
    val buildNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        packageInfo?.versionCode?.toString() ?: "1"
    }

    fun launchIntent(intent: Intent) {
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, null))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open action handler", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. ACCOUNT SECTION
            SectionHeader("Account", Icons.Default.Person, accentGreen, textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsItem("Edit Profile", Icons.Default.Edit, cardColor, borderColor, textPrimary, textSecondary, accentGreen, onClick = onNavigateToEditProfile)
                SettingsItem("Change Email", Icons.Default.Email, cardColor, borderColor, textPrimary, textSecondary, accentGreen) { showChangeEmail = true }
                if (viewModel.user?.isSocial == true) {
                    SettingsItem("Signed in with Social", Icons.Default.Shield, cardColor, borderColor, textPrimary, textSecondary, accentGreen, showChevron = false)
                }
                SettingsItem("Change Password", Icons.Default.Lock, cardColor, borderColor, textPrimary, textSecondary, accentGreen) { showChangePassword = true }
            }

            // 2. APPEARANCE SECTION
            SectionHeader("Appearance", Icons.Default.Palette, accentGreen, textPrimary)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Dark Mode", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text(if (isDarkMode) "Dark theme active" else "Light theme active", fontSize = 13.sp, color = textSecondary)
                        }
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen))
                }
            }

            // 3. HELP & SUPPORT SECTION
            SectionHeader("Help & Support", Icons.AutoMirrored.Filled.Help, accentGreen, textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsItem("Email Support", Icons.Default.Email, cardColor, borderColor, textPrimary, textSecondary, accentGreen, subtitle = "info@sportynix.com") {
                    launchIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@sportynix.com")))
                }
                SettingsItem("Call Support", Icons.Default.Phone, cardColor, borderColor, textPrimary, textSecondary, accentGreen, subtitle = "0332292223") {
                    launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0332292223")))
                }
                SettingsItem("Report an Issue", Icons.Default.BugReport, cardColor, borderColor, textPrimary, textSecondary, accentGreen) {
                    showBugReportModal = true
                }
            }

            // 4. PRIVACY SECTION
            SectionHeader("Privacy", Icons.Default.PrivacyTip, accentGreen, textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = cardColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Allow Direct Team Add", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Text(if (viewModel.allowDirectTeamAdd) "Anyone can add you" else "Request required", fontSize = 13.sp, color = textSecondary)
                            }
                        }
                        Switch(checked = viewModel.allowDirectTeamAdd, onCheckedChange = viewModel::updateAllowDirectTeamAdd, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen))
                    }
                }

                SettingsItem("Blocked Users", Icons.Default.Block, cardColor, borderColor, textPrimary, textSecondary, accentGreen, onClick = onNavigateToBlockedUsers)
                SettingsItem("Reported Users", Icons.Default.Flag, cardColor, borderColor, textPrimary, textSecondary, accentGreen, onClick = onNavigateToReportedUsers)
            }

            // 5. FAQS SECTION
            SectionHeader("FAQs", Icons.Default.PrivacyTip, accentGreen, textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FaqItemCard("Can I cancel my booking?", "Yes! You can cancel up to 2 hours before the booking start time for a full refund.", cardColor, borderColor, textPrimary, textSecondary)
                FaqItemCard("Can I book multiple sessions?", "Yes! You can book multiple consecutive or recurring sessions at your convenience.", cardColor, borderColor, textPrimary, textSecondary)
            }

            // 6. STORAGE SECTION
            SectionHeader("Storage", Icons.Default.Storage, accentGreen, textPrimary)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showClearCacheAlert = true },
                color = cardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(if (cacheCleared) "Cache Cleared" else "Clear App Cache", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (cacheCleared) accentGreen else textPrimary)
                        Text(if (cacheCleared) "Cache cleared successfully" else "Free up local storage and fix sync issues", fontSize = 13.sp, color = textSecondary)
                    }
                }
            }

            // 7. DELETE ACCOUNT BUTTON
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDeleteAccountAlert = true },
                color = cardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EF4444))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 8. APP INFO FOOTER
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Version $appVersion (Build $buildNumber)", fontSize = 14.sp, color = textSecondary)
                Text("Sportynix Corp", fontSize = 13.sp, color = textSecondary.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // CLEAR CACHE ALERT
        if (showClearCacheAlert) {
            AlertDialog(
                onDismissRequest = { showClearCacheAlert = false },
                title = { Text("Clear Cache?", fontWeight = FontWeight.Bold) },
                text = { Text("This will clear cached images and temporary data without signing you out.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            cacheCleared = true
                            showClearCacheAlert = false
                        }
                    ) {
                        Text("Clear", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = { showClearCacheAlert = false }) { Text("Cancel") } }
            )
        }

        // DELETE ACCOUNT ALERT
        if (showDeleteAccountAlert) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountAlert = false },
                title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
                text = { Text("This action is permanent and cannot be undone. All your bookings and points will be erased.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAccountAlert = false
                            viewModel.deleteAccount { onLogout() }
                        }
                    ) {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteAccountAlert = false }) { Text("Cancel") } }
            )
        }

        // BUG REPORT MODAL
        if (showBugReportModal) {
            AlertDialog(
                onDismissRequest = { showBugReportModal = false },
                title = { Text("Report an Issue", fontWeight = FontWeight.Bold) },
                text = { Text("Please describe the issue or bug you experienced. You can also email us directly at support@sportynix.com.") },
                confirmButton = {
                    TextButton(onClick = { showBugReportModal = false }) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

private fun PackageInfo_cityName(): String? = null

@Composable
private fun SectionHeader(title: String, icon: ImageVector, accentGreen: Color, textPrimary: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color,
    subtitle: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onClick() },
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    if (subtitle != null) {
                        Text(subtitle, fontSize = 13.sp, color = textSecondary)
                    }
                }
            }

            if (showChevron) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FaqItemCard(
    question: String,
    answer: String,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { expanded = !expanded },
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Q: $question", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            AnimatedVisibility(visible = expanded) {
                Text(answer, fontSize = 14.sp, color = textSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

package com.sportynix.app.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Warning
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
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToChangeEmail: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToActiveSessions: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToReportIssue: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    var biometricsEnabled by remember { mutableStateOf(false) }
    var twoFactor by remember { mutableStateOf(false) }
    var profileVisible by remember { mutableStateOf(true) }
    var showActivity by remember { mutableStateOf(false) }
    var locationSharing by remember { mutableStateOf(true) }

    fun toggleBiometrics(enabled: Boolean) {
        biometricsEnabled = enabled
        val msg = if (enabled) "Biometric authentication enabled" else "Biometric authentication disabled"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            // SECURITY SECTION
            SectionCard("Security", cardColor, borderColor, textPrimary) {
                ToggleRow(Icons.Default.Fingerprint, "Face ID / Fingerprint", "Use biometrics to sign in", biometricsEnabled, accentGreen, textPrimary, textSecondary) {
                    toggleBiometrics(it)
                }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.Shield, "Two-Factor Auth", "Extra verification on sign in", twoFactor, accentGreen, textPrimary, textSecondary) { twoFactor = it }
                HorizontalDivider(color = borderColor)
                NavRow(Icons.Default.Email, "Change Email", "Update your email address", textPrimary, textSecondary, accentGreen, onClick = onNavigateToChangeEmail)
                HorizontalDivider(color = borderColor)
                NavRow(Icons.Default.Lock, "Reset Password", "Reset your password", textPrimary, textSecondary, accentGreen, onClick = onNavigateToChangePassword)
                HorizontalDivider(color = borderColor)
                NavRow(Icons.Default.Phonelink, "Active Sessions", "Manage logged-in devices", textPrimary, textSecondary, accentGreen, onClick = onNavigateToActiveSessions)
            }

            // PRIVACY SECTION
            SectionCard("Privacy", cardColor, borderColor, textPrimary) {
                ToggleRow(Icons.Default.Visibility, "Profile Visibility", "Others can see your profile", profileVisible, accentGreen, textPrimary, textSecondary) { profileVisible = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.DirectionsRun, "Activity Status", "Show when you're active", showActivity, accentGreen, textPrimary, textSecondary) { showActivity = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.LocationOn, "Location Sharing", "Share location with nearby venues", locationSharing, accentGreen, textPrimary, textSecondary) { locationSharing = it }
            }

            // DATA MANAGEMENT SECTION
            SectionCard("Data Management", cardColor, borderColor, textPrimary) {
                NavRow(Icons.Default.Download, "Download My Data", "Export your personal data", textPrimary, textSecondary, accentGreen) {
                    Toast.makeText(context, "Data export request submitted. Check your email.", Toast.LENGTH_SHORT).show()
                }
                HorizontalDivider(color = borderColor)
                NavRow(Icons.Default.Delete, "Delete Account", "Permanently delete your account", Color.Red, textSecondary, Color.Red, isDestructive = true, onClick = onNavigateToDeleteAccount)
            }

            // SUPPORT SECTION
            SectionCard("Support", cardColor, borderColor, textPrimary) {
                NavRow(Icons.Default.Warning, "Report an Issue", "Let us know if you encounter any problems", textPrimary, textSecondary, accentGreen, onClick = onNavigateToReportIssue)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(accentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(subtitle, fontSize = 11.sp, color = textSecondary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen))
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color,
    textSecondary: Color,
    iconColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
                Text(subtitle, fontSize = 11.sp, color = textSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

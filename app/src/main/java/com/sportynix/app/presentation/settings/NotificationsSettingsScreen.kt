package com.sportynix.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    // Toggles state
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(false) }
    var smsNotifications by remember { mutableStateOf(false) }

    var bookingReminders by remember { mutableStateOf(true) }
    var teamUpdates by remember { mutableStateOf(true) }
    var reviewReplies by remember { mutableStateOf(true) }
    var pointsAlerts by remember { mutableStateOf(true) }
    var promotions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            // Notification Channels Section
            SectionCard(title = "NOTIFICATION CHANNELS", cardColor = cardColor, borderColor = borderColor, textPrimary = textPrimary, textSecondary = textSecondary) {
                ToggleRow(Icons.Default.Notifications, "Push Notifications", "Receive push notifications", pushNotifications, accentGreen, textPrimary, textSecondary) { pushNotifications = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.Email, "Email Notifications", "Receive email updates", emailNotifications, accentGreen, textPrimary, textSecondary) { emailNotifications = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.Message, "SMS Notifications", "Receive text messages", smsNotifications, accentGreen, textPrimary, textSecondary) { smsNotifications = it }
            }

            // Activity Notifications Section
            SectionCard(title = "ACTIVITY NOTIFICATIONS", cardColor = cardColor, borderColor = borderColor, textPrimary = textPrimary, textSecondary = textSecondary) {
                ToggleRow(Icons.Default.CalendarMonth, "Booking Reminders", "Reminders before your bookings", bookingReminders, accentGreen, textPrimary, textSecondary) { bookingReminders = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.Group, "Team Updates", "Team invites and match updates", teamUpdates, accentGreen, textPrimary, textSecondary) { teamUpdates = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.Star, "Review Replies", "When someone replies to your review", reviewReplies, accentGreen, textPrimary, textSecondary) { reviewReplies = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.CardGiftcard, "Points & Rewards", "Points earned and reward alerts", pointsAlerts, accentGreen, textPrimary, textSecondary) { pointsAlerts = it }
                HorizontalDivider(color = borderColor)
                ToggleRow(Icons.Default.LocalOffer, "Promotions & Offers", "Special deals and discounts", promotions, accentGreen, textPrimary, textSecondary) { promotions = it }
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
    textSecondary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen)
        )
    }
}

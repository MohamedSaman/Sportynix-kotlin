package com.sportynix.app.presentation.settings

import android.content.Intent
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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

data class FAQItemModel(
    val id: Int,
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    var searchText by remember { mutableStateOf("") }
    var expandedFaqId by remember { mutableStateOf<Int?>(null) }

    val faqs = listOf(
        FAQItemModel(1, "How do I book a sports facility?", "Go to the Home tab, search for a complex, choose a sport, and tap 'Book Now'. Select your preferred date, time slot, and complete the payment."),
        FAQItemModel(2, "Can I cancel my booking?", "Yes, you can cancel up to 2 hours before the scheduled time for a full refund. Go to Booking History, find your booking, and tap 'Cancel'."),
        FAQItemModel(3, "How do I earn points?", "Earn points by completing bookings (+100-200), writing reviews (+50), referring friends (+200), and maintaining daily login streaks (+100)."),
        FAQItemModel(4, "How can I redeem my points?", "Points can be redeemed as discounts during checkout. 1 point = Rs 1. Minimum 100 points required for redemption."),
        FAQItemModel(5, "How do I change my password?", "Go to Profile > Settings > Change Password. Enter your current password and set a new one."),
        FAQItemModel(6, "Can I add multiple payment methods?", "Yes, go to Profile > Payment Methods and tap 'Add New Card'. You can set any card as your default payment method.")
    )

    val filteredFaqs = remember(searchText) {
        if (searchText.isBlank()) faqs
        else faqs.filter {
            it.question.contains(searchText, ignoreCase = true) || it.answer.contains(searchText, ignoreCase = true)
        }
    }

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot handle action", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search help topics...", color = textSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentGreen,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = cardColor,
                    unfocusedContainerColor = cardColor
                ),
                singleLine = true
            )

            // Quick Help Action Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick Help", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(Icons.Default.ChatBubble, "Live Chat", accentGreen, cardColor, borderColor, textPrimary, modifier = Modifier.weight(1f)) {
                        Toast.makeText(context, "Opening live chat support...", Toast.LENGTH_SHORT).show()
                    }
                    QuickActionCard(Icons.Default.Email, "Email Us", Color(0xFF3B82F6), cardColor, borderColor, textPrimary, modifier = Modifier.weight(1f)) {
                        launchIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@sportynix.com")))
                    }
                    QuickActionCard(Icons.Default.Phone, "Call Us", Color(0xFFF97316), cardColor, borderColor, textPrimary, modifier = Modifier.weight(1f)) {
                        launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+94112345678")))
                    }
                }
            }

            // FAQs Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Frequently Asked Questions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                if (filteredFaqs.isEmpty()) {
                    Text("No help topics matching '$searchText'", fontSize = 14.sp, color = textSecondary)
                } else {
                    filteredFaqs.forEach { faq ->
                        val isExpanded = expandedFaqId == faq.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { expandedFaqId = if (isExpanded) null else faq.id },
                            color = cardColor,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(faq.question, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Text(faq.answer, fontSize = 13.sp, color = textSecondary, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Contact Us Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Contact Us", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                ContactRow(Icons.Default.Email, "Email", "support@sportynix.com", cardColor, borderColor, textPrimary, textSecondary, accentGreen)
                ContactRow(Icons.Default.Phone, "Phone", "+94 11 234 5678", cardColor, borderColor, textPrimary, textSecondary, accentGreen)
                ContactRow(Icons.Default.AccessTime, "Hours", "Mon-Fri 9AM-6PM", cardColor, borderColor, textPrimary, textSecondary, accentGreen)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable { onClick() },
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    title: String,
    value: String,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(accentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, fontSize = 12.sp, color = textSecondary)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            }
        }
    }
}

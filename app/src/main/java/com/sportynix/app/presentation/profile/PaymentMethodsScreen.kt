package com.sportynix.app.presentation.profile

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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.theme.*

data class PaymentCardItem(
    val id: String,
    val brand: String,
    val last4: String,
    val expiry: String,
    val isDefault: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var cards by remember {
        mutableStateOf(
            listOf(
                PaymentCardItem("1", "Visa", "4242", "12/27", true),
                PaymentCardItem("2", "Mastercard", "8888", "06/28", false)
            )
        )
    }

    var cardToRemove by remember { mutableStateOf<PaymentCardItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saved Cards List
            cards.forEach { card ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = accentGreen, modifier = Modifier.size(24.dp))
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${card.brand} •••• ${card.last4}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    if (card.isDefault) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentGreen.copy(alpha = 0.12f)
                                        ) {
                                            Text("Default", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                                Text("Expires ${card.expiry}", fontSize = 12.sp, color = textSecondary)
                            }
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = textSecondary)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                if (!card.isDefault) {
                                    DropdownMenuItem(
                                        text = { Text("Set as Default") },
                                        onClick = {
                                            cards = cards.map { it.copy(isDefault = it.id == card.id) }
                                            menuExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Remove", color = Color.Red) },
                                    onClick = {
                                        cardToRemove = card
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Other Payment Methods Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Other Methods", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                listOf(
                    Triple(Icons.Default.Payments, "Bank Transfer", "Direct bank payment"),
                    Triple(Icons.Default.PhoneIphone, "Mobile Wallet", "Pay via mobile wallet"),
                    Triple(Icons.Default.AttachMoney, "Cash on Arrival", "Pay at the venue")
                ).forEach { (icon, title, subtitle) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = cardColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                    Text(subtitle, fontSize = 12.sp, color = textSecondary)
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Add New Card Button
            Button(
                onClick = { /* Add Card Flow */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Card", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Removal Confirmation Dialog
        cardToRemove?.let { card ->
            AlertDialog(
                onDismissRequest = { cardToRemove = null },
                title = { Text("Remove Payment Card?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove ${card.brand} ending in ${card.last4}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            cards = cards.filter { it.id != card.id }
                            cardToRemove = null
                        }
                    ) {
                        Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToRemove = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

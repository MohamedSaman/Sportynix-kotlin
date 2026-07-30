package com.sportynix.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

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
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR).toString() }

    fun openUrl(urlStr: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // HERO CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(accentGreen.copy(alpha = 0.28f), accentGreen.copy(alpha = 0.1f))
                                )
                            )
                            .border(1.dp, accentGreen.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sports, contentDescription = null, tint = accentGreen, modifier = Modifier.size(38.dp))
                    }

                    Text("Sportynix", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                    Text(
                        "Find, book, and enjoy the best indoor sports venues around you.",
                        fontSize = 14.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgePill("v$appVersion", accentGreen)
                        BadgePill("Build $buildNumber", accentGreen)
                        BadgePill("Since 2026", accentGreen)
                    }
                }
            }

            // ABOUT SECTION
            SectionCard("About Sportynix", Icons.Default.Info, accentGreen, cardColor, borderColor, textPrimary) {
                Text(
                    "Sportynix is your premier platform for discovering and booking indoor sports venues. We connect sports enthusiasts with the best facilities in their area, making it easy to find, book, and play your favorite sports.",
                    fontSize = 14.sp,
                    color = textSecondary,
                    lineHeight = 20.sp
                )
            }

            // FEATURE PILLS
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeaturePill(Icons.Default.FlashOn, "Fast booking", accentGreen, textPrimary, modifier = Modifier.weight(1f))
                FeaturePill(Icons.Default.CheckCircle, "Verified venues", accentGreen, textPrimary, modifier = Modifier.weight(1f))
                FeaturePill(Icons.Default.Headphones, "24/7 support", accentGreen, textPrimary, modifier = Modifier.weight(1f))
            }

            // PRIVACY & LEGAL SECTION
            SectionCard("Privacy & Legal", Icons.Default.Shield, accentGreen, cardColor, borderColor, textPrimary) {
                ActionRow("Terms & Conditions", Icons.Default.Description, Icons.Default.ChevronRight, textPrimary, textSecondary, accentGreen) {
                    openUrl("https://sportynix.com/terms")
                }
                ActionRow("Privacy Policy", Icons.Default.Shield, Icons.Default.ChevronRight, textPrimary, textSecondary, accentGreen) {
                    openUrl("https://sportynix.com/privacy")
                }
            }

            // CONTACT SECTION
            SectionCard("Contact", Icons.Default.Email, accentGreen, cardColor, borderColor, textPrimary) {
                ActionRow("info@sportynix.com", Icons.Default.Email, Icons.Default.OpenInNew, textPrimary, textSecondary, accentGreen) {
                    openUrl("mailto:info@sportynix.com")
                }
                ActionRow("sportynix.com", Icons.Default.Language, Icons.Default.OpenInNew, textPrimary, textSecondary, accentGreen) {
                    openUrl("https://sportynix.com")
                }
            }

            // FOOTER SECTION
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Version $appVersion", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                Text("© $currentYear Sportynix Corp. All rights reserved.", fontSize = 13.sp, color = textSecondary.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    accentGreen: Color,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(18.dp))
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
            content()
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    icon: ImageVector,
    trailingIcon: ImageVector,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onClick() },
        color = accentGreen.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(16.dp))
                }
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            }
            Icon(trailingIcon, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BadgePill(text: String, accentGreen: Color) {
    Surface(shape = CircleShape, color = accentGreen.copy(alpha = 0.12f)) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, text: String, accentGreen: Color, textPrimary: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = accentGreen.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        }
    }
}

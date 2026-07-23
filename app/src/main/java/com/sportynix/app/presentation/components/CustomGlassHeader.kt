package com.sportynix.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.R
import com.sportynix.app.presentation.theme.GlassBorderDark
import com.sportynix.app.presentation.theme.GlassBorderLight
import com.sportynix.app.presentation.theme.GlassSurfaceDark
import com.sportynix.app.presentation.theme.GlassSurfaceLight
import com.sportynix.app.presentation.theme.NeonGreen
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun CustomGlassHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    showBack: Boolean = false,
    onBackPress: (() -> Unit)? = null,
    onMessagesPress: (() -> Unit)? = null,
    onNotificationsPress: (() -> Unit)? = null,
    unreadMessagesCount: Int = 0,
    unreadNotificationsCount: Int = 0
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val borderCol = if (isDark) GlassBorderDark else GlassBorderLight
    val iconBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9).copy(alpha = 0.8f)

    // Pulse animation for brand icon
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                ambientColor = if (isDark) NeonGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (isDark) NeonGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(bg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderCol,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT SECTION (Back Button or Animated Logo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack && onBackPress != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable { onBackPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SportynixGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Logo & Title
                Box(
                    modifier = Modifier.scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    val logoRes = if (isDark) R.drawable.logo_white else R.drawable.logo
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Sportynix Logo",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title ?: "Sportynix",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.5.sp
                    )
                    if (title == null) {
                        Text(
                            text = "Play • Book • Compete",
                            fontSize = 11.sp,
                            color = SportynixGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // RIGHT SECTION (Messages & Notifications Glass Buttons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Messages Button
                if (onMessagesPress != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBg)
                            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                            .clickable { onMessagesPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Messages",
                            tint = SportynixGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )

                        if (unreadMessagesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadMessagesCount > 9) "9+" else unreadMessagesCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Notifications Button
                if (onNotificationsPress != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBg)
                            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                            .clickable { onNotificationsPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = SportynixGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )

                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 2.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

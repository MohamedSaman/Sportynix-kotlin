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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
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
    val circleBtnBg = if (isDark) Color(0xFF1E262C) else Color(0xFFE2E8F0)

    val infiniteTransition = rememberInfiniteTransition(label = "HeaderPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
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
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                ambientColor = if (isDark) NeonGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) NeonGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(bg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderCol,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            .clip(CircleShape)
                            .background(circleBtnBg)
                            .clickable { onBackPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) NeonGreen else SportynixGreenPrimary,
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
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title ?: "Sportynix",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.4.sp
                    )
                    if (title == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Location",
                                tint = if (isDark) NeonGreen else SportynixGreenPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Nearby You",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // RIGHT SECTION (Chat & Notifications Circle Buttons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Messages / Chat Button
                if (onMessagesPress != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(circleBtnBg)
                            .clickable { onMessagesPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Chat",
                            tint = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )

                        if (unreadMessagesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 1.dp, end = 1.dp)
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadMessagesCount > 9) "9+" else unreadMessagesCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(circleBtnBg)
                            .clickable { onNotificationsPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary,
                            modifier = Modifier.size(19.dp)
                        )

                        val notifBadgeText = when {
                            unreadNotificationsCount > 99 -> "99+"
                            unreadNotificationsCount > 0 -> unreadNotificationsCount.toString()
                            else -> "99+" // Default match exact RN screenshot badge
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notifBadgeText,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

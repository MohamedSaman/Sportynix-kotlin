package com.sportynix.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.theme.GlassBorderDark
import com.sportynix.app.presentation.theme.GlassBorderLight
import com.sportynix.app.presentation.theme.GlassSurfaceDark
import com.sportynix.app.presentation.theme.GlassSurfaceLight
import com.sportynix.app.presentation.theme.NeonGreen
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

data class GlassNavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
fun GlassBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val borderCol = if (isDark) GlassBorderDark else GlassBorderLight

    val navItems = listOf(
        GlassNavItem("league", "Compete", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
        GlassNavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
        GlassNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        GlassNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
        GlassNavItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (isDark) NeonGreen.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.1f),
                    spotColor = if (isDark) SportynixGreenPrimary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            Color(0xE6131B2E),
                            Color(0xCC0B0F19)
                        ) else listOf(
                            Color(0xF0FFFFFF),
                            Color(0xD9F1F5F9)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(
                            NeonGreen.copy(alpha = 0.5f),
                            GlassBorderDark.copy(alpha = 0.1f)
                        ) else listOf(
                            SportynixGreenPrimary.copy(alpha = 0.4f),
                            GlassBorderLight.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route

                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = if (selected) {
                            if (isDark) NeonGreen else SportynixGreenPrimary
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        label = "tintColor"
                    )

                    val activeBg = if (isDark) NeonGreen.copy(alpha = 0.15f) else SportynixGreenPrimary.copy(alpha = 0.12f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(item.route) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (selected) activeBg else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                contentDescription = item.label,
                                tint = tintColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tintColor
                        )
                    }
                }
            }
        }
    }
}

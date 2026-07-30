package com.sportynix.app.presentation.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquidTabBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val pillHeight = 68.dp
    val pillCornerRadius = 34.dp
    val bubbleHeight = 48.dp

    // Color definitions matching Swift LiquidTabBar.swift — NO green icons
    val activeColor = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF101014).copy(alpha = 0.92f)
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF666670)

    val pillBackground = if (isDark) Color(0xFF1E262C).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.78f)
    val pillBorderColor = if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.50f)
    val bubbleBackground = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF0F172A).copy(alpha = 0.10f)
    val bubbleBorderColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.65f)

    var totalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(pillHeight)
            .shadow(
                elevation = if (isDark) 16.dp else 10.dp,
                shape = RoundedCornerShape(pillCornerRadius),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.4f else 0.08f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.2f else 0.04f)
            )
            .clip(RoundedCornerShape(pillCornerRadius))
            .background(pillBackground)
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        pillBorderColor,
                        pillBorderColor.copy(alpha = 0.06f),
                        pillBorderColor.copy(alpha = 0.22f)
                    )
                ),
                shape = RoundedCornerShape(pillCornerRadius)
            )
            .pointerInput(selectedTab) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        val threshold = 40.dp.toPx()
                        if (totalDrag < -threshold) {
                            val next = selectedTab.next
                            if (next != selectedTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(next)
                            }
                        } else if (totalDrag > threshold) {
                            val prev = selectedTab.previous
                            if (prev != selectedTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(prev)
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
            .padding(horizontal = 6.dp)
    ) {
        // Top refraction sheen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillHeight * 0.42f)
                .clip(RoundedCornerShape(topStart = pillCornerRadius, topEnd = pillCornerRadius))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.10f else 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem.values().forEach { tab ->
                val isActive = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable {
                            if (!isActive) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(tab)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        // Liquid glass bubble matching Swift .ultraThinMaterial bubble
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(bubbleHeight)
                                .clip(CapsuleShape)
                                .background(bubbleBackground)
                                .border(
                                    width = 0.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            bubbleBorderColor,
                                            Color.Transparent,
                                            bubbleBorderColor.copy(alpha = 0.15f)
                                        )
                                    ),
                                    shape = CapsuleShape
                                )
                        )
                    }

                    // Icon + Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.title,
                            tint = if (isActive) activeColor else inactiveColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) activeColor else inactiveColor
                        )
                    }
                }
            }
        }
    }
}

private val CapsuleShape = RoundedCornerShape(50.dp)

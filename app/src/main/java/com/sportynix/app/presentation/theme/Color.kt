package com.sportynix.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Brand Colors - Electric Sportynix Green
val SportynixGreenPrimary = Color(0xFF10B981)
val SportynixGreenDark = Color(0xFF059669)
val SportynixGreenLight = Color(0xFF6EE7B7)
val NeonGreen = Color(0xFF00E676)
val NeonGreenGlow = Color(0x3300E676)

// Dark Theme Slate Palette (Futuristic Dark)
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1F2937)
val GlassSurfaceDark = Color(0xD90D1322)
val GlassCardDark = Color(0xA6162032)
val GlassBorderDark = Color(0x3D00E676)

// Light Theme Slate Palette (Clean Glass Light)
val LightBackground = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val GlassSurfaceLight = Color(0xD9F8FAFC)
val GlassCardLight = Color(0xB3FFFFFF)
val GlassBorderLight = Color(0x3310B981)

// Accent & Typography Colors
val AccentGold = Color(0xFFF59E0B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)

// Status Colors
val StatusSuccess = Color(0xFF10B981)
val StatusError = Color(0xFFEF4444)
val StatusWarning = Color(0xFFF59E0B)

// Gradient Brushes for Glass UI
val PrimaryNeonGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF00E676))
)

val GlassHighlightGradientDark = Brush.verticalGradient(
    colors = listOf(Color(0x33FFFFFF), Color(0x05FFFFFF))
)

val GlassHighlightGradientLight = Brush.verticalGradient(
    colors = listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))
)

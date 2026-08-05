package com.sportynix.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Brand Colors - Electric Sportynix Green
val SportynixGreenPrimary = Color(0xFF00D982)
val SportynixGreenLightTheme = Color(0xFF0D8A4F)
val SportynixGreenDarkTheme = Color(0xFF00D982)
val SportynixGreenDark = Color(0xFF00B86B)
val SportynixGreenLight = Color(0xFF6EE7B7)
val NeonGreen = Color(0xFF00D982)
val NeonGreenGlow = Color(0x3300D982)

// Shared premium navy-black palette. Every dark screen uses this canvas.
val DarkBackground = Color(0xFF070C16)
val DarkSurface = Color(0xFF101A2B)
val DarkSurfaceVariant = Color(0xFF1C2A40)
val GlassSurfaceDark = Color(0xE60B1220)
val GlassCardDark = Color(0xB31B2A40)
val GlassBorderDark = Color(0x3D00E676)

// Light Theme Slate Palette (Clean Glass Light)
val LightBackground = Color(0xFFF8FAF9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F0)
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
    colors = listOf(Color(0xFF00B86B), Color(0xFF00E58A))
)

val GlassHighlightGradientDark = Brush.verticalGradient(
    colors = listOf(Color(0x33FFFFFF), Color(0x05FFFFFF))
)

val GlassHighlightGradientLight = Brush.verticalGradient(
    colors = listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))
)

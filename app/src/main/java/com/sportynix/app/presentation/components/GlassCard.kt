package com.sportynix.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sportynix.app.presentation.theme.GlassBorderDark
import com.sportynix.app.presentation.theme.GlassBorderLight
import com.sportynix.app.presentation.theme.GlassCardDark
import com.sportynix.app.presentation.theme.GlassCardLight
import com.sportynix.app.presentation.theme.GlassHighlightGradientDark
import com.sportynix.app.presentation.theme.GlassHighlightGradientLight

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val defaultBg = if (isDark) GlassCardDark else GlassCardLight
    val defaultBorder = if (isDark) GlassBorderDark else GlassBorderLight
    val highlightBrush = if (isDark) GlassHighlightGradientDark else GlassHighlightGradientLight

    val finalBg = backgroundColor ?: defaultBg
    val finalBorder = borderColor ?: defaultBorder

    var cardModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = if (isDark) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
            spotColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f)
        )
        .clip(shape)
        .background(finalBg)
        .border(width = borderWidth, color = finalBorder, shape = shape)

    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }

    Box(modifier = cardModifier) {
        // Inner subtle glass highlight at top
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(highlightBrush)
        )
        content()
    }
}

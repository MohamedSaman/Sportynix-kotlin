package com.sportynix.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sportynix.app.presentation.theme.*

@Composable
fun SportynixGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = backgroundColor ?: if (isDark) GlassCardDark else GlassCardLight
    val border = borderColor ?: if (isDark) GlassBorderDark else GlassBorderLight

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )

    val mod = if (onClick != null) {
        modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        modifier
    }

    SportynixGlassCard(
        modifier = mod,
        shape = shape,
        content = content
    )
}

@Composable
fun SportynixBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SportynixGreenPrimary.copy(alpha = 0.15f),
    contentColor: Color = SportynixGreenPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
fun SportynixGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) PrimaryNeonGradient
                    else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

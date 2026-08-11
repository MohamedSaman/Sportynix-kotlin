package com.sportynix.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sportynix.app.presentation.theme.*

/**
 * Reusable Liquid Glass Card with spring interaction scale, top highlight gradient,
 * translucent background, and subtle green shadow glow.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalThemeController.current.isDark
    val defaultBg = if (isDark) GlassCardDark else GlassCardLight
    val defaultBorder = if (isDark) GlassBorderDark else GlassBorderLight
    val highlightBrush = if (isDark) GlassHighlightGradientDark else GlassHighlightGradientLight

    val finalBg = backgroundColor ?: defaultBg
    val finalBorder = borderColor ?: defaultBorder

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    var cardModifier = modifier
        .scale(cardScale)
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = if (isDark) NeonGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
            spotColor = if (isDark) SportynixGreenPrimary.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f)
        )
        .clip(shape)
        .background(finalBg)
        .border(width = borderWidth, color = finalBorder, shape = shape)

    if (onClick != null) {
        cardModifier = cardModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }

    Box(modifier = cardModifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(highlightBrush)
        )
        content()
    }
}

/**
 * Liquid Glass Button with spring scale feedback, green gradient background,
 * glowing shadow, and loading state support.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "glassBtnScale"
    )

    val alpha = if (enabled && !isLoading) 1.0f else 0.5f

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .scale(buttonScale)
            .shadow(
                elevation = if (enabled && !isLoading) 12.dp else 0.dp,
                shape = shape,
                ambientColor = NeonGreen.copy(alpha = 0.3f),
                spotColor = NeonGreen.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(PrimaryNeonGradient, alpha = alpha)
            .border(
                width = 1.dp,
                color = if (enabled && !isLoading) NeonGreen.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f),
                shape = shape
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Translucent Glass Text Field with animated border color on focus.
 */
@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false
) {
    val isDark = LocalThemeController.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> StatusError
            isFocused -> NeonGreen
            isDark -> GlassBorderDark
            else -> GlassBorderLight
        },
        label = "fieldBorder"
    )

    val containerBg = if (isDark) GlassSurfaceDark else GlassSurfaceLight

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        isError = isError,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            errorBorderColor = StatusError,
            focusedContainerColor = containerBg,
            unfocusedContainerColor = containerBg,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Translucent Glass Filter Chip with glowing emerald selection indicator.
 */
@Composable
fun LiquidGlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isDark = LocalThemeController.current.isDark
    val bgCol by animateColorAsState(
        targetValue = if (selected) NeonGreen else if (isDark) GlassCardDark else GlassCardLight,
        label = "chipBg"
    )
    val textCol by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgCol,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) NeonGreen else if (isDark) GlassBorderDark else GlassBorderLight
        ),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textCol,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = textCol,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Liquid Glass Search Bar with clear icon and debounced search support.
 */
@Composable
fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    LiquidGlassTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonGreen)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Status Badge component with glass border and colored indicators.
 */
@Composable
fun LiquidGlassBadge(
    text: String,
    badgeColor: Color = NeonGreen,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Liquid Glass Empty State Component.
 */
@Composable
fun LiquidGlassEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth().padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LiquidGlassButton(
                    text = actionText,
                    onClick = onActionClick
                )
            }
        }
    }
}

/**
 * Liquid Glass Error Card with retry button.
 */
@Composable
fun LiquidGlassErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        borderColor = StatusError.copy(alpha = 0.4f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Error Occurred", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StatusError)
                Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusError)
            ) {
                Text("Retry", color = StatusError, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Liquid Glass Dialog container with frosted glass overlay.
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 16.dp
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

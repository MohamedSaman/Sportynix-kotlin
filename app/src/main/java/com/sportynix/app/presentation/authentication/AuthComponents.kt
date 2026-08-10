package com.sportynix.app.presentation.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import com.sportynix.app.presentation.theme.PrimaryNeonGradient
import com.sportynix.app.R

@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = LocalThemeController.current.isDark
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF06100D), Color(0xFF09130F), Color(0xFF050907))
                    else listOf(Color(0xFFF8FCFA), Color(0xFFF1F9F5), Color(0xFFFBFDFC))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = SportynixGreenPrimary.copy(alpha = if (dark) 0.10f else 0.07f),
                radius = size.minDimension * .48f,
                center = androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .08f)
            )
            drawCircle(
                color = Color(0xFF59E6A7).copy(alpha = if (dark) 0.055f else 0.045f),
                radius = size.minDimension * .42f,
                center = androidx.compose.ui.geometry.Offset(size.width * .02f, size.height * .90f)
            )
        }
        content()
    }
}

@Composable
fun CircularBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalThemeController.current.isDark
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = if (dark) Color(0xB31A2923) else Color(0xCCFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, SportynixGreenPrimary.copy(alpha = .18f)),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (dark) Color.White else Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConcentricGlowIcon(
    icon: ImageVector? = null,
    logoRes: Int? = null,
    modifier: Modifier = Modifier
) {
    val dark = LocalThemeController.current.isDark
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        // Outer glow ring
        Box(
            Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0x1400E676) else Color(0x1A0D8A4F))
        )
        // Inner glow ring
        Box(
            Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0x2800E676) else Color(0x280D8A4F))
        )
        // Center icon/logo container
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0xFF181A1E) else Color.White)
                .border(1.5.dp, SportynixGreenPrimary.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = "Logo",
                    modifier = Modifier.size(48.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SportynixGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun AuthHeader(title: String, subtitle: String, icon: ImageVector? = null, logoRes: Int? = null) {
    val dark = LocalThemeController.current.isDark
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null || logoRes != null) {
            ConcentricGlowIcon(icon = icon, logoRes = logoRes)
            Spacer(Modifier.height(14.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            color = if (dark) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AuthCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (dark) Color(0xB814211C) else Color(0xD9FFFFFF),
        shadowElevation = if (dark) 3.dp else 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (dark) Color(0x4200E676) else Color(0x3310B981)
        )
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun PremiumAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable (() -> Unit))? = null
) {
    var focused by remember { mutableStateOf(false) }
    val dark = LocalThemeController.current.isDark
    val border by animateColorAsState(
        if (focused) SportynixGreenPrimary else SportynixGreenPrimary.copy(alpha = if (dark) .28f else .20f),
        label = "authFieldBorder"
    )
    val fieldColor by animateColorAsState(
        targetValue = when {
            dark && focused -> Color(0xA320342B)
            dark -> Color(0x7A203029)
            focused -> Color(0xF2F9FFFC)
            else -> Color.White.copy(alpha = 0.50f)
        },
        animationSpec = tween(180),
        label = "authFieldSurface"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (dark) Color.White else Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp
        ),
        cursorBrush = SolidColor(SportynixGreenPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(17.dp))
                    .background(fieldColor)
                    .border(if (focused) 1.5.dp else 1.dp, border, RoundedCornerShape(16.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    RoundedCornerShape(12.dp),
                    color = SportynixGreenPrimary.copy(alpha = if (dark) 0.16f else 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = SportynixGreenPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
                if (trailing != null) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) { trailing() }
                }
            }
        }
    )
}

@Composable
fun GoogleAuthButton(onClick: () -> Unit, loading: Boolean, modifier: Modifier = Modifier) {
    val dark = LocalThemeController.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, spring(stiffness = 650f), label = "googlePress")
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        interactionSource = interactionSource,
        enabled = !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) Color(0xC713211C) else Color(0xE6FFFFFF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (dark) Color(0x4200E676) else Color(0x3310B981)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = SportynixGreenPrimary
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(painterResource(R.drawable.google), "Google", Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) Color.White else Color(0xFF18181B),
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, spring(stiffness = 650f), label = "primaryPress")
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else .52f)
            .shadow(if (enabled) 12.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = SportynixGreenPrimary.copy(alpha = .35f))
            .background(PrimaryNeonGradient, RoundedCornerShape(16.dp)),
        interactionSource = interactionSource,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = SportynixGreenPrimary.copy(alpha = 0.45f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                if (showArrow) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    val dark = LocalThemeController.current.isDark
    BasicTextField(
        value = value,
        onValueChange = { input ->
            val digitsOnly = input.filter(Char::isDigit).take(6)
            onValueChange(digitsOnly)
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(6) { index ->
                    val char = value.getOrNull(index)?.toString().orEmpty()
                    val active = value.length == index || (index == 5 && value.length == 6)
                    val filled = char.isNotEmpty()

                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (dark) Color(0xC713211C) else Color(0xE6FFFFFF)
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) SportynixGreenPrimary
                                else if (filled) SportynixGreenPrimary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun NoticeBannerCard(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val dark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (dark) Color(0xC713211C) else Color(0xE6FFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SportynixGreenPrimary.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = SportynixGreenPrimary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Icon(icon, null, tint = SportynixGreenPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "G",
                            fontWeight = FontWeight.Bold,
                            color = SportynixGreenPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TipCard(title: String = "Tip", body: String, modifier: Modifier = Modifier) {
    val dark = LocalThemeController.current.isDark
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (dark) Color(0xC713211C) else Color(0xE6FFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SportynixGreenPrimary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = SportynixGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SportynixGreenPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun PasswordRequirementCard(password: String, confirmPassword: String? = null) {
    val dark = LocalThemeController.current.isDark
    val hasMinLength = password.length >= 8
    val hasUpper = password.any(Char::isUpperCase)
    val hasLower = password.any(Char::isLowerCase)
    val hasNumber = password.any(Char::isDigit)
    val matchesConfirm = confirmPassword == null || (confirmPassword.isNotEmpty() && password == confirmPassword)

    val items = mutableListOf(
        "At least 8 characters" to hasMinLength,
        "At least one uppercase letter" to hasUpper,
        "At least one lowercase letter" to hasLower,
        "At least one number" to hasNumber
    )
    if (confirmPassword != null) {
        items.add("Passwords match" to matchesConfirm)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (dark) Color(0xC713211C) else Color(0xE6FFFFFF),
        shadowElevation = if (dark) 3.dp else 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (dark) Color(0x1F00E676) else Color(0x14000000)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Password must contain:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else Color.Black
            )
            Spacer(Modifier.height(10.dp))
            items.forEach { (label, valid) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (valid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (valid) SportynixGreenPrimary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = if (valid) SportynixGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StepNumberBadge(number: String, label: String, isActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isActive) SportynixGreenPrimary else Color.Transparent)
                .border(
                    1.5.dp,
                    if (isActive) SportynixGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) SportynixGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun AuthMessage(message: String?, success: Boolean = false) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        message?.let {
            Surface(
                color = (if (success) SportynixGreenPrimary else MaterialTheme.colorScheme.error).copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    color = if (success) SportynixGreenPrimary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

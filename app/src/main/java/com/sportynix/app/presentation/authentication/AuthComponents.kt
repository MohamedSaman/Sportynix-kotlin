package com.sportynix.app.presentation.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.sportynix.app.R

@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = LocalThemeController.current.isDark
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(if (dark) listOf(Color(0xFF0B110D), Color(0xFF111115), Color(0xFF07150D))
            else listOf(Color(0xFFF0FDF4), Color(0xFFFFFFFF), Color(0xFFECFDF5)))
        ), content = content
    )
}

@Composable
fun AuthCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalThemeController.current.isDark
    Surface(
        modifier.shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = SportynixGreenPrimary.copy(alpha = .14f)),
        shape = RoundedCornerShape(24.dp),
        color = if (dark) Color(0xE618181B) else Color(0xEFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, SportynixGreenPrimary.copy(alpha = if (dark) .25f else .12f))
    ) { Column(Modifier.padding(22.dp), content = content) }
}

@Composable
fun AuthHeader(title: String, subtitle: String, icon: ImageVector? = null) {
    if (icon != null) Surface(Modifier.size(72.dp), CircleShape, SportynixGreenPrimary.copy(alpha = .10f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp), tint = SportynixGreenPrimary) }
    }
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(7.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp)
}

@Composable
fun AuthTextField(
    value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector,
    modifier: Modifier = Modifier, error: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None, trailing: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value, onValueChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(icon, null) }, trailingIcon = trailing, isError = error != null,
        supportingText = error?.let { { Text(it) } }, visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportynixGreenPrimary, focusedLeadingIconColor = SportynixGreenPrimary,
            focusedLabelColor = SportynixGreenPrimary
        )
    )
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
    val border = if (focused) SportynixGreenPrimary else SportynixGreenPrimary.copy(alpha = .32f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(64.dp).onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .72f))
                    .border(if (focused) 1.5.dp else 1.dp, border, RoundedCornerShape(18.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(Modifier.fillMaxHeight().aspectRatio(1f), RoundedCornerShape(14.dp), SportynixGreenPrimary.copy(alpha = .10f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = SportynixGreenPrimary) }
                }
                Box(Modifier.weight(1f).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    inner()
                }
                trailing?.invoke()
            }
        }
    )
}

@Composable
fun GoogleAuthButton(onClick: () -> Unit, loading: Boolean, modifier: Modifier = Modifier) {
    val dark = LocalThemeController.current.isDark
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (dark) Color(0xFF2E2E33) else Color(0xFFE2E8F0))
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.Image(painterResource(R.drawable.google), "Google", Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Continue with Google", fontWeight = FontWeight.SemiBold, color = if (dark) Color.White else Color.Black, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AppleAuthButton(onClick: () -> Unit, loading: Boolean, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        enabled = !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        )
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painterResource(R.drawable.apple), "Apple", Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Continue with Apple", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PremiumButton(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false, modifier: Modifier = Modifier) {
    Button(
        onClick, modifier.fillMaxWidth().height(56.dp), enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SportynixGreenPrimary)
    ) { if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
fun OtpInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    BasicTextField(
        value = value, onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) }, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(6) { index ->
                    val active = value.length == index
                    Box(
                        Modifier.weight(1f).aspectRatio(.86f).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(if (active) 2.dp else 1.dp, if (active) SportynixGreenPrimary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(value.getOrNull(index)?.toString().orEmpty(), fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    )
}

@Composable
fun PasswordChecklist(password: String) {
    val checks = listOf("At least 8 characters" to (password.length >= 8), "One uppercase letter" to password.any(Char::isUpperCase), "One lowercase letter" to password.any(Char::isLowerCase), "One number" to password.any(Char::isDigit))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { checks.forEach { (label, valid) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (valid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(16.dp), tint = if (valid) SportynixGreenPrimary else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(7.dp)); Text(label, style = MaterialTheme.typography.bodySmall, color = if (valid) SportynixGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } }
}

@Composable
fun AuthMessage(message: String?, success: Boolean = false) {
    AnimatedVisibility(message != null) { message?.let {
        Surface(color = (if (success) SportynixGreenPrimary else MaterialTheme.colorScheme.error).copy(alpha = .10f), shape = RoundedCornerShape(12.dp)) {
            Text(it, Modifier.fillMaxWidth().padding(12.dp), color = if (success) SportynixGreenPrimary else MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    } }
}

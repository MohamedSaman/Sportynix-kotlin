package com.sportynix.app.presentation.messages.components

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.domain.model.ChatMessage
import java.io.File
import java.util.Locale

// Futuristic Liquid Glass Color Palette for Sportynix
object LiquidGlassTheme {
    val PrimaryGreen = Color(0xFF10B981)
    val SecondaryGreen = Color(0xFF059669)
    val AccentGreen = Color(0xFF0D8A4F)
    val EmeraldGlow = Color(0x4010B981)

    val LightGlassBackground = Color(0xF7FFFFFF)
    val LightGlassBorder = Color(0x1F10B981)

    val DarkGlassBackground = Color(0xF218181B)
    val DarkGlassBorder = Color(0x1FFFFFFF)

    val LightBackground = Color(0xFFF4F6F8)
    val DarkBackground = Color(0xFF09090B)

    @Composable
    fun cardBackground(): Color {
        return if (isSystemInDarkTheme()) DarkGlassBackground else LightGlassBackground
    }

    @Composable
    fun cardBorder(): Color {
        return if (isSystemInDarkTheme()) DarkGlassBorder else LightGlassBorder
    }

    @Composable
    fun screenBackground(): Color {
        return if (isSystemInDarkTheme()) DarkBackground else LightBackground
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = LiquidGlassTheme.cardBackground()
    val border = LiquidGlassTheme.cardBorder()

    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = bg,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassBadge(
    text: String,
    containerColor: Color = LiquidGlassTheme.PrimaryGreen.copy(alpha = 0.15f),
    contentColor: Color = LiquidGlassTheme.PrimaryGreen
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PulsingRecordingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun VoiceMessagePlayer(
    audioUrlOrPath: String,
    durationSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioUrlOrPath) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioUrlOrPath)
                        prepareAsync()
                        setOnPreparedListener {
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            currentProgress = 0f
                        }
                    }
                } else {
                    mediaPlayer?.start()
                    isPlaying = true
                }
            } catch (e: Exception) {
                isPlaying = false
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { togglePlay() },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LiquidGlassTheme.PrimaryGreen)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause Voice",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = LiquidGlassTheme.PrimaryGreen,
                trackColor = LiquidGlassTheme.PrimaryGreen.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "%d:%02d", durationSeconds / 60, durationSeconds % 60),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

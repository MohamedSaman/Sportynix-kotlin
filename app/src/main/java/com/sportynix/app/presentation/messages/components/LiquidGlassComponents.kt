package com.sportynix.app.presentation.messages.components

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Futuristic Liquid Glass Color Palette for Sportynix
object LiquidGlassTheme {
    // Keep Messages on the exact same brand green used by Home and the app theme.
    val PrimaryGreen = SportynixGreenPrimary
    val SecondaryGreen = Color(0xFF059669)
    val AccentGreen = Color(0xFF0D8A4F)
    val EmeraldGlow = Color(0x4010B981)

    val LightGlassBackground = Color(0xDFFFFFFF)
    val LightGlassBorder = Color(0x3D10B981)

    val DarkGlassBackground = Color(0xD9161D1B)
    val DarkGlassBorder = Color(0x3310B981)

    val LightBackground = Color(0xFFF8FAF9)
    val DarkBackground = Color(0xFF070C16)

    val GreenGradient = Brush.linearGradient(listOf(Color(0xFF20D997), Color(0xFF08B875), Color(0xFF078F5D)))

    @Composable
    fun cardBackground(): Color {
        return if (MaterialTheme.colorScheme.background.luminance() < .5f) DarkGlassBackground else LightGlassBackground
    }

    @Composable
    fun cardBorder(): Color {
        return if (MaterialTheme.colorScheme.background.luminance() < .5f) DarkGlassBorder else LightGlassBorder
    }

    @Composable
    fun screenBackground(): Color {
        return MaterialTheme.colorScheme.background
    }
}

@Composable
fun PremiumMessagesBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    // Home uses MaterialTheme.background as its canvas. Messages uses the same
    // canvas, with only restrained glass highlights layered above it.
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.size(300.dp).offset(x = 190.dp, y = (-95).dp).blur(70.dp).background(LiquidGlassTheme.PrimaryGreen.copy(alpha = if (dark) .10f else .065f), CircleShape))
        Box(Modifier.size(240.dp).align(Alignment.BottomStart).offset(x = (-120).dp, y = 80.dp).blur(75.dp).background(LiquidGlassTheme.PrimaryGreen.copy(alpha = if (dark) .055f else .04f), CircleShape))
        content()
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(if (pressed) .982f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "glassPress")

    Surface(
        modifier = modifier
            .scale(cardScale)
            .clip(shape)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() } else Modifier),
        shape = shape,
        color = bg,
        shadowElevation = if (pressed) 1.dp else 7.dp,
        tonalElevation = 0.dp
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
    isOutgoing: Boolean = false,
    avatarUrl: String? = null,
    avatarFallback: String = "S",
    onResolveSource: (suspend () -> String?)? = null,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    var effectiveSource by remember(audioUrlOrPath) { mutableStateOf(audioUrlOrPath) }
    var isResolving by remember(audioUrlOrPath) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val active = VoicePlaybackCoordinator.activeSource == effectiveSource
    val isPlaying = active && VoicePlaybackCoordinator.isPlaying
    val currentProgress = if (active) VoicePlaybackCoordinator.progress else 0f
    val elapsedSeconds = if (active) VoicePlaybackCoordinator.elapsedSeconds else 0
    val totalSeconds = if (active && VoicePlaybackCoordinator.totalSeconds > 0) VoicePlaybackCoordinator.totalSeconds else durationSeconds
    val actionColor = when {
        isOutgoing && dark -> Color(0xFF39D69B)
        else -> LiquidGlassTheme.PrimaryGreen
    }
    val unplayedColor = when {
        isOutgoing && dark -> Color.White.copy(alpha = .24f)
        isOutgoing -> Color(0xFF6E8177).copy(alpha = .34f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .28f)
    }
    val timeColor = when {
        isOutgoing && dark -> Color.White.copy(alpha = .66f)
        isOutgoing -> Color(0xFF52645A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val playScale by animateFloatAsState(if (isPlaying) 1.06f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "voicePlayScale")

    LaunchedEffect(isPlaying, effectiveSource) {
        while (VoicePlaybackCoordinator.activeSource == effectiveSource && VoicePlaybackCoordinator.isPlaying) {
            VoicePlaybackCoordinator.updateProgress()
            delay(70)
        }
    }

    Row(
        modifier = modifier
            .widthIn(min = 220.dp, max = 292.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp)) {
            if (!avatarUrl.isNullOrBlank()) AsyncImage(avatarUrl, "Voice sender profile", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().clip(CircleShape).background(actionColor.copy(alpha=.18f)), contentAlignment=Alignment.Center) {
                Text(
                    avatarFallback.trim().firstOrNull()?.uppercase() ?: "S",
                    color = actionColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Surface(Modifier.size(16.dp).align(Alignment.BottomEnd), shape=CircleShape, color=actionColor) { Icon(Icons.Default.Mic, null, tint=Color.White, modifier=Modifier.padding(2.dp)) }
        }
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = {
                if (isResolving) return@IconButton
                scope.launch {
                    isResolving = true
                    val resolved = onResolveSource?.invoke() ?: audioUrlOrPath
                    isResolving = false
                    if (!resolved.isNullOrBlank()) { effectiveSource = resolved; VoicePlaybackCoordinator.toggle(resolved) }
                }
            },
            modifier = Modifier
                .size(34.dp)
                .scale(playScale)
                .clip(CircleShape)
                .background(actionColor)
        ) {
            Icon(
                imageVector = if (isResolving) Icons.Default.Downloading else if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause Voice",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(24) { index ->
                    val fraction = (index + 1) / 24f
                    val height = (6 + ((index * 11 + 5) % 15)).dp
                    val barColor by animateColorAsState(
                        if (fraction <= currentProgress) actionColor else unplayedColor,
                        tween(120), label = "voiceWave$index"
                    )
                    Box(Modifier.weight(1f).height(height).clip(RoundedCornerShape(2.dp)).background(barColor))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "%d:%02d / %d:%02d", elapsedSeconds / 60, elapsedSeconds % 60, totalSeconds / 60, totalSeconds % 60),
                fontSize = 11.sp,
                color = timeColor
            )
        }
    }
}

private object VoicePlaybackCoordinator {
    var activeSource by mutableStateOf<String?>(null)
    var isPlaying by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var elapsedSeconds by mutableIntStateOf(0)
    var totalSeconds by mutableIntStateOf(0)
    private var player: MediaPlayer? = null

    fun toggle(source: String) {
        if (source.isBlank()) return
        if (activeSource == source && player != null) {
            if (isPlaying) player?.pause() else player?.start()
            isPlaying = !isPlaying
            return
        }
        stop()
        activeSource = source
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(source)
                setOnPreparedListener { prepared ->
                    VoicePlaybackCoordinator.totalSeconds = (prepared.duration / 1000).coerceAtLeast(0)
                    prepared.start()
                    VoicePlaybackCoordinator.isPlaying = true
                }
                setOnCompletionListener { stop() }
                prepareAsync()
            }
        }.onFailure { stop() }
    }

    fun updateProgress() {
        val current = player ?: return
        if (current.duration > 0) {
            progress = (current.currentPosition.toFloat() / current.duration).coerceIn(0f, 1f)
            elapsedSeconds = current.currentPosition / 1000
        }
    }

    private fun stop() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        activeSource = null
        isPlaying = false
        progress = 0f
        elapsedSeconds = 0
        totalSeconds = 0
    }
}

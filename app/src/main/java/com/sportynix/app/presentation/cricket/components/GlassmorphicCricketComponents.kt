package com.sportynix.app.presentation.cricket.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.theme.*

val SportynixEmerald = Color(0xFF10B981)
val SportynixWicketRed = Color(0xFFEF4444)
val SportynixExtraOrange = Color(0xFFF59E0B)

@Composable
fun GlassScoreHeaderCard(
    liveState: LiveStateDto?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val activeInning = if (liveState?.currentInnings == 2) liveState.inning2 else liveState?.inning1

    SportynixGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        borderColor = SportynixEmerald.copy(alpha = 0.4f),
        backgroundColor = if (isDark) Color(0xFF111827).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Live indicator & Inning indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SportynixEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = (liveState?.status ?: "LIVE").uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SportynixEmerald
                    )
                }

                Text(
                    text = "Innings ${liveState?.currentInnings ?: 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Teams & Main Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Batting Team
                Column {
                    Text(
                        text = activeInning?.battingTeamName ?: "Batting Team",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${activeInning?.score ?: 0}/${activeInning?.wickets ?: 0}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = SportynixEmerald
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${activeInning?.overs ?: 0.0} ov)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Bowling Team / Target
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = activeInning?.bowlingTeamName ?: "Bowling Team",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeInning?.target != null) {
                        Text(
                            text = "Target: ${activeInning.target}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SportynixExtraOrange
                        )
                    }
                    Text(
                        text = "CRR: ${activeInning?.currentRunRate ?: 0.0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Display message / Toss text
            val message = liveState?.displayMessage ?: liveState?.resultText ?: liveState?.tossDecision?.let { "Toss won by ${liveState.tossWinnerId} - elected to $it" }
            if (!message.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SportynixEmerald.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = SportynixEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun GlassThisOverBar(
    recentBalls: List<String>,
    modifier: Modifier = Modifier
) {
    SportynixGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderColor = SportynixEmerald.copy(alpha = 0.25f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "THIS OVER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SportynixEmerald,
                modifier = Modifier.padding(end = 12.dp)
            )

            if (recentBalls.isEmpty()) {
                Text(
                    text = "No balls yet in this over",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(recentBalls) { ball ->
                        BallChip(ball)
                    }
                }
            }
        }
    }
}

@Composable
fun BallChip(ballText: String) {
    val bgColor = when {
        ballText.contains("W", ignoreCase = true) -> SportynixWicketRed
        ballText == "4" || ballText == "6" -> SportynixEmerald
        ballText.contains("Wd") || ballText.contains("Nb") -> SportynixExtraOrange
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (bgColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ballText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun GlassScoringPad(
    onRunClick: (Int) -> Unit,
    onWicketClick: () -> Unit,
    onExtrasClick: () -> Unit,
    onSwapClick: () -> Unit,
    onUndoClick: () -> Unit,
    isFreeHit: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isFreeHit) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SportynixExtraOrange)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FREE HIT BALL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Run Buttons Row 1: 0, 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 2, 3).forEach { runs ->
                RunButton(
                    runs = runs,
                    onClick = { onRunClick(runs) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Run Buttons Row 2: 4 (Boundary), 6 (Six), Extras, Wicket
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RunButton(
                runs = 4,
                onClick = { onRunClick(4) },
                enabled = enabled,
                isBoundary = true,
                modifier = Modifier.weight(1f)
            )
            RunButton(
                runs = 6,
                onClick = { onRunClick(6) },
                enabled = enabled,
                isSix = true,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "EXTRAS",
                color = SportynixExtraOrange,
                onClick = onExtrasClick,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = "WICKET",
                color = SportynixWicketRed,
                onClick = onWicketClick,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom Actions: Swap Batsmen & Undo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedActionButton(
                text = "Swap Batsmen",
                icon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = onSwapClick,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            OutlinedActionButton(
                text = "Undo Ball",
                icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = onUndoClick,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RunButton(
    runs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBoundary: Boolean = false,
    isSix: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(150),
        label = "runBtn"
    )

    val bgColor = when {
        isSix -> Brush.horizontalGradient(listOf(SportynixEmerald, Color(0xFF047857)))
        isBoundary -> Brush.horizontalGradient(listOf(Color(0xFF34D399), SportynixEmerald))
        else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
    }

    val textColor = if (isSix || isBoundary) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, if (isSix || isBoundary) SportynixEmerald else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = runs.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun OutlinedActionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SportynixEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SportynixEmerald
            )
        }
    }
}

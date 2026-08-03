package com.sportynix.app.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    var started by remember { mutableStateOf(false) }
    val entranceScale by animateFloatAsState(if (started) 1f else .5f, tween(800, easing = FastOutSlowInEasing), label = "splashScale")
    val entranceAlpha by animateFloatAsState(if (started) 1f else 0f, tween(800), label = "splashAlpha")
    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val ringScale by pulse.animateFloat(.92f, 1.08f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ringScale")
    val ringAlpha by pulse.animateFloat(.22f, .58f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "ringAlpha")
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val green = MaterialTheme.colorScheme.primary
    val logoRes = if (isDark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light

    LaunchedEffect(Unit) {
        started = true
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                SplashEvent.NavigateToHome -> onNavigateToHome()
                SplashEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Box(Modifier.size(320.dp).offset((-40).dp, (-150).dp).blur(90.dp).background(green.copy(alpha = .12f), CircleShape).scale(ringScale))
        Box(Modifier.size(280.dp).offset(60.dp, 150.dp).blur(80.dp).background(green.copy(alpha = .08f), CircleShape).scale(ringScale))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(180.dp).scale(ringScale).alpha(ringAlpha).border(2.dp, green.copy(alpha = .45f), CircleShape))
                Box(Modifier.size(140.dp).blur(20.dp).background(green.copy(alpha = .08f), CircleShape))
                Image(painterResource(logoRes), "Sportynix logo", Modifier.size(100.dp).scale(entranceScale).alpha(entranceAlpha))
            }
            Spacer(Modifier.height(28.dp))
            Text("Sportynix", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.alpha(entranceAlpha))
            Spacer(Modifier.height(8.dp))
            Text("Play  ·  Compete  ·  Win", fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = green.copy(alpha = .78f), modifier = Modifier.alpha(entranceAlpha))
        }
    }
}

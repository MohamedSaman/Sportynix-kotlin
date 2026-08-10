package com.sportynix.app.presentation.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.R
import com.sportynix.app.domain.repository.AuthRepository
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(repository: AuthRepository) : ViewModel() {
    val loggedIn = repository.isLoggedIn().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

@Composable
fun WelcomeScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onNavigateToHome: () -> Unit = onContinueAsGuest,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val loggedIn by viewModel.loggedIn.collectAsState()
    var countdown by remember { mutableIntStateOf(3) }
    var visible by remember { mutableStateOf(false) }
    val dark = LocalThemeController.current.isDark

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            delay(1800)
            onNavigateToHome()
        } else {
            countdown = 3
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            onNavigateToSignIn()
        }
    }

    AuthBackground {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500), initialOffsetY = { 40 })
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glow Rings Logo Container
                ConcentricGlowIcon(logoRes = if (dark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (loggedIn) "Welcome Back!" else "Sportynix",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = if (dark) Color.White else Color.Black
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (loggedIn) "Taking you to your dashboard..." else "Discover & book your favorite sports venues",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )

                if (!loggedIn) {
                    Spacer(Modifier.height(28.dp))

                    val features = listOf(
                        Triple(Icons.Outlined.Bolt, "Instant Booking", "Book courts in just a few taps"),
                        Triple(Icons.Outlined.Groups, "Sports Community", "Connect with fellow players"),
                        Triple(Icons.Outlined.QueryStats, "Activity Tracking", "Monitor your sports journey")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        features.forEach { (icon, title, body) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = if (dark) Color(0xFF181A1E) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (dark) Color(0x1F00E676) else Color(0x14000000)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = SportynixGreenPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(icon, null, tint = SportynixGreenPrimary, modifier = Modifier.size(22.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (dark) Color.White else Color.Black
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    PremiumButton(
                        text = "Get Started",
                        onClick = onNavigateToSignIn,
                        showArrow = true
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SportynixGreenPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Redirecting...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(Modifier.height(28.dp))
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            }
        }
    }
}

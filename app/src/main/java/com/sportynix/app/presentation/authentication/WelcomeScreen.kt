package com.sportynix.app.presentation.authentication

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.R
import com.sportynix.app.domain.repository.AuthRepository
import com.sportynix.app.presentation.theme.LocalThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel class WelcomeViewModel @Inject constructor(repository: AuthRepository) : ViewModel() {
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
    val dark = LocalThemeController.current.isDark
    LaunchedEffect(loggedIn) {
        if (loggedIn) { delay(2000); onNavigateToHome() }
        else { countdown = 3; while (countdown > 0) { delay(1000); countdown-- }; onNavigateToSignIn() }
    }
    AuthBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(if (dark) R.drawable.sportynix_logo_dark else R.drawable.sportynix_logo_light), "Sportynix", Modifier.size(104.dp))
            Spacer(Modifier.height(14.dp)); Text(if (loggedIn) "Welcome Back!" else "Sportynix", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(if (loggedIn) "Taking you to your dashboard..." else "Discover & book your favorite sports venues", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (!loggedIn) {
                Spacer(Modifier.height(34.dp))
                val features = listOf(Triple(Icons.Outlined.Bolt, "Instant Booking", "Book courts in just a few taps"), Triple(Icons.Outlined.Groups, "Sports Community", "Connect with fellow players"), Triple(Icons.Outlined.QueryStats, "Activity Tracking", "Monitor your sports journey"))
                AuthCard { features.forEachIndexed { i, (icon, title, body) -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; if (i < 2) HorizontalDivider(Modifier.padding(vertical = 12.dp)) } }
                Spacer(Modifier.height(28.dp)); PremiumButton("Get Started", onNavigateToSignIn)
                Spacer(Modifier.height(12.dp)); Text("Redirecting in ${countdown}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else { Spacer(Modifier.height(24.dp)); CircularProgressIndicator() }
        }
    }
}

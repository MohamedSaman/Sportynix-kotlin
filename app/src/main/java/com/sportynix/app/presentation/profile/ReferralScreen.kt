package com.sportynix.app.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sportynix.app.presentation.theme.*

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    var referralCode by mutableStateOf("")
        private set
    var friendsInvited by mutableStateOf("0")
        private set
    var pointsEarned by mutableStateOf("0")
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        loadReferrals()
    }

    fun loadReferrals() {
        viewModelScope.launch {
            isLoading = true
            val profileRes = profileRepository.fetchProfile()
            profileRes.onSuccess { u ->
                referralCode = u.referralCode.orEmpty()
            }
            val refRes = profileRepository.getReferrals()
            refRes.onSuccess { resp ->
                referralCode = resp.referralCode ?: referralCode
                friendsInvited = "${resp.referrals?.size ?: 0}"
                pointsEarned = "${resp.stats?.totalPoints ?: 0}"
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ReferralViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val steps = listOf(
        Triple(Icons.Default.Share, "Share Code", "Share your unique referral code with friends"),
        Triple(Icons.Default.People, "Friend Signs Up", "Your friend registers using your code"),
        Triple(Icons.Default.CardGiftcard, "Both Earn Rewards", "You both get bonus points on completed signup!")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referral", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isLoading && viewModel.referralCode.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentGreen)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // HERO HEADER
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(accentGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = accentGreen, modifier = Modifier.size(36.dp))
                        }
                        Text("Refer & Earn", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(
                            "Invite friends to Sportynix and earn rewards for every successful referral!",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // REFERRAL CODE CARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Your Referral Code", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)

                            Text(
                                text = viewModel.referralCode.ifBlank { "SPORTYNIX" },
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentGreen,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Copy Button
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Referral Code", viewModel.referralCode)
                                        clipboard.setPrimaryClip(clip)
                                        isCopied = true
                                        coroutineScope.launch {
                                            delay(2000)
                                            isCopied = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCopied) "Copied!" else "Copy Code", fontWeight = FontWeight.Bold)
                                }

                                // Share Button
                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join me on Sportynix! Use my referral code: ${viewModel.referralCode} to get bonus points!"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentGreen),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = accentGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", fontWeight = FontWeight.Bold, color = accentGreen)
                                }
                            }
                        }
                    }

                    // HOW IT WORKS
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("How It Works", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        steps.forEachIndexed { index, (icon, title, desc) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = cardColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(accentGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Step ${index + 1}: $title", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text(desc, fontSize = 13.sp, color = textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // STATS CARDS
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            value = viewModel.friendsInvited,
                            label = "Friends Invited",
                            icon = Icons.Default.People,
                            cardColor = cardColor,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            value = viewModel.pointsEarned,
                            label = "Points Earned",
                            icon = Icons.Default.Star,
                            cardColor = cardColor,
                            borderColor = borderColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentGreen = accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(24.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(label, fontSize = 12.sp, color = textSecondary)
        }
    }
}

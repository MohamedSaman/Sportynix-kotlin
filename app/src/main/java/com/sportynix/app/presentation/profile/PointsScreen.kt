package com.sportynix.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.dto.PointsHistoryItemDto
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.sportynix.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class PointsTransactionItem(
    val id: Int,
    val title: String,
    val category: String,
    val points: Int,
    val isEarned: Boolean,
    val date: String,
    val icon: ImageVector
)

data class PointsReward(
    val name: String,
    val description: String,
    val requiredPoints: Int,
    val isUnlocked: Boolean
)

data class PointsPenalty(
    val id: Int,
    val points: Int,
    val reason: String
)

@HiltViewModel
class PointsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var totalPoints by mutableIntStateOf(0)
        private set
    var earnedPoints by mutableIntStateOf(0)
        private set
    var redeemedPoints by mutableIntStateOf(0)
        private set
    var history by mutableStateOf<List<PointsTransactionItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var historyFilter by mutableStateOf("All") // "All", "Earned", "Spent"
        private set
    var showAllHistory by mutableStateOf(false)
        private set

    init {
        loadPointsData()
    }

    fun loadPointsData() {
        viewModelScope.launch {
            isLoading = true
            val profileRes = profileRepository.fetchProfile()
            profileRes.onSuccess { u ->
                totalPoints = u.points ?: 0
            }

            val historyRes = profileRepository.getPointsHistory(100)
            historyRes.onSuccess { resp ->
                val mapped = resp.results.map { item -> mapHistoryItem(item) }
                val sorted = mapped.sortedByDescending { it.id }

                earnedPoints = resp.summary?.totalEarned ?: sorted.filter { it.isEarned }.sumOf { it.points }
                redeemedPoints = resp.summary?.totalSpent ?: sorted.filter { !it.isEarned }.sumOf { it.points }
                history = sorted
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    fun setFilter(filter: String) {
        historyFilter = filter
    }

    fun toggleShowAllHistory() {
        showAllHistory = !showAllHistory
    }

    private fun mapHistoryItem(item: PointsHistoryItemDto): PointsTransactionItem {
        val label = if (!item.reasonLabel.isNullOrBlank()) item.reasonLabel else item.reason ?: "Points transaction"
        val key = label.lowercase()
        val category: String
        val icon: ImageVector
        when {
            key.contains("booking") -> {
                category = "Booking"
                icon = Icons.Default.EventAvailable
            }
            key.contains("referral") -> {
                category = "Referral"
                icon = Icons.Default.People
            }
            key.contains("penalty") || key.contains("cancellation") || key.contains("no_show") -> {
                category = "Penalty"
                icon = Icons.Default.Warning
            }
            key.contains("signup") -> {
                category = "Signup"
                icon = Icons.Default.AutoAwesome
            }
            else -> {
                category = "General"
                icon = Icons.Default.CreditCard
            }
        }
        val isEarned = item.direction.equals("earned", ignoreCase = true) || item.amount >= 0
        return PointsTransactionItem(
            id = item.id,
            title = label,
            category = category,
            points = kotlin.math.abs(item.amount),
            isEarned = isEarned,
            date = formatDate(item.createdAt),
            icon = icon
        )
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val d = inputFormat.parse(dateStr) ?: return dateStr
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            outputFormat.format(d)
        } catch (e: Exception) {
            dateStr
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PointsViewModel = hiltViewModel()
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    val totalPoints = viewModel.totalPoints
    val rankInfo = remember(totalPoints) {
        when {
            totalPoints >= 5000 -> "Diamond" to Color(0xFF06B6D4)
            totalPoints >= 3000 -> "Gold" to Color(0xFFF59E0B)
            totalPoints >= 1500 -> "Silver" to Color(0xFF94A3B8)
            else -> "Bronze" to Color(0xFFB45309)
        }
    }

    val milestones = listOf(1500, 3000, 5000)
    val nextMilestone = milestones.firstOrNull { it > totalPoints } ?: 5000
    val progress = (totalPoints.toFloat() / nextMilestone.toFloat()).coerceIn(0f, 1f)

    val rewards = listOf(
        PointsReward("Badminton", "Badminton court booking", 1500, totalPoints >= 1500),
        PointsReward("Futsal/Cricket", "Futsal/Cricket slot", 3000, totalPoints >= 3000),
        PointsReward("Premium", "Premium court booking + drink", 5000, totalPoints >= 5000)
    )

    val penalties = listOf(
        PointsPenalty(1, 50, "Booking cancellation (>2 hours before start)"),
        PointsPenalty(2, 150, "Late cancellation (<2 hours before start)"),
        PointsPenalty(3, 200, "No-show penalty")
    )

    val filteredHistory = remember(viewModel.history, viewModel.historyFilter) {
        when (viewModel.historyFilter) {
            "Earned" -> viewModel.history.filter { it.isEarned }
            "Spent" -> viewModel.history.filter { !it.isEarned }
            else -> viewModel.history
        }
    }

    val visibleHistory = if (viewModel.showAllHistory) filteredHistory else filteredHistory.take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Points & Rewards", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            if (viewModel.isLoading && viewModel.history.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentGreen)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // HERO CARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF0F2B1D), Color(0xFF06140D))
                                    )
                                )
                                .border(1.dp, accentGreen.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("TOTAL POINTS BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                                        Text("$totalPoints pts", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = accentGreen)
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = rankInfo.second.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, rankInfo.second.copy(alpha = 0.4f))
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = rankInfo.second, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(rankInfo.first, color = rankInfo.second, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Milestone Progress
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color = accentGreen,
                                        trackColor = Color.White.copy(alpha = 0.15f)
                                    )
                                    Text(
                                        text = "${nextMilestone - totalPoints} pts to unlock next milestone ($nextMilestone pts)",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }

                                // Stats Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Earned", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text("+$viewModel.earnedPoints pts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentGreen)
                                    }
                                    Column {
                                        Text("Total Spent", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text("-$viewModel.redeemedPoints pts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }

                    // REWARDS GRID
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AVAILABLE REWARDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
                        rewards.forEach { reward ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = cardColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (reward.isUnlocked) accentGreen.copy(alpha = 0.4f) else borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reward.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text(reward.description, fontSize = 13.sp, color = textSecondary)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (reward.isUnlocked) accentGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (reward.isUnlocked) "Unlocked" else "${reward.requiredPoints} pts",
                                            color = if (reward.isUnlocked) accentGreen else textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // PENALTIES SECTION
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PENALTIES & DEDUCTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
                        penalties.forEach { penalty ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = cardColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(penalty.reason, fontSize = 13.sp, color = textPrimary, modifier = Modifier.weight(1f))
                                    Text("-${penalty.points} pts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }

                    // TRANSACTION HISTORY
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TRANSACTION HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)

                            // Filter Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("All", "Earned", "Spent").forEach { filterOpt ->
                                    val isSelected = viewModel.historyFilter == filterOpt
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) accentGreen else cardColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) accentGreen else borderColor),
                                        modifier = Modifier.clickable { viewModel.setFilter(filterOpt) }
                                    ) {
                                        Text(
                                            text = filterOpt,
                                            color = if (isSelected) Color.White else textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (visibleHistory.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = cardColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Text("No transactions recorded yet.", fontSize = 13.sp, color = textSecondary, modifier = Modifier.padding(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        } else {
                            visibleHistory.forEach { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(item.icon, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                                            }
                                            Column {
                                                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                                Text(item.date, fontSize = 11.sp, color = textSecondary)
                                            }
                                        }

                                        Text(
                                            text = if (item.isEarned) "+${item.points} pts" else "-${item.points} pts",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isEarned) accentGreen else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }

                            if (filteredHistory.size > 5) {
                                TextButton(
                                    onClick = { viewModel.toggleShowAllHistory() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = if (viewModel.showAllHistory) "Show Less" else "Show All History (${filteredHistory.size})",
                                        color = accentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

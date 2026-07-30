package com.sportynix.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.ReportItemDto
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.sportynix.app.presentation.theme.*

@HiltViewModel
class ReportedUsersViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var reports by mutableStateOf<List<ReportItemDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var cancellingIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = profileRepository.getReports()
            result.onSuccess { list ->
                reports = list
                isLoading = false
            }.onFailure { err ->
                isLoading = false
                errorMessage = err.message ?: "Failed to load reports"
            }
        }
    }

    fun cancelReport(reportId: Int) {
        val set = cancellingIds.toMutableSet()
        set.add(reportId)
        cancellingIds = set

        viewModelScope.launch {
            val result = profileRepository.cancelReport(reportId)
            if (result.isSuccess) {
                reports = reports.filter { it.id != reportId }
            }
            val doneSet = cancellingIds.toMutableSet()
            doneSet.remove(reportId)
            cancellingIds = doneSet
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedUsersScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ReportedUsersViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reported Users", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            if (viewModel.isLoading && viewModel.reports.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentGreen)
            } else if (!viewModel.errorMessage.isNullOrBlank() && viewModel.reports.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(44.dp))
                    Text(viewModel.errorMessage!!, fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center)
                    Button(onClick = viewModel::loadData, colors = ButtonDefaults.buttonColors(containerColor = accentGreen)) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (viewModel.reports.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Text("No Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("You haven't submitted any user reports yet.", fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(viewModel.reports, key = { it.id }) { report ->
                        val isCancelling = viewModel.cancellingIds.contains(report.id)
                        val statusInfo = getStatusInfo(report.status)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = cardColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!report.reportedUserProfilePicture.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = report.reportedUserProfilePicture,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                val initial = report.reportedUserName?.take(1) ?: report.reportedUserUsername?.take(1) ?: "U"
                                                Text(initial.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentGreen)
                                            }
                                        }

                                        Column {
                                            Text(report.reportedUserName ?: report.reportedUserUsername ?: "Unknown User", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                            report.createdAt?.let { dateStr ->
                                                Text(formatDate(dateStr), fontSize = 11.sp, color = textSecondary)
                                            }
                                        }
                                    }

                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusInfo.second.copy(alpha = 0.12f)
                                    ) {
                                        Text(statusInfo.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusInfo.second, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }

                                HorizontalDivider(color = borderColor)

                                // Reason & Notes
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (!report.reason.isNullOrBlank()) {
                                        Text("Reason: ${report.reason}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                    }
                                    if (!report.notes.isNullOrBlank()) {
                                        Text(report.notes, fontSize = 13.sp, color = textSecondary, fontStyle = FontStyle.Italic)
                                    }
                                    if (!report.reviewedNote.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(backgroundColor).padding(8.dp)
                                        ) {
                                            Column {
                                                Text("Moderator Note:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                                Text(report.reviewedNote, fontSize = 12.sp, color = textSecondary)
                                            }
                                        }
                                    }
                                }

                                // Cancel Action
                                if (report.canCancel == true) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(
                                            onClick = { viewModel.cancelReport(report.id) },
                                            enabled = !isCancelling
                                        ) {
                                            if (isCancelling) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Red)
                                            else Text("Cancel Report", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val d = iso.parse(dateString) ?: return dateString
        val out = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        out.format(d)
    } catch (e: Exception) {
        dateString
    }
}

private fun getStatusInfo(status: String?): Pair<String, Color> {
    return when (status?.lowercase()) {
        "pending" -> "Pending Review" to Color(0xFFF59E0B)
        "in_review" -> "In Review" to Color(0xFF3B82F6)
        "resolved" -> "Reviewed" to Color(0xFF10B981)
        "rejected" -> "Rejected" to Color(0xFFEF4444)
        else -> (status?.replaceFirstChar { it.uppercase() } ?: "Unknown") to Color.Gray
    }
}

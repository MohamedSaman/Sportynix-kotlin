package com.sportynix.app.presentation.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.data.remote.dto.LeaguePlayerApplicationDto
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguePlayerApplicationsScreen(
    leagueId: String,
    onNavigateBack: () -> Unit,
    viewModel: LeaguePlayerApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground

    LaunchedEffect(leagueId) {
        viewModel.loadApplications(leagueId)
    }

    val filters = listOf("pending" to "Pending", "approved" to "Approved", "rejected" to "Rejected", "all" to "All")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Applications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        bottomBar = {
            if (uiState.selectedAppIds.isNotEmpty()) {
                Surface(
                    color = if (isDark) DarkSurface else LightSurface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${uiState.selectedAppIds.size} Selected", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.bulkReview("rejected") },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                            ) {
                                Text("Reject All")
                            }
                            Button(
                                onClick = { viewModel.bulkReview("approved") },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                            ) {
                                Text("Approve All")
                            }
                        }
                    }
                }
            }
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(filters) { (key, label) ->
                    val selected = uiState.selectedFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            } else if (uiState.filteredApplications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No applications found", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.filteredApplications, key = { it.id }) { app ->
                        val isSelected = uiState.selectedAppIds.contains(app.id)
                        ApplicationCardItem(
                            app = app,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleAppSelection(app.id) },
                            onApprove = { viewModel.reviewSingle(app.id, "approved") },
                            onReject = { viewModel.reviewSingle(app.id, "rejected") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationCardItem(
    app: LeaguePlayerApplicationDto,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AnimatedGlassCard(onClick = onToggleSelect) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.user.fullName ?: app.user.name ?: "Applicant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!app.cricketPrimaryRole.isNull_or_empty()) {
                        SportynixBadge(text = app.cricketPrimaryRole!!.uppercase())
                    }
                    if (!app.cricketPreferredVariant.isNull_or_empty()) {
                        Text(app.cricketPreferredVariant!!, style = MaterialTheme.typography.bodySmall, color = SportynixGreenPrimary)
                    }
                }
            }

            if (app.status.lowercase() == "pending") {
                Row {
                    IconButton(onClick = onReject) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = StatusError)
                    }
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = StatusSuccess)
                    }
                }
            } else {
                SportynixBadge(
                    text = app.status.uppercase(),
                    backgroundColor = if (app.status.lowercase() == "approved") StatusSuccess.copy(0.2f) else StatusError.copy(0.2f),
                    contentColor = if (app.status.lowercase() == "approved") StatusSuccess else StatusError
                )
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

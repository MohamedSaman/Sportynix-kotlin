package com.sportynix.app.presentation.tournaments

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.FullTournamentDto
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTournamentDetail: (String) -> Unit,
    onNavigateToCreateTournament: () -> Unit = {},
    viewModel: TournamentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme

    val sports = listOf("All", "Cricket", "Football", "Basketball", "Volleyball")
    val statuses = listOf("All", "Applications Open", "Upcoming", "Ongoing", "Completed")

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onNavigateBack,
                        shape = CircleShape,
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.7f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Tournaments & Cups",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Discover and compete in championship tournaments",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = onNavigateToCreateTournament,
                        shape = CircleShape,
                        color = green.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.4f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Tournament",
                                tint = green,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToCreateTournament,
                    containerColor = green,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Tournament", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            containerColor = bg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Row
                LiquidGlassTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = "Search tournaments by name or location...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                // Sport Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(sports) { sport ->
                        val isSelected = state.selectedSportFilter == sport
                        LiquidGlassFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onSportFilterChanged(sport) },
                            label = sport
                        )
                    }
                }

                // Status Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(statuses) { status ->
                        val isSelected = state.selectedStatusFilter == status
                        LiquidGlassFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onStatusFilterChanged(status) },
                            label = status
                        )
                    }
                }

                if (state.isLoading && state.tournaments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green)
                    }
                } else if (state.tournaments.isEmpty()) {
                    LiquidGlassEmptyState(
                        title = "No Tournaments Available",
                        description = "Create a new tournament or check back soon for open championship cups.",
                        icon = Icons.Outlined.EmojiEvents
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(state.tournaments, key = { it.id }) { tournament ->
                            RichTournamentCard(
                                tournament = tournament,
                                green = green,
                                onClick = { onNavigateToTournamentDetail(tournament.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichTournamentCard(
    tournament: FullTournamentDto,
    green: Color,
    onClick: () -> Unit
) {
    val isDark = LocalThemeController.current.isDark

    val effectiveStatus = tournament.effectiveStatus ?: tournament.status
    val statusColor = when (effectiveStatus.lowercase()) {
        "applications_open", "ongoing", "live", "approved" -> green
        "applications_closed", "finalized" -> Color(0xFF3B82F6)
        "pending_approval", "pending" -> AccentGold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Optional banner preview
            if (!tournament.banner.isNullOrBlank() || !tournament.image.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    AsyncImage(
                        model = tournament.banner ?: tournament.image,
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sport Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(green.copy(alpha = 0.15f))
                        .border(1.dp, green.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.displayTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${tournament.actualSportType.replaceFirstChar { it.uppercase() }} • ${tournament.format.replace("_", " ").uppercase()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LiquidGlassBadge(
                    text = effectiveStatus.replace("_", " ").uppercase(),
                    badgeColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Teams: ${tournament.actualApprovedCount}/${tournament.actualTeamCapacity}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val prize = tournament.prizePool?.toString() ?: "Trophy"
                Text(
                    text = "Prize: $prize",
                    fontWeight = FontWeight.ExtraBold,
                    color = green,
                    fontSize = 12.sp
                )
            }

            if (!tournament.startDate.isNullOrBlank() || !tournament.applicationDeadline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!tournament.startDate.isNullOrBlank()) {
                        Text(
                            text = "Starts: ${tournament.startDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!tournament.applicationDeadline.isNullOrBlank()) {
                        Text(
                            text = "Deadline: ${tournament.applicationDeadline.take(10)}",
                            fontSize = 11.sp,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

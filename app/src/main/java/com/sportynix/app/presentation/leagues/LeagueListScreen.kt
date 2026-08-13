package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsCricket
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.FullLeagueDto
import com.sportynix.app.presentation.components.AnimatedGlassCard
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixBadge
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground

    val sportsList = listOf(
        "all" to "All Sports",
        "cricket" to "Cricket",
        "football" to "Football",
        "volleyball" to "Volleyball",
        "basketball" to "Basketball"
    )

    val statusList = listOf(
        "all" to "All Status",
        "upcoming" to "Upcoming",
        "in_progress" to "Live",
        "registration" to "Registration",
        "completed" to "Completed"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Leagues & Tournaments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SportynixGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create League",
                                tint = Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search leagues, venues, formats...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SportynixGreenPrimary) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportynixGreenPrimary,
                    unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorderLight,
                    focusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
                    unfocusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
                ),
                singleLine = true
            )

            // Filter Chips - Sports
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(sportsList) { (key, label) ->
                    val isSelected = uiState.selectedSport == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSportSelected(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SportynixGreenPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
                            labelColor = if (isDark) TextPrimaryDark else TextPrimaryLight
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isDark) GlassBorderDark else GlassBorderLight,
                            selectedBorderColor = SportynixGreenPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(statusList) { (key, label) ->
                    val isSelected = uiState.selectedStatus == key
                    SuggestionChip(
                        onClick = { viewModel.onStatusSelected(key) },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) SportynixGreenPrimary.copy(alpha = 0.2f) else Color.Transparent,
                            labelColor = if (isSelected) SportynixGreenPrimary else (if (isDark) TextSecondaryDark else TextSecondaryLight)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) SportynixGreenPrimary else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Content List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            } else if (uiState.leagues.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No leagues found",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + above to create a new league!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SportynixGreenPrimary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.leagues, key = { it.id }) { league ->
                        LeagueCardItem(league = league, onClick = { onNavigateToDetail(league.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun LeagueCardItem(
    league: FullLeagueDto,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AnimatedGlassCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo Image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SportynixGreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!league.logo.isNullOrEmpty()) {
                    AsyncImage(
                        model = league.logo,
                        contentDescription = league.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = null,
                        tint = SportynixGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = league.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SportynixBadge(
                        text = league.status.uppercase(),
                        backgroundColor = when (league.status.lowercase()) {
                            "in_progress", "live" -> StatusSuccess.copy(alpha = 0.2f)
                            "upcoming" -> StatusWarning.copy(alpha = 0.2f)
                            else -> SportynixGreenPrimary.copy(alpha = 0.15f)
                        },
                        contentColor = when (league.status.lowercase()) {
                            "in_progress", "live" -> StatusSuccess
                            "upcoming" -> StatusWarning
                            else -> SportynixGreenPrimary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${league.sportType.capitalize()} • ${league.format.replace("_", " ").capitalize()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight
                    )
                    if (!league.cricketVariant.isNullOrEmpty()) {
                        Text(
                            text = " • ${league.cricketVariant}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SportynixGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${league.teamsCount ?: league.numTeams} Teams",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight
                    )
                    val dateText = league.startDate ?: ""
                    if (dateText.isNotEmpty()) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }
            }
        }
    }
}

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

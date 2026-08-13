package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.SportynixGlassCard
import com.sportynix.app.presentation.components.SportynixGradientButton
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueCreateScreen(
    onNavigateBack: () -> Unit,
    leagueId: String? = null,
    viewModel: LeagueCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground
    val context = LocalContext.current

    LaunchedEffect(leagueId) {
        if (!leagueId.isNullOrEmpty()) {
            viewModel.initForEdit(leagueId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit League" else "Create League",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (isDark) TextPrimaryDark else TextPrimaryLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        bottomBar = {
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
                    if (uiState.currentStep > 1) {
                        OutlinedButton(
                            onClick = viewModel::prevStep,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    val isLastStep = uiState.currentStep == 6
                    SportynixGradientButton(
                        text = if (isLastStep) (if (uiState.isEditMode) "Update League" else "Create League") else "Next Step",
                        onClick = {
                            if (isLastStep) {
                                viewModel.submitLeague(context)
                            } else {
                                viewModel.nextStep()
                            }
                        },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
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
                .verticalScroll(rememberScrollState())
        ) {
            // Stepper Header
            StepperProgressHeader(currentStep = uiState.currentStep)

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.error != null) {
                SportynixGlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    borderColor = StatusError
                ) {
                    Text(text = uiState.error!!, color = StatusError, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Step Content
            AnimatedContent(
                targetState = uiState.currentStep,
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1BasicInfo(uiState = uiState, viewModel = viewModel)
                    2 -> Step2VenueSelection(uiState = uiState, viewModel = viewModel)
                    3 -> Step3CricketSettings(uiState = uiState, viewModel = viewModel)
                    4 -> Step4TeamConfiguration(uiState = uiState, viewModel = viewModel)
                    5 -> Step5ScheduleAndDates(uiState = uiState, viewModel = viewModel)
                    6 -> Step6AdvancedRules(uiState = uiState, viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepperProgressHeader(currentStep: Int) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..6).forEach { step ->
            val isCompleted = step < currentStep
            val isCurrent = step == currentStep

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> SportynixGreenPrimary
                            isCompleted -> SportynixGreenPrimary.copy(alpha = 0.4f)
                            else -> if (isDark) GlassSurfaceDark else GlassSurfaceLight
                        }
                    )
                    .border(
                        1.dp,
                        if (isCurrent) SportynixGreenPrimary else (if (isDark) GlassBorderDark else GlassBorderLight),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = "$step",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCurrent) Color.Black else (if (isDark) TextPrimaryDark else TextPrimaryLight),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (step < 6) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (step < currentStep) SportynixGreenPrimary else Color.Gray.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1BasicInfo(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 1: Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = { viewModel.updateBasicInfo(it, uiState.description, uiState.sportType, uiState.format) },
            label = { Text("League Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateBasicInfo(uiState.name, it, uiState.sportType, uiState.format) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sport Type", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf("cricket", "football", "volleyball").forEach { sport ->
                val selected = uiState.sportType == sport
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateBasicInfo(uiState.name, uiState.description, sport, uiState.format) },
                    label = { Text(sport.capitalize()) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Format", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf("round_robin" to "Round Robin", "knockout" to "Knockout").forEach { (fmt, label) ->
                val selected = uiState.format == fmt
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateBasicInfo(uiState.name, uiState.description, uiState.sportType, fmt) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun Step2VenueSelection(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 2: Venue Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Is Hosted at Registered Venue?")
            Switch(
                checked = uiState.isVenueHosted,
                onCheckedChange = { viewModel.updateVenueConfig(it, uiState.primaryVenueId, uiState.customVenueText) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.customVenueText,
            onValueChange = { viewModel.updateVenueConfig(uiState.isVenueHosted, uiState.primaryVenueId, it) },
            label = { Text("Venue Name / Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun Step3CricketSettings(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 3: Cricket Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Cricket Ball Variant", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf("softball" to "Soft Ball", "hardball" to "Hard Ball").forEach { (v, label) ->
                val selected = uiState.cricketVariant == v
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateCricketSettings(v, uiState.overs, uiState.ballsPerOver, uiState.powerplayOvers, uiState.deathOvers) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Overs Per Innings: ${uiState.overs}")
        Slider(
            value = uiState.overs.toFloat(),
            onValueChange = { viewModel.updateCricketSettings(uiState.cricketVariant, it.toInt(), uiState.ballsPerOver, uiState.powerplayOvers, uiState.deathOvers) },
            valueRange = 1f..50f,
            steps = 49
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Balls Per Over: ${uiState.ballsPerOver} (Clamped 4–6)")
        Slider(
            value = uiState.ballsPerOver.toFloat(),
            onValueChange = { viewModel.updateCricketSettings(uiState.cricketVariant, uiState.overs, it.toInt().coerceIn(4, 6), uiState.powerplayOvers, uiState.deathOvers) },
            valueRange = 4f..6f,
            steps = 1
        )
    }
}

@Composable
fun Step4TeamConfiguration(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 4: Teams & Squad Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Number of Teams: ${uiState.numTeams}")
            Row {
                IconButton(onClick = { viewModel.updateNumTeams(uiState.numTeams - 1) }) { Text("-") }
                IconButton(onClick = { viewModel.updateNumTeams(uiState.numTeams + 1) }) { Text("+") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        uiState.teamsList.forEachIndexed { idx, team ->
            OutlinedTextField(
                value = team.name,
                onValueChange = { viewModel.updateTeamItem(idx, it, team.shortName, team.jerseyColor) },
                label = { Text("Team ${idx + 1} Name") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun Step5ScheduleAndDates(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 5: Schedule & Important Dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.startDate,
            onValueChange = { viewModel.updateDates(uiState.registrationStart, uiState.registrationEnd, it, uiState.endDate) },
            label = { Text("Start Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.endDate,
            onValueChange = { viewModel.updateDates(uiState.registrationStart, uiState.registrationEnd, uiState.startDate, it) },
            label = { Text("End Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun Step6AdvancedRules(uiState: LeagueCreateUiState, viewModel: LeagueCreateViewModel) {
    SportynixGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Step 6: Prize Pool & Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.prizePool,
            onValueChange = { viewModel.updateAdvanced(it, uiState.rulesText, uiState.isPublic, uiState.isFeatured) },
            label = { Text("Prize Pool Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.rulesText,
            onValueChange = { viewModel.updateAdvanced(uiState.prizePool, it, uiState.isPublic, uiState.isFeatured) },
            label = { Text("Rules & Guidelines") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Public League")
            Switch(
                checked = uiState.isPublic,
                onCheckedChange = { viewModel.updateAdvanced(uiState.prizePool, uiState.rulesText, it, uiState.isFeatured) }
            )
        }
    }
}

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

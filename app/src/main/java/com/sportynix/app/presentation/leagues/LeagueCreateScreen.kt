package com.sportynix.app.presentation.leagues

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueCreateScreen(
    onNavigateBack: () -> Unit,
    leagueId: String? = null,
    viewModel: LeagueCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme
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
                                imageVector = Icons.Default.ArrowBack,
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
                            text = if (uiState.isEditMode) "Edit League" else "Create League",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "STEP ${uiState.currentStep} OF 6",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = green,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = if (isDark) DarkSurface else LightSurface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.currentStep > 1) {
                            OutlinedButton(
                                onClick = viewModel::prevStep,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        val isLastStep = uiState.currentStep == 6
                        Button(
                            onClick = {
                                if (isLastStep) {
                                    viewModel.submitLeague(context)
                                } else {
                                    viewModel.nextStep()
                                }
                            },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(if (uiState.currentStep > 1) 1.5f else 1f).height(48.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isLastStep) (if (uiState.isEditMode) "Update League" else "Create League") else "Next Step",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Stepper Progress Header
                StepperProgressHeader(
                    currentStep = uiState.currentStep,
                    green = green
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.error != null) {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        borderColor = StatusError
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = uiState.error!!, color = StatusError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Animated Step Content
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + slideInHorizontally { 30 })
                            .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally { -30 })
                    },
                    label = "stepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1BasicInfo(uiState = uiState, green = green, viewModel = viewModel)
                        2 -> Step2VenueSelection(uiState = uiState, green = green, viewModel = viewModel)
                        3 -> Step3CricketSettings(uiState = uiState, green = green, viewModel = viewModel)
                        4 -> Step4TeamConfiguration(uiState = uiState, green = green, viewModel = viewModel)
                        5 -> Step5ScheduleAndDates(uiState = uiState, green = green, viewModel = viewModel)
                        6 -> Step6AdvancedRules(uiState = uiState, green = green, viewModel = viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StepperProgressHeader(
    currentStep: Int,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark
    val stepLabels = listOf("Basic", "Venue", "Sport", "Teams", "Dates", "Rules")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..6).forEach { step ->
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> green
                                isCompleted -> green.copy(alpha = 0.35f)
                                else -> if (isDark) DarkSurfaceVariant else LightSurfaceVariant
                            }
                        )
                        .border(
                            1.dp,
                            if (isCurrent) green else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = "$step",
                            fontSize = 12.sp,
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (step < 6) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 4.dp)
                            .background(if (step < currentStep) green else Color.Gray.copy(alpha = 0.25f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepLabels.forEachIndexed { idx, label ->
                val isCurrent = (idx + 1) == currentStep
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = if (isCurrent) green else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}

@Composable
private fun Step1BasicInfo(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    val sports = listOf("cricket" to "Cricket", "football" to "Football", "volleyball" to "Volleyball", "basketball" to "Basketball")
    val formats = listOf("round_robin" to "Round Robin", "knockout" to "Knockout", "group_knockout" to "Group + Knockout", "league_playoff" to "Playoffs")

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 1: Basic Information", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Set the competition name, sport type, and format", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Text("League Name *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateBasicInfo(it, uiState.description, uiState.sportType, uiState.format) },
                placeholder = "e.g. Premier Cricket League Season 4",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Description", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateBasicInfo(uiState.name, it, uiState.sportType, uiState.format) },
                placeholder = "Describe the league, eligibility, rules...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Sport Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(sports) { (key, label) ->
                    val selected = uiState.sportType == key
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.updateBasicInfo(uiState.name, uiState.description, key, uiState.format) },
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Tournament Format", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(formats) { (fmt, label) ->
                    val selected = uiState.format == fmt
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.updateBasicInfo(uiState.name, uiState.description, uiState.sportType, fmt) },
                        label = label
                    )
                }
            }
        }
    }
}

@Composable
private fun Step2VenueSelection(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 2: Venue & Location", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Select or enter the venue where matches will be played", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hosted at Official Venue", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Use integrated ground booking system", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isVenueHosted,
                    onCheckedChange = { viewModel.updateVenueConfig(it, uiState.primaryVenueId, uiState.customVenueText) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = green)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Venue Name / Ground Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.customVenueText,
                onValueChange = { viewModel.updateVenueConfig(uiState.isVenueHosted, uiState.primaryVenueId, it) },
                placeholder = "e.g. City Sports Complex, Ground 1",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Step3CricketSettings(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    val variants = listOf("softball" to "Soft Ball", "hardball" to "Hard Ball")

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 3: Cricket Configurations", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Specify ball type, match overs, and powerplay rules", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Text("Cricket Ball Variant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                variants.forEach { (v, label) ->
                    val selected = uiState.cricketVariant == v
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.updateCricketSettings(v, uiState.overs, uiState.ballsPerOver, uiState.powerplayOvers, uiState.deathOvers) },
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Overs Per Innings:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${uiState.overs} Overs", fontSize = 13.sp, color = green, fontWeight = FontWeight.Black)
            }
            Slider(
                value = uiState.overs.toFloat(),
                onValueChange = { viewModel.updateCricketSettings(uiState.cricketVariant, it.toInt(), uiState.ballsPerOver, uiState.powerplayOvers, uiState.deathOvers) },
                valueRange = 1f..50f,
                steps = 49,
                colors = SliderDefaults.colors(thumbColor = green, activeTrackColor = green)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balls Per Over:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${uiState.ballsPerOver} Balls (Clamped 4–6)", fontSize = 13.sp, color = green, fontWeight = FontWeight.Black)
            }
            Slider(
                value = uiState.ballsPerOver.toFloat(),
                onValueChange = { viewModel.updateCricketSettings(uiState.cricketVariant, uiState.overs, it.toInt().coerceIn(4, 6), uiState.powerplayOvers, uiState.deathOvers) },
                valueRange = 4f..6f,
                steps = 1,
                colors = SliderDefaults.colors(thumbColor = green, activeTrackColor = green)
            )
        }
    }
}

@Composable
private fun Step4TeamConfiguration(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 4: Teams & Squad Setup", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Define team slots and squad capacity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Number of Teams: ${uiState.numTeams}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { viewModel.updateNumTeams(uiState.numTeams - 1) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = green.copy(alpha = 0.2f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = green)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = { viewModel.updateNumTeams(uiState.numTeams + 1) },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = green),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            uiState.teamsList.forEachIndexed { idx, team ->
                Text("Team ${idx + 1} Name", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                LiquidGlassTextField(
                    value = team.name,
                    onValueChange = { viewModel.updateTeamItem(idx, it, team.shortName, team.jerseyColor) },
                    placeholder = "e.g. Royal Warriors",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Step5ScheduleAndDates(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 5: Schedule & Important Dates", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Set registration timeline and competition dates", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Text("Competition Start Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.startDate,
                onValueChange = { viewModel.updateDates(uiState.registrationStart, uiState.registrationEnd, it, uiState.endDate) },
                placeholder = "YYYY-MM-DD (e.g. 2026-10-15)",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Competition End Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.endDate,
                onValueChange = { viewModel.updateDates(uiState.registrationStart, uiState.registrationEnd, uiState.startDate, it) },
                placeholder = "YYYY-MM-DD (e.g. 2026-11-20)",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Step6AdvancedRules(uiState: LeagueCreateUiState, green: Color, viewModel: LeagueCreateViewModel) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 6: Prize Pool & Rules", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Specify rewards, tournament rules, and public visibility", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Text("Prize Pool / Trophy Description", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.prizePool,
                onValueChange = { viewModel.updateAdvanced(it, uiState.rulesText, uiState.isPublic, uiState.isFeatured) },
                placeholder = "e.g. 50,000 LKR + Champions Trophy",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Rules & Guidelines", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.rulesText,
                onValueChange = { viewModel.updateAdvanced(uiState.prizePool, it, uiState.isPublic, uiState.isFeatured) },
                placeholder = "Enter custom rules, points criteria, tie-breakers...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Public League", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Visible in discover feed and search", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isPublic,
                    onCheckedChange = { viewModel.updateAdvanced(uiState.prizePool, uiState.rulesText, it, uiState.isFeatured) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = green)
                )
            }
        }
    }
}

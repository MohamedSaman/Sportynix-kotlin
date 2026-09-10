package com.sportynix.app.presentation.tournaments

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.presentation.components.*
import com.sportynix.app.presentation.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: TournamentCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val bg = if (isDark) DarkBackground else LightBackground
    val green = if (isDark) NeonGreen else SportynixGreenLightTheme
    val context = LocalContext.current

    LaunchedEffect(uiState.createdTournamentId) {
        uiState.createdTournamentId?.let { id ->
            onNavigateToDetail(id)
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
                            text = "Create Tournament",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "STEP ${uiState.currentStep} OF ${uiState.totalSteps}",
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
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
                                )
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        val isLastStep = uiState.currentStep == uiState.totalSteps
                        Button(
                            onClick = {
                                if (isLastStep) {
                                    viewModel.createTournament(context)
                                } else {
                                    viewModel.nextStep()
                                }
                            },
                            enabled = !uiState.isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = green),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(if (uiState.currentStep > 1) 1.5f else 1f).height(48.dp)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isLastStep) "Publish Tournament" else "Next Step",
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
                TournamentStepperHeader(
                    currentStep = uiState.currentStep,
                    totalSteps = uiState.totalSteps,
                    green = green
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Error alert pill
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

                // Step Content
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + slideInHorizontally { 30 })
                            .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally { -30 })
                    },
                    label = "stepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1TournamentBasics(uiState = uiState, green = green, viewModel = viewModel)
                        2 -> Step2TournamentSetup(uiState = uiState, green = green, viewModel = viewModel)
                        3 -> Step3TournamentVenueAndDates(uiState = uiState, green = green, viewModel = viewModel)
                        4 -> Step4TournamentReview(uiState = uiState, green = green)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TournamentStepperHeader(
    currentStep: Int,
    totalSteps: Int,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark
    val stepLabels = listOf("Basics", "Setup", "Venue & Dates", "Review")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..totalSteps).forEach { step ->
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                Box(
                    modifier = Modifier
                        .size(34.dp)
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
                            fontSize = 13.sp,
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (step < totalSteps) {
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepLabels.forEachIndexed { idx, label ->
                val isCurrent = (idx + 1) == currentStep
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (isCurrent) green else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

@Composable
private fun Step1TournamentBasics(
    uiState: TournamentCreateUiState,
    green: Color,
    viewModel: TournamentCreateViewModel
) {
    val isDark = LocalThemeController.current.isDark
    val sports = listOf("cricket" to "Cricket", "football" to "Football", "basketball" to "Basketball", "volleyball" to "Volleyball")
    val variants = listOf("softball" to "Soft Ball", "hardball" to "Hard Ball")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.updateBasics(
            name = uiState.name,
            description = uiState.description,
            sportType = uiState.sportType,
            cricketVariant = uiState.cricketVariant,
            bannerUri = uri
        )
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 1: Tournament Basics", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Name your tournament, choose sport, variant, and banner image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Tournament Name
            Text("Tournament Name *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateBasics(it, uiState.description, uiState.sportType, uiState.cricketVariant, uiState.bannerUri) },
                placeholder = "e.g. Colombo Cup 2026",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text("Description (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidGlassTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateBasics(uiState.name, it, uiState.sportType, uiState.cricketVariant, uiState.bannerUri) },
                placeholder = "Describe your tournament, prizes, format, entry details...",
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tournament Banner
            Text("Tournament Banner Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                if (uiState.bannerUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = uiState.bannerUri,
                            contentDescription = "Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = green, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Upload Banner Image", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Tap to browse from gallery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sport Type
            Text("Sport Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(sports) { (key, label) ->
                    val selected = uiState.sportType == key
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.updateBasics(uiState.name, uiState.description, key, uiState.cricketVariant, uiState.bannerUri) },
                        label = label
                    )
                }
            }

            if (uiState.sportType == "cricket") {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Cricket Variant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    variants.forEach { (v, label) ->
                        val selected = uiState.cricketVariant == v
                        LiquidGlassFilterChip(
                            selected = selected,
                            onClick = { viewModel.updateBasics(uiState.name, uiState.description, uiState.sportType, v, uiState.bannerUri) },
                            label = label
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2TournamentSetup(
    uiState: TournamentCreateUiState,
    green: Color,
    viewModel: TournamentCreateViewModel
) {
    val formats = listOf(
        "knockout" to "Knockout",
        "group_knockout" to "Group + Knockout",
        "round_robin" to "Round Robin",
        "swiss" to "Swiss",
        "custom" to "Custom"
    )

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 2: Tournament Setup & Rules", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Configure match format, team slots, and roster limits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.sportType == "cricket") {
                Text("Cricket Match Settings", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = green)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Overs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        LiquidGlassTextField(
                            value = uiState.overs,
                            onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, uiState.rosterLimit, uiState.minRoster, uiState.playingPlayersCount, overs = it) },
                            placeholder = "20",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Powerplay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        LiquidGlassTextField(
                            value = uiState.powerplayOvers,
                            onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, uiState.rosterLimit, uiState.minRoster, uiState.playingPlayersCount, powerplayOvers = it) },
                            placeholder = "6",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Balls/Over", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        LiquidGlassTextField(
                            value = uiState.ballsPerOver,
                            onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, uiState.rosterLimit, uiState.minRoster, uiState.playingPlayersCount, ballsPerOver = it) },
                            placeholder = "6",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tournament Format
            Text("Competition Format", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(formats) { (fmt, label) ->
                    val selected = uiState.format == fmt
                    LiquidGlassFilterChip(
                        selected = selected,
                        onClick = { viewModel.updateSetup(fmt, uiState.teamCapacity, uiState.rosterLimit, uiState.minRoster, uiState.playingPlayersCount) },
                        label = label
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Squad and Team Limits
            Text("Team Capacity & Squad Limits", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = green)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Teams", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    LiquidGlassTextField(
                        value = uiState.teamCapacity,
                        onValueChange = { viewModel.updateSetup(uiState.format, it, uiState.rosterLimit, uiState.minRoster, uiState.playingPlayersCount) },
                        placeholder = "8",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Max Roster", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    LiquidGlassTextField(
                        value = uiState.rosterLimit,
                        onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, it, uiState.minRoster, uiState.playingPlayersCount) },
                        placeholder = "15",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Min Roster Size", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    LiquidGlassTextField(
                        value = uiState.minRoster,
                        onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, uiState.rosterLimit, it, uiState.playingPlayersCount) },
                        placeholder = "7",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Playing XI / Players", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    LiquidGlassTextField(
                        value = uiState.playingPlayersCount,
                        onValueChange = { viewModel.updateSetup(uiState.format, uiState.teamCapacity, uiState.rosterLimit, uiState.minRoster, it) },
                        placeholder = "11",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Step3TournamentVenueAndDates(
    uiState: TournamentCreateUiState,
    green: Color,
    viewModel: TournamentCreateViewModel
) {
    val isDark = LocalThemeController.current.isDark
    val context = LocalContext.current
    var showVenuePickerModal by remember { mutableStateOf(false) }

    val openDatePicker = { initialDateStr: String?, onDateSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        if (!initialDateStr.isNullOrBlank()) {
            try {
                val parts = initialDateStr.split("-")
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            } catch (e: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 3: Venue & Key Dates", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Set venue location and application/tournament schedules", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Venue Selection
            Text("Venue Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.selectedVenue != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = green.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, green.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Place, null, tint = green, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.selectedVenue.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(uiState.selectedVenue.address ?: "Venue Ground", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.updateVenueConfig(null, uiState.customVenueText, uiState.isVenueHosted) }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showVenuePickerModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Venue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    LiquidGlassTextField(
                        value = uiState.customVenueText,
                        onValueChange = { viewModel.updateVenueConfig(null, it, uiState.isVenueHosted) },
                        placeholder = "Or custom venue name...",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hosted at Venue Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Official Venue Hosted", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Enable venue booking coordination", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isVenueHosted,
                    onCheckedChange = { viewModel.updateVenueConfig(uiState.selectedVenue, uiState.customVenueText, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = green)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Dates
            Text("Registration & Tournament Schedule", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = green)
            Spacer(modifier = Modifier.height(8.dp))

            // Date Picker Row 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Applications Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        onClick = {
                            openDatePicker(uiState.applicationOpenDate) { date ->
                                viewModel.updateDates(date, uiState.applicationCloseDate, uiState.tournamentStartDate, uiState.tournamentEndDate)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(uiState.applicationOpenDate ?: "Select Date", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Application Deadline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        onClick = {
                            openDatePicker(uiState.applicationCloseDate) { date ->
                                viewModel.updateDates(uiState.applicationOpenDate, date, uiState.tournamentStartDate, uiState.tournamentEndDate)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(uiState.applicationCloseDate ?: "Select Date", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Picker Row 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tournament Starts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        onClick = {
                            openDatePicker(uiState.tournamentStartDate) { date ->
                                viewModel.updateDates(uiState.applicationOpenDate, uiState.applicationCloseDate, date, uiState.tournamentEndDate)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(uiState.tournamentStartDate ?: "Select Date", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Tournament Ends", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        onClick = {
                            openDatePicker(uiState.tournamentEndDate) { date ->
                                viewModel.updateDates(uiState.applicationOpenDate, uiState.applicationCloseDate, uiState.tournamentStartDate, date)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = green, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(uiState.tournamentEndDate ?: "Select Date", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showVenuePickerModal) {
        LiquidGlassDialog(onDismissRequest = { showVenuePickerModal = false }) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(8.dp)) {
                Text("Select Venue", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(10.dp))

                LiquidGlassTextField(
                    value = uiState.venueSearch,
                    onValueChange = viewModel::searchVenues,
                    placeholder = "Search venues by name or area...",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.loadingVenues) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = green)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items(uiState.venues) { venue ->
                            Surface(
                                onClick = {
                                    viewModel.updateVenueConfig(venue, venue.name, uiState.isVenueHosted)
                                    showVenuePickerModal = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) DarkSurfaceVariant.copy(alpha = 0.6f) else LightSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, null, tint = green, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(venue.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(venue.address ?: "Venue Ground", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun Step4TournamentReview(
    uiState: TournamentCreateUiState,
    green: Color
) {
    val isDark = LocalThemeController.current.isDark

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 4: Review & Confirm", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Verify all tournament parameters before publishing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Banner Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) DarkSurfaceVariant else LightSurfaceVariant,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                if (uiState.bannerUri != null) {
                    AsyncImage(
                        model = uiState.bannerUri,
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(green.copy(alpha = 0.3f), Color(0xFF3B82F6).copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.name.ifBlank { "Tournament" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewRow("Tournament Name", uiState.name.ifBlank { "Not set" })
                ReviewRow("Sport & Variant", "${uiState.sportType.replaceFirstChar { it.uppercase() }} • ${uiState.cricketVariant.replaceFirstChar { it.uppercase() }}")
                ReviewRow("Format", uiState.format.replace("_", " ").uppercase())
                ReviewRow("Team Capacity", "${uiState.teamCapacity} Teams (Max Squad: ${uiState.rosterLimit})")
                ReviewRow("Playing Squad", "${uiState.playingPlayersCount} Players (Min: ${uiState.minRoster})")
                if (uiState.sportType == "cricket") {
                    ReviewRow("Cricket Rules", "${uiState.overs} Overs (${uiState.powerplayOvers} PP Overs, ${uiState.ballsPerOver} Balls/Over)")
                }
                ReviewRow("Venue", uiState.selectedVenue?.name ?: uiState.customVenueText.ifBlank { "Not set" })
                ReviewRow("Applications Timeline", "${uiState.applicationOpenDate ?: "TBD"} to ${uiState.applicationCloseDate ?: "TBD"}")
                ReviewRow("Tournament Dates", "${uiState.tournamentStartDate ?: "TBD"} to ${uiState.tournamentEndDate ?: "TBD"}")
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

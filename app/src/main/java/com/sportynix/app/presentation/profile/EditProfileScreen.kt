package com.sportynix.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import com.sportynix.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditProfileEffect.NavigateBack -> onNavigateBack()
                is EditProfileEffect.ShowToast -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile(context) },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentGreen)
                        } else {
                            Text(
                                text = "Save",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentGreen
                            )
                        }
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
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentGreen)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Subtitle
                    Column {
                        Text(
                            text = "Edit Profile",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Update your personal and sports profile.",
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }

                    // 1. PROFILE PICTURE SECTION
                    SectionCard(title = "PROFILE PICTURE", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(accentGreen.copy(alpha = 0.15f))
                                    .border(2.dp, accentGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.imageUri != null) {
                                    AsyncImage(
                                        model = state.imageUri,
                                        contentDescription = "New Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (!state.user?.avatarUrl.isNullOrBlank() && !state.imageWasRemoved) {
                                    AsyncImage(
                                        model = state.user?.avatarUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val initials = "${state.firstName.take(1)}${state.lastName.take(1)}".uppercase()
                                    Text(
                                        text = initials.ifBlank { "U" },
                                        color = accentGreen,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = accentGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change", color = accentGreen, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.onRemovePhotoRequested() },
                                    enabled = state.imageUri != null || !state.user?.avatarUrl.isNullOrBlank(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. PERSONAL INFORMATION SECTION
                    SectionCard(title = "PERSONAL INFORMATION", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.firstName,
                                onValueChange = viewModel::onFirstNameChanged,
                                label = { Text("First Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.lastName,
                                onValueChange = viewModel::onLastNameChanged,
                                label = { Text("Last Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Username
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = viewModel::onUsernameChanged,
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val changesRemaining = state.user?.usernameChangesRemaining ?: 3
                        Text(
                            text = "Username changes remaining: $changesRemaining",
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        // Gender Selection
                        Text("Gender", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "prefer_not_to_say" to "Not set",
                                "male" to "Male",
                                "female" to "Female"
                            ).forEach { (value, label) ->
                                val isSelected = state.gender == value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onGenderChanged(value) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Date of Birth
                        Text("Date of Birth", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setShowDobPicker(true) },
                            color = backgroundColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                                Text(sdf.format(state.dobDate), fontSize = 15.sp, color = textPrimary)
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Email
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (state.user?.isEmailUnverified == true) {
                                    Text(
                                        text = "Verify",
                                        color = Color(0xFFF97316),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clickable { viewModel.sendEmailVerificationLink() }
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = accentGreen)
                                }
                            }
                        )

                        // Phone
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = viewModel::onPhoneChanged,
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (state.user?.isPhoneVerified == true) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = accentGreen)
                                } else {
                                    Text(
                                        text = "Verify",
                                        color = Color(0xFFF97316),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clickable { viewModel.setShowPhoneModal(true) }
                                    )
                                }
                            }
                        )

                        // Bio
                        OutlinedTextField(
                            value = state.bio,
                            onValueChange = viewModel::onBioChanged,
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        // Address & Location Picker
                        OutlinedTextField(
                            value = state.address,
                            onValueChange = viewModel::onAddressChanged,
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setShowLocationPicker(true) },
                            color = backgroundColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Home Location", fontSize = 12.sp, color = textSecondary)
                                    val loc = listOfNotNull(state.homeCity.ifBlank { null }, state.homeDistrict.ifBlank { null }, state.homeProvince.ifBlank { null }).joinToString(", ")
                                    Text(loc.ifBlank { "Select your city" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                }
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentGreen)
                            }
                        }
                    }

                    // 3. CRICKET PROFILE SECTION
                    SectionCard(title = "CRICKET PROFILE", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        Text("Preferred Variant", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("all" to "Both", "softball" to "Softball", "hardball" to "Hardball").forEach { (valKey, label) ->
                                FilterChip(
                                    selected = state.cricketPreferredVariant == valKey,
                                    onClick = { viewModel.onCricketVariantChanged(valKey) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentGreen, selectedLabelColor = Color.White)
                                )
                            }
                        }

                        DropdownField("Primary Role", state.cricketPrimaryRole, listOf("All-Rounder", "Batsman", "Bowler", "Wicketkeeper"), viewModel::onCricketPrimaryRoleChanged)
                        DropdownField("Playing Position", state.cricketPlayingPosition, listOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper", "Top Order Batter", "Middle Order Batter", "Finisher", "Opening Bowler", "Strike Bowler"), viewModel::onCricketPlayingPositionChanged)
                        OutlinedTextField(
                            value = state.cricketJerseyNumber,
                            onValueChange = viewModel::onCricketJerseyNumberChanged,
                            label = { Text("Jersey Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownField("Batting Style", state.cricketBattingStyle, listOf("Right Hand", "Left Hand"), viewModel::onCricketBattingStyleChanged)
                        DropdownField("Bowling Style", state.cricketBowlingStyle, listOf("Right Arm Fast", "Right Arm Medium", "Right Arm Spin", "Left Arm Fast", "Left Arm Medium", "Left Arm Spin"), viewModel::onCricketBowlingStyleChanged)
                    }

                    // 4. SPORTS PREFERENCES SECTION
                    SectionCard(title = "SPORTS PREFERENCES", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        val allSports = listOf("Futsal", "Basketball", "Cricket", "Badminton", "Tennis", "Volleyball")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allSports.chunked(3).forEach { rowSports ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    rowSports.forEach { sport ->
                                        val isSelected = state.selectedSports.contains(sport)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) accentGreen else accentGreen.copy(alpha = 0.1f))
                                                .border(1.dp, if (isSelected) Color.Transparent else accentGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                .clickable { viewModel.toggleSport(sport) }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(sport, color = if (isSelected) Color.White else accentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. AVAILABILITY SECTION
                    SectionCard(title = "AVAILABILITY", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("weekdays" to "Weekdays", "weekends" to "Weekends", "both" to "Both").forEach { (valKey, label) ->
                                FilterChip(
                                    selected = state.availability == valKey,
                                    onClick = { viewModel.onAvailabilityChanged(valKey) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentGreen, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }

                    // 6. PRIVACY SETTINGS SECTION
                    SectionCard(title = "PRIVACY SETTINGS", cardColor = cardColor, borderColor = borderColor, textSecondary = textSecondary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Public Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Allow others to see your profile", fontSize = 12.sp, color = textSecondary)
                            }
                            Switch(checked = state.isPublicProfile, onCheckedChange = viewModel::onPublicProfileChanged, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Contact Info", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Display phone and email to team members", fontSize = 12.sp, color = textSecondary)
                            }
                            Switch(checked = state.isShowContact, onCheckedChange = viewModel::onShowContactChanged, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentGreen))
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Banner Message
            if (!state.bannerMessage.isNullOrBlank()) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = viewModel::clearBanner) { Text("OK", color = Color.White) } },
                    containerColor = accentGreen
                ) {
                    Text(state.bannerMessage, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Error Message Alert
            if (!state.errorMessage.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = viewModel::clearError,
                    title = { Text("Profile Update", fontWeight = FontWeight.Bold) },
                    text = { Text(state.errorMessage) },
                    confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
                )
            }

            // Remove Photo Dialog
            if (state.showRemoveConfirmation) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissRemoveConfirmation,
                    title = { Text("Remove Profile Photo?", fontWeight = FontWeight.Bold) },
                    text = { Text("Your profile photo will be removed.") },
                    confirmButton = { TextButton(onClick = viewModel::confirmRemovePhoto) { Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = viewModel::dismissRemoveConfirmation) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    cardColor: Color,
    borderColor: Color,
    textSecondary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun DropdownField(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = value.ifBlank { "None" },
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expanded = true })
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(""); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

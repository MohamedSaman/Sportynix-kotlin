package com.sportynix.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.LiquidGlassButton
import com.sportynix.app.presentation.components.LiquidGlassCard
import com.sportynix.app.presentation.components.LiquidGlassTextField
import com.sportynix.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRegistrationScreen(
    leagueId: String,
    leagueName: String,
    sportType: String,
    onNavigateBack: () -> Unit,
    viewModel: TeamRegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isDark = LocalThemeController.current.isDark
    val green = if (isDark) NeonGreen else SportynixGreenPrimary
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.setLogoUri(uri)
        }
    }

    Scaffold(
        containerColor = if (isDark) DarkBackground else LightBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Team League Registration",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$leagueName • ${sportType.replaceFirstChar { it.uppercase() }}",
                            color = green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Main Card
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Team Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Logo Selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) GlassSurfaceDark else GlassSurfaceLight)
                                .border(
                                    1.dp,
                                    if (isDark) GlassBorderDark else GlassBorderLight,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { imagePicker.launch("image/*") }
                                .padding(12.dp)
                        ) {
                            if (state.logoUri != null) {
                                AsyncImage(
                                    model = state.logoUri,
                                    contentDescription = "Team Logo",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(green.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = green,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.logoUri != null) "Team Logo Selected" else "Upload Team Logo (Optional)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to choose photo from gallery",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Form Inputs
                        RegistrationInputField(
                            label = "Team Name *",
                            value = state.teamName,
                            onValueChange = { viewModel.update { curr -> curr.copy(teamName = it) } },
                            placeholder = "Enter full team name",
                            leadingIcon = Icons.Default.Shield,
                            error = state.fieldErrors["teamName"],
                            capitalization = KeyboardCapitalization.Words
                        )

                        RegistrationInputField(
                            label = "Short Name (Max 5 chars) *",
                            value = state.shortName,
                            onValueChange = { if (it.length <= 5) viewModel.update { curr -> curr.copy(shortName = it.uppercase()) } },
                            placeholder = "e.g. LFC, CSK",
                            leadingIcon = Icons.Default.Groups,
                            error = state.fieldErrors["shortName"],
                            capitalization = KeyboardCapitalization.Characters
                        )

                        RegistrationInputField(
                            label = "Captain Name *",
                            value = state.captainName,
                            onValueChange = { viewModel.update { curr -> curr.copy(captainName = it) } },
                            placeholder = "Enter captain's full name",
                            leadingIcon = Icons.Default.Person,
                            error = state.fieldErrors["captainName"],
                            capitalization = KeyboardCapitalization.Words
                        )

                        RegistrationInputField(
                            label = "Captain Email (Optional)",
                            value = state.captainEmail,
                            onValueChange = { viewModel.update { curr -> curr.copy(captainEmail = it) } },
                            placeholder = "captain@example.com",
                            leadingIcon = Icons.Default.Email,
                            error = state.fieldErrors["captainEmail"],
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None
                        )

                        RegistrationInputField(
                            label = "Captain Phone (Optional)",
                            value = state.captainPhone,
                            onValueChange = { viewModel.update { curr -> curr.copy(captainPhone = it) } },
                            placeholder = "+94 77 123 4567",
                            leadingIcon = Icons.Default.Phone,
                            error = state.fieldErrors["captainPhone"],
                            keyboardType = KeyboardType.Phone
                        )

                        RegistrationInputField(
                            label = "Home Ground / Stadium (Optional)",
                            value = state.homeGround,
                            onValueChange = { viewModel.update { curr -> curr.copy(homeGround = it) } },
                            placeholder = "Enter home venue",
                            leadingIcon = Icons.Default.Home
                        )

                        RegistrationInputField(
                            label = "Additional Notes (Optional)",
                            value = state.notes,
                            onValueChange = { viewModel.update { curr -> curr.copy(notes = it) } },
                            placeholder = "Jersey colors, special requests, etc.",
                            leadingIcon = Icons.Default.Notes,
                            singleLine = false
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        LiquidGlassButton(
                            text = if (state.submitting) "Submitting..." else "Submit Team Registration",
                            onClick = { viewModel.submit(leagueId) },
                            enabled = !state.submitting,
                            isLoading = state.submitting,
                            icon = Icons.Default.Check,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }
                }
            }
        }
    }

    // Alert / Confirmation Dialog
    if (state.error != null || state.success != null) {
        AlertDialog(
            onDismissRequest = {
                if (state.success != null) onNavigateBack() else viewModel.clearMessage()
            },
            title = {
                Text(
                    text = if (state.success != null) "Registration Submitted!" else "Registration Notice",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = state.success ?: state.error.orEmpty())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.success != null) onNavigateBack() else viewModel.clearMessage()
                    }
                ) {
                    Text("OK", color = green, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RegistrationInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences
) {
    val isDark = LocalThemeController.current.isDark
    val green = if (isDark) NeonGreen else SportynixGreenPrimary

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )
        LiquidGlassTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (error != null) StatusError else green,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = singleLine,
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                color = StatusError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

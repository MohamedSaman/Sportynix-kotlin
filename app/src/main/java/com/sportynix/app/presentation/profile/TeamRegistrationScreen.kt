package com.sportynix.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.DarkBackground
import com.sportynix.app.presentation.theme.LightBackground
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.NeonGreen
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun TeamRegistrationScreen(
    leagueId: String,
    leagueName: String,
    sportType: String,
    onNavigateBack: () -> Unit,
    viewModel: TeamRegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val green = if (LocalThemeController.current.isDark) NeonGreen else SportynixGreenPrimary
    Scaffold(containerColor = if (LocalThemeController.current.isDark) DarkBackground else LightBackground) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }; Column { Text("Register Team", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("$leagueName • ${sportType.replaceFirstChar { it.uppercase() }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } } }
            item { GlassCard(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), elevation = 4.dp) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.Groups, null, tint = green, modifier = Modifier.padding(bottom = 2.dp)); RegistrationField("Team name", state.teamName) { value -> viewModel.update { current -> current.copy(teamName = value) } }; RegistrationField("Short name (max 5)", state.shortName) { value -> viewModel.update { current -> current.copy(shortName = value) } }; RegistrationField("Captain name", state.captainName) { value -> viewModel.update { current -> current.copy(captainName = value) } }; RegistrationField("Captain email (optional)", state.captainEmail) { value -> viewModel.update { current -> current.copy(captainEmail = value) } }; RegistrationField("Captain phone (optional)", state.captainPhone) { value -> viewModel.update { current -> current.copy(captainPhone = value) } }; RegistrationField("Home ground (optional)", state.homeGround) { value -> viewModel.update { current -> current.copy(homeGround = value) } }; RegistrationField("Notes (optional)", state.notes, minLines = 3) { value -> viewModel.update { current -> current.copy(notes = value) } }; Spacer(Modifier.height(4.dp)); Button(onClick = { viewModel.submit(leagueId) }, enabled = !state.submitting, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = green)) { if (state.submitting) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) else Text("Submit registration", fontWeight = FontWeight.Bold) } } } }
        }
    }
    if (state.error != null || state.success != null) AlertDialog(onDismissRequest = { if (state.success != null) onNavigateBack() else viewModel.clearMessage() }, title = { Text(if (state.success != null) "Registration submitted" else "Registration failed") }, text = { Text(state.success ?: state.error.orEmpty()) }, confirmButton = { TextButton(onClick = { if (state.success != null) onNavigateBack() else viewModel.clearMessage() }) { Text("OK", color = green) } })
}

@Composable private fun RegistrationField(label: String, value: String, minLines: Int = 1, onValueChange: (String) -> Unit) { OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = minLines, singleLine = minLines == 1) }

package com.sportynix.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.BlockedUserDto
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sportynix.app.presentation.theme.*

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var blockedUsers by mutableStateOf<List<BlockedUserDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var unblockingIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = profileRepository.getBlockedUsers()
            result.onSuccess { list ->
                blockedUsers = list
                isLoading = false
            }.onFailure { err ->
                isLoading = false
                errorMessage = err.message ?: "Failed to load blocked users"
            }
        }
    }

    fun unblock(userId: Int) {
        val set = unblockingIds.toMutableSet()
        set.add(userId)
        unblockingIds = set

        viewModelScope.launch {
            val result = profileRepository.unblockUser(userId)
            if (result.isSuccess) {
                blockedUsers = blockedUsers.filter { it.id != userId }
            }
            val doneSet = unblockingIds.toMutableSet()
            doneSet.remove(userId)
            unblockingIds = doneSet
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
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
            if (viewModel.isLoading && viewModel.blockedUsers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentGreen)
            } else if (!viewModel.errorMessage.isNullOrBlank() && viewModel.blockedUsers.isEmpty()) {
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
            } else if (viewModel.blockedUsers.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Text("No Blocked Users", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("People you block won't be able to send you messages or join your matches.", fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(viewModel.blockedUsers, key = { it.id }) { user ->
                        val isProcessing = viewModel.unblockingIds.contains(user.id)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = cardColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(accentGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user.profilePicture.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user.profilePicture,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            val initial = user.fullName?.take(1) ?: user.username?.take(1) ?: "U"
                                            Text(initial.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentGreen)
                                        }
                                    }

                                    Column {
                                        Text(user.fullName ?: user.username ?: "User", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                        if (!user.username.isNullOrBlank() && user.fullName != null) {
                                            Text("@${user.username}", fontSize = 13.sp, color = textSecondary)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.unblock(user.id) },
                                    enabled = !isProcessing,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentGreen.copy(alpha = 0.3f))
                                ) {
                                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentGreen)
                                    else Text("Unblock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

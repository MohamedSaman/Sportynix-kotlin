package com.sportynix.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.annotation.ExperimentalCoilApi
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.data.remote.dto.UserDataDto
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.repository.AuthRepository
import com.sportynix.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class EmailChangeStep { INITIAL, VERIFY_CURRENT, VERIFY_NEW }
enum class ResetStep { INITIAL, VERIFY, SET }

data class SettingsUiState(
    val user: UserDataDto? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val allowDirectTeamAdd: Boolean = true,
    val cacheBytes: Long = 0,
    val cacheFiles: Int = 0,
    val isLoadingCache: Boolean = true,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    var state by mutableStateOf(SettingsUiState())
        private set

    init {
        loadProfile()
        loadCacheStats()
    }

    fun clearMessage() { state = state.copy(message = null) }
    private fun success(message: String) { state = state.copy(isProcessing = false, message = message, isError = false) }
    private fun fail(error: Throwable?, fallback: String) {
        state = state.copy(isProcessing = false, message = error?.message?.takeIf(String::isNotBlank) ?: fallback, isError = true)
    }

    fun loadProfile() = viewModelScope.launch {
        state = state.copy(isLoading = true)
        profileRepository.fetchProfile().fold(
            onSuccess = { user -> state = state.copy(user = user, allowDirectTeamAdd = user.allowDirectTeamAdd ?: true, isLoading = false) },
            onFailure = { state = state.copy(isLoading = false, message = it.message ?: "Failed to load settings", isError = true) }
        )
    }

    fun updateAllowDirectTeamAdd(value: Boolean) {
        if (state.isUpdatingPrivacy) return
        val previous = state.allowDirectTeamAdd
        state = state.copy(allowDirectTeamAdd = value, isUpdatingPrivacy = true)
        viewModelScope.launch {
            profileRepository.updateAllowDirectTeamAdd(value).fold(
                onSuccess = { user -> state = state.copy(user = user, allowDirectTeamAdd = user.allowDirectTeamAdd ?: value, isUpdatingPrivacy = false, message = "Team privacy updated", isError = false) },
                onFailure = { state = state.copy(allowDirectTeamAdd = previous, isUpdatingPrivacy = false, message = it.message ?: "Failed to update team privacy setting", isError = true) }
            )
        }
    }

    private fun guarded(block: suspend () -> Unit) {
        if (state.isProcessing) return
        state = state.copy(isProcessing = true, message = null)
        viewModelScope.launch { block() }
    }

    fun sendEmailChangeOtp(onSuccess: () -> Unit) = guarded {
        profileRepository.sendEmailChangeOtp().fold({ onSuccess(); success("OTP sent to your current email") }, { fail(it, "Failed to send OTP") })
    }

    fun verifyCurrentEmail(otp: String, newEmail: String, onSuccess: () -> Unit) = guarded {
        profileRepository.verifyCurrentEmailForChange(otp, newEmail).fold({ onSuccess(); success("Verification code sent to $newEmail") }, { fail(it, "Failed to verify current email") })
    }

    fun verifyNewEmail(otp: String, newEmail: String, onSuccess: () -> Unit) = guarded {
        profileRepository.verifyNewEmail(otp, newEmail).fold({ onSuccess(); success("Email changed successfully") }, { fail(it, "Failed to verify new email") })
    }

    fun changePassword(old: String, new: String, confirm: String, onSuccess: () -> Unit) = guarded {
        profileRepository.changePassword(old, new, confirm).fold({ onSuccess(); success("Password changed successfully") }, { fail(it, "Failed to change password") })
    }

    fun setPassword(new: String, confirm: String, onSuccess: () -> Unit) = guarded {
        profileRepository.setPassword(new, confirm).fold(
            { profileRepository.fetchProfile().onSuccess { state = state.copy(user = it) }; onSuccess(); success("Password set successfully") },
            { fail(it, "Failed to set password") }
        )
    }

    fun sendResetOtp(onSuccess: () -> Unit) = guarded {
        val email = state.user?.email.orEmpty()
        when (val result = authRepository.forgotPassword(email)) {
            is ApiResult.Success -> { onSuccess(); success(result.data.message ?: "Verification code sent") }
            else -> fail(null, result.apiMessage("Failed to send verification code"))
        }
    }

    fun verifyResetOtp(otp: String, onSuccess: () -> Unit) = guarded {
        when (val result = authRepository.verifyPasswordResetOtp(state.user?.email.orEmpty(), otp)) {
            is ApiResult.Success -> { onSuccess(); success("Code verified") }
            else -> fail(null, result.apiMessage("Invalid code"))
        }
    }

    fun completeReset(otp: String, new: String, confirm: String, onSuccess: () -> Unit) = guarded {
        when (val result = authRepository.resetPassword(state.user?.email.orEmpty(), otp, new, confirm)) {
            is ApiResult.Success -> { profileRepository.fetchProfile().onSuccess { state = state.copy(user = it) }; onSuccess(); success("Password reset successfully") }
            else -> fail(null, result.apiMessage("Failed to reset password"))
        }
    }

    fun submitBug(subject: String, comment: String, onSuccess: () -> Unit) = guarded {
        profileRepository.submitBugReport(subject, comment).fold({ onSuccess(); success(it) }, { fail(it, "Failed to submit bug report") })
    }

    fun deleteAccount(password: String, confirmation: String, reason: String, feedback: String, onSuccess: () -> Unit) = guarded {
        val hasPassword = state.user?.hasPassword == true
        val body = mutableMapOf<String, Any?>()
        if (hasPassword) body["password"] = password else body["confirmation"] = confirmation.uppercase()
        reason.takeIf(String::isNotBlank)?.let { body["deletion_reason"] = it }
        feedback.trim().takeIf(String::isNotBlank)?.let { body["deletion_feedback"] = it }
        profileRepository.deleteAccount(body).fold({ authRepository.logout(); onSuccess(); success("Account deleted") }, { fail(it, "Failed to delete account") })
    }

    fun logout(onSuccess: () -> Unit) = guarded {
        authRepository.logout()
        state = state.copy(isProcessing = false)
        onSuccess()
    }

    fun loadCacheStats() = viewModelScope.launch {
        state = state.copy(isLoadingCache = true)
        val stats = withContext(Dispatchers.IO) { cacheStats(context.cacheDir) }
        state = state.copy(cacheBytes = stats.first, cacheFiles = stats.second, isLoadingCache = false)
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        if (state.isProcessing) return
        state = state.copy(isProcessing = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach(File::deleteRecursively) }
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
            }.fold(
                onSuccess = { loadCacheStats(); success("Local media cache cleared") },
                onFailure = { fail(it, "Failed to clear local cache") }
            )
        }
    }

    private fun cacheStats(root: File): Pair<Long, Int> {
        var bytes = 0L; var count = 0
        root.walkTopDown().filter(File::isFile).forEach { bytes += it.length(); count++ }
        return bytes to count
    }
}

private fun ApiResult<*>.apiMessage(fallback: String) = when (this) {
    is ApiResult.Error -> message
    is ApiResult.ServerError -> message
    ApiResult.NoInternet -> "No internet connection"
    ApiResult.Timeout -> "Request timed out"
    ApiResult.Unauthorized -> "Session expired"
    else -> fallback
}

private fun passwordError(password: String): String? = when {
    password.length < 8 -> "Password must be at least 8 characters long"
    password.none(Char::isUpperCase) -> "Password must include at least one uppercase letter"
    password.none { it in "!@#$%^&*(),.?\"{}|<>_-+=/\\[];:'`~" } -> "Password must include at least one special character"
    else -> null
}

private fun validEmail(value: String) = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble(); var unit = 0
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return if (value >= 10 || unit == 0) "%.0f %s".format(value, units[unit]) else "%.1f %s".format(value, units[unit])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    onNavigateToReportedUsers: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val theme = LocalThemeController.current
    val dark = theme.isDark
    val green = if (dark) SportynixGreenDarkTheme else SportynixGreenLightTheme
    val glass = if (dark) GlassCardDark else GlassCardLight
    val border = if (dark) GlassBorderDark else GlassBorderLight
    val text = MaterialTheme.colorScheme.onSurface
    val secondary = if (dark) TextSecondaryDark else TextSecondaryLight
    var dialog by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    val packageInfo = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() }
    val version = packageInfo?.versionName ?: "1.0.0"
    val build = packageInfo?.longVersionCode ?: 1L

    fun launch(intent: Intent) = runCatching { context.startActivity(intent) }.onFailure { Toast.makeText(context, "No compatible app found", Toast.LENGTH_SHORT).show() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(if (dark) listOf(DarkBackground, Color(0xFF071710), DarkBackground) else listOf(LightBackground, Color(0xFFEAF8F0), LightBackground)))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (state.isLoading) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = green) }
            else Column(
                Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth().align(Alignment.TopCenter).padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(state.message != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    state.message?.let { StatusBanner(it, state.isError, green) { viewModel.clearMessage() } }
                }

                SectionTitle("Account", Icons.Default.Person, green)
                GlassGroup(glass, border) {
                    SettingRow("Edit Profile", "Update your personal and sports information", Icons.Default.Edit, green, text, secondary, onNavigateToEditProfile)
                    SettingRow("Change Email", state.user?.email.orEmpty(), Icons.Default.Email, green, text, secondary) { dialog = "email" }
                    val social = state.user?.isSocial == true
                    val hasPassword = state.user?.hasPassword == true
                    SettingRow(
                        when { !hasPassword -> "Set Password"; social -> "Reset Password"; else -> "Change Password" },
                        when { !hasPassword -> "Add email/password sign-in"; social -> "Verify by email to reset"; else -> "Update your account password" },
                        Icons.Default.Lock, green, text, secondary
                    ) { dialog = "password" }
                }

                SectionTitle("Privacy & Safety", Icons.Default.Shield, green)
                GlassGroup(glass, border) {
                    ToggleRow("Allow Direct Team Add", if (state.allowDirectTeamAdd) "Users can add you directly" else "A request is required", Icons.Default.GroupAdd, state.allowDirectTeamAdd, state.isUpdatingPrivacy, green, text, secondary, viewModel::updateAllowDirectTeamAdd)
                    SettingRow("Blocked Users", "Manage accounts you have blocked", Icons.Default.Block, green, text, secondary, onNavigateToBlockedUsers)
                    SettingRow("Reported Users", "Review your submitted reports", Icons.Default.Flag, green, text, secondary, onNavigateToReportedUsers)
                }

                SectionTitle("Appearance", Icons.Default.Palette, green)
                ThemeGlassCard(dark, green, glass, border, text, secondary) { theme.toggle() }

                SectionTitle("Help & Support", Icons.AutoMirrored.Filled.Help, green)
                GlassGroup(glass, border) {
                    SettingRow("Email Support", "info@sportynix.com", Icons.Default.Email, green, text, secondary) { launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@sportynix.com?subject=Support%20Request"))) }
                    SettingRow("Call Support", "0332292223", Icons.Default.Phone, green, text, secondary) { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0332292223"))) }
                    SettingRow("Report an Issue", "Send a bug report to Sportynix", Icons.Default.BugReport, green, text, secondary) { dialog = "bug" }
                    SettingRow("Help Center", "FAQs and support information", Icons.Default.HelpOutline, green, text, secondary, onNavigateToHelp)
                }

                SectionTitle("Storage", Icons.Default.Storage, green)
                GlassGroup(glass, border) {
                    SettingRow("Clear App Cache", if (state.isLoadingCache) "Calculating…" else "${formatBytes(state.cacheBytes)} · ${state.cacheFiles} files", Icons.Default.CleaningServices, green, text, secondary) { dialog = "cache" }
                }

                SectionTitle("Session", Icons.AutoMirrored.Filled.Logout, green)
                GlassGroup(glass, border) {
                    SettingRow("Log Out", "Sign out of this device", Icons.AutoMirrored.Filled.Logout, green, text, secondary) { confirmLogout = true }
                    SettingRow("Delete Account", "Permanently erase your account and data", Icons.Default.DeleteForever, Color(0xFFEF4444), text, secondary) { dialog = "delete" }
                }

                Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sportynix", fontWeight = FontWeight.Bold, color = green)
                    Text("Version $version (Build $build)", fontSize = 12.sp, color = secondary)
                }
            }
        }
    }

    when (dialog) {
        "email" -> EmailChangeDialog(
            state = state,
            vm = viewModel,
            green = green,
            dismiss = { dialog = null },
            logout = { viewModel.logout { dialog = null; onLogout() } }
        )
        "password" -> PasswordDialog(state, viewModel, green, { dialog = null }, { viewModel.logout { dialog = null; onLogout() } })
        "bug" -> BugDialog(state, viewModel, green) { dialog = null }
        "cache" -> ConfirmDialog("Clear App Cache?", "This removes cached media and temporary files. Your account and bookings will not be deleted.", "Clear Cache", state.isProcessing, { dialog = null }) { viewModel.clearCache(); dialog = null }
        "delete" -> DeleteAccountDialog(state, viewModel, green, { dialog = null }) { dialog = null; onLogout() }
    }
    if (confirmLogout) ConfirmDialog("Log out?", "You will need to sign in again to use your account.", "Log Out", state.isProcessing, { confirmLogout = false }) { viewModel.logout { confirmLogout = false; onLogout() } }
}

@Composable private fun SectionTitle(title: String, icon: ImageVector, green: Color) = Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(icon, null, tint = green, modifier = Modifier.size(18.dp)); Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
}

@Composable private fun GlassGroup(color: Color, border: Color, content: @Composable ColumnScope.() -> Unit) = Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), color, border = androidx.compose.foundation.BorderStroke(1.dp, border), shadowElevation = 8.dp) { Column(content = content) }

@Composable private fun SettingRow(title: String, subtitle: String, icon: ImageVector, accent: Color, text: Color, secondary: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }; val pressed by source.collectIsPressedAsState(); val scale by animateFloatAsState(if (pressed) .985f else 1f, spring(stiffness = 700f), label = "settingPress")
    Row(Modifier.fillMaxWidth().scale(scale).clickable(source, null, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = .12f)), Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) { Text(title, color = text, fontWeight = FontWeight.SemiBold); Text(subtitle, color = secondary, fontSize = 12.sp, lineHeight = 16.sp) }
        Icon(Icons.Default.ChevronRight, null, tint = secondary.copy(alpha = .55f))
    }
}

@Composable private fun ToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, loading: Boolean, green: Color, text: Color, secondary: Color, onChange: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(green.copy(alpha = .12f)), Alignment.Center) { Icon(icon, null, tint = green) }
    Column(Modifier.weight(1f).padding(horizontal = 13.dp)) { Text(title, color = text, fontWeight = FontWeight.SemiBold); Text(subtitle, color = secondary, fontSize = 12.sp) }
    if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = green) else Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = green))
}

@Composable private fun ThemeGlassCard(dark: Boolean, green: Color, glass: Color, border: Color, text: Color, secondary: Color, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (dark) 180f else 0f, tween(350), label = "themeIcon")
    val tint by animateColorAsState(if (dark) Color(0xFFB8C7FF) else Color(0xFFFFB020), tween(300), label = "themeTint")
    Surface(Modifier.fillMaxWidth().clickable(onClick = onToggle), RoundedCornerShape(22.dp), glass, border = androidx.compose.foundation.BorderStroke(1.dp, border), shadowElevation = 8.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).rotate(rotation).clip(CircleShape).background(tint.copy(alpha = .14f)), Alignment.Center) { AnimatedContent(dark, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "sunMoon") { if (it) Icon(Icons.Default.DarkMode, null, tint = tint) else Icon(Icons.Default.LightMode, null, tint = tint) } }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(if (dark) "Dark Mode" else "Light Mode", color = text, fontWeight = FontWeight.Bold); Text("Tap to switch appearance", color = secondary, fontSize = 12.sp) }
            Switch(dark, { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = green))
        }
    }
}

@Composable private fun StatusBanner(message: String, error: Boolean, green: Color, dismiss: () -> Unit) = Surface(Modifier.fillMaxWidth().clickable(onClick = dismiss), RoundedCornerShape(16.dp), (if (error) MaterialTheme.colorScheme.error else green).copy(alpha = .12f)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (error) Icons.Default.Error else Icons.Default.CheckCircle, null, tint = if (error) MaterialTheme.colorScheme.error else green); Text(message, Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 13.sp); Icon(Icons.Default.Close, "Dismiss", Modifier.size(18.dp)) } }

@Composable private fun GlassField(value: String, onChange: (String) -> Unit, label: String, password: Boolean = false, singleLine: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3, visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None, trailingIcon = if (password) ({ IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show password") } }) else null, shape = RoundedCornerShape(16.dp))
}

@Composable private fun DialogFrame(title: String, processing: Boolean, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) = AlertDialog(onDismissRequest = { if (!processing) dismiss() }, confirmButton = {}, title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold); IconButton(dismiss, enabled = !processing) { Icon(Icons.Default.Close, "Close") } } }, text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }, shape = RoundedCornerShape(24.dp))

@Composable private fun ActionButton(text: String, loading: Boolean, enabled: Boolean = true, onClick: () -> Unit) = Button(onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled && !loading, shape = RoundedCornerShape(16.dp)) { if (loading) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp) else Text(text, fontWeight = FontWeight.Bold) }

@Composable private fun EmailChangeDialog(state: SettingsUiState, vm: SettingsViewModel, green: Color, dismiss: () -> Unit, logout: () -> Unit) {
    var step by remember { mutableStateOf(EmailChangeStep.INITIAL) }; var currentOtp by remember { mutableStateOf("") }; var newEmail by remember { mutableStateOf("") }; var newOtp by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    DialogFrame("Change Email", state.isProcessing, dismiss) {
        AnimatedContent(step, label = "emailStep") { current -> when (current) {
            EmailChangeStep.INITIAL -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("We’ll first verify your current email address."); ActionButton("Send OTP to Current Email", state.isProcessing) { vm.sendEmailChangeOtp { step = EmailChangeStep.VERIFY_CURRENT } } }
            EmailChangeStep.VERIFY_CURRENT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { GlassField(currentOtp, { currentOtp = it.filter(Char::isDigit).take(6) }, "Current email OTP"); GlassField(newEmail, { newEmail = it.trim() }, "New email"); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; ActionButton("Verify & Send New OTP", state.isProcessing) { error = when { currentOtp.isBlank() || newEmail.isBlank() -> "Please fill in all fields"; !validEmail(newEmail) -> "Please enter a valid email address"; else -> null }; if (error == null) vm.verifyCurrentEmail(currentOtp, newEmail) { step = EmailChangeStep.VERIFY_NEW } } }
            EmailChangeStep.VERIFY_NEW -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Enter the verification code sent to $newEmail."); GlassField(newOtp, { newOtp = it.filter(Char::isDigit).take(6) }, "New email OTP"); ActionButton("Complete Email Change", state.isProcessing, newOtp.isNotBlank()) { vm.verifyNewEmail(newOtp, newEmail, logout) } }
        } }
    }
}

@Composable private fun PasswordDialog(state: SettingsUiState, vm: SettingsViewModel, green: Color, dismiss: () -> Unit, logout: () -> Unit) {
    val social = state.user?.isSocial == true; val hasPassword = state.user?.hasPassword == true
    var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var otp by remember { mutableStateOf("") }; var resetStep by remember { mutableStateOf(ResetStep.INITIAL) }; var localError by remember { mutableStateOf<String?>(null) }
    fun validate(): String? = passwordError(new) ?: if (new != confirm) "Passwords do not match" else null
    DialogFrame(when { !hasPassword -> "Set Password"; social -> "Reset Password"; else -> "Change Password" }, state.isProcessing, dismiss) {
        if (social && hasPassword) AnimatedContent(resetStep, label = "resetStep") { step -> when (step) {
            ResetStep.INITIAL -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Verify ${state.user?.email.orEmpty()} before choosing a new password."); ActionButton("Send Verification Code", state.isProcessing) { vm.sendResetOtp { resetStep = ResetStep.VERIFY } } }
            ResetStep.VERIFY -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { GlassField(otp, { otp = it.filter(Char::isDigit).take(6) }, "Verification code"); ActionButton("Verify Code", state.isProcessing, otp.isNotBlank()) { vm.verifyResetOtp(otp) { resetStep = ResetStep.SET } } }
            ResetStep.SET -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { GlassField(new, { new = it }, "New password", true); GlassField(confirm, { confirm = it }, "Confirm password", true); localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Text("At least 8 characters, one uppercase letter and one special character.", fontSize = 12.sp); ActionButton("Reset Password", state.isProcessing) { localError = validate(); if (localError == null) vm.completeReset(otp, new, confirm, dismiss) } }
        } } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasPassword) GlassField(old, { old = it }, "Current password", true)
            GlassField(new, { new = it }, "New password", true); GlassField(confirm, { confirm = it }, "Confirm password", true)
            Text("At least 8 characters, one uppercase letter and one special character.", fontSize = 12.sp)
            localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ActionButton(if (!hasPassword) "Set Password" else "Change Password", state.isProcessing) { localError = when { hasPassword && old.isBlank() -> "Please fill in all fields"; else -> validate() }; if (localError == null) { if (!hasPassword) vm.setPassword(new, confirm, dismiss) else vm.changePassword(old, new, confirm, logout) } }
        }
    }
}

@Composable private fun BugDialog(state: SettingsUiState, vm: SettingsViewModel, green: Color, dismiss: () -> Unit) { var subject by remember { mutableStateOf("") }; var message by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; DialogFrame("Report an Issue", state.isProcessing, dismiss) { GlassField(subject, { subject = it }, "Subject"); GlassField(message, { message = it }, "Describe the problem", singleLine = false); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; ActionButton("Submit Bug Report", state.isProcessing) { error = if (subject.isBlank() || message.isBlank()) "Please fill in the subject and message" else null; if (error == null) vm.submitBug(subject, message, dismiss) } } }

@Composable private fun DeleteAccountDialog(state: SettingsUiState, vm: SettingsViewModel, green: Color, dismiss: () -> Unit, deleted: () -> Unit) { val needsPassword = state.user?.hasPassword == true; var password by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var feedback by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; DialogFrame("Delete Account", state.isProcessing, dismiss) { Text("This action is permanent and cannot be undone.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold); if (needsPassword) GlassField(password, { password = it }, "Password", true) else GlassField(confirmation, { confirmation = it }, "Type DELETE"); GlassField(reason, { reason = it }, "Reason (optional)"); GlassField(feedback, { feedback = it }, "Additional feedback (optional)", singleLine = false); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button({ error = when { needsPassword && password.isBlank() -> "Please enter your password"; !needsPassword && confirmation != "DELETE" -> "Please type DELETE in capital letters"; else -> null }; if (error == null) vm.deleteAccount(password, confirmation, reason, feedback, deleted) }, Modifier.fillMaxWidth(), enabled = !state.isProcessing, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(16.dp)) { if (state.isProcessing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Permanently Delete Account") } } }

@Composable private fun ConfirmDialog(title: String, body: String, action: String, loading: Boolean, dismiss: () -> Unit, confirm: () -> Unit) = AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { TextButton(confirm, enabled = !loading) { Text(if (loading) "Please wait…" else action) } }, dismissButton = { TextButton(dismiss, enabled = !loading) { Text("Cancel") } }, shape = RoundedCornerShape(24.dp))

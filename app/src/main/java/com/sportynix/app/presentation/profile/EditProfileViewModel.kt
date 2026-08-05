package com.sportynix.app.presentation.profile

import android.content.Context
import android.net.Uri
import coil.imageLoader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.model.User
import com.sportynix.app.data.remote.dto.LocationCityDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "prefer_not_to_say",
    val dobDate: Date = Date(946684800000L), // Jan 1 2000
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val address: String = "",
    val homeDistrict: String = "",
    val homeCity: String = "",
    val homeCityId: Int? = null,
    val homeProvince: String = "",
    val selectedSports: Set<String> = emptySet(),
    val availability: String = "both",
    val isPublicProfile: Boolean = true,
    val isShowContact: Boolean = false,
    val cricketPreferredVariant: String = "all",
    val cricketPrimaryRole: String = "",
    val cricketPlayingPosition: String = "",
    val cricketBattingStyle: String = "",
    val cricketBowlingStyle: String = "",
    val cricketJerseyNumber: String = "",

    val imageUri: Uri? = null,
    val imageWasChanged: Boolean = false,
    val imageWasRemoved: Boolean = false,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSendingEmailLink: Boolean = false,
    val isSendingPhoneOtp: Boolean = false,
    val isVerifyingPhoneOtp: Boolean = false,
    val phoneChallengeId: Int? = null,
    val phoneOtp: String = "",

    val bannerMessage: String? = null,
    val errorMessage: String? = null,

    val showPhoneModal: Boolean = false,
    val showDobPicker: Boolean = false,
    val showLocationPicker: Boolean = false,
    val showRemoveConfirmation: Boolean = false,
    val locationSearch: String = "",
    val locationResults: List<LocationCityDto> = emptyList(),
    val isSearchingLocations: Boolean = false,
    val phoneVerificationError: String? = null
)

sealed class EditProfileEffect {
    object NavigateBack : EditProfileEffect()
    data class ShowToast(val message: String) : EditProfileEffect()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var state by mutableStateOf(EditProfileUiState())
        private set

    private val _effect = MutableSharedFlow<EditProfileEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (state.isLoading || state.isSaving) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            val result = profileRepository.fetchProfile()
            result.onSuccess { dto ->
                val u = dto.toDomain()
                val parsedDob = u.dateOfBirth?.let { parseDobDate(it) } ?: Date(946684800000L)
                state = state.copy(
                    user = u,
                    username = u.username,
                    firstName = u.firstName,
                    lastName = u.lastName,
                    gender = u.gender,
                    dobDate = parsedDob,
                    email = u.email,
                    phone = u.phone,
                    bio = u.bio.orEmpty(),
                    address = u.address,
                    homeDistrict = u.homeDistrict,
                    homeCity = u.homeCity,
                    homeCityId = u.homeCityId,
                    homeProvince = u.homeProvinceName,
                    selectedSports = u.sportsPreferences.toSet(),
                    availability = u.availability,
                    isPublicProfile = u.isPublicProfile,
                    isShowContact = u.isShowContact,
                    cricketPreferredVariant = u.cricketPreferredVariant,
                    cricketPrimaryRole = normalizePrimaryRole(u.cricketPrimaryRole),
                    cricketPlayingPosition = normalizeOption(u.cricketPlayingPosition, PLAYING_POSITIONS),
                    cricketBattingStyle = normalizeBattingStyle(u.cricketBattingStyle),
                    cricketBowlingStyle = normalizeBowlingStyle(u.cricketBowlingStyle),
                    cricketJerseyNumber = u.cricketJerseyNumber,
                    isLoading = false
                )
            }.onFailure { err ->
                state = state.copy(isLoading = false, errorMessage = err.message)
            }
        }
    }

    fun onUsernameChanged(v: String) { state = state.copy(username = v.lowercase(Locale.US).filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(30), errorMessage = null) }
    fun onFirstNameChanged(v: String) { state = state.copy(firstName = v, errorMessage = null) }
    fun onLastNameChanged(v: String) { state = state.copy(lastName = v, errorMessage = null) }
    fun onGenderChanged(v: String) { state = state.copy(gender = v) }
    fun onDobChanged(date: Date) {
        val maximum = Calendar.getInstance().apply { add(Calendar.YEAR, -13) }.time
        state = state.copy(dobDate = if (date.after(maximum)) maximum else date)
    }
    fun onPhoneChanged(v: String) {
        val digits = v.filter { it.isDigit() }.take(10)
        state = state.copy(phone = digits, errorMessage = null)
    }
    fun onBioChanged(v: String) { state = state.copy(bio = v) }
    fun onAddressChanged(v: String) { state = state.copy(address = v) }
    fun onCitySelected(cityId: Int, cityName: String, districtName: String, provinceName: String) {
        state = state.copy(
            homeCityId = cityId,
            homeCity = cityName,
            homeDistrict = districtName,
            homeProvince = provinceName,
            showLocationPicker = false
        )
    }
    fun toggleSport(sport: String) {
        val set = state.selectedSports.toMutableSet()
        if (set.contains(sport)) set.remove(sport) else set.add(sport)
        state = state.copy(selectedSports = set)
    }
    fun onAvailabilityChanged(v: String) { state = state.copy(availability = v) }
    fun onPublicProfileChanged(v: Boolean) { state = state.copy(isPublicProfile = v) }
    fun onShowContactChanged(v: Boolean) { state = state.copy(isShowContact = v) }

    fun onCricketVariantChanged(v: String) { state = state.copy(cricketPreferredVariant = v) }
    fun onCricketPrimaryRoleChanged(v: String) { state = state.copy(cricketPrimaryRole = v) }
    fun onCricketPlayingPositionChanged(v: String) { state = state.copy(cricketPlayingPosition = v) }
    fun onCricketBattingStyleChanged(v: String) { state = state.copy(cricketBattingStyle = v) }
    fun onCricketBowlingStyleChanged(v: String) { state = state.copy(cricketBowlingStyle = v) }
    fun onCricketJerseyNumberChanged(v: String) { state = state.copy(cricketJerseyNumber = v.filter { it.isDigit() }) }

    fun onImageSelected(uri: Uri?) {
        if (uri != null) {
            state = state.copy(imageUri = uri, imageWasChanged = true, imageWasRemoved = false)
        }
    }
    fun onRemovePhotoRequested() {
        state = state.copy(showRemoveConfirmation = true)
    }
    fun confirmRemovePhoto() {
        state = state.copy(
            imageUri = null,
            imageWasChanged = false,
            imageWasRemoved = true,
            showRemoveConfirmation = false
        )
    }
    fun dismissRemoveConfirmation() {
        state = state.copy(showRemoveConfirmation = false)
    }

    fun setShowPhoneModal(show: Boolean) { state = state.copy(showPhoneModal = show, phoneOtp = "", phoneChallengeId = if (show) state.phoneChallengeId else null, phoneVerificationError = null) }
    fun setShowDobPicker(show: Boolean) { state = state.copy(showDobPicker = show) }
    fun setShowLocationPicker(show: Boolean) {
        state = state.copy(showLocationPicker = show, locationSearch = if (show) state.locationSearch else "", locationResults = if (show) state.locationResults else emptyList())
        if (show && state.locationResults.isEmpty()) searchLocations("")
    }
    fun onPhoneOtpChanged(v: String) { state = state.copy(phoneOtp = v.filter { it.isDigit() }.take(6), phoneVerificationError = null) }

    fun searchLocations(query: String) {
        state = state.copy(locationSearch = query)
        viewModelScope.launch {
            delay(300)
            if (state.locationSearch != query) return@launch
            state = state.copy(isSearchingLocations = true)
            profileRepository.getLocationCities(search = query.trim().takeIf { it.isNotEmpty() }).fold(
                onSuccess = { if (state.locationSearch == query) state = state.copy(locationResults = it, isSearchingLocations = false) },
                onFailure = { if (state.locationSearch == query) state = state.copy(isSearchingLocations = false, errorMessage = it.message) }
            )
        }
    }

    fun sendEmailVerificationLink() {
        if (state.isSendingEmailLink) return
        viewModelScope.launch {
            state = state.copy(isSendingEmailLink = true, errorMessage = null)
            val result = profileRepository.resendEmailVerificationLink()
            result.onSuccess {
                state = state.copy(isSendingEmailLink = false, bannerMessage = "Verification link sent to your email")
            }.onFailure { err ->
                state = state.copy(isSendingEmailLink = false, errorMessage = err.message)
            }
        }
    }

    fun sendPhoneOtp() {
        if (state.isSendingPhoneOtp) return
        if (!state.phone.matches(Regex("^07\\d{8}$"))) {
            state = state.copy(phoneVerificationError = "Enter a valid phone number in the format 07XXXXXXXX.")
            return
        }
        viewModelScope.launch {
            state = state.copy(isSendingPhoneOtp = true, phoneVerificationError = null)
            val result = profileRepository.sendPhoneOtp(state.phone)
            result.onSuccess { resp ->
                state = state.copy(
                    isSendingPhoneOtp = false,
                    phoneChallengeId = resp.challengeId,
                    bannerMessage = "OTP sent to your phone",
                    phoneVerificationError = resp.error
                )
            }.onFailure { err ->
                state = state.copy(isSendingPhoneOtp = false, phoneVerificationError = err.message)
            }
        }
    }

    fun verifyPhoneOtp() {
        if (state.isVerifyingPhoneOtp) return
        val challengeId = state.phoneChallengeId ?: return
        if (state.phoneOtp.length != 6) {
            state = state.copy(phoneVerificationError = "Enter the 6-digit OTP.")
            return
        }
        viewModelScope.launch {
            state = state.copy(isVerifyingPhoneOtp = true, phoneVerificationError = null)
            val result = profileRepository.verifyPhoneOtp(challengeId, state.phoneOtp)
            result.onSuccess { userDto ->
                state = state.copy(
                    user = userDto.toDomain(),
                    isVerifyingPhoneOtp = false,
                    showPhoneModal = false,
                    phoneOtp = "",
                    phoneChallengeId = null,
                    bannerMessage = "Phone number verified successfully"
                )
            }.onFailure { err ->
                state = state.copy(isVerifyingPhoneOtp = false, phoneVerificationError = err.message)
            }
        }
    }

    fun saveProfile(context: Context) {
        if (state.isSaving) return
        val user = state.user
        val normalizedUsername = state.username.trim().lowercase()
        val currentUsername = user?.username.orEmpty().trim().lowercase()
        val isUsernameChanging = currentUsername.isNotEmpty() && normalizedUsername != currentUsername
        val trimmedPhone = state.phone.trim()

        if (normalizedUsername.isBlank()) {
            state = state.copy(errorMessage = "Username is required.")
            return
        }
        if (!normalizedUsername.matches(Regex("^[a-z0-9_-]{4,30}$"))) {
            state = state.copy(errorMessage = "Username must be 4-30 chars using lowercase letters, numbers, - or _.")
            return
        }
        if (state.firstName.trim().isEmpty() || state.lastName.trim().isEmpty()) {
            state = state.copy(errorMessage = "First name and last name are required.")
            return
        }
        if (calculateAge(state.dobDate) < 13) {
            state = state.copy(errorMessage = "You must be at least 13 years old to use this app.")
            return
        }
        if (isUsernameChanging) {
            if (user?.usernameChangesRemaining ?: 3 <= 0) {
                state = state.copy(errorMessage = "You have reached the username change limit.")
                return
            }
            if (user?.canChangeUsernameNow == false || (user?.usernameChangeCooldownDaysRemaining ?: 0) > 0) {
                val cooldown = user?.usernameChangeCooldownDaysRemaining ?: 1
                state = state.copy(errorMessage = "You can change your username again in $cooldown day(s).")
                return
            }
        }
        if (!trimmedPhone.matches(Regex("^07\\d{8}$"))) {
            state = state.copy(errorMessage = "Enter a valid phone number in the format 07XXXXXXXX.")
            return
        }
        val savedPhone = user?.phone.orEmpty().trim()
        if (trimmedPhone != savedPhone && !(user?.phoneVerifiedAt != null && user.phone == trimmedPhone)) {
            state = state.copy(errorMessage = "Please verify your changed phone number before saving.", showPhoneModal = true)
            return
        }

        val dobFormatted = formatDateForApi(state.dobDate)

        viewModelScope.launch {
            state = state.copy(isSaving = true, errorMessage = null)
            val result = profileRepository.updateProfile(
                context = context,
                username = normalizedUsername,
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                gender = state.gender,
                dateOfBirth = dobFormatted,
                phone = trimmedPhone,
                bio = state.bio.trim(),
                address = state.address.trim(),
                homeDistrict = state.homeDistrict,
                homeCity = state.homeCity,
                homeCityId = state.homeCityId,
                sportsPreferences = state.selectedSports.toList(),
                availability = state.availability,
                isPublicProfile = state.isPublicProfile,
                isShowContact = state.isShowContact,
                cricketPreferredVariant = state.cricketPreferredVariant,
                cricketPrimaryRole = state.cricketPrimaryRole,
                cricketPlayingPosition = state.cricketPlayingPosition,
                cricketBattingStyle = state.cricketBattingStyle,
                cricketBowlingStyle = state.cricketBowlingStyle,
                cricketJerseyNumber = state.cricketJerseyNumber,
                imageUri = if (state.imageWasChanged) state.imageUri else null,
                removeImage = state.imageWasRemoved
            )

            result.onSuccess { updated ->
                val mapped = updated.toDomain()
                if (state.imageWasChanged || state.imageWasRemoved) {
                    context.imageLoader.memoryCache?.clear()
                    state.user?.avatarUrl?.let { oldUrl ->
                        context.imageLoader.diskCache?.remove(oldUrl)
                    }
                    mapped.avatarUrl?.let { newUrl ->
                        context.imageLoader.diskCache?.remove(newUrl)
                    }
                }
                state = state.copy(
                    user = mapped,
                    imageUri = null,
                    imageWasChanged = false,
                    imageWasRemoved = false,
                    isSaving = false,
                    bannerMessage = "Profile updated successfully"
                )
                _effect.emit(EditProfileEffect.ShowToast("Profile updated successfully"))
                delay(700)
                _effect.emit(EditProfileEffect.NavigateBack)
            }.onFailure { err ->
                state = state.copy(isSaving = false, errorMessage = err.message ?: "Failed to update profile")
            }
        }
    }

    fun clearBanner() {
        state = state.copy(bannerMessage = null)
    }

    fun clearError() {
        state = state.copy(errorMessage = null)
    }

    private fun parseDobDate(dobString: String): Date? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dobString)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDateForApi(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun calculateAge(dob: Date): Int {
        val dobCal = Calendar.getInstance().apply { time = dob }
        val nowCal = Calendar.getInstance()
        var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    private fun selectorKey(value: String) = value.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
    private fun normalizeOption(value: String, options: List<String>, aliases: Map<String, String> = emptyMap()): String {
        if (value.isBlank() || selectorKey(value) == "none") return ""
        val key = selectorKey(value)
        return options.firstOrNull { selectorKey(it) == key }
            ?: aliases[key]?.let { target -> options.firstOrNull { selectorKey(it) == selectorKey(target) } }
            ?: ""
    }
    private fun normalizePrimaryRole(value: String) = normalizeOption(value, PRIMARY_ROLES, mapOf(
        "batter" to "Batsman", "allround" to "All-Rounder", "allrounder" to "All-Rounder", "wicketkeeperbatsman" to "Wicketkeeper"
    ))
    private fun normalizeBattingStyle(value: String) = normalizeOption(value, BATTING_STYLES, mapOf(
        "righthandbat" to "Right Hand", "righthanded" to "Right Hand", "lefthandbat" to "Left Hand", "lefthanded" to "Left Hand"
    ))
    private fun normalizeBowlingStyle(value: String) = normalizeOption(value, BOWLING_STYLES, mapOf(
        "rightarmoffspin" to "Right Arm Spin", "rightarmlegspin" to "Right Arm Spin", "leftarmorthodox" to "Left Arm Spin", "leftarmchinaman" to "Left Arm Spin"
    ))

    companion object {
        val PRIMARY_ROLES = listOf("All-Rounder", "Batsman", "Bowler", "Wicketkeeper")
        val PLAYING_POSITIONS = listOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper", "Top Order Batter", "Middle Order Batter", "Finisher", "Opening Bowler", "Strike Bowler")
        val BATTING_STYLES = listOf("Right Hand", "Left Hand")
        val BOWLING_STYLES = listOf("Right Arm Fast", "Right Arm Medium", "Right Arm Spin", "Left Arm Fast", "Left Arm Medium", "Left Arm Spin")
    }
}

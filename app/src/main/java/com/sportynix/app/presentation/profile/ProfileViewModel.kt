package com.sportynix.app.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.data.repository.ProfileRepository
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isDarkTheme: Boolean = true,
    val showImageModal: Boolean = false,
    val showPhoneVerifyModal: Boolean = false,
    val phoneInput: String = "",
    val phoneOtpCode: String = "",
    val phoneChallengeId: Int? = null,
    val isPhoneSending: Boolean = false,
    val isPhoneVerifying: Boolean = false,
    val phoneVerifyError: String? = null
)

sealed class ProfileUiEffect {
    object NavigateToLogin : ProfileUiEffect()
    data class ShowToast(val message: String) : ProfileUiEffect()
    data class NavigateTo(val route: String) : ProfileUiEffect()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(ProfileUiState())
        private set

    private val _effect = MutableSharedFlow<ProfileUiEffect>()
    val effect = _effect.asSharedFlow()

    init {
        observeCurrentUser()
        loadProfile()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            profileRepository.currentUser.collect { dto ->
                if (dto != null) {
                    state = state.copy(user = dto.toDomain())
                }
            }
        }
    }

    fun loadProfile(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                state = state.copy(isRefreshing = true, errorMessage = null)
            } else {
                state = state.copy(isLoading = state.user == null, errorMessage = null)
            }

            val result = profileRepository.fetchProfile()
            result.onSuccess { dto ->
                state = state.copy(
                    user = dto.toDomain(),
                    isLoading = false,
                    isRefreshing = false
                )
            }.onFailure { err ->
                state = state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = err.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effect.emit(ProfileUiEffect.NavigateToLogin)
        }
    }

    fun setImageModalVisible(visible: Boolean) {
        state = state.copy(showImageModal = visible)
    }

    fun setPhoneVerifyModalVisible(visible: Boolean) {
        state = state.copy(showPhoneVerifyModal = visible)
    }

    fun updatePhoneInput(input: String) {
        state = state.copy(phoneInput = input, phoneVerifyError = null)
    }

    fun updatePhoneOtp(otp: String) {
        state = state.copy(phoneOtpCode = otp, phoneVerifyError = null)
    }

    fun sendPhoneOtp() {
        val phone = state.phoneInput.trim()
        if (phone.isEmpty()) return
        viewModelScope.launch {
            state = state.copy(isPhoneSending = true, phoneVerifyError = null)
            val result = profileRepository.sendPhoneOtp(phone)
            result.onSuccess { resp ->
                state = state.copy(
                    isPhoneSending = false,
                    phoneChallengeId = resp.challengeId
                )
            }.onFailure { err ->
                state = state.copy(
                    isPhoneSending = false,
                    phoneVerifyError = err.message ?: "Failed to send OTP"
                )
            }
        }
    }

    fun verifyPhoneOtp() {
        val challengeId = state.phoneChallengeId ?: return
        val otp = state.phoneOtpCode.trim()
        if (otp.length != 6) return

        viewModelScope.launch {
            state = state.copy(isPhoneVerifying = true, phoneVerifyError = null)
            val result = profileRepository.verifyPhoneOtp(challengeId, otp)
            result.onSuccess { userDto ->
                state = state.copy(
                    user = userDto.toDomain(),
                    isPhoneVerifying = false,
                    showPhoneVerifyModal = false,
                    phoneInput = "",
                    phoneOtpCode = "",
                    phoneChallengeId = null
                )
                _effect.emit(ProfileUiEffect.ShowToast("Phone number verified successfully!"))
            }.onFailure { err ->
                state = state.copy(
                    isPhoneVerifying = false,
                    phoneVerifyError = err.message ?: "Invalid OTP"
                )
            }
        }
    }

    fun toggleTheme() {
        state = state.copy(isDarkTheme = !state.isDarkTheme)
    }
}

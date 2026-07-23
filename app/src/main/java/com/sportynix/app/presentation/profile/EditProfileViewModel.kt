package com.sportynix.app.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.data.remote.api.UserApiService
import com.sportynix.app.data.remote.dto.PhoneOtpVerifyRequestDto
import com.sportynix.app.data.remote.dto.PhoneVerifyRequestDto
import com.sportynix.app.data.remote.dto.UpdateProfileRequestDto
import com.sportynix.app.data.mapper.toDomain
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val bio: String = "",
    val phone: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    // Phone verify flow
    val showPhoneVerifySheet: Boolean = false,
    val phoneVerifyChallengeId: Int? = null,
    val phoneOtp: String = "",
    val isVerifyingPhone: Boolean = false,
    val phoneVerifyError: String? = null
)

sealed class EditProfileEffect {
    object NavigateBack : EditProfileEffect()
    data class ShowSnackbar(val message: String) : EditProfileEffect()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userApiService: UserApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(EditProfileUiState())
        private set

    private val _effect = MutableSharedFlow<EditProfileEffect>()
    val effect = _effect.asSharedFlow()

    init { loadUser() }

    private fun loadUser() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val resp = userApiService.getCurrentUser()
                if (resp.isSuccessful && resp.body() != null) {
                    val dto = resp.body()!!
                    val user = dto.toDomain()
                    state = state.copy(
                        user = user,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        username = user.username,
                        bio = user.bio ?: "",
                        phone = user.phone,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun onFirstNameChanged(v: String) { state = state.copy(firstName = v) }
    fun onLastNameChanged(v: String) { state = state.copy(lastName = v) }
    fun onUsernameChanged(v: String) { state = state.copy(username = v) }
    fun onBioChanged(v: String) { state = state.copy(bio = v) }
    fun onPhoneChanged(v: String) { state = state.copy(phone = v) }
    fun onLocationChanged(v: String) { state = state.copy(location = v) }
    fun onPhoneOtpChanged(v: String) { state = state.copy(phoneOtp = v) }

    fun saveProfile() {
        viewModelScope.launch {
            state = state.copy(isSaving = true, errorMessage = null)
            try {
                val request = UpdateProfileRequestDto(
                    firstName = state.firstName.ifBlank { null },
                    lastName = state.lastName.ifBlank { null },
                    username = state.username.ifBlank { null },
                    bio = state.bio.ifBlank { null },
                    phoneNumber = state.phone.ifBlank { null },
                    location = state.location.ifBlank { null }
                )
                val resp = userApiService.updateProfile(request)
                if (resp.isSuccessful) {
                    state = state.copy(isSaving = false, successMessage = "Profile updated successfully")
                    _effect.emit(EditProfileEffect.ShowSnackbar("Profile updated successfully"))
                    // Refresh auth repo user
                    authRepository.getCurrentUser()
                } else {
                    val error = resp.errorBody()?.string() ?: "Update failed"
                    state = state.copy(isSaving = false, errorMessage = error)
                    _effect.emit(EditProfileEffect.ShowSnackbar(error))
                }
            } catch (e: Exception) {
                state = state.copy(isSaving = false, errorMessage = e.message)
                _effect.emit(EditProfileEffect.ShowSnackbar(e.message ?: "Update failed"))
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            state = state.copy(isUploadingAvatar = true)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestBody)
                val resp = userApiService.uploadAvatar(part)
                if (resp.isSuccessful && resp.body() != null) {
                    val updated = resp.body()!!.toDomain()
                    state = state.copy(
                        user = updated,
                        isUploadingAvatar = false
                    )
                    _effect.emit(EditProfileEffect.ShowSnackbar("Avatar updated"))
                } else {
                    state = state.copy(isUploadingAvatar = false)
                    _effect.emit(EditProfileEffect.ShowSnackbar("Failed to upload avatar"))
                }
            } catch (e: Exception) {
                state = state.copy(isUploadingAvatar = false)
                _effect.emit(EditProfileEffect.ShowSnackbar(e.message ?: "Upload failed"))
            }
        }
    }

    fun requestPhoneVerification() {
        val phone = state.phone.trim()
        if (phone.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isVerifyingPhone = true, phoneVerifyError = null)
            try {
                val resp = userApiService.requestPhoneVerification(PhoneVerifyRequestDto(phone))
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    state = state.copy(
                        phoneVerifyChallengeId = body.challengeId,
                        showPhoneVerifySheet = true,
                        isVerifyingPhone = false
                    )
                } else {
                    state = state.copy(isVerifyingPhone = false, phoneVerifyError = "Failed to send OTP")
                }
            } catch (e: Exception) {
                state = state.copy(isVerifyingPhone = false, phoneVerifyError = e.message)
            }
        }
    }

    fun verifyPhoneOtp() {
        val challengeId = state.phoneVerifyChallengeId ?: return
        val otp = state.phoneOtp.trim()
        if (otp.length < 4) {
            state = state.copy(phoneVerifyError = "Enter the OTP")
            return
        }
        viewModelScope.launch {
            state = state.copy(isVerifyingPhone = true, phoneVerifyError = null)
            try {
                val resp = userApiService.verifyPhoneOtp(PhoneOtpVerifyRequestDto(challengeId, otp))
                if (resp.isSuccessful) {
                    state = state.copy(
                        showPhoneVerifySheet = false,
                        isVerifyingPhone = false,
                        phoneOtp = ""
                    )
                    _effect.emit(EditProfileEffect.ShowSnackbar("Phone verified successfully!"))
                } else {
                    state = state.copy(isVerifyingPhone = false, phoneVerifyError = "Invalid OTP")
                }
            } catch (e: Exception) {
                state = state.copy(isVerifyingPhone = false, phoneVerifyError = e.message)
            }
        }
    }

    fun dismissPhoneVerifySheet() {
        state = state.copy(showPhoneVerifySheet = false, phoneOtp = "", phoneVerifyError = null)
    }
}

package com.sportynix.app.presentation.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.usecase.auth.ForgotPasswordUseCase
import com.sportynix.app.domain.usecase.auth.ResetPasswordUseCase
import com.sportynix.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val emailInput: String = "",
    val otpInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val sessionId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSocialUser: Boolean = false,
    val canSetPassword: Boolean = false
)

sealed class ForgotPasswordUiEffect {
    data class NavigateToOtp(val sessionId: String, val email: String, val isSocialUser: Boolean = false, val canSetPassword: Boolean = false) : ForgotPasswordUiEffect()
    data class NavigateToReset(val sessionId: String, val email: String, val otpCode: String) : ForgotPasswordUiEffect()
    object NavigateToLogin : ForgotPasswordUiEffect()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(ForgotPasswordUiState())
        private set

    private val _effect = MutableSharedFlow<ForgotPasswordUiEffect>()
    val effect = _effect.asSharedFlow()

    fun onEmailChanged(email: String) { state = state.copy(emailInput = email, errorMessage = null) }
    fun onOtpChanged(otp: String) { state = state.copy(otpInput = otp.filter(Char::isDigit).take(6), errorMessage = null) }
    fun onNewPasswordChanged(pass: String) { state = state.copy(newPasswordInput = pass, errorMessage = null) }
    fun onConfirmPasswordChanged(pass: String) { state = state.copy(confirmPasswordInput = pass, errorMessage = null) }

    fun sendResetLink() {
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = forgotPasswordUseCase(state.emailInput)) {
                is ApiResult.Success -> {
                    state = state.copy(
                        isLoading = false,
                        sessionId = result.data.sessionId,
                        isSocialUser = result.data.isSocialUser,
                        canSetPassword = result.data.canSetPassword,
                        successMessage = result.data.message
                    )
                    _effect.emit(ForgotPasswordUiEffect.NavigateToOtp(result.data.sessionId, state.emailInput.trim(), result.data.isSocialUser, result.data.canSetPassword))
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.ServerError -> state = state.copy(isLoading = false, errorMessage = result.message)
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "Failed to send reset link. Please try again.")
                }
            }
        }
    }

    fun clearMessage() { state = state.copy(errorMessage = null, successMessage = null) }

    fun verifyResetOtp(email: String) {
        val code = state.otpInput.trim()
        if (code.length != 6 || !code.all(Char::isDigit)) {
            state = state.copy(errorMessage = "Please enter a valid 6-digit code")
            return
        }
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null, emailInput = email)
            when (val result = authRepository.verifyPasswordResetOtp(email.trim(), code)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false)
                    _effect.emit(ForgotPasswordUiEffect.NavigateToReset(state.sessionId.orEmpty(), email.trim(), code))
                }
                is ApiResult.Error -> state = state.copy(isLoading = false, otpInput = "", errorMessage = result.message)
                is ApiResult.ServerError -> state = state.copy(isLoading = false, otpInput = "", errorMessage = result.message)
                else -> state = state.copy(isLoading = false, errorMessage = "Could not verify code")
            }
        }
    }

    fun submitResetPassword() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = resetPasswordUseCase(state.emailInput, state.otpInput, state.newPasswordInput, state.confirmPasswordInput)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false, successMessage = "Password reset successfully!")
                    _effect.emit(ForgotPasswordUiEffect.NavigateToLogin)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.ServerError -> state = state.copy(isLoading = false, errorMessage = result.message)
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "Failed to reset password. Please check input.")
                }
            }
        }
    }
}

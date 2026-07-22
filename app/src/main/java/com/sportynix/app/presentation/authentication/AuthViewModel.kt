package com.sportynix.app.presentation.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.usecase.auth.LoginUseCase
import com.sportynix.app.domain.usecase.auth.SignUpUseCase
import com.sportynix.app.domain.usecase.auth.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val usernameInput: String = "",
    val firstNameInput: String = "",
    val lastNameInput: String = "",
    val phoneInput: String = "",
    val dobInput: String = "",
    val referralCodeInput: String = "",
    val agreeToTerms: Boolean = false,
    val usernameAvailable: Boolean? = null,
    val isCheckingUsername: Boolean = false,
    val sessionId: String? = null,
    val otpInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentStep: Int = 1
)

sealed class AuthUiEffect {
    object NavigateToHome : AuthUiEffect()
    data class NavigateToOtp(val sessionId: String, val phone: String, val email: String) : AuthUiEffect()
    data class ShowToast(val message: String) : AuthUiEffect()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val checkUsernameUseCase: com.sportynix.app.domain.usecase.auth.CheckUsernameUseCase
) : ViewModel() {

    var state by mutableStateOf(AuthUiState())
        private set

    private val _effect = MutableSharedFlow<AuthUiEffect>()
    val effect = _effect.asSharedFlow()

    private var usernameCheckJob: Job? = null

    fun onEmailChanged(email: String) { state = state.copy(emailInput = email, errorMessage = null) }
    fun onPasswordChanged(password: String) { state = state.copy(passwordInput = password, errorMessage = null) }
    fun onConfirmPasswordChanged(confirm: String) { state = state.copy(confirmPasswordInput = confirm, errorMessage = null) }
    fun onFirstNameChanged(name: String) { state = state.copy(firstNameInput = name, errorMessage = null) }
    fun onLastNameChanged(name: String) { state = state.copy(lastNameInput = name, errorMessage = null) }
    fun onPhoneChanged(phone: String) { state = state.copy(phoneInput = phone, errorMessage = null) }
    fun onDobChanged(dob: String) { state = state.copy(dobInput = dob, errorMessage = null) }
    fun onReferralChanged(code: String) { state = state.copy(referralCodeInput = code, errorMessage = null) }
    fun onTermsToggled(agree: Boolean) { state = state.copy(agreeToTerms = agree, errorMessage = null) }
    fun onOtpChanged(otp: String) { state = state.copy(otpInput = otp, errorMessage = null) }
    fun setStep(step: Int) { state = state.copy(currentStep = step) }

    fun onUsernameChanged(username: String) {
        val trimmed = username.trim().lowercase()
        state = state.copy(usernameInput = trimmed, usernameAvailable = null, errorMessage = null)
        usernameCheckJob?.cancel()

        if (trimmed.length >= 4) {
            usernameCheckJob = viewModelScope.launch {
                delay(500)
                state = state.copy(isCheckingUsername = true)
                when (val result = checkUsernameUseCase(trimmed)) {
                    is ApiResult.Success -> {
                        state = state.copy(isCheckingUsername = false, usernameAvailable = result.data)
                    }
                    else -> {
                        state = state.copy(isCheckingUsername = false, usernameAvailable = false)
                    }
                }
            }
        }
    }

    fun login() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = loginUseCase(state.emailInput, state.passwordInput)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false)
                    _effect.emit(AuthUiEffect.NavigateToHome)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.ServerError -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.Unauthorized -> {
                    state = state.copy(isLoading = false, errorMessage = "Invalid username/email or password")
                }
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "Login failed. Please check network connection.")
                }
            }
        }
    }

    fun signUp() {
        if (!state.agreeToTerms) {
            state = state.copy(errorMessage = "You must agree to the Terms & Conditions")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            val result = signUpUseCase(
                username = state.usernameInput,
                firstName = state.firstNameInput,
                lastName = state.lastNameInput,
                email = state.emailInput,
                phone = state.phoneInput,
                dob = state.dobInput,
                pass = state.passwordInput,
                referralCode = state.referralCodeInput
            )
            when (result) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false, sessionId = result.data)
                    _effect.emit(AuthUiEffect.NavigateToOtp(result.data, state.phoneInput, state.emailInput))
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                is ApiResult.ServerError -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "Sign up failed. Please try again.")
                }
            }
        }
    }

    fun verifyOtp(sessionId: String, otpCode: String = state.otpInput) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = verifyOtpUseCase.verifySignUpOtp(sessionId, otpCode)) {
                is ApiResult.Success -> {
                    state = state.copy(isLoading = false)
                    _effect.emit(AuthUiEffect.NavigateToHome)
                }
                is ApiResult.Error -> {
                    state = state.copy(isLoading = false, errorMessage = result.message)
                }
                else -> {
                    state = state.copy(isLoading = false, errorMessage = "OTP Verification failed. Please check code.")
                }
            }
        }
    }
}

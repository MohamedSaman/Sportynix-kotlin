package com.sportynix.app.presentation.authentication

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.auth.AuthValidators
import com.sportynix.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmailVerificationState(val otp: String = "", val loading: Boolean = false, val resending: Boolean = false, val error: String? = null)
sealed class EmailVerificationEffect { data object Verified : EmailVerificationEffect(); data object Resent : EmailVerificationEffect() }

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    var state by mutableStateOf(EmailVerificationState()); private set
    private val _effects = MutableSharedFlow<EmailVerificationEffect>(); val effects = _effects.asSharedFlow()
    fun onOtpChanged(value: String) { state = state.copy(otp = AuthValidators.otp(value), error = null) }
    fun verify(email: String) { if (state.loading || state.otp.length != 6) return; viewModelScope.launch {
        state = state.copy(loading = true, error = null)
        when (val result = repository.verifyEmailOtp(email, state.otp)) {
            is ApiResult.Success -> { state = state.copy(loading = false); _effects.emit(EmailVerificationEffect.Verified) }
            is ApiResult.Error -> state = state.copy(loading = false, otp = "", error = result.message)
            is ApiResult.ServerError -> state = state.copy(loading = false, otp = "", error = result.message)
            else -> state = state.copy(loading = false, otp = "", error = "Verification failed")
        }
    } }
    fun resend(email: String) { if (state.resending) return; viewModelScope.launch {
        state = state.copy(resending = true, error = null)
        when (val result = repository.resendEmailOtp(email)) {
            is ApiResult.Success -> { state = state.copy(resending = false, otp = ""); _effects.emit(EmailVerificationEffect.Resent) }
            is ApiResult.Error -> state = state.copy(resending = false, error = result.message)
            is ApiResult.ServerError -> state = state.copy(resending = false, error = result.message)
            else -> state = state.copy(resending = false, error = "Failed to resend code")
        }
    } }
}

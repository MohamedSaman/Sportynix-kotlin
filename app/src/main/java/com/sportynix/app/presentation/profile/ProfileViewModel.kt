package com.sportynix.app.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.network.ApiResult
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
    val showPhoneVerifyModal: Boolean = false
)

sealed class ProfileUiEffect {
    object NavigateToLogin : ProfileUiEffect()
    data class ShowToast(val message: String) : ProfileUiEffect()
    data class NavigateTo(val route: String) : ProfileUiEffect()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(ProfileUiState())
        private set

    private val _effect = MutableSharedFlow<ProfileUiEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                state = state.copy(isRefreshing = true, errorMessage = null)
            } else {
                state = state.copy(isLoading = true, errorMessage = null)
            }

            when (val result = authRepository.getCurrentUser()) {
                is ApiResult.Success -> {
                    state = state.copy(
                        user = result.data,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                is ApiResult.Unauthorized -> {
                    state = state.copy(isLoading = false, isRefreshing = false)
                    _effect.emit(ProfileUiEffect.NavigateToLogin)
                }
                is ApiResult.Error -> {
                    state = state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.ServerError -> {
                    state = state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    state = state.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
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

    fun toggleTheme() {
        state = state.copy(isDarkTheme = !state.isDarkTheme)
    }
}

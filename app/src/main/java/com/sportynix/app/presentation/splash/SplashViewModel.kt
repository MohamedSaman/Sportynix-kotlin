package com.sportynix.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.core.datastore.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashEvent {
    object NavigateToHome : SplashEvent()
    object NavigateToLogin : SplashEvent()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    // Replay the one-shot resolution because DataStore can resolve before the
    // first composition starts collecting.
    private val _eventFlow = MutableSharedFlow<SplashEvent>(replay = 1)
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn.firstOrNull() ?: false
            val accessToken = sessionManager.accessToken.firstOrNull()
            val refreshToken = sessionManager.refreshToken.firstOrNull()
            val hasCachedUser = !sessionManager.userId.firstOrNull().isNullOrBlank()
            val hasUsableSession = isLoggedIn && hasCachedUser &&
                !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()

            if (hasUsableSession) {
                _eventFlow.emit(SplashEvent.NavigateToHome)
            } else {
                _eventFlow.emit(SplashEvent.NavigateToLogin)
            }
        }
    }
}

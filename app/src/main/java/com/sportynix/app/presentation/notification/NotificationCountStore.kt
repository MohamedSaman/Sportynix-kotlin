package com.sportynix.app.presentation.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationCountStore @Inject constructor() {
    private val _refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshRequests = _refreshRequests.asSharedFlow()
    fun requestRefresh() { _refreshRequests.tryEmit(Unit) }
}

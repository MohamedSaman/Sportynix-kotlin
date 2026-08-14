package com.sportynix.app.core.bootstrap

import android.content.Context
import com.sportynix.app.core.media.MediaDownloadQueueManager
import com.sportynix.app.core.network.ConnectivityService
import com.sportynix.app.data.remote.websocket.ChatWebSocketService
import com.sportynix.app.data.repository.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatBootstrap @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val webSocketService: ChatWebSocketService,
    private val connectivityService: ConnectivityService,
    private val mediaDownloadQueueManager: MediaDownloadQueueManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        Timber.d("Initializing ChatBootstrap...")

        scope.launch {
            connectivityService.isOnline.collect { isOnline ->
                if (isOnline) {
                    Timber.d("Network is online. Starting full sync workflow...")
                    syncManager.runSync()
                    mediaDownloadQueueManager.flush()
                } else {
                    Timber.d("Network is offline. Postponing sync workflow.")
                }
            }
        }
    }
}

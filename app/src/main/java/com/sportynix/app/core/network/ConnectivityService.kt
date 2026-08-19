package com.sportynix.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.sportynix.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var healthCheckJob: Job? = null
    
    private var hasSystemNetwork = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("System network available")
            hasSystemNetwork = true
            startHealthCheck()
        }

        override fun onLost(network: Network) {
            Timber.d("System network lost")
            hasSystemNetwork = false
            stopHealthCheck()
            _isOnline.value = false
        }
    }

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        // Initial state check
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            hasSystemNetwork = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            if (hasSystemNetwork) {
                startHealthCheck()
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun startHealthCheck() {
        if (healthCheckJob?.isActive == true) return

        healthCheckJob = scope.launch {
            while (isActive && hasSystemNetwork) {
                val isReachable = performHealthCheck()
                if (_isOnline.value != isReachable) {
                    Timber.d("Backend reachability changed: \$isReachable")
                    _isOnline.value = isReachable
                }
                
                // If we are online, check less frequently. If offline but system network is available, check more frequently
                val delayMs = if (isReachable) 30_000L else 5_000L
                delay(delayMs)
            }
        }
    }

    private fun stopHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    private suspend fun performHealthCheck(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL.trimEnd('/')}/health/")
                .build()
                
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

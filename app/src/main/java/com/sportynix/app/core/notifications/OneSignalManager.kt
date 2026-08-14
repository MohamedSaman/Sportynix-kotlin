package com.sportynix.app.core.notifications

import android.content.Context
import com.google.gson.Gson
import com.onesignal.OneSignal
import com.sportynix.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneSignalManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var initialized = false
    private val ONESIGNAL_APP_ID = "e446828f-ee85-4252-88bb-9deb2f35cfdf"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // We would inject a Router or Navigator here to handle deep links
    // private var router: Router? = null

    fun initialize() {
        if (initialized) return

        try {
            OneSignal.initWithContext(context)
            OneSignal.setAppId(ONESIGNAL_APP_ID)
            
            // Note: OneSignal 4.x/5.x initialization requires handling NotificationOpenedHandler
            // which usually goes here. For simplicity in this port, we set up a generic listener.
            OneSignal.setNotificationOpenedHandler { result ->
                val action = result.action.actionId
                val data = result.notification.additionalData
                val title = result.notification.title
                val body = result.notification.body
                val notificationId = result.notification.notificationId

                Timber.d("OneSignal: Notification clicked with data: \$data")
                
                val notificationType = data?.optString("type") ?: data?.optString("notification_type") ?: "general"
                
                // Ensure it's synced if it was opened from background/quit state
                syncNotificationWithBackend(title, body, data?.toString(), notificationId)
                
                // Handle navigation deep link
                handleNotificationClick(notificationType, data?.toString())
            }

            initialized = true
            Timber.d("OneSignal initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "OneSignal initialization failed")
        }
    }
    
    fun promptForPushNotifications(fallbackToSettings: Boolean = false) {
        if (!initialized) return
        OneSignal.promptForPushNotifications(fallbackToSettings)
    }

    fun setExternalUserId(userId: String) {
        if (!initialized) return
        OneSignal.setExternalUserId(userId)
    }

    fun removeExternalUserId() {
        if (!initialized) return
        OneSignal.removeExternalUserId()
    }
    
    private fun handleNotificationClick(type: String, rawData: String?) {
        // Router logic for deep linking, maps 'type' to specific destinations
        // Example: if (type == "booking_confirmed") navigate("BookingHistory")
        Timber.d("Routing to deep link for type: \$type")
    }

    private fun syncNotificationWithBackend(title: String?, body: String?, rawData: String?, notificationId: String?) {
        scope.launch {
            try {
                val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("accessToken", null)
                if (token == null) return@launch

                val url = "\${BuildConfig.BASE_URL.trimEnd('/')}/api/notifications/sync_onesignal/"
                val payload = mapOf(
                    "title" to (title ?: "New Notification"),
                    "message" to (body ?: ""),
                    "data" to (rawData?.let { gson.fromJson(it, Map::class.java) } ?: emptyMap<String, Any>()),
                    "type" to "general", // We would extract the inferred type here
                    "onesignal_id" to (notificationId ?: "os_\${System.currentTimeMillis()}")
                )

                val request = Request.Builder()
                    .url(url)
                    .post(gson.toJson(payload).toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("Authorization", "Bearer \$token")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.w("Error syncing onesignal notification: HTTP \${response.code}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing onesignal notification")
            }
        }
    }
    
    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            val token = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("accessToken", null)
            if (token == null) return

            val url = "\${BuildConfig.BASE_URL.trimEnd('/')}/api/notifications/\$notificationId/mark_as_read/"
            val request = Request.Builder()
                .url(url)
                .post("{}".toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("Authorization", "Bearer \$token")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Timber.d("OneSignal: Notification marked as read successfully: \$notificationId")
                } else {
                    Timber.w("OneSignal: Failed to mark notification as read. Status: \${response.code}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OneSignal: Error marking notification as read")
        }
    }
}

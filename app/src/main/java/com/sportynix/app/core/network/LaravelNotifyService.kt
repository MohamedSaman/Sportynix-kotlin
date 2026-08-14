package com.sportynix.app.core.network

import com.sportynix.app.BuildConfig
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
class LaravelNotifyService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // In reality, these should come from BuildConfig or a Secrets manager
    private val laravelNotifyUrl = "\${BuildConfig.BASE_URL.trimEnd('/')}/api/laravel-notify"
    private val laravelWebhookSecret = BuildConfig.LARAVEL_WEBHOOK_SECRET

    fun notifyLaravelBooking(bookingId: Long) {
        if (bookingId <= 0) {
            Timber.w("Invalid booking id: \$bookingId")
            return
        }

        if (laravelWebhookSecret.isNullOrBlank()) {
            Timber.w("Missing LARAVEL_WEBHOOK_SECRET. Skipping notify.")
            return
        }

        scope.launch {
            try {
                Timber.i("Sending Laravel booking notify for bookingId: \$bookingId")
                
                val payload = "{\"booking_id\": \$bookingId}"
                val request = Request.Builder()
                    .url(laravelNotifyUrl)
                    .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("X-Webhook-Secret", laravelWebhookSecret)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Timber.i("Laravel notify succeeded for bookingId: \$bookingId")
                    } else {
                        Timber.e("Laravel notify failed for bookingId: \$bookingId, status: \${response.code}")
                    }
                }
            } catch (e: Exception) {
                Timber.w("Failed to notify Laravel: \${e.message}")
            }
        }
    }

    fun notifyLaravelBookings(bookingIds: List<Long>) {
        Timber.i("Triggered for booking ids: \$bookingIds")
        bookingIds.forEach { id ->
            notifyLaravelBooking(id)
        }
    }
}

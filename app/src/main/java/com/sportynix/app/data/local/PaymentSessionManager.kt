package com.sportynix.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sportynix.app.data.remote.dto.*
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val PENDING_BOOKING_PAYMENT_SESSION_KEY = "pending_booking_payment_session"

data class PendingBookingPaymentSession(
    val bookingData: BookingPayload? = null,
    val bookingType: String = "Normal",
    val payment: PaymentOrderInfoDto? = null,
    val checkout: PaymentCheckoutUrlDto? = null,
    val bookings: List<ConfirmedBookingDto>? = emptyList(),
    val reservationExpiresAt: String? = null,
    val checkoutOpenedAt: String? = null
)

@Singleton
class PaymentSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences("sportynix_payment_prefs", Context.MODE_PRIVATE)

    fun getPendingSession(): PendingBookingPaymentSession? {
        val json = prefs.getString(PENDING_BOOKING_PAYMENT_SESSION_KEY, null) ?: return null
        return try {
            gson.fromJson(json, PendingBookingPaymentSession::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error reading pending payment session")
            null
        }
    }

    fun savePendingSession(session: PendingBookingPaymentSession) {
        val sanitizedCheckout = session.checkout?.copy(nextActionHtml = null)
        val sanitizedSession = session.copy(checkout = sanitizedCheckout)
        prefs.edit().putString(PENDING_BOOKING_PAYMENT_SESSION_KEY, gson.toJson(sanitizedSession)).apply()
    }

    fun clearPendingSession() {
        prefs.edit().remove(PENDING_BOOKING_PAYMENT_SESSION_KEY).apply()
    }
}

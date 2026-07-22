package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Payment

interface PaymentRepository {
    suspend fun processPayment(bookingId: String, amount: Double, paymentMethod: String): ApiResult<Payment>
}

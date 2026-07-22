package com.sportynix.app.domain.model

data class Payment(
    val paymentId: String,
    val bookingId: String,
    val amount: Double,
    val currency: String = "USD",
    val paymentMethod: String,
    val status: PaymentStatus,
    val transactionTimestamp: String
)

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}

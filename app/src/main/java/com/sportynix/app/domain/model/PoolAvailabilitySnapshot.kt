package com.sportynix.app.domain.model

import com.google.gson.annotations.SerializedName

data class PoolAvailabilitySnapshot(
    @SerializedName("occurrence_id") val occurrenceId: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("held") val held: Int,
    @SerializedName("booked") val booked: Int,
    @SerializedName("available") val available: Int,
    @SerializedName("is_fully_available") val isFullyAvailable: Boolean = false,
    @SerializedName("private_booking_available") val privateBookingAvailable: Boolean = false
)

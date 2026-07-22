package com.sportynix.app.data.mapper

import com.sportynix.app.data.local.entity.BookingEntity
import com.sportynix.app.data.local.entity.VenueEntity
import com.sportynix.app.data.remote.dto.BookingDto
import com.sportynix.app.data.remote.dto.TimeSlotDto
import com.sportynix.app.data.remote.dto.UserDto
import com.sportynix.app.data.remote.dto.VenueDto
import com.sportynix.app.domain.model.Booking
import com.sportynix.app.domain.model.BookingStatus
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.domain.model.User
import com.sportynix.app.domain.model.Venue

fun UserDto.toDomain(): User {
    val fullName = when {
        !firstName.isNullOrBlank() || !lastName.isNullOrBlank() -> "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        !username.isNullOrBlank() -> username
        else -> email.substringBefore("@")
    }
    val isEmailVerif = !emailVerifiedAt.isNullOrBlank()
    val isPhoneVerif = !phoneVerifiedAt.isNullOrBlank() || isPhoneVerified == true || mustVerifyPhone == false

    return User(
        id = id,
        username = username.orEmpty(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        name = fullName,
        email = email,
        phone = phone.orEmpty(),
        avatarUrl = avatarUrl,
        bio = bio,
        points = points ?: 0,
        role = role ?: "USER",
        isEmailVerified = isEmailVerif,
        isPhoneVerified = isPhoneVerif,
        mustVerifyPhone = mustVerifyPhone == true
    )
}

fun TimeSlotDto.toDomain(): TimeSlot = TimeSlot(
    id = id,
    startTime = startTime,
    endTime = endTime,
    price = price ?: 0.0,
    isAvailable = isAvailable ?: true
)

fun VenueDto.toDomain(): Venue {
    val imageList = when {
        !imageUrlsList.isNullOrEmpty() -> imageUrlsList
        !imageUrl.isNullOrEmpty() -> listOf(imageUrl)
        else -> emptyList()
    }
    return Venue(
        id = id,
        name = name,
        description = description.orEmpty(),
        sportType = sportType ?: "GENERAL",
        location = location.orEmpty(),
        address = address.orEmpty(),
        pricePerHour = pricePerHour ?: 0.0,
        rating = rating ?: 4.5f,
        reviewCount = reviewCount ?: 0,
        imageUrls = imageList,
        availableSlots = availableSlots?.map { it.toDomain() } ?: emptyList(),
        amenities = amenities ?: emptyList(),
        isFeatured = isFeatured ?: false
    )
}

fun VenueDto.toEntity(): VenueEntity {
    val firstImage = imageUrlsList?.firstOrNull() ?: imageUrl.orEmpty()
    return VenueEntity(
        id = id,
        name = name,
        description = description.orEmpty(),
        sportType = sportType ?: "GENERAL",
        location = location.orEmpty(),
        address = address.orEmpty(),
        pricePerHour = pricePerHour ?: 0.0,
        rating = rating ?: 4.5f,
        reviewCount = reviewCount ?: 0,
        imageUrl = firstImage,
        isFeatured = isFeatured ?: false
    )
}

fun VenueEntity.toDomain(): Venue = Venue(
    id = id,
    name = name,
    description = description,
    sportType = sportType,
    location = location,
    address = address,
    pricePerHour = pricePerHour,
    rating = rating,
    reviewCount = reviewCount,
    imageUrls = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
    availableSlots = emptyList(),
    amenities = emptyList(),
    isFeatured = isFeatured
)

fun BookingDto.toDomain(): Booking = Booking(
    id = id,
    venueId = venueId,
    venueName = venueName.orEmpty(),
    venueImageUrl = venueImageUrl,
    slotTime = slotTime.orEmpty(),
    bookingDate = bookingDate.orEmpty(),
    totalPrice = totalPrice ?: 0.0,
    status = try { BookingStatus.valueOf(status?.uppercase() ?: "PENDING") } catch (e: Exception) { BookingStatus.PENDING },
    createdAt = createdAt.orEmpty()
)

fun BookingDto.toEntity(): BookingEntity = BookingEntity(
    id = id,
    venueId = venueId,
    venueName = venueName.orEmpty(),
    venueImageUrl = venueImageUrl,
    slotTime = slotTime.orEmpty(),
    bookingDate = bookingDate.orEmpty(),
    totalPrice = totalPrice ?: 0.0,
    status = status ?: "PENDING",
    createdAt = createdAt.orEmpty()
)

fun BookingEntity.toDomain(): Booking = Booking(
    id = id,
    venueId = venueId,
    venueName = venueName,
    venueImageUrl = venueImageUrl,
    slotTime = slotTime,
    bookingDate = bookingDate,
    totalPrice = totalPrice,
    status = try { BookingStatus.valueOf(status.uppercase()) } catch (e: Exception) { BookingStatus.PENDING },
    createdAt = createdAt
)

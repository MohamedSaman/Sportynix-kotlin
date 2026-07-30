package com.sportynix.app.data.mapper

import com.sportynix.app.data.local.entity.BookingEntity
import com.sportynix.app.data.local.entity.VenueEntity
import com.sportynix.app.data.remote.dto.*
import com.sportynix.app.domain.model.*

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

fun VenueSportDto.toDomain(): VenueSport = VenueSport(
    id = id ?: 0,
    name = name.orEmpty(),
    price = price.orEmpty(),
    imageUrl = image.orEmpty(),
    rating = rating ?: 0.0f,
    reviewsCount = reviews ?: 0
)

fun VenueReviewDto.toDomain(): VenueReview = VenueReview(
    id = id ?: 0,
    userName = userName ?: "User",
    userAvatar = userAvatar,
    rating = rating ?: 5.0f,
    createdAt = createdAt.orEmpty(),
    comment = comment.orEmpty(),
    recommends = recommends ?: true
)

fun RatingBreakdownDto.toDomain(): RatingBreakdown = RatingBreakdown(
    star5 = star5 ?: 0,
    star4 = star4 ?: 0,
    star3 = star3 ?: 0,
    star2 = star2 ?: 0,
    star1 = star1 ?: 0
)

fun VenueDto.toDomain(): Venue {
    val primaryImage = imageUrlsList?.firstOrNull() ?: imageUrl.orEmpty()
    val imageList = when {
        !imageUrlsList.isNullOrEmpty() -> imageUrlsList
        !imageUrl.isNullOrEmpty() -> listOf(imageUrl)
        else -> emptyList()
    }
    val galleryList = galleryImagesList?.mapNotNull { it.imageUrl } ?: imageList
    return Venue(
        id = id,
        name = name,
        description = description.orEmpty(),
        sportType = sportType ?: "GENERAL",
        location = location.orEmpty(),
        address = address.orEmpty(),
        pricePerHour = pricePerHour ?: 0.0,
        rating = rating ?: 5.0f,
        reviewCount = reviewCount ?: reviewsList?.size ?: 0,
        imageUrl = primaryImage,
        imageUrls = imageList,
        galleryImages = galleryList,
        availableSlots = availableSlots?.map { it.toDomain() } ?: emptyList(),
        amenities = amenities ?: emptyList(),
        isFeatured = isFeatured ?: false,
        distance = distance,
        distanceDisplay = distanceDisplay,
        sports = sports?.map { it.toDomain() } ?: emptyList(),
        reviewsList = reviewsList?.map { it.toDomain() } ?: emptyList(),
        ratingBreakdown = ratingBreakdown?.toDomain() ?: RatingBreakdown()
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
    imageUrl = imageUrl,
    imageUrls = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
    availableSlots = emptyList(),
    amenities = emptyList(),
    isFeatured = isFeatured
)

fun BookingDto.toDomain(): Booking = Booking(
    id = id,
    venueId = venueId.orEmpty(),
    venueName = venueName.orEmpty(),
    venueImageUrl = venueImageUrl,
    sportName = sportName ?: "Badminton",
    slotTime = slotTime.orEmpty(),
    endTime = endTime,
    bookingDate = bookingDate.orEmpty(),
    totalPrice = totalPrice ?: 0.0,
    status = try { BookingStatus.valueOf(status?.uppercase() ?: "PENDING") } catch (e: Exception) { BookingStatus.PENDING },
    financialStatus = financialStatus,
    paymentStatus = paymentStatus,
    paymentAmount = paymentAmount,
    qrCodeUrl = qrCodeUrl,
    bookingReference = bookingReference,
    teamId = team?.id,
    teamName = team?.name,
    teamMembersCount = team?.membersCount ?: 0,
    createdAt = createdAt.orEmpty()
)

fun BookingDto.toEntity(): BookingEntity = BookingEntity(
    id = id,
    venueId = venueId.orEmpty(),
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

fun BookingQuoteResponseDto.toDomain(): QuoteBreakdown = QuoteBreakdown(
    bookingTotal = bookingTotal ?: "0.00",
    advanceRequired = advanceRequired == true,
    advanceAmount = advanceAmount ?: "0.00",
    gatewayAmount = gatewayAmount ?: advanceAmount ?: "0.00",
    remainingBalance = remainingBalance ?: "0.00",
    pointsDiscount = pointsDiscount ?: "0.00",
    acceptedPoints = acceptedPoints ?: 0,
    paymentOption = paymentOption ?: "advance"
)

fun SavedCardDto.toDomain(): SavedCard = SavedCard(
    id = id,
    brand = brand ?: "Card",
    maskedNumber = maskedNumber ?: "**** **** **** $last4",
    last4 = last4 ?: "",
    expiryMonth = expiryMonth,
    expiryYear = expiryYear,
    isDefault = isDefault
)

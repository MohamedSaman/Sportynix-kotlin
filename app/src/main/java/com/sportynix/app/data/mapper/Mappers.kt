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

    return User(
        id = id,
        username = username.orEmpty(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        name = fullName,
        email = email,
        emailVerifiedAt = emailVerifiedAt,
        phoneVerifiedAt = phoneVerifiedAt,
        phone = phone.orEmpty(),
        avatarUrl = avatarUrl,
        bio = bio,
        points = points ?: 0,
        role = role ?: "USER",
        mustVerifyPhone = mustVerifyPhone == true
    )
}

fun UserDataDto.toDomain(): User {
    val full = when {
        !fullName.isNullOrBlank() -> fullName
        !firstName.isNullOrBlank() || !lastName.isNullOrBlank() -> "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        !username.isNullOrBlank() -> username
        else -> email.orEmpty().substringBefore("@")
    }
    return User(
        id = id,
        username = username.orEmpty(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty(),
        name = full,
        email = email.orEmpty(),
        emailVerifiedAt = emailVerifiedAt,
        phoneVerifiedAt = phoneVerifiedAt,
        phone = phoneNumber.orEmpty(),
        mustVerifyPhone = mustVerifyPhone == true,
        gender = gender ?: "prefer_not_to_say",
        dateOfBirth = dateOfBirth,
        avatarUrl = profilePicture,
        bio = bio,
        points = points ?: 0,
        role = "USER",
        address = address.orEmpty(),
        homeDistrict = homeDistrict.orEmpty(),
        homeCity = homeCity.orEmpty(),
        homeCityId = homeCityId,
        homeProvinceName = homeProvinceName.orEmpty(),
        sportsPreferences = sportsPreferences ?: emptyList(),
        availability = availability ?: "both",
        isPublicProfile = isPublicProfile != false,
        isShowContact = isShowContact == true,
        referralCode = referralCode.orEmpty(),
        isSocial = isSocial == true,
        hasPassword = hasPassword != false,
        cricketPreferredVariant = cricketProfile?.preferredVariant ?: "all",
        cricketPrimaryRole = cricketProfile?.primaryRole.orEmpty(),
        cricketPlayingPosition = cricketProfile?.playingPosition.orEmpty(),
        cricketBattingStyle = cricketProfile?.battingStyle.orEmpty(),
        cricketBowlingStyle = cricketProfile?.bowlingStyle.orEmpty(),
        cricketJerseyNumber = cricketProfile?.jerseyNumber?.toString().orEmpty(),
        usernameChangesUsed = usernameChangesUsed ?: 0,
        usernameChangesRemaining = usernameChangesRemaining ?: 3,
        usernameLastChangedAt = usernameLastChangedAt,
        usernameNextChangeAt = usernameNextChangeAt,
        usernameChangeCooldownDaysRemaining = usernameChangeCooldownDaysRemaining ?: 0,
        canChangeUsernameNow = canChangeUsernameNow != false,
        allowDirectTeamAdd = allowDirectTeamAdd == true
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
    rating = (rating ?: 5).toFloat(),
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
        name = name.orEmpty(),
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
        name = name.orEmpty(),
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

fun APIBooking.toDomain(): Booking {
    val statusVal = when (status?.lowercase()?.trim()) {
        "playing" -> "Ongoing"
        "confirmed", "upcoming", "pending" -> "Upcoming"
        "completed" -> "Completed"
        "no-show", "noshow" -> "No-Show"
        else -> "Cancelled"
    }

    return Booking(
        id = id,
        complexName = venue ?: "N/A",
        sport = sport ?: "N/A",
        courtName = court ?: "N/A",
        teamName = teamInfo?.name ?: players ?: "Personal",
        memberCount = teamInfo?.membersCount ?: 0,
        teamId = teamInfo?.id,
        playDateStart = date ?: "N/A",
        playDateEnd = date ?: "N/A",
        timeSlot = time ?: "N/A",
        duration = duration ?: "N/A",
        location = location ?: "N/A",
        price = "LKR ${price ?: "0.00"}",
        slotCount = 0,
        bookingId = id,
        bookedDate = bookedDate ?: "N/A",
        status = statusVal,
        isPermanent = isPermanent == true,
        permanentSourceId = permanentSourceId ?: permanentSource?.id,
        imageURL = image ?: "",
        qrCode = qrCode != null && qrCode != false && qrCode != "false",
        venueId = venueId,
        sportId = sportId,
        reviewId = review?.id ?: reviewId,
        reviewRating = review?.rating ?: reviewRating,
        isChallengeBooking = isChallengeBooking == true,
        opponentTeamName = opponentTeamInfo?.name,
        opponentMemberCount = opponentTeamInfo?.membersCount,
        userId = userId,
        canCancel = canCancel == true,
        createdAt = bookedAt ?: createdAt ?: bookedDate
    )
}

fun BookingDto.toDomain(): Booking = Booking(
    id = id.toIntOrNull() ?: 0,
    complexName = venueName ?: "N/A",
    sport = sportName ?: "N/A",
    courtName = "Court 1",
    teamName = team?.name ?: "Personal",
    memberCount = team?.membersCount ?: 0,
    teamId = team?.id?.toInt(),
    playDateStart = bookingDate ?: "N/A",
    playDateEnd = bookingDate ?: "N/A",
    timeSlot = slotTime ?: "N/A",
    duration = "60 mins",
    location = "N/A",
    price = "LKR ${totalPrice ?: 0.0}",
    slotCount = 1,
    bookingId = id.toIntOrNull() ?: 0,
    bookedDate = bookingDate ?: "N/A",
    status = status ?: "Upcoming",
    isPermanent = false,
    permanentSourceId = null,
    imageURL = venueImageUrl ?: "",
    qrCode = !qrCodeUrl.isNullOrEmpty(),
    qrCodeURL = qrCodeUrl,
    venueId = venueId?.toIntOrNull(),
    sportId = 1,
    reviewId = null,
    reviewRating = null,
    isChallengeBooking = false,
    opponentTeamName = null,
    opponentMemberCount = null,
    userId = null,
    canCancel = true,
    createdAt = createdAt ?: bookingDate
)

fun BookingEntity.toDomain(): Booking = Booking(
    id = id.toIntOrNull() ?: 0,
    complexName = venueName,
    sport = "Badminton",
    courtName = "Court 1",
    teamName = "Personal",
    memberCount = 0,
    teamId = null,
    playDateStart = bookingDate,
    playDateEnd = bookingDate,
    timeSlot = slotTime,
    duration = "60 mins",
    location = "N/A",
    price = "LKR $totalPrice",
    slotCount = 1,
    bookingId = id.toIntOrNull() ?: 0,
    bookedDate = bookingDate,
    status = status,
    isPermanent = false,
    permanentSourceId = null,
    imageURL = venueImageUrl ?: "",
    qrCode = false,
    qrCodeURL = null,
    venueId = venueId.toIntOrNull(),
    sportId = 1,
    reviewId = null,
    reviewRating = null,
    isChallengeBooking = false,
    opponentTeamName = null,
    opponentMemberCount = null,
    userId = null,
    canCancel = true,
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

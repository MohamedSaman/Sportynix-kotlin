package com.sportynix.app.domain.model

enum class SearchResultType { VENUE, SPORT, TEAM }

data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val rating: Float? = null,
    val reviews: Int? = null,
    val distance: Double? = null,
    val price: Double? = null,
    val imageUrl: String? = null,
    val sportName: String? = null,
    val venueId: String? = null,
    val venueName: String? = null,
    val venueAddress: String? = null,
    val isAvailable: Boolean? = null,
    val facilitiesCount: Int? = null,
    val sports: List<VenueSport> = emptyList()
)

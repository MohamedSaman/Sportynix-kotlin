package com.sportynix.app.presentation.auction

import androidx.compose.runtime.Composable
import com.sportynix.app.presentation.leagues.LeagueAuctionScreen

@Composable
fun AuctionScreen(
    auctionId: String,
    onNavigateBack: () -> Unit
) {
    LeagueAuctionScreen(
        leagueId = auctionId,
        onNavigateBack = onNavigateBack
    )
}

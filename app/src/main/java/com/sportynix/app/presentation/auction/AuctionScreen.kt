package com.sportynix.app.presentation.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    auctionId: String,
    onNavigateBack: () -> Unit,
    viewModel: AuctionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(auctionId) {
        viewModel.enterAuctionRoom(auctionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Player Auction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            state.activeAuction?.let { auction ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = SportynixGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(auction.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("ON THE BLOCK", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            auction.currentPlayerName ?: "Waiting for next player...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("CURRENT HIGHEST BID", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "₹${auction.currentBidAmount}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SportynixGreenPrimary
                        )
                        Text(
                            "Bidder: ${auction.currentHighestBidder ?: "No bids yet"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val nextBid = auction.currentBidAmount + 50000
                            PrimaryButton(
                                text = "+ ₹50,000",
                                onClick = {
                                    state.teams.firstOrNull()?.let { team ->
                                        viewModel.placeBid(auctionId, team.id, nextBid)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            val nextBigBid = auction.currentBidAmount + 100000
                            PrimaryButton(
                                text = "+ ₹1,00,000",
                                onClick = {
                                    state.teams.firstOrNull()?.let { team ->
                                        viewModel.placeBid(auctionId, team.id, nextBigBid)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Team Purses Remaining", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.teams) { team ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(team.name, fontWeight = FontWeight.Bold)
                                    Text("Bought: ${team.playersBought} players", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${team.purseRemaining}", fontWeight = FontWeight.Bold, color = SportynixGreenPrimary)
                            }
                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportynixGreenPrimary)
                }
            }
        }
    }
}

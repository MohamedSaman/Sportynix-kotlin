package com.sportynix.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sportynix.app.presentation.components.CustomGlassHeader
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.theme.*

data class FavoriteVenueItem(
    val id: String,
    val name: String,
    val location: String,
    val rating: Float,
    val reviewCount: Int,
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVenueDetail: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Venues, 1 for Sports

    // Dummy sample favorite items matching reference UI
    val sampleVenues = remember {
        listOf(
            FavoriteVenueItem(
                id = "1",
                name = "Sportynix sport's complex",
                location = "Warana Rd, Kalagedihena, Gampaha",
                rating = 5.0f,
                reviewCount = 2,
                imageUrl = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800"
            )
        )
    }

    Scaffold(
        topBar = {
            CustomGlassHeader(
                title = "Favorites",
                showBack = true,
                onBackPress = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Segmented Glass Tabs: Venues (N) / Sports (M)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Venues Tab
                    val venuesSelected = selectedTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (venuesSelected) {
                                    if (isDark) NeonGreen else SportynixGreenPrimary
                                } else Color.Transparent
                            )
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Venues (${sampleVenues.size})",
                            color = if (venuesSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sports Tab
                    val sportsSelected = selectedTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (sportsSelected) {
                                    if (isDark) NeonGreen else SportynixGreenPrimary
                                } else Color.Transparent
                            )
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sports (1)",
                            color = if (sportsSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(sampleVenues) { venue ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToVenueDetail(venue.id) },
                            shape = RoundedCornerShape(24.dp),
                            elevation = 8.dp
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                ) {
                                    AsyncImage(
                                        model = venue.imageUrl,
                                        contentDescription = venue.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = venue.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0x33F59E0B))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Rating",
                                                tint = AccentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${venue.rating}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Location",
                                            tint = SportynixGreenPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = venue.location,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "(${venue.reviewCount} reviews)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PrimaryButton(
                                            text = "Book Now",
                                            onClick = { onNavigateToVenueDetail(venue.id) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                        )

                                        OutlinedButton(
                                            onClick = { /* Remove action */ },
                                            modifier = Modifier.height(44.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color(0x11EF4444)
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Remove", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Sports empty/content view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No favorite sports added yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

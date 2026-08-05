package com.sportynix.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.sportynix.app.data.remote.dto.APIFavoriteDto
import com.sportynix.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.sportynix.app.presentation.theme.*
import java.util.Locale
import javax.inject.Inject

data class FavoriteVenueItem(
    val favoriteId: Int,
    val venueId: String,
    val name: String,
    val location: String,
    val rating: Double,
    val reviewCount: Int,
    val imageUrl: String
)

data class FavoriteSportItem(
    val favoriteId: Int,
    val sportId: Int,
    val name: String,
    val venueId: String?,
    val venueName: String?,
    val venueLocation: String,
    val price: String,
    val rating: Double,
    val reviewsCount: Int,
    val imageUrl: String,
    val venueRating: Double,
    val venueReviewCount: Int
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var favorites by mutableStateOf<List<APIFavoriteDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var removingIds by mutableStateOf<Set<Int>>(emptySet())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private var loadJob: Job? = null

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = profileRepository.getFavorites()
            result.onSuccess { list ->
                favorites = list
                isLoading = false
            }.onFailure {
                isLoading = false
                errorMessage = it.message ?: "Unable to load favorites"
            }
        }
    }

    fun removeFavorite(id: Int) {
        if (id in removingIds) return
        val snapshot = favorites
        val currentSet = removingIds.toMutableSet()
        currentSet.add(id)
        removingIds = currentSet

        // Optimistic removal
        favorites = favorites.filter { it.id != id }

        viewModelScope.launch {
            val result = profileRepository.removeFavorite(id)
            if (result.isFailure) {
                favorites = snapshot
                errorMessage = result.exceptionOrNull()?.message ?: "Unable to remove favorite"
            } else {
                message = "Removed from favorites"
            }
            val doneSet = removingIds.toMutableSet()
            doneSet.remove(id)
            removingIds = doneSet
        }
    }

    fun clearMessage() { message = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToVenueDetail: (String) -> Unit = {},
    onNavigateToSportDetail: (sportId: String, venueId: String) -> Unit = { _, _ -> },
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val backgroundColor = if (isDark) DarkBackground else LightBackground
    val cardColor = if (isDark) DarkSurface else LightSurface
    val borderColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val accentGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary

    var selectedTab by remember { mutableIntStateOf(0) }

    // Deduplicated venues
    val uniqueVenues = remember(viewModel.favorites) {
        val list = mutableListOf<FavoriteVenueItem>()
        val seenVenueIds = mutableSetOf<String>()
        for (fav in viewModel.favorites) {
            val v = fav.venue ?: continue
            if (seenVenueIds.add(v.id)) {
                list.add(
                    FavoriteVenueItem(
                        favoriteId = fav.id,
                        venueId = v.id,
                        name = v.name ?: "Venue",
                        location = v.address ?: "",
                        rating = v.rating ?: 0.0,
                        reviewCount = v.reviews ?: 0,
                        imageUrl = v.imageUrlSecure ?: v.imageUrl ?: ""
                    )
                )
            }
        }
        list
    }

    // Deduplicated sports
    val favoriteSports = remember(viewModel.favorites) {
        val list = mutableListOf<FavoriteSportItem>()
        val seenSportIds = mutableSetOf<Int>()
        for (fav in viewModel.favorites) {
            val s = fav.sport ?: continue
            if (seenSportIds.add(s.id)) {
                val nestedVenue = s.venue ?: fav.venue
                val vName = nestedVenue?.name
                val vAddr = nestedVenue?.address ?: ""
                val img = s.imageSecure ?: s.image ?: nestedVenue?.imageUrlSecure ?: nestedVenue?.imageUrl ?: ""
                list.add(
                    FavoriteSportItem(
                        favoriteId = fav.id,
                        sportId = s.id,
                        name = s.name ?: "Sport",
                        venueId = nestedVenue?.id,
                        venueName = vName,
                        venueLocation = vAddr,
                        price = s.price ?: "0",
                        rating = s.averageRating ?: 0.0,
                        reviewsCount = s.reviewsCount ?: 0,
                        imageUrl = img,
                        venueRating = nestedVenue?.rating ?: 0.0,
                        venueReviewCount = nestedVenue?.reviews ?: 0
                    )
                )
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
      Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading && viewModel.favorites.isNotEmpty(),
            onRefresh = viewModel::loadFavorites,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
          Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = cardColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, accentGreen.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val venuesSelected = selectedTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (venuesSelected) accentGreen else Color.Transparent)
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Venues (${uniqueVenues.size})",
                            color = if (venuesSelected) Color.White else textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val sportsSelected = selectedTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (sportsSelected) accentGreen else Color.Transparent)
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sports (${favoriteSports.size})",
                            color = if (sportsSelected) Color.White else textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading && viewModel.favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentGreen)
                }
            } else if (viewModel.errorMessage != null && viewModel.favorites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Favorite, null, tint = accentGreen.copy(alpha = .55f), modifier = Modifier.size(48.dp))
                        Text("Couldn't load favorites", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                        Text(viewModel.errorMessage.orEmpty(), color = textSecondary, textAlign = TextAlign.Center)
                        Button(onClick = viewModel::loadFavorites, colors = ButtonDefaults.buttonColors(containerColor = accentGreen)) {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Try Again")
                        }
                    }
                }
            } else if (selectedTab == 0) {
                if (uniqueVenues.isEmpty()) {
                    EmptyFavoritesView("No Favorite Venues Yet", "Tap the heart icon on any venue to save it to your favorites.", accentGreen, textPrimary, textSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uniqueVenues, key = { it.favoriteId }) { complex ->
                            FavoriteVenueCard(
                                venue = complex,
                                cardColor = cardColor,
                                borderColor = borderColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accentGreen = accentGreen,
                                isRemoving = viewModel.removingIds.contains(complex.favoriteId),
                                onBook = { onNavigateToVenueDetail(complex.venueId) },
                                onRemove = { viewModel.removeFavorite(complex.favoriteId) }
                            )
                        }
                    }
                }
            } else {
                if (favoriteSports.isEmpty()) {
                    EmptyFavoritesView("No Favorite Sports Yet", "Tap the heart on a sport listing to save your favorite sports.", accentGreen, textPrimary, textSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(favoriteSports, key = { it.favoriteId }) { sport ->
                            FavoriteSportCard(
                                sport = sport,
                                cardColor = cardColor,
                                borderColor = borderColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accentGreen = accentGreen,
                                isRemoving = viewModel.removingIds.contains(sport.favoriteId),
                                onBook = { sport.venueId?.let { onNavigateToSportDetail(sport.sportId.toString(), it) } },
                                onRemove = { viewModel.removeFavorite(sport.favoriteId) }
                            )
                        }
                    }
                }
            }
          }
        }
        viewModel.message?.let { message ->
            Snackbar(Modifier.padding(16.dp).align(Alignment.BottomCenter), action = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } }) { Text(message) }
        }
      }
    }
}

@Composable
private fun EmptyFavoritesView(
    title: String,
    message: String,
    accentGreen: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = accentGreen, modifier = Modifier.size(36.dp))
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(message, fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FavoriteVenueCard(
    venue: FavoriteVenueItem,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color,
    isRemoving: Boolean,
    onBook: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBook() },
        color = cardColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (venue.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = venue.imageUrl,
                        contentDescription = venue.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = accentGreen, strokeWidth = 2.dp) } },
                        error = { FavoriteImageFallback(accentGreen) },
                        success = { SubcomposeAsyncImageContent() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(accentGreen.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentGreen, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(venue.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = textSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(venue.location, fontSize = 12.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (venue.rating > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentGreen.copy(alpha = 0.12f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(String.format(Locale.US, "%.1f", venue.rating), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }
                    }
                }

                if (venue.reviewCount > 0) {
                    Text("(${venue.reviewCount} reviews)", fontSize = 11.sp, color = textSecondary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onBook,
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !isRemoving,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSportCard(
    sport: FavoriteSportItem,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentGreen: Color,
    isRemoving: Boolean,
    onBook: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBook() },
        color = cardColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (sport.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = sport.imageUrl,
                        contentDescription = sport.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = accentGreen, strokeWidth = 2.dp) } },
                        error = { FavoriteImageFallback(accentGreen) },
                        success = { SubcomposeAsyncImageContent() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(accentGreen.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = accentGreen, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(sport.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = accentGreen.copy(alpha = 0.12f)) {
                            Text("Rs. ${sport.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (sport.venueName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sport.venueName, fontSize = 12.sp, color = textSecondary, maxLines = 1)
                        }
                    }
                    val displayRating = if (sport.rating > 0) sport.rating else sport.venueRating
                    val displayReviews = if (sport.reviewsCount > 0) sport.reviewsCount else sport.venueReviewCount
                    if (displayRating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(String.format(Locale.US, "%.1f", displayRating), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            if (displayReviews > 0) Text(" ($displayReviews reviews)", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                    if (sport.venueLocation.isNotBlank()) {
                        Text(sport.venueLocation, fontSize = 11.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onBook,
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !isRemoving,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteImageFallback(accentGreen: Color) {
    Box(Modifier.fillMaxSize().background(accentGreen.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = accentGreen.copy(alpha = .65f), modifier = Modifier.size(40.dp))
    }
}

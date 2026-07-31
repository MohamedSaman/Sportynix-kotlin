package com.sportynix.app.presentation.venue.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sportynix.app.data.remote.dto.VenueReviewDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ReviewPhotoItem(
    val file: File? = null,
    val bitmap: Bitmap? = null,
    val backendPhotoId: Int? = null,
    val remoteUrl: String? = null
)

@Composable
fun ReviewCard(
    review: VenueReviewDto,
    currentUserId: String?,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onPhotoTap: ((List<String>, Int) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val isCurrentUser = remember(review.user, currentUserId) {
        val uStr = review.user?.toString() ?: ""
        uStr.isNotEmpty() && currentUserId != null && (uStr == currentUserId || uStr.contains(currentUserId))
    }

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderClr, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!review.userAvatarSecure.isNullOrEmpty()) {
                    AsyncImage(
                        model = review.userAvatarSecure,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = review.userName?.take(1)?.uppercase() ?: "A"
                        Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isCurrentUser) "You" else (review.userName ?: "Anonymous"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (isCurrentUser) {
                            Text("(Your review)", fontSize = 11.sp, color = primaryGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    review.createdAt?.let { dateStr ->
                        Text(dateStr.take(10), fontSize = 12.sp, color = textSecondary)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val r = review.rating ?: 5f
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= r) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= r) Color(0xFFEAB308) else textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (isCurrentUser && (onEdit != null || onDelete != null)) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(30.dp)) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = textSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            onEdit?.let { action ->
                                DropdownMenuItem(
                                    text = { Text("Edit Review") },
                                    onClick = {
                                        showMenu = false
                                        action()
                                    },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) }
                                )
                            }
                            onDelete?.let { action ->
                                DropdownMenuItem(
                                    text = { Text("Delete Review", color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        action()
                                    },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Comment Text
        review.comment?.takeIf { it.isNotEmpty() }?.let { commentText ->
            Text(commentText, fontSize = 14.sp, color = textPrimary, lineHeight = 20.sp)
        }

        // Categories / Standout Tags
        review.categories?.takeIf { it.isNotEmpty() }?.let { tags ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(primaryGreen.copy(alpha = 0.12f))
                            .border(1.dp, primaryGreen.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = primaryGreen)
                    }
                }
            }
        }

        // Review Photos
        val photoUrls = remember(review.reviewPhotos) {
            review.reviewPhotos?.mapNotNull { it.imageUrlSecure ?: it.image } ?: emptyList()
        }

        if (photoUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photoUrls) { idx, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, borderClr, RoundedCornerShape(8.dp))
                            .clickable { onPhotoTap?.invoke(photoUrls, idx) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Recommendation Status
        val rec = review.wouldRecommend ?: review.recommends
        rec?.let { isRec ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (isRec) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                    contentDescription = null,
                    tint = if (isRec) primaryGreen else Color.Red,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isRec) "Recommends" else "Doesn't recommend",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRec) primaryGreen else Color.Red
                )
            }
        }
    }
}

@Composable
fun ReviewImagePreviewModal(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, imageUrls.size - 1)) { imageUrls.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = imageUrls[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top Bar with Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Bottom Counter
            if (imageUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewBottomSheet(
    titleName: String,
    existingReview: VenueReviewDto? = null,
    onDismiss: () -> Unit,
    onSubmit: (rating: Double, comment: String, standOutTags: List<String>, categoryTags: List<String>, newImageFiles: List<File>, recommends: Boolean, keepPhotoIds: List<Int>) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E262C) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var rating by remember { mutableDoubleStateOf(existingReview?.rating?.toDouble() ?: 0.0) }
    var reviewText by remember { mutableStateOf(existingReview?.comment ?: "") }
    var selectedStandOutTags by remember { mutableStateOf((existingReview?.categories ?: emptyList()).toSet()) }
    var selectedCategoryTags by remember { mutableStateOf(setOf<String>()) }
    var recommends by remember { mutableStateOf(existingReview?.wouldRecommend ?: existingReview?.recommends ?: true) }

    val initialPhotos = remember(existingReview) {
        existingReview?.reviewPhotos?.map { p ->
            ReviewPhotoItem(backendPhotoId = p.id, remoteUrl = p.imageUrlSecure ?: p.image)
        } ?: emptyList()
    }
    var photos by remember { mutableStateOf(initialPhotos) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 5 - photos.size
            val selectedUris = uris.take(remainingSlots)
            val newItems = selectedUris.mapNotNull { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val file = File(context.cacheBufferDir(), "review_img_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
                    val out = FileOutputStream(file)
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 75, out)
                    out.flush()
                    out.close()
                    ReviewPhotoItem(file = file, bitmap = bitmap)
                } catch (e: Exception) {
                    null
                }
            }
            photos = photos + newItems
        }
    }

    val standOutOptions = listOf("Facilities", "Cleanliness", "Staff Service", "Value for Money")
    val standOutIcons = listOf(Icons.Default.Apartment, Icons.Default.AutoAwesome, Icons.Default.People, Icons.Default.Paid)
    val categoryOptions = listOf("Location", "Equipment", "Atmosphere", "Booking Process")
    val categoryIcons = listOf(Icons.Default.LocationOn, Icons.Default.SportsBasketball, Icons.Default.TheaterComedy, Icons.Default.CalendarMonth)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (existingReview != null) "Edit Review" else "Write a Review", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("for $titleName", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryGreen)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Star Rating
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How was your experience?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star.toDouble() }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = if (star.toDouble() <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (star.toDouble() <= rating) Color(0xFFEAB308) else textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Review Text
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your Review", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { if (it.length <= 1000) reviewText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Share details of your experience...", fontSize = 14.sp, color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryGreen,
                        unfocusedBorderColor = borderClr
                    )
                )
                Text(
                    text = "${reviewText.length}/1000",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // What Stood Out
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("What stood out?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    itemsIndexed(standOutOptions) { idx, tag ->
                        val isSelected = selectedStandOutTags.contains(tag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) primaryGreen else borderClr, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedStandOutTags = if (isSelected) selectedStandOutTags - tag else selectedStandOutTags + tag
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = standOutIcons[idx], contentDescription = null, tint = if (isSelected) primaryGreen else textSecondary, modifier = Modifier.size(18.dp))
                            Text(tag, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) primaryGreen else textPrimary, modifier = Modifier.weight(1f))
                            if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Rate Categories
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rate categories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    itemsIndexed(categoryOptions) { idx, tag ->
                        val isSelected = selectedCategoryTags.contains(tag)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) primaryGreen else borderClr, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCategoryTags = if (isSelected) selectedCategoryTags - tag else selectedCategoryTags + tag
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = categoryIcons[idx], contentDescription = null, tint = if (isSelected) primaryGreen else textSecondary, modifier = Modifier.size(18.dp))
                            Text(tag, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) primaryGreen else textPrimary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Photos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Photos (optional)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Upload up to 5 photos of your experience", fontSize = 13.sp, color = textSecondary)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (photos.size < 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryGreen.copy(alpha = 0.08f))
                                    .border(1.5.dp, primaryGreen, RoundedCornerShape(12.dp))
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(24.dp))
                                    Text("Add Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                                }
                            }
                        }
                    }

                    itemsIndexed(photos) { idx, photo ->
                        Box(modifier = Modifier.size(90.dp)) {
                            if (photo.bitmap != null) {
                                Image(
                                    bitmap = photo.bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (photo.remoteUrl != null) {
                                AsyncImage(
                                    model = photo.remoteUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .clickable {
                                        photos = photos.filterIndexed { index, _ -> index != idx }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Recommendation Choice
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Would you recommend to others?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (recommends) primaryGreen.copy(alpha = 0.12f) else Color.Transparent)
                            .border(if (recommends) 1.5.dp else 1.dp, if (recommends) primaryGreen else borderClr, RoundedCornerShape(12.dp))
                            .clickable { recommends = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = null, tint = if (recommends) primaryGreen else textSecondary, modifier = Modifier.size(16.dp))
                        Text("Yes, I recommend", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (recommends) primaryGreen else textPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!recommends) Color.Red.copy(alpha = 0.12f) else Color.Transparent)
                            .border(if (!recommends) 1.5.dp else 1.dp, if (!recommends) Color.Red else borderClr, RoundedCornerShape(12.dp))
                            .clickable { recommends = false }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ThumbDown, contentDescription = null, tint = if (!recommends) Color.Red else textSecondary, modifier = Modifier.size(16.dp))
                        Text("No, I don't", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (!recommends) Color.Red else textPrimary)
                    }
                }
            }

            // Buttons
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val newFiles = photos.mapNotNull { it.file }
                        val keptIds = photos.mapNotNull { it.backendPhotoId }
                        onSubmit(rating, reviewText, selectedStandOutTags.toList(), selectedCategoryTags.toList(), newFiles, recommends, keptIds)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    enabled = rating > 0.0
                ) {
                    Text("Submit", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun Context.cacheBufferDir(): File {
    val dir = File(cacheDir, "review_images")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

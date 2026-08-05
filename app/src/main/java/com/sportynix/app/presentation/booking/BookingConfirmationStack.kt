package com.sportynix.app.presentation.booking

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.presentation.theme.SportynixGreenPrimary
import kotlinx.coroutines.launch

private val SLOT_ACCENTS = listOf(
    Color(0xFF2ECC71),
    Color(0xFF3B82F6),
    Color(0xFFF59E0B),
    Color(0xFFA855F7)
)

@Composable
fun BookingConfirmationStack(
    bookings: List<ConfirmedBookingDto>,
    bookingType: String = "Normal",
    onAssignTeam: () -> Unit,
    onHome: () -> Unit,
    onOpenReceipt: ((ConfirmedBookingDto) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = com.sportynix.app.presentation.theme.LocalThemeController.current.isDark
    val primaryGreen = if (isDark) Color(0xFF00D982) else SportynixGreenPrimary
    val textPrimary = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1C1C26) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var cardOrder by remember(bookings) { mutableStateOf(bookings.indices.toList()) }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun cycleNextCard() {
        if (cardOrder.size <= 1) return
        val next = cardOrder.toMutableList()
        val first = next.removeAt(0)
        next.add(first)
        cardOrder = next
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF070C16) else Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(primaryGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(32.dp))
                }

                Text("Booking Confirmed!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    text = if (cardOrder.size > 1) "Swipe cards to view all ${cardOrder.size} bookings" else "Your court is reserved successfully",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            // Stacked Cards Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val visibleCount = Math.min(4, cardOrder.size)
                for (depth in visibleCount - 1 downTo 0) {
                    val bookingIdx = cardOrder.getOrNull(depth) ?: continue
                    val booking = bookings.getOrNull(bookingIdx) ?: continue
                    val isTop = depth == 0
                    val accent = SLOT_ACCENTS[bookingIdx % SLOT_ACCENTS.size]

                    val scale = 1f - (depth * 0.03f)
                    val offsetY = (depth * 14).dp

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .offset(y = offsetY)
                            .scale(scale)
                            .graphicsLayer {
                                if (isTop) {
                                    translationX = offsetX.value
                                    rotationZ = (offsetX.value / 20f).coerceIn(-18f, 18f)
                                }
                            }
                            .pointerInput(isTop) {
                                if (isTop && cardOrder.size > 1) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                if (Math.abs(offsetX.value) > 250f) {
                                                    offsetX.animateTo(if (offsetX.value > 0) 1000f else -1000f, tween(200))
                                                    offsetX.snapTo(0f)
                                                    cycleNextCard()
                                                } else {
                                                    offsetX.animateTo(0f, tween(200))
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isTop) 8.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderClr, RoundedCornerShape(24.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Accent Strip & Reference
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Ref #${booking.bookingReference ?: booking.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                                }

                                val pStatus = booking.paymentStatus ?: "Confirmed"
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(primaryGreen)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(pStatus.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Generate the QR locally from the backend value; never send booking
                            // data to a third-party QR service.
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = remember(booking.qrCode, booking.id) {
                                        createQrBitmap(booking.qrCode?.takeIf { it.isNotBlank() }
                                            ?: "booking-${booking.id}").asImageBitmap()
                                    },
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            // Date & Time
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(booking.bookingDate ?: "Date", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("${booking.startTime ?: "00:00"} - ${booking.endTime ?: "01:00"}", fontSize = 14.sp, color = textSecondary)
                            }

                            // Receipt Button
                            val receiptUrl = booking.receiptDownloadUrl
                            if (!receiptUrl.isNullOrEmpty()) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(receiptUrl))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Receipt PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAssignTeam,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Assign Team", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                Button(
                    onClick = onHome,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                ) {
                    Text("Return Home", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun createQrBitmap(value: String, size: Int = 512): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) for (x in 0 until size) {
        pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

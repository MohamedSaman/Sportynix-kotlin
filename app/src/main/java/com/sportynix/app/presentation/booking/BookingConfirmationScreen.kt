package com.sportynix.app.presentation.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportynix.app.data.remote.dto.ConfirmedBookingDto
import com.sportynix.app.presentation.components.GlassCard
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun BookingConfirmationScreen(
    confirmedBookings: List<ConfirmedBookingDto> = emptyList(),
    onHome: () -> Unit,
    onNavigateToDetails: (bookingId: String) -> Unit,
    onAssignTeam: (bookingId: String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val primaryGreen = if (isDark) Color(0xFF22C55E) else SportynixGreenPrimary

    var currentCardIndex by remember { mutableIntStateOf(0) }
    val bookingsList = if (confirmedBookings.isNotEmpty()) confirmedBookings else listOf(
        ConfirmedBookingDto(
            id = "b_1",
            qrCode = "SPN-2026-88412",
            startTime = "07:00 AM",
            endTime = "08:00 AM",
            price = 400.0,
            duration = 60,
            bookingDate = "2026-07-29",
            bookingReference = "SPN-2026-88412",
            paymentStatus = "Confirmed",
            paymentAmount = 400.0,
            paymentCurrency = "LKR",
            receiptNumber = "REC-88412",
            receiptDownloadUrl = null
        )
    )

    val currentBooking = bookingsList[currentCardIndex % bookingsList.size]

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToDetails(currentBooking.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("View Details", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Go Home", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Circle Checkmark
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(primaryGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Booking Confirmed!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your venue reservation is locked in and ready",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Swappable Confirmation Card Stack
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            if (delta < -30) {
                                currentCardIndex = (currentCardIndex + 1) % bookingsList.size
                            } else if (delta > 30) {
                                currentCardIndex = (currentCardIndex - 1 + bookingsList.size) % bookingsList.size
                            }
                        }
                    )
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ref: ${currentBooking.bookingReference ?: "SPN-88412"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGreen
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentBooking.paymentStatus ?: "Confirmed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Code Box Placeholder / Icon
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF1E242B) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = primaryGreen,
                                modifier = Modifier.size(90.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${currentBooking.bookingDate} · ${currentBooking.startTime} - ${currentBooking.endTime}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Paid Amount: LKR %.2f".format(currentBooking.paymentAmount ?: 400.0),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (bookingsList.size > 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Swipe left/right to view all ${bookingsList.size} permanent slots (${currentCardIndex + 1}/${bookingsList.size})",
                                fontSize = 11.sp,
                                color = primaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

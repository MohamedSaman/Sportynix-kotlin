package com.sportynix.app.presentation.venue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.domain.model.TimeSlot
import com.sportynix.app.presentation.components.PrimaryButton
import com.sportynix.app.presentation.components.SportynixTopBar
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

@Composable
fun VenueSlotPickerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookingSummary: (venueId: String, slotId: String, date: String) -> Unit,
    viewModel: VenueViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val sampleSlots = listOf(
        TimeSlot("1", "07:00 AM", "08:00 AM", 25.0, true),
        TimeSlot("2", "08:00 AM", "09:00 AM", 25.0, true),
        TimeSlot("3", "09:00 AM", "10:00 AM", 30.0, false),
        TimeSlot("4", "10:00 AM", "11:00 AM", 30.0, true),
        TimeSlot("5", "04:00 PM", "05:00 PM", 35.0, true),
        TimeSlot("6", "05:00 PM", "06:00 PM", 35.0, true),
        TimeSlot("7", "06:00 PM", "07:00 PM", 40.0, true),
        TimeSlot("8", "07:00 PM", "08:00 PM", 40.0, true)
    )

    Scaffold(
        topBar = {
            SportynixTopBar(
                title = "Select Time Slot",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                PrimaryButton(
                    text = if (state.selectedSlot != null) "Proceed to Booking" else "Select a Time Slot",
                    onClick = {
                        val vId = viewModel.venueId
                        if (state.selectedSlot != null && vId != null) {
                            onNavigateToBookingSummary(vId, state.selectedSlot!!.id, state.selectedDate)
                        }
                    },
                    enabled = state.selectedSlot != null
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Available Slots for Today",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sampleSlots) { slot ->
                    val isSelected = state.selectedSlot?.id == slot.id
                    val bgColor = when {
                        !slot.isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                        isSelected -> SportynixGreenPrimary
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val textColor = when {
                        !slot.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(bgColor, RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SportynixGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = slot.isAvailable) { viewModel.selectSlot(slot) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${slot.startTime} - ${slot.endTime}",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (slot.isAvailable) "$${slot.price}" else "Booked",
                                color = textColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.sportynix.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TabItem(val id: Int, val title: String) {
    LEAGUE(0, "Events"),
    BOOKING(1, "History"),
    HOME(2, "Home"),
    SEARCH(3, "Search"),
    PROFILE(4, "Profile");

    val activeIcon: ImageVector
        get() = when (this) {
            LEAGUE -> Icons.Filled.EmojiEvents
            BOOKING -> Icons.Filled.History
            HOME -> Icons.Filled.Home
            SEARCH -> Icons.Filled.Search
            PROFILE -> Icons.Filled.Person
        }

    val inactiveIcon: ImageVector
        get() = when (this) {
            LEAGUE -> Icons.Outlined.EmojiEvents
            BOOKING -> Icons.Outlined.History
            HOME -> Icons.Outlined.Home
            SEARCH -> Icons.Outlined.Search
            PROFILE -> Icons.Outlined.Person
        }

    val next: TabItem
        get() {
            val values = values()
            val nextIdx = (ordinal + 1).coerceAtMost(values.size - 1)
            return values[nextIdx]
        }

    val previous: TabItem
        get() {
            val values = values()
            val prevIdx = (ordinal - 1).coerceAtLeast(0)
            return values[prevIdx]
        }
}

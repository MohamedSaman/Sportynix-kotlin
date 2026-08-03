package com.sportynix.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class ThemeController(
    val mode: ThemeMode,
    val isDark: Boolean,
    val setMode: (ThemeMode) -> Unit
) {
    val setDark: (Boolean) -> Unit = { enabled ->
        setMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    fun toggle() = setDark(!isDark)
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("ThemeController is not provided")
}

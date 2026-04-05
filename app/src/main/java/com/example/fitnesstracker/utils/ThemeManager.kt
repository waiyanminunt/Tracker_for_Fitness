package com.example.fitnesstracker.utils

import androidx.compose.runtime.mutableStateOf

/**
 * Process-wide singleton that holds the app's dark mode state.
 *
 * Why a singleton with mutableStateOf?
 * - `mutableStateOf` is a Compose observable — any Composable reading
 *   `ThemeManager.isDarkMode.value` will automatically recompose when
 *   the value changes, even across Activity boundaries (same process).
 * - Initialized once in FitnessApp.onCreate() from SharedPreferences.
 * - Written by ProfileActivity's Dark Mode toggle.
 * - Read by FitnesstrackerTheme as its default darkTheme parameter.
 */
object ThemeManager {
    val isDarkMode = mutableStateOf(false)
}

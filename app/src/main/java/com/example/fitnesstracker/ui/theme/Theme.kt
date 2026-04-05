package com.example.fitnesstracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.fitnesstracker.utils.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = VitalRed,
    secondary = PulsePink,
    tertiary = SoftWhite,
    background = CharcoalBlack,
    surface = GunmetalGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    error = VitalRed
)

private val LightColorScheme = lightColorScheme(
    primary = VitalRed,
    secondary = DeepCrimson,
    tertiary = LightSlate,
    background = Color.White,
    surface = SoftWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CharcoalBlack,
    onSurface = CharcoalBlack
)

/**
 * Global theme function.
 *
 * darkTheme resolution priority:
 *   1. ThemeManager.isDarkMode.value  — process-wide Compose observable state,
 *      updated instantly by the Dark Mode toggle in ProfileActivity.
 *      Every Activity observing this will recompose the moment it changes.
 *   2. isSystemInDarkTheme()          — fallback before user sets a preference.
 *
 * NO Activity needs to pass any argument. Just call:
 *   FitnesstrackerTheme { ... }
 * and every screen automatically responds to the global toggle.
 *
 * ProfileActivity may still pass darkTheme = isDarkMode explicitly for
 * its own live-toggle behavior — that is also fine and takes priority.
 */
@Composable
fun FitnesstrackerTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode.value,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = LiquidShapes,
        typography = Typography,
        content = content
    )
}
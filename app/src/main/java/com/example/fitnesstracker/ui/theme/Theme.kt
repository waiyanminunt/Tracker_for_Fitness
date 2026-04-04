package com.example.fitnesstracker.ui.theme
import com.example.fitnesstracker.R

import android.app.Activity
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

@Composable
fun FitnesstrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to use our custom fitness colors consistently
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
        typography = Typography,
        content = content
    )
}
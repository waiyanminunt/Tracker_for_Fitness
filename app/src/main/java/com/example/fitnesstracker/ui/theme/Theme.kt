package com.example.fitnesstracker.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fitnesstracker.utils.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary        = VitalRed,
    secondary      = PulsePink,
    tertiary       = SoftWhite,
    background     = CharcoalBlack,
    surface        = GunmetalGray,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = SoftWhite,
    onSurface      = SoftWhite,
    error          = VitalRed
)

private val LightColorScheme = lightColorScheme(
    primary        = VitalRed,
    secondary      = DeepCrimson,
    tertiary       = LightSlate,
    background     = Color.White,
    surface        = SoftWhite,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = CharcoalBlack,
    onSurface      = CharcoalBlack
)

/**
 * Global theme function — wraps all screens in the app.
 *
 * darkTheme resolution priority:
 *   1. Explicit [darkTheme] argument (ProfileActivity passes its live toggle value).
 *   2. [ThemeManager.isDarkMode.value] — process-wide Compose observable state.
 *      Every Activity that calls FitnesstrackerTheme{} without an argument
 *      automatically recomposes when the Dark Mode toggle fires in ProfileActivity.
 *
 * Edge-to-Edge System Bar Sync (SideEffect):
 *   A [SideEffect] runs after every successful recomposition and pushes the correct
 *   system-bar configuration into the Window:
 *     • Both bars remain TRANSPARENT (set once in BaseActivity / themes.xml).
 *     • [WindowInsetsControllerCompat] flips icon tint:
 *         Dark mode  → isAppearanceLightStatusBars = false → white icons
 *         Light mode → isAppearanceLightStatusBars = true  → dark icons
 *   This covers live-toggle updates from ProfileActivity without requiring
 *   the Accompanist SystemUiController library.
 */
@Composable
fun FitnesstrackerTheme(
    darkTheme:    Boolean = ThemeManager.isDarkMode.value,
    dynamicColor: Boolean = false,
    content:      @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // ── Edge-to-edge system bar sync ──────────────────────────────────────────
    // Runs after every recomposition — keeps icon tints correct when the user
    // toggles Dark Mode inside the running app (live ProfileActivity toggle).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window   = activity.window

            // Keep bars fully transparent (belt-and-suspenders over themes.xml)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor     = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = AndroidColor.TRANSPARENT

            // Flip icon tint to match current theme
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme  // true = dark icons (for light bg)
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes      = LiquidShapes,
        typography  = Typography,
        content     = content
    )
}
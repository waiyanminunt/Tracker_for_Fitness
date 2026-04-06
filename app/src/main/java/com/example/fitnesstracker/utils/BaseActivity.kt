package com.example.fitnesstracker.utils

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// ============================================
// INHERITANCE EXAMPLE 1: BaseActivity
// ============================================
// This is a BASE CLASS that provides common functionality
// Child activities can EXTEND this class to inherit:
// - Edge-to-edge system bar setup
// - Common colors
// - Common UI components (back button header)
// - Common helper methods
// ============================================

/**
 * BaseActivity — Base class for all activities in the app.
 *
 * OOP Principles Demonstrated:
 * 1. INHERITANCE: Child classes extend this base class
 * 2. ENCAPSULATION: Protected properties are accessible to subclasses
 * 3. POLYMORPHISM: Child classes can override methods
 *
 * Edge-to-Edge System:
 *   [onCreate] calls [WindowCompat.setDecorFitsSystemWindows](false) so Compose
 *   content draws behind the status and navigation bars. Both bars are made
 *   fully transparent. [WindowInsetsControllerCompat] is used to set icon
 *   tint (light/dark) based on [ThemeManager.isDarkMode], ensuring white icons
 *   in dark mode and dark icons in light mode, with no Accompanist dependency.
 *
 *   All Compose screens must use .statusBarsPadding() + .navigationBarsPadding()
 *   (already in place) to avoid content hiding behind the bars.
 */
abstract class BaseActivity : ComponentActivity() {

    // ========================================
    // ENCAPSULATION - Protected properties
    // Accessible to this class and subclasses only
    // ========================================

    // Legacy colors - kept for compatibility; subclasses should prefer MaterialTheme
    protected val darkPurple: Color = Color(0xFF0B132B)
    protected val purple: Color     = Color(0xFF00E5FF)
    protected val lightPurple: Color = Color(0xFFC6FF00)
    protected val cardBg: Color     = Color(0xFF1C2541)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()
    }

    /**
     * Configures the Window for a fully immersive edge-to-edge experience:
     *
     *   Step 1 — [WindowCompat.setDecorFitsSystemWindows](false)
     *     Instructs the system NOT to resize the window to fit inside the bars.
     *     Compose content now physically occupies the area behind status + nav bars.
     *
     *   Step 2 — Transparent bar colors
     *     Both bars are painted transparent so the Compose background shows through.
     *
     *   Step 3 — Icon appearance via [WindowInsetsControllerCompat]
     *     Dark mode  → light (white) icons on the dark background.
     *     Light mode → dark (black) icons on the white background.
     *     This is re-applied here from ThemeManager so it's correct on Activity start.
     *     FitnesstrackerTheme's SideEffect handles live-toggle updates mid-session.
     */
    private fun applyEdgeToEdge() {
        // Step 1: Let Compose draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Step 2: Transparent bars (belt-and-suspenders alongside themes.xml)
        @Suppress("DEPRECATION")
        window.statusBarColor     = AndroidColor.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT

        // Step 3: Correct icon tint based on current theme
        val isDark = ThemeManager.isDarkMode.value
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = !isDark   // true = dark icons (for light bg)
            isAppearanceLightNavigationBars = !isDark
        }
    }

    // ========================================
    // INHERITANCE - Methods that subclasses can use
    // ========================================

    /** Common method to get user ID from intent — all child activities can use this. */
    protected fun getUserId(): Int = intent.getIntExtra("USER_ID", 0)

    /** Common method to get user name from intent. */
    protected fun getUserName(): String = intent.getStringExtra("USER_NAME") ?: "User"

    /** Common method to get user email from intent. */
    protected fun getUserEmail(): String = intent.getStringExtra("USER_EMAIL") ?: ""

    // ========================================
    // POLYMORPHISM - Open methods that can be overridden
    // ========================================

    /** Open method — child classes can override to customize behavior. */
    protected open fun onActivityReady() {
        // Default: no-op. Child classes can override.
    }
}

/**
 * Composable function for the common back button header.
 * Used across multiple screens.
 */
@Composable
fun CommonHeader(
    title:       String,
    subtitle:    String = "",
    onBackClick: () -> Unit,
    darkPurple:  Color = MaterialTheme.colorScheme.background
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(darkPurple)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text       = title,
                color      = MaterialTheme.colorScheme.onBackground,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text     = subtitle,
                    color    = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Composable function for a small statistics box.
 * Displays a value, unit, and label in a card.
 */
@Composable
fun StatBox(
    label:  String,
    value:  String,
    unit:   String,
    cardBg: Color
) {
    val primaryColor   = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.width(100.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = onSurfaceColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = unit,  color = primaryColor.copy(alpha = 0.7f), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
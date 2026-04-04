package com.example.fitnesstracker.utils

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// INHERITANCE EXAMPLE 1: BaseActivity
// ============================================
// This is a BASE CLASS that provides common functionality
// Child activities can EXTEND this class to inherit:
// - Common colors
// - Common UI components (back button header)
// - Common helper methods
// ============================================

/**
 * BaseActivity - Base class for all activities in the app
 *
 * OOP Principles Demonstrated:
 * 1. INHERITANCE: Child classes extend this base class
 * 2. ENCAPSULATION: Protected properties are accessible to subclasses
 * 3. POLYMORPHISM: Child classes can override methods
 *
 * Benefits:
 * - Code Reuse: Common colors and UI in one place
 * - Maintainability: Change once, affects all child activities
 * - Consistency: All activities have the same look and feel
 */
abstract class BaseActivity : ComponentActivity() {

    // ========================================
    // ENCAPSULATION - Protected properties
    // Accessible to this class and subclasses only
    // ========================================

    // Legacy colors - kept for compatibility but subclasses should use MaterialTheme
    protected val darkPurple: Color = Color(0xFF0B132B) // New DarkNavy
    protected val purple: Color = Color(0xFF00E5FF)     // New NeonCyan
    protected val lightPurple: Color = Color(0xFFC6FF00)  // New ElectricLime
    protected val cardBg: Color = Color(0xFF1C2541)     // New DeepNavy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Child classes will call setContent with their own UI
    }

    // ========================================
    // INHERITANCE - Methods that subclasses can use
    // ========================================

    /**
     * Common method to get user ID from intent
     * All child activities can use this
     */
    protected fun getUserId(): Int {
        return intent.getIntExtra("USER_ID", 0)
    }

    /**
     * Common method to get user name from intent
     */
    protected fun getUserName(): String {
        return intent.getStringExtra("USER_NAME") ?: "User"
    }

    /**
     * Common method to get user email from intent
     */
    protected fun getUserEmail(): String {
        return intent.getStringExtra("USER_EMAIL") ?: ""
    }

    // ========================================
    // POLYMORPHISM - Open methods that can be overridden
    // ========================================

    /**
     * Open method - child classes can override to customize behavior
     */
    protected open fun onActivityReady() {
        // Default implementation - child classes can override
    }
}

/**
 * Composable function for common back button header
 * Used across multiple screens
 */
@Composable
fun CommonHeader(
    title: String,
    subtitle: String = "",
    onBackClick: () -> Unit,
    darkPurple: Color = MaterialTheme.colorScheme.background
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
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Composable function for a small statistics box
 * Displays a value, unit, and label in a card
 */
@Composable
fun StatBox(
    label: String,
    value: String,
    unit: String,
    cardBg: Color
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = onSurfaceColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                color = primaryColor.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
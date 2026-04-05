package com.example.fitnesstracker.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize

// ─────────────────────────────────────────────────────────────────────────────
// LIQUID SHAPE SYSTEM
// All corners are large and smooth, giving a fluid, pill-like feel.
// ─────────────────────────────────────────────────────────────────────────────

/** Material 3 Shapes override — applied globally through Theme.kt. */
val LiquidShapes = Shapes(
    // Small: chips, text fields, snackbars
    small  = RoundedCornerShape(16.dp),
    // Medium: cards, dialogs, bottom sheets
    medium = RoundedCornerShape(24.dp),
    // Large: FABs, full-screen bottom sheets, hero cards
    large  = RoundedCornerShape(32.dp),
    // Extra large: modals, sheets that cover most of the screen
    extraLarge = RoundedCornerShape(36.dp)
)

// Convenience shortcuts to use directly in Card / Button shapes
val ShapeCard    = RoundedCornerShape(24.dp)
val ShapeDialog  = RoundedCornerShape(28.dp)
val ShapeButton  = RoundedCornerShape(50.dp)   // fully pill-shaped
val ShapeChip    = RoundedCornerShape(16.dp)
val ShapeField   = RoundedCornerShape(16.dp)

// ─────────────────────────────────────────────────────────────────────────────
// SOFT AMBIENT SHADOW
// Replaces harsh Material elevation shadows with a soft, dispersed glow
// that makes cards look like they're floating gently.
// Usage:  Modifier.liquidShadow(color = VitalRed.copy(alpha = 0.18f))
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.liquidShadow(
    elevation: Dp = 12.dp,
    shape: RoundedCornerShape = ShapeCard,
    color: Color = Color.Black.copy(alpha = 0.12f)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = color,
    spotColor = color
)

// ─────────────────────────────────────────────────────────────────────────────
// FLUID CONTENT SIZE ANIMATION
// Apply to any Card / Column / Row that expands / collapses (e.g. FAQ items,
// activity grid cards, expandable settings rows).
// Usage:  Modifier.fluidAnimate()
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.fluidAnimate(): Modifier = this.animateContentSize(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessLow
    )
)

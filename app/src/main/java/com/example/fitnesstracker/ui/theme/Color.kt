package com.example.fitnesstracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// LIQUID PALETTE — Health Red, softened for fluid/glassmorphism feel
// ─────────────────────────────────────────────────────────────────────────────

// Primary reds  (slightly softened from full-intensity 0xFFE53935)
val VitalRed     = Color(0xFFEF4444)   // softer cherry red
val DeepCrimson  = Color(0xFFDC2626)   // rich but not harsh
val PulsePink    = Color(0xFFF87171)   // warm coral-pink

// Dark surface tones  (slightly warmer/softer than pure black)
val CharcoalBlack  = Color(0xFF0F0F0F)  // near-black background
val GunmetalGray   = Color(0xFF1C1C1E)  // iOS-style dark surface
val CardDark       = Color(0xFF242428)  // lifted card surface in dark mode

// Light surface tones  (creamy whites, not harsh pure white)
val SoftWhite      = Color(0xFFF8F8FA)  // slightly cool off-white
val CardLight      = Color(0xFFFFFFFF)  // card background in light mode
val LightSlate     = Color(0xFF64748B)  // muted slate for subtitles

// Utility
val MutedGray      = Color(0xFF9E9E9E)

// ─────────────────────────────────────────────────────────────────────────────
// LIQUID GRADIENTS
// Ready-made Brush objects for primary buttons, hero cards, progress arcs.
// Usage:  Box(modifier = Modifier.background(LiquidRedGradient))
// ─────────────────────────────────────────────────────────────────────────────

/** Primary action gradient — flows from deep crimson → coral pink (top-left → bottom-right) */
val LiquidRedGradient = Brush.linearGradient(
    colors = listOf(VitalRed, PulsePink)
)

/** Subtle warm gradient for stat / summary hero cards */
val LiquidCardGradient = Brush.linearGradient(
    colors = listOf(
        VitalRed.copy(alpha = 0.85f),
        PulsePink.copy(alpha = 0.70f)
    )
)

/** Dark-mode glass surface: very subtle translucent overlay */
val LiquidGlassDark = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.02f)
    )
)

/** Light-mode glass surface: soft white to near-white */
val LiquidGlassLight = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.80f),
        Color.White.copy(alpha = 0.60f)
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// LEGACY — kept for compatibility; do not remove
// ─────────────────────────────────────────────────────────────────────────────
val NeonCyan     = Color(0xFF00E5FF)
val ElectricLime = Color(0xFFC6FF00)
val DarkNavy     = Color(0xFF0B132B)
val DeepNavy     = Color(0xFF1C2541)

// Material defaults (kept for any generated preview code)
val Purple80      = Color(0xFFD0BCFF)
val PurpleGrey80  = Color(0xFFCCC2DC)
val Pink80        = Color(0xFFEFB8C8)
val Purple40      = Color(0xFF6650a4)
val PurpleGrey40  = Color(0xFF625b71)
val Pink40        = Color(0xFF7D5260)
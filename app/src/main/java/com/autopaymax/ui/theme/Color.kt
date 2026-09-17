package com.autopaymax.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// ============================================================
// Flight Instrument Panel — brand glow (interactive blue)
// One accent, used for the primary instrument glow and controls.
// Kept as the existing names/values from the app-wide rebrand;
// everything else in this file builds the instrument system around it.
// ============================================================
val NavyPrimary = Color(0xFF1E3A8A)
val NavySecondary = Color(0xFF2563EB)
val NavyAccent = Color(0xFF3B82F6)

val PremiumNavyGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF2563EB))
)

// Aliases for compatibility with older call sites
val OrangePrimary = NavyPrimary
val OrangeSecondary = NavySecondary
val OrangeAccent = NavyAccent

// ============================================================
// Instrument grounds — the panel itself. Dark is the primary
// register (cockpit at night); light is a first-class equivalent,
// never a quick invert.
// ============================================================
val AppDarkBackground = Color(0xFF0A0E14)      // near-black instrument ground
val AppDarkCardBackground = Color(0xFF141B26)  // bezel plate
val AppDarkBorderColor = Color(0xFF2A3646)     // bezel hairline
val AppDarkTextWhite = Color(0xFFF4F7FB)       // luminous instrument ink
val AppDarkTextGray = Color(0xFF8A96A8)        // placard label gray

val AppLightBackground = Color(0xFFF2F4F8)     // pale instrument-white ground
val AppLightCardBackground = Color(0xFFFFFFFF) // bezel plate
val AppLightBorderColor = Color(0xFFE1E6EE)    // bezel hairline
val AppLightTextDark = Color(0xFF10161F)       // instrument ink
val AppLightTextGray = Color(0xFF5B6472)       // placard label gray

// Tonal surface tiers (explicit — darkColorScheme/lightColorScheme only
// derive these from a Material baseline palette when left unset, which
// would leak Material's default purple through surfaceContainer*).
val AppDarkSurfaceDim = Color(0xFF0A0E14)
val AppDarkSurfaceBright = Color(0xFF2C3849)
val AppDarkSurfaceContainerLowest = Color(0xFF060810)
val AppDarkSurfaceContainerLow = Color(0xFF10151F)
val AppDarkSurfaceContainer = Color(0xFF141B26)
val AppDarkSurfaceContainerHigh = Color(0xFF1A2230)
val AppDarkSurfaceContainerHighest = Color(0xFF212B3A)

val AppLightSurfaceDim = Color(0xFFDEE3EA)
val AppLightSurfaceBright = Color(0xFFFFFFFF)
val AppLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val AppLightSurfaceContainerLow = Color(0xFFF7F9FB)
val AppLightSurfaceContainer = Color(0xFFEEF1F5)
val AppLightSurfaceContainerHigh = Color(0xFFE6EAF0)
val AppLightSurfaceContainerHighest = Color(0xFFDEE3EA)

// Secondary / tertiary roles
val AppDarkSecondary = Color(0xFF7C8CA6)
val AppLightSecondary = Color(0xFF475569)

val AppDarkTertiary = Color(0xFFF5A623)   // amber — pending status light
val AppLightTertiary = Color(0xFFB45309)  // amber, tuned for light-mode contrast

// Error / overdue
val AppDarkError = Color(0xFFF2545B)
val AppLightError = Color(0xFFDC2626)

// Fixed dark-tuned status tone, for the hero instrument card, which
// stays a dark bezel in both app themes (a physical gauge insert,
// not a surface that flips with the rest of the page).
val StatusActiveDarkTone = Color(0xFF34D399)

// Categorical chart colors — decorative, for multi-series data (e.g. the
// Reports donut chart) that has nothing to do with status. Deliberately
// distinct from StatusActive/StatusPending/StatusOverdue so a chart slice
// never reads as a status light.
val ChartViolet = Color(0xFF8B5CF6)
val ChartCyan = Color(0xFF06B6D4)
val ChartRose = Color(0xFFEC4899)

// ============================================================
// Compatibility getters for existing screens (resolve dynamically
// from the active Material3 ColorScheme — swap Theme.kt, not call sites)
// ============================================================
val DarkBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val CardBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val BorderColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outlineVariant

val PrimaryIndigo: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val SecondaryPurple: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val AccentTeal: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val TextWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val TextGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

// ============================================================
// Status lights — the three reserved instrument states. Used
// nowhere else in the palette; a mandate is either steady, amber,
// or red, never decorated with these colors for any other purpose.
// ============================================================
val StatusActive: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)

val StatusPending: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val StatusOverdue: Color
    @Composable
    get() = MaterialTheme.colorScheme.error

// Legacy names some screens still import directly — delegate to the
// same reserved status colors so every screen reads one source of truth.
val AccentCoral: Color
    @Composable
    get() = StatusOverdue

val AccentEmerald: Color
    @Composable
    get() = StatusActive

// Fallback legacy light tokens (unused, kept for reference/compilation if imported)
val LightBackground = Color(0xFFF8FAFC)
val LightCardBackground = Color(0xFFFFFFFF)
val LightBorderColor = Color(0xFFE2E8F0)
val LightTextDark = Color(0xFF0F172A)
val LightTextGray = Color(0xFF64748B)

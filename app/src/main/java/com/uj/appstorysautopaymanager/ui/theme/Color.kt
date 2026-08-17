package com.uj.appstorysautopaymanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// Define the static colors first (for ColorScheme mapping)
val OrangePrimary = Color(0xFFFF7600) // SoundBox vibrant orange
val OrangeSecondary = Color(0xFFFF9E40) // SoundBox secondary soft orange
val OrangeAccent = Color(0xFFFFB066) // SoundBox accent peach orange

val AppDarkBackground = Color(0xFF0F172A)
val AppDarkCardBackground = Color(0xFF1E293B)
val AppDarkBorderColor = Color(0xFF334155)
val AppDarkTextWhite = Color(0xFFF8FAFC)
val AppDarkTextGray = Color(0xFF94A3B8)

val AppLightBackground = Color(0xFFFAFAFA) // Off-white warm grey
val AppLightCardBackground = Color(0xFFFFFFFF) // White cards
val AppLightBorderColor = Color(0xFFEEEEEE) // Light grey card border
val AppLightTextDark = Color(0xFF1C1C1E) // Dark text
val AppLightTextGray = Color(0xFF8E8E93) // Medium grey text

// Compatibility getters for existing screens:
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

val AccentCoral: Color
    @Composable
    get() = Color(0xFFFF453A) // System Red/Coral

val AccentEmerald: Color
    @Composable
    get() = Color(0xFF34C759) // System Green

val TextWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val TextGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

// Fallback legacy light tokens (unused, kept for reference/compilation if imported)
val LightBackground = Color(0xFFF8FAFC)
val LightCardBackground = Color(0xFFFFFFFF)
val LightBorderColor = Color(0xFFE2E8F0)
val LightTextDark = Color(0xFF0F172A)
val LightTextGray = Color(0xFF64748B)
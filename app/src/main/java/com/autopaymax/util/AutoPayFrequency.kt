package com.autopaymax.util

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

// Shared by AutoPayScreen's frequency chips and the screenshot-import auto-create path - one
// day-length table instead of two copies that can drift apart.
fun frequencyOffsetMillis(frequency: String): Long {
    val days = when (frequency) {
        "Weekly" -> 7L
        "Monthly" -> 30L
        "Quarterly" -> 90L
        "Yearly" -> 365L
        else -> 30L
    }
    return days * DAY_MILLIS
}

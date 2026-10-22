package com.autopaymax.util

import java.util.regex.Pattern

// Best-effort merchant/amount/frequency extraction from OCR'd screenshot text (a subscription
// confirmation, a Play Store billing page, a bank/UPI app screenshot, ...). Same spirit as
// EmailParser, but there's no sender to key off - the whole recognized text is scanned instead.
// Only a confident result (merchant AND amount both non-null) is trusted to auto-create a
// mandate; the caller (MainActivity) is what enforces that gate.
object ScreenshotOcrParser {

    data class Result(val merchant: String?, val amount: Double?, val frequency: String?)

    // High confidence: a currency symbol right next to the number.
    private val AMOUNT_WITH_SYMBOL = Pattern.compile("(?:₹|Rs\\.?|INR|\\$|USD|€|£)\\s?([\\d,]+(?:\\.\\d{1,2})?)")

    // Fallback: real captured Play Store subscription screenshots show ML Kit dropping the ₹
    // glyph entirely ("Next payment: 1,999.00 on 20 Sept 2026", no symbol at all - see
    // ScreenshotOcrParserTest). A price-context word right before the number recovers those.
    private val AMOUNT_NEAR_KEYWORD = Pattern.compile(
        "(?i)(?:payment|amount|price|total|bill|due|cost|charge)s?\\s*[:\\-]?\\s*" +
            "(?:₹|Rs\\.?|INR|\\$|USD|€|£)?\\s?([\\d,]+(?:\\.\\d{1,2})?)"
    )

    private fun amountsMatching(pattern: Pattern, text: String): List<Double> {
        val matcher = pattern.matcher(text)
        val values = mutableListOf<Double>()
        while (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let(values::add)
        }
        return values.filter { it > 0.0 }
    }

    fun parse(recognizedText: String): Result {
        val merchant = matchAutoPayApp(recognizedText)?.displayName

        // A plan-comparison or billing-history screenshot can show several prices - the smallest
        // is the safer guess for "the recurring charge" over a yearly total or a struck-through
        // price. The symbol-anchored match is trusted first (higher confidence); the
        // keyword-anchored one only kicks in when no currency symbol survived OCR at all.
        val symbolAmounts = amountsMatching(AMOUNT_WITH_SYMBOL, recognizedText)
        val amount = symbolAmounts.minOrNull()
            ?: amountsMatching(AMOUNT_NEAR_KEYWORD, recognizedText).minOrNull()

        val lower = recognizedText.lowercase()
        val frequency = when {
            "year" in lower || "annual" in lower -> "Yearly"
            "quarter" in lower || "3 month" in lower -> "Quarterly"
            "week" in lower -> "Weekly"
            "month" in lower -> "Monthly"
            else -> null
        }

        return Result(merchant, amount, frequency)
    }
}

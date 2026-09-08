package com.uj.appstorysautopaymanager.util

import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import java.util.regex.Pattern

// Parses the emails a user gets after setting up a mandate (autopay) and the recurring-charge /
// receipt emails after. Same shape as SmsParser: a mandate branch (-> ParsedResult(txn, mandate))
// and a transaction branch (-> ParsedResult(txn, null)). Fed by GmailSyncWorker; routed through
// AutoPayRepository.applyMandateEvent / applyTransactionEvent like the SMS/notification sources.
// Reuses ParsedResult (SmsParser.kt) and UpiPatternConfig.TxnPattern - no new types.
//
// VENDOR_SENDER_DOMAINS is hardcoded (senders are public/structural facts); only the wording
// regex is remote-configurable (EmailPatternConfig). Google Play patterns ship as real defaults
// (from actual captured receipts); everything else is empty until real captures exist.
object EmailParser {

    // vendorKey -> list of sender domains OR full addresses. A bare "netflix.com" matches the
    // address domain or any subdomain of it; a full "x@y.com" must match the address exactly.
    private val VENDOR_SENDER_DOMAINS: Map<String, List<String>> = mapOf(
        // Google Play (subscription receipts for anything billed through Play - Claude, Feenko, ...)
        "google_play" to listOf("googleplay-noreply@google.com"),
        // Indian UPI autopay / e-mandate senders
        "phonepe" to listOf("phonepe.com"),
        "gpay" to listOf("googlepay.com", "payments.google.com"),
        "paytm" to listOf("paytm.com", "paytmbank.com"),
        "npci" to listOf("npci.org.in"),
        "hdfc" to listOf("hdfcbank.net", "hdfcbank.com"),
        "icici" to listOf("icicibank.com"),
        "sbi" to listOf("sbi.co.in"),
        "axis" to listOf("axisbank.com"),
        // Global subscription vendors (direct billing, not via Play)
        "netflix" to listOf("netflix.com"),
        "spotify" to listOf("spotify.com"),
        "amazon_prime" to listOf("amazon.com", "amazon.in", "primevideo.com"),
        "disney_plus" to listOf("disneyplus.com"),
        "youtube_premium" to listOf("youtube.com"),
        "apple" to listOf("apple.com", "email.apple.com")
    )

    // Fallback (merchant name, category) used only when a pattern doesn't capture a name itself.
    private val VENDOR_DISPLAY: Map<String, Pair<String, String>> = mapOf(
        "google_play" to ("Google Play" to "Others"),
        "phonepe" to ("PhonePe" to "Others"),
        "gpay" to ("Google Pay" to "Others"),
        "paytm" to ("Paytm" to "Others"),
        "npci" to ("UPI Autopay" to "Others"),
        "hdfc" to ("HDFC Bank" to "Others"),
        "icici" to ("ICICI Bank" to "Others"),
        "sbi" to ("SBI" to "Others"),
        "axis" to ("Axis Bank" to "Others"),
        "netflix" to ("Netflix" to "OTT"),
        "spotify" to ("Spotify" to "Music"),
        "amazon_prime" to ("Amazon Prime" to "OTT"),
        "disney_plus" to ("Disney+" to "OTT"),
        "youtube_premium" to ("YouTube Premium" to "Music"),
        "apple" to ("Apple" to "Others")
    )

    // Extra Gmail `subject:` narrowing per vendor - Google Play sends dozens of non-payment
    // emails from the same address (terms updates, game promos, statements), so restrict the
    // fetch to order receipts AND cancellation notices. Vendors without an entry match on
    // sender alone.
    private val SUBJECT_FILTER: Map<String, String> = mapOf(
        "google_play" to "(\"Order Receipt\" OR cancelled OR canceled OR cancellation)"
    )

    // Per-vendor Gmail search clauses, e.g.
    //   (from:googleplay-noreply@google.com subject:"Order Receipt")
    //   (from:phonepe.com)
    // The worker ORs these together and AND-s a date window.
    fun gmailQueryClauses(): List<String> = VENDOR_SENDER_DOMAINS.map { (key, senders) ->
        val from = senders.joinToString(" OR ") { "from:$it" }
        val subj = SUBJECT_FILTER[key]?.let { " subject:$it" }.orEmpty()
        "($from$subj)"
    }

    // From a raw From-header ("Google Play <googleplay-noreply@google.com>") pick the vendor.
    fun vendorKeyFor(fromHeader: String): String? {
        val addr = fromHeader.substringAfterLast('<').substringBefore('>').ifBlank { fromHeader }.trim().lowercase()
        val domain = addr.substringAfterLast('@').ifBlank { return null }
        return VENDOR_SENDER_DOMAINS.entries.firstOrNull { (_, senders) ->
            senders.any { s ->
                if (s.contains('@')) addr == s.lowercase()
                else domain == s || domain.endsWith(".$s")
            }
        }?.key
    }

    fun parse(
        vendorKey: String,
        subject: String,
        bodyText: String,
        receivedAt: Long,
        gmailMessageId: String
    ): ParsedResult? = parseWith(
        vendorKey, subject, bodyText, receivedAt, gmailMessageId,
        EmailPatternConfig.mandatePatterns(vendorKey),
        EmailPatternConfig.debitPatterns(vendorKey),
        EmailPatternConfig.cancelPatterns(vendorKey)
    )

    // Pattern lists passed in (not read from Remote Config here) so this branch/loop logic is
    // unit-testable without Firebase - see EmailParserTest.
    internal fun parseWith(
        vendorKey: String,
        subject: String,
        bodyText: String,
        receivedAt: Long,
        gmailMessageId: String,
        mandatePatterns: List<UpiPatternConfig.TxnPattern>,
        debitPatterns: List<UpiPatternConfig.TxnPattern>,
        cancelPatterns: List<UpiPatternConfig.TxnPattern> = emptyList()
    ): ParsedResult? {
        val smsId = "email_$gmailMessageId"
        val combined = "$subject\n$bodyText".replace('\r', ' ')
        val (displayName, category) = VENDOR_DISPLAY[vendorKey]
            ?: (vendorKey.replaceFirstChar { it.uppercase() } to "Others")

        // The app/product name is what shows on Home (and drives the brand-icon match) - Google
        // Play receipts put it in the parentheses right before the price. Falls through to the
        // pattern's own capture, then the vendor display name.
        fun merchantFor(patternName: String?): String =
            playAppName(combined) ?: patternName?.takeIf { it.isNotBlank() } ?: displayName

        // Cancellation branch first - a "your <app> subscription will be cancelled" email must
        // never be misread as a new/renewed mandate. Flips the matching ACTIVE mandate to
        // CANCELLED via AutoPayRepository.applyMandateEvent. The subject's app name is the most
        // reliable source here, so prefer it over any parenthetical in the body.
        matchName(cancelPatterns, combined)?.let { name ->
            val merchant = name.ifBlank { null } ?: playAppName(combined) ?: displayName
            val transaction = Transaction(
                smsId = smsId, merchant = merchant, amount = 0.0, date = receivedAt,
                bankName = displayName, accountNumber = "XXXX", referenceNumber = "",
                transactionType = "DEBIT", category = category, smsBody = combined, isAutoPay = false
            )
            val mandate = Mandate(
                merchant = merchant, amount = 0.0, frequency = "MONTHLY",
                nextExpectedDebit = receivedAt, bank = displayName,
                status = "CANCELLED", referenceNumber = "", category = category
            )
            return ParsedResult(transaction, mandate)
        }

        // Mandate branch - an autopay-setup / renewal email. The pattern's amount is the
        // RECURRING price (goes on the Mandate); the amount actually billed in this email comes
        // from the receipt "Total:" line (0 for a free-trial sign-up -> GmailSyncWorker logs no
        // Passbook charge for it).
        matchFirst(mandatePatterns, combined)?.let { (recurringAmount, name) ->
            val merchant = merchantFor(name)
            val chargedNow = receiptTotal(combined) ?: recurringAmount
            val freq = frequencyOf(combined)
            val transaction = Transaction(
                smsId = smsId, merchant = merchant, amount = chargedNow, date = receivedAt,
                bankName = displayName, accountNumber = "XXXX", referenceNumber = "",
                transactionType = "DEBIT", category = category, smsBody = combined, isAutoPay = true
            )
            val mandate = Mandate(
                merchant = merchant, amount = recurringAmount, frequency = freq,
                nextExpectedDebit = receivedAt + periodMillis(freq),
                bank = displayName, status = "ACTIVE", referenceNumber = "", category = category
            )
            return ParsedResult(transaction, mandate)
        }

        // Debit branch - a recurring charge / receipt with no explicit mandate wording. Autopay
        // inference falls to AutoPayRepository.detectRecurringMandate once enough line up.
        matchFirst(debitPatterns, combined)?.let { (amount, name) ->
            val transaction = Transaction(
                smsId = smsId, merchant = merchantFor(name), amount = amount, date = receivedAt,
                bankName = displayName, accountNumber = "XXXX", referenceNumber = "",
                transactionType = "DEBIT", category = category, smsBody = combined, isAutoPay = false
            )
            return ParsedResult(transaction, null)
        }

        return null
    }

    // "<Product> (<App Name>)   ₹1,999.00/month" -> "App Name". The parens group immediately
    // before a price. Null when there isn't one (non-Play emails, plain item names).
    private val PLAY_APP_NAME = Pattern.compile("\\(([^()]{2,80})\\)\\s+(?:₹|Rs\\.?|INR)\\s*[\\d,]")
    private fun playAppName(text: String): String? {
        val m = PLAY_APP_NAME.matcher(text)
        return if (m.find()) m.group(1)?.trim()?.takeIf { it.isNotEmpty() } else null
    }

    // For cancel patterns (no amount) - returns the senderGroup name, "" if matched with no
    // capture, null if no pattern matched.
    private fun matchName(patterns: List<UpiPatternConfig.TxnPattern>, text: String): String? {
        for (p in patterns) {
            val m = p.regex.matcher(text)
            if (!m.find()) continue
            return if (p.senderGroup != -1) m.group(p.senderGroup)?.trim().orEmpty() else ""
        }
        return null
    }

    private fun matchFirst(
        patterns: List<UpiPatternConfig.TxnPattern>,
        text: String
    ): Pair<Double, String?>? {
        for (p in patterns) {
            val m = p.regex.matcher(text)
            if (!m.find()) continue
            val amount = m.group(p.amountGroup)?.replace(",", "")?.toDoubleOrNull() ?: continue
            if (amount <= 0.0) continue
            val name = if (p.senderGroup != -1) m.group(p.senderGroup)?.trim()?.takeIf { it.isNotEmpty() } else null
            return amount to name
        }
        return null
    }

    // Best-effort "was anything actually billed in this email" - a receipt "Total:" line. Absent
    // for vendors whose emails have no such line, in which case the caller assumes a real charge.
    private val RECEIPT_TOTAL = Pattern.compile("(?i)Total:\\s*(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{2})?)")
    private fun receiptTotal(text: String): Double? {
        val m = RECEIPT_TOTAL.matcher(text)
        return if (m.find()) m.group(1)?.replace(",", "")?.toDoubleOrNull() else null
    }

    private fun frequencyOf(text: String): String {
        val t = text.lowercase()
        return when {
            "6 month" in t || "half-year" in t || "semi-annual" in t -> "HALF_YEARLY"
            "3 month" in t || "quarter" in t -> "QUARTERLY"
            "/week" in t || "weekly" in t -> "WEEKLY"
            "/year" in t || "yearly" in t || "annual" in t || "12 month" in t -> "YEARLY"
            else -> "MONTHLY"
        }
    }

    private const val DAY = 24L * 60 * 60 * 1000
    private fun periodMillis(freq: String): Long = when (freq) {
        "WEEKLY" -> 7 * DAY
        "QUARTERLY" -> 91 * DAY
        "HALF_YEARLY" -> 182 * DAY
        "YEARLY" -> 365 * DAY
        else -> 30 * DAY
    }
}

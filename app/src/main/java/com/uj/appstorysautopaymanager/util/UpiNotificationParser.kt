package com.uj.appstorysautopaymanager.util

import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import java.util.regex.Pattern

// GPay/PhonePe/BHIM/generic credit patterns below are adopted from com.uj.appstoryssoundbox's
// sibling UpiParser (same org, already real-world validated - it's the parser behind the
// "SoundBoxOne" payment-received announcer app), not guessed. The debit-direction patterns
// (money going OUT) have no equivalent there - SoundBox only ever needed to announce payments
// *received*, so those remain ponytail-flagged calculated guesses. Correct them against a real
// captured outgoing-payment notification the first time one misfires.
object UpiNotificationParser {

    const val PAYTM_PACKAGE = "net.one97.paytm"
    const val PAYTM_BUSINESS_PACKAGE = "com.paytm.business"
    const val GPAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"
    const val GPAY_MERCHANT_PACKAGE = "com.google.android.apps.nbu.paisa.merchant"
    const val PHONEPE_PACKAGE = "com.phonepe.app"
    const val PHONEPE_BUSINESS_PACKAGE = "com.phonepe.app.business"
    const val PHONEPE_MERCHANT_PACKAGE = "com.phonepe.merchant"
    const val BHIM_PACKAGE = "in.org.npci.upiapp"

    // No app-specific wording known for these, but they commonly share the same "paid you" /
    // "received" / "credited" phrasing the generic patterns below cover. (Deliberately excludes
    // com.google.android.gms - too broad, fires for unrelated system notifications - and non-UPI
    // apps like Truecaller/SMS clients that SoundBox's own package map also lists but never
    // parses payments from.)
    //
    // WhatsApp/WhatsApp Business were removed (spoof hole, not an oversight): every other package
    // here only ever shows notification text the app itself generates from its own backend event -
    // WhatsApp is a free-text messaging surface where any contact can type "Received Rs.500 from
    // X" in a normal chat message and have it parsed as a real UPI credit. If WhatsApp Pay ever
    // needs tracking, it needs its own narrowly-scoped pattern, not blanket trust of chat text.
    private val GENERIC_TRACKED_PACKAGES = setOf(
        "com.amazon.mShop.android.shopping", // Amazon Pay
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.dreamplug.androidapp" // CRED
    )

    private val TRACKED_PACKAGES: Set<String> = setOf(
        PAYTM_PACKAGE, PAYTM_BUSINESS_PACKAGE,
        GPAY_PACKAGE, GPAY_MERCHANT_PACKAGE,
        PHONEPE_PACKAGE, PHONEPE_BUSINESS_PACKAGE, PHONEPE_MERCHANT_PACKAGE,
        BHIM_PACKAGE
    ) + GENERIC_TRACKED_PACKAGES

    fun isTracked(packageName: String): Boolean = packageName in TRACKED_PACKAGES

    fun parse(packageName: String, title: String, text: String, postedAt: Long, notifId: String): ParsedResult? {
        val combined = normalize("$title $text")
        return when (packageName) {
            PAYTM_PACKAGE, PAYTM_BUSINESS_PACKAGE -> parsePaytm(combined, postedAt, notifId)
            GPAY_PACKAGE, GPAY_MERCHANT_PACKAGE ->
                matchPatterns(GPAY_CREDIT_PATTERNS, combined, true, postedAt, notifId, "GPAY")
                    ?: matchPatterns(DEBIT_GUESS_PATTERNS, combined, false, postedAt, notifId, "GPAY")
            PHONEPE_PACKAGE, PHONEPE_BUSINESS_PACKAGE, PHONEPE_MERCHANT_PACKAGE ->
                matchPatterns(PHONEPE_CREDIT_PATTERNS, combined, true, postedAt, notifId, "PHONEPE")
                    ?: matchPatterns(DEBIT_GUESS_PATTERNS, combined, false, postedAt, notifId, "PHONEPE")
            BHIM_PACKAGE -> matchPatterns(BHIM_CREDIT_PATTERNS, combined, true, postedAt, notifId, "BHIM")
            in GENERIC_TRACKED_PACKAGES -> matchPatterns(GENERIC_CREDIT_PATTERNS, combined, true, postedAt, notifId, "UPI")
            else -> null
        }
    }

    // ---- Paytm (mandate templates given directly; transfer templates confirmed on-device) ----

    private fun parsePaytm(combined: String, postedAt: Long, notifId: String): ParsedResult? {
        val mandateId = MANDATE_ID.matcher(combined).let { if (it.find()) it.group(1) ?: "" else "" }

        val created = PAYTM_MANDATE_CREATED.matcher(combined)
        if (created.find()) {
            val merchant = created.group(1)?.trim() ?: "Merchant"
            val amount = created.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val freq = mapFrequency(created.group(3))
            return mandateCreatedResult(merchant, amount, freq, "PAYTM", postedAt, notifId, combined, mandateId)
        }

        val revoked = PAYTM_MANDATE_REVOKED.matcher(combined)
        if (revoked.find()) {
            val merchant = revoked.group(1)?.trim() ?: "Merchant"
            return mandateRevokedResult(merchant, "PAYTM", postedAt, notifId, combined, mandateId)
        }

        // Confirmed on-device: title "Received ₹1 from Ujjawal Nigam", text "Deposited in your
        // Bank Of Baroda - 4326 on 19 August at 3:40 PM". Sender-name bound by a broad set of
        // trailing keywords (adopted from SoundBox's PAYTM_PATTERNS) so it survives wording
        // variants, not just the one literal example seen.
        val creditNamed = PAYTM_CREDIT_WITH_NAME.matcher(combined)
        if (creditNamed.find()) {
            val amount = creditNamed.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = cleanSenderName(creditNamed.group(2)) ?: "UPI Credit"
            val (bank, account) = extractBankAccount(combined)
            return transferResult(name, amount, true, bank, postedAt, notifId, combined, account)
        }

        // ponytail: guessed by symmetry with the confirmed credit format above, not yet seen in
        // a real capture - fix the wording the first time an actual debit notification misfires.
        val debitNamed = PAYTM_DEBIT_WITH_NAME.matcher(combined)
        if (debitNamed.find()) {
            val amount = debitNamed.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = cleanSenderName(debitNamed.group(2)) ?: "Merchant"
            val (bank, account) = extractBankAccount(combined)
            return transferResult(name, amount, false, bank, postedAt, notifId, combined, account)
        }

        // Fallback: originally guessed generic wallet template, kept in case some other Paytm
        // notification variant actually uses it.
        val transfer = PAYTM_DAILY_TRANSFER.matcher(combined)
        if (transfer.find()) {
            val amount = transfer.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val isCredit = transfer.group(2)?.equals("credited", ignoreCase = true) == true
            return transferResult(if (isCredit) "UPI Credit" else "Merchant", amount, isCredit, "PAYTM", postedAt, notifId, combined)
        }

        return null
    }

    private fun extractBankAccount(combined: String): Pair<String, String> {
        val matcher = PAYTM_BANK_ACCOUNT.matcher(combined)
        return if (matcher.find()) {
            val bank = matcher.group(1)?.trim()?.uppercase() ?: "PAYTM"
            val account = "XX" + (matcher.group(2) ?: "")
            bank to account
        } else {
            "PAYTM" to "XXXX"
        }
    }

    // ---- shared pattern matching (GPay/PhonePe/BHIM/generic) ----

    private data class TxnPattern(val regex: Pattern, val amountGroup: Int, val senderGroup: Int = -1)

    private fun matchPatterns(
        patterns: List<TxnPattern>, combined: String, isCredit: Boolean,
        postedAt: Long, notifId: String, bank: String
    ): ParsedResult? {
        for (p in patterns) {
            val m = p.regex.matcher(combined)
            if (!m.find()) continue
            val amountStr = m.group(p.amountGroup) ?: continue
            val amount = amountStr.replace(",", "").toDoubleOrNull() ?: continue
            if (amount <= 0.0) continue
            val name = if (p.senderGroup != -1) cleanSenderName(m.group(p.senderGroup)) else null
            val fallbackMerchant = if (isCredit) "UPI Credit" else "Merchant"
            return transferResult(name ?: fallbackMerchant, amount, isCredit, bank, postedAt, notifId, combined)
        }
        return null
    }

    private fun cleanSenderName(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        // A greedy capture sometimes swallows trailing boilerplate instead of stopping at the
        // sender's name - treat that as "no usable name" rather than showing the boilerplate.
        if (lower.contains("credited to") || lower.contains("received in")) return null
        var cleaned = trimmed
        if (lower.startsWith("money received")) {
            cleaned = cleaned.substring("money received".length).trim()
        }
        cleaned = cleaned.trimEnd('.', ',', '!')
        return cleaned.takeIf { it.isNotEmpty() }
    }

    private fun normalize(text: String): String = text
        .replace('\u00A0', ' ') // non-breaking space
        .replace('\u200B', ' ') // zero-width space
        .replace('\n', ' ')
        .replace(WHITESPACE, " ")
        .trim()

    // ---- shared result builders (same shape SmsParser builds) ----

    private fun mandateCreatedResult(
        merchant: String, amount: Double, freq: String, bank: String,
        postedAt: Long, notifId: String, raw: String, mandateId: String
    ): ParsedResult {
        val periodDays = when (freq) { "WEEKLY" -> 7L; "YEARLY" -> 365L; else -> 30L }
        val transaction = Transaction(
            smsId = notifId, merchant = merchant, amount = amount, date = postedAt, bankName = bank,
            accountNumber = "XXXX", referenceNumber = mandateId, transactionType = "DEBIT",
            category = "Others", smsBody = raw, isAutoPay = true
        )
        val mandate = Mandate(
            merchant = merchant, amount = amount, frequency = freq,
            nextExpectedDebit = postedAt + (periodDays * 24L * 60L * 60L * 1000L),
            bank = bank, status = "ACTIVE", referenceNumber = mandateId
        )
        return ParsedResult(transaction, mandate)
    }

    private fun mandateRevokedResult(
        merchant: String, bank: String, postedAt: Long, notifId: String, raw: String, mandateId: String
    ): ParsedResult {
        val transaction = Transaction(
            smsId = notifId, merchant = merchant, amount = 0.0, date = postedAt, bankName = bank,
            accountNumber = "XXXX", referenceNumber = mandateId, transactionType = "DEBIT",
            category = "Others", smsBody = raw, isAutoPay = true
        )
        val mandate = Mandate(
            merchant = merchant, amount = 0.0, frequency = "MONTHLY", nextExpectedDebit = postedAt,
            bank = bank, status = "CANCELLED", referenceNumber = mandateId
        )
        return ParsedResult(transaction, mandate)
    }

    private fun transferResult(
        merchant: String, amount: Double, isCredit: Boolean, bank: String,
        postedAt: Long, notifId: String, raw: String, accountNumber: String = "XXXX"
    ): ParsedResult {
        val transaction = Transaction(
            smsId = notifId, merchant = merchant, amount = amount, date = postedAt, bankName = bank,
            accountNumber = accountNumber, referenceNumber = "",
            transactionType = if (isCredit) "CREDIT" else "DEBIT",
            category = "Others", smsBody = raw, isAutoPay = false
        )
        return ParsedResult(transaction, null)
    }

    private fun mapFrequency(word: String?): String = when {
        word == null -> "MONTHLY"
        word.startsWith("week", ignoreCase = true) -> "WEEKLY"
        word.startsWith("year", ignoreCase = true) -> "YEARLY"
        else -> "MONTHLY"
    }

    private val WHITESPACE = Regex("""\s+""")
    private val MANDATE_ID = Pattern.compile("(?i)mandate id:\\s*([A-Za-z0-9]+)")

    // Shared currency/amount building blocks (mirrors SoundBox's CURRENCY_PREFIX/AMOUNT_PATTERN).
    private const val CURRENCY_PREFIX = """(?:₹|Rs\.?|INR|Rupees)\s*"""
    private const val AMOUNT_PATTERN = """([0-9,]+(?:\.[0-9]{1,2})?)"""

    // Paytm - mandate templates given directly by the user; transfer templates confirmed
    // on-device (PAYTM_CREDIT_WITH_NAME).
    private val PAYTM_MANDATE_CREATED = Pattern.compile(
        "(?i)autopay mandate for (.+?) has been successfully created\\.?\\s*amount:\\s*up to inr\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*per\\s*(\\w+)"
    )
    private val PAYTM_MANDATE_REVOKED = Pattern.compile(
        "(?i)autopay mandate for (.+?)\\s*\\(mandate id:[^)]*\\)\\s*has been successfully (?:revoked|cancelled)"
    )
    private val PAYTM_CREDIT_WITH_NAME = Pattern.compile(
        """(?i)received\s*(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d{1,2})?)\s+from\s+(.*?)(?:\s+(?:deposited|credited|sent|received|in|on|at|via|to)\b|$)"""
    )
    // ponytail: unverified guess by symmetry with PAYTM_CREDIT_WITH_NAME above.
    private val PAYTM_DEBIT_WITH_NAME = Pattern.compile(
        """(?i)(?:paid|sent)\s*(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(.*?)(?:\s+(?:debited|paid|sent|deducted|from|on|at|via)\b|$)"""
    )
    private val PAYTM_BANK_ACCOUNT = Pattern.compile(
        "(?i)(?:deposited in|debited from) your (.+?)\\s*-\\s*(\\d{3,6})"
    )
    private val PAYTM_DAILY_TRANSFER = Pattern.compile(
        "(?i)inr\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*has been (debited|credited)\\s*(?:from|to)\\s*your paytm wallet"
    )

    // Google Pay - credit direction adopted from SoundBox's real GPAY_PATTERNS.
    private val GPAY_CREDIT_PATTERNS = listOf(
        TxnPattern(Pattern.compile("""(?i)(.+?)\s+paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
        TxnPattern(Pattern.compile("""(?i)(.+?)\s+sent\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
        TxnPattern(Pattern.compile("""(?i)paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+payment\s+received"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)You\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)Payment\s+of\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2)
    )

    // PhonePe - credit direction adopted from SoundBox's real PHONEPE_PATTERNS.
    private val PHONEPE_CREDIT_PATTERNS = listOf(
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)Payment\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)Received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)Payment\s+of\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)Payment\s+of\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)You(?:'ve|\s+have)?\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)You(?:'ve|\s+have)?\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)Received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+credited"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)(.+?)\s+has\s+sent\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1)
    )

    // BHIM - adopted from SoundBox's real BHIM_PATTERNS.
    private val BHIM_CREDIT_PATTERNS = listOf(
        TxnPattern(Pattern.compile("""(?i)Transaction\s+successful\.\s*$CURRENCY_PREFIX$AMOUNT_PATTERN\s+credited"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)You\s+have\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2)
    )

    // Generic fallback for the extra tracked packages with no dedicated wording known - adopted
    // from SoundBox's real GENERIC_PATTERNS.
    private val GENERIC_CREDIT_PATTERNS = listOf(
        TxnPattern(Pattern.compile("""(?i)(.+?)\s+paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
        TxnPattern(Pattern.compile("""(?i)\+\s*$CURRENCY_PREFIX$AMOUNT_PATTERN\s+UPI\s+transfer\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
        TxnPattern(Pattern.compile("""(?i)received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)credited\s+with\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+credited"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received"""), amountGroup = 1),
        TxnPattern(Pattern.compile("""(?i)(.+?)\s+has\s+sent\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1)
    )

    // ponytail: GPay/PhonePe outgoing-payment wording, unverified guess by symmetry with the
    // confirmed "<Name> paid/sent you" credit wording above (SoundBox never needed this
    // direction). Fix against a real captured debit notification the first time it misfires.
    private val DEBIT_GUESS_PATTERNS = listOf(
        TxnPattern(Pattern.compile("""(?i)you\s+(?:paid|sent)\s+(.+?)\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
        TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+(?:paid|sent)\s+to\s+(.+)"""), amountGroup = 1, senderGroup = 2)
    )
}

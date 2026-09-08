package com.autopaymax.util

import com.autopaymax.data.local.entity.Mandate
import com.autopaymax.data.local.entity.Transaction

// GPay/PhonePe/BHIM/generic credit patterns below are adopted from com.uj.appstoryssoundbox's
// sibling UpiParser (same org, already real-world validated - it's the parser behind the
// "SoundBoxOne" payment-received announcer app), not guessed. The debit-direction patterns
// (money going OUT) have no equivalent there - SoundBox only ever needed to announce payments
// *received*, so those remain ponytail-flagged calculated guesses. Correct them against a real
// captured outgoing-payment notification the first time one misfires.
private typealias TxnPattern = UpiPatternConfig.TxnPattern

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
                matchPatterns(UpiPatternConfig.gpayCreditPatterns, combined, true, postedAt, notifId, "GPAY")
                    ?: matchPatterns(UpiPatternConfig.debitGuessPatterns, combined, false, postedAt, notifId, "GPAY")
            PHONEPE_PACKAGE, PHONEPE_BUSINESS_PACKAGE, PHONEPE_MERCHANT_PACKAGE ->
                matchPatterns(UpiPatternConfig.phonepeCreditPatterns, combined, true, postedAt, notifId, "PHONEPE")
                    ?: matchPatterns(UpiPatternConfig.debitGuessPatterns, combined, false, postedAt, notifId, "PHONEPE")
            BHIM_PACKAGE -> matchPatterns(UpiPatternConfig.bhimCreditPatterns, combined, true, postedAt, notifId, "BHIM")
            in GENERIC_TRACKED_PACKAGES -> matchPatterns(UpiPatternConfig.genericCreditPatterns, combined, true, postedAt, notifId, "UPI")
            else -> null
        }
    }

    // ---- Paytm (mandate templates given directly; transfer templates confirmed on-device) ----

    private fun parsePaytm(combined: String, postedAt: Long, notifId: String): ParsedResult? {
        val mandateId = UpiPatternConfig.mandateIdPattern.matcher(combined).let { if (it.find()) it.group(1) ?: "" else "" }

        val created = UpiPatternConfig.paytmMandateCreatedPattern.matcher(combined)
        if (created.find()) {
            val merchant = created.group(1)?.trim() ?: "Merchant"
            val amount = created.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val freq = mapFrequency(created.group(3))
            return mandateCreatedResult(merchant, amount, freq, "PAYTM", postedAt, notifId, combined, mandateId)
        }

        val revoked = UpiPatternConfig.paytmMandateRevokedPattern.matcher(combined)
        if (revoked.find()) {
            val merchant = revoked.group(1)?.trim() ?: "Merchant"
            return mandateRevokedResult(merchant, "PAYTM", postedAt, notifId, combined, mandateId)
        }

        // Confirmed on-device: title "Received ₹1 from Ujjawal Nigam", text "Deposited in your
        // Bank Of Baroda - 4326 on 19 August at 3:40 PM". Sender-name bound by a broad set of
        // trailing keywords (adopted from SoundBox's PAYTM_PATTERNS) so it survives wording
        // variants, not just the one literal example seen.
        val creditNamed = UpiPatternConfig.paytmCreditWithNamePattern.matcher(combined)
        if (creditNamed.find()) {
            val amount = creditNamed.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = cleanSenderName(creditNamed.group(2)) ?: "UPI Credit"
            val (bank, account) = extractBankAccount(combined)
            return transferResult(name, amount, true, bank, postedAt, notifId, combined, account)
        }

        // ponytail: guessed by symmetry with the confirmed credit format above, not yet seen in
        // a real capture - fix the wording the first time an actual debit notification misfires.
        val debitNamed = UpiPatternConfig.paytmDebitWithNamePattern.matcher(combined)
        if (debitNamed.find()) {
            val amount = debitNamed.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = cleanSenderName(debitNamed.group(2)) ?: "Merchant"
            val (bank, account) = extractBankAccount(combined)
            return transferResult(name, amount, false, bank, postedAt, notifId, combined, account)
        }

        // Fallback: originally guessed generic wallet template, kept in case some other Paytm
        // notification variant actually uses it.
        val transfer = UpiPatternConfig.paytmDailyTransferPattern.matcher(combined)
        if (transfer.find()) {
            val amount = transfer.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val isCredit = transfer.group(2)?.equals("credited", ignoreCase = true) == true
            return transferResult(if (isCredit) "UPI Credit" else "Merchant", amount, isCredit, "PAYTM", postedAt, notifId, combined)
        }

        return null
    }

    private fun extractBankAccount(combined: String): Pair<String, String> {
        val matcher = UpiPatternConfig.paytmBankAccountPattern.matcher(combined)
        return if (matcher.find()) {
            val bank = matcher.group(1)?.trim()?.uppercase() ?: "PAYTM"
            val account = "XX" + (matcher.group(2) ?: "")
            bank to account
        } else {
            "PAYTM" to "XXXX"
        }
    }

    // ---- shared pattern matching (GPay/PhonePe/BHIM/generic) ----

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
}

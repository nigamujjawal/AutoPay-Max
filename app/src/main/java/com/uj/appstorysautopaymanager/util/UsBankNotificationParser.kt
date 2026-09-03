package com.uj.appstorysautopaymanager.util

import com.uj.appstorysautopaymanager.data.local.entity.Transaction

// Mirrors UpiNotificationParser's shape (same NotificationListenerService, same
// title+text -> regex -> amount/merchant extraction flow) but for US banking/P2P apps instead of
// Indian UPI apps - kept as a separate file rather than folded into UpiNotificationParser since
// "Upi" doesn't apply to any of these. There is no US equivalent of NPCI's e-mandate SMS, so this
// never sets a Transaction's isAutoPay/emits a Mandate directly - recurring/autopay detection for
// these sources instead relies on AutoPayRepository's amount+merchant recurrence heuristic (see
// its detectRecurringMandate).
//
// Package names below are best-effort from public knowledge, NOT verified against current Play
// Store listings - confirm each one (adb shell pm list packages on a device with the app
// installed, or the app's Play Store URL) before relying on this to actually intercept anything.
// A wrong package name is a silent no-op (nothing to match against), not a false positive.
object UsBankNotificationParser {

    private val BANK_PACKAGES: Map<String, String> = mapOf(
        "chase" to "com.chase.sig.android",
        "bofa" to "com.infonow.bofa",
        "wells_fargo" to "com.wf.wellsfargomobile",
        "capital_one" to "com.konylabs.capitalone",
        "venmo" to "com.venmo",
        "cash_app" to "com.squareup.cash"
    )

    private val PACKAGE_TO_KEY: Map<String, String> = BANK_PACKAGES.entries.associate { (k, v) -> v to k }

    fun isTracked(packageName: String): Boolean = packageName in PACKAGE_TO_KEY

    fun parse(packageName: String, title: String, text: String, postedAt: Long, notifId: String): ParsedResult? {
        val bankKey = PACKAGE_TO_KEY[packageName] ?: return null
        val combined = "$title $text".replace('\n', ' ').trim()
        return matchPatterns(UsBankPatternConfig.creditPatterns(bankKey), combined, true, postedAt, notifId, bankKey)
            ?: matchPatterns(UsBankPatternConfig.debitPatterns(bankKey), combined, false, postedAt, notifId, bankKey)
    }

    private fun matchPatterns(
        patterns: List<UpiPatternConfig.TxnPattern>, combined: String, isCredit: Boolean,
        postedAt: Long, notifId: String, bankKey: String
    ): ParsedResult? {
        for (p in patterns) {
            val m = p.regex.matcher(combined)
            if (!m.find()) continue
            val amountStr = m.group(p.amountGroup) ?: continue
            val amount = amountStr.replace(",", "").toDoubleOrNull() ?: continue
            if (amount <= 0.0) continue
            val name = if (p.senderGroup != -1) m.group(p.senderGroup)?.trim()?.takeIf { it.isNotEmpty() } else null
            val merchant = name ?: if (isCredit) "Payment Received" else "Merchant"
            val transaction = Transaction(
                smsId = notifId, merchant = merchant, amount = amount, date = postedAt,
                bankName = bankKey.uppercase(), accountNumber = "XXXX", referenceNumber = "",
                transactionType = if (isCredit) "CREDIT" else "DEBIT",
                category = "Others", smsBody = combined, isAutoPay = false
            )
            return ParsedResult(transaction, null)
        }
        return null
    }
}

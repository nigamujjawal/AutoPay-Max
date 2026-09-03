package com.uj.appstorysautopaymanager.util

import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import java.util.regex.Pattern

object SmsParser {

    // "+ ₹500" style leading-plus credit convention (some bank SMS), confirmed real wording via
    // com.uj.appstoryssoundbox's sibling parser.
    private val PLUS_CREDIT_PATTERN = Pattern.compile("(?i)\\+\\s*(?:₹|rs\\.?|inr)")

    fun parseSms(smsBody: String, smsDate: Long, smsId: String, senderAddress: String = ""): ParsedResult? {
        val lowercaseBody = smsBody.lowercase()

        // 1. Detect if it's a financial transaction (credit/debit/autopay/mandate)
        // Note: no bare "credit"/"debit" catch-all here - those false-positive on
        // "Credit Card"/"Debit Card" mentions, which appear in almost every card SMS
        // regardless of which direction the money actually moved.
        val isDebit = lowercaseBody.contains("debited") ||
                      lowercaseBody.contains("deducted") ||
                      lowercaseBody.contains("spent") ||
                      // "paid" alone doesn't mean a debit if it's "<Name> paid you" - that's
                      // someone else paying the user, a credit (see isCredit below).
                      (lowercaseBody.contains("paid") && !lowercaseBody.contains("paid you")) ||
                      lowercaseBody.contains("withdrawn") ||
                      lowercaseBody.contains("payment to") ||
                      lowercaseBody.contains("auto debit") ||
                      lowercaseBody.contains("mandate executed") ||
                      // "Dr."/"Cr." shorthand (e.g. BOB: "Rs.X Dr. from A/C... and Cr. to
                      // <vpa>") never contains the full words "debited"/"credited" at all, so
                      // it was falling through every check above and getting dropped entirely.
                      lowercaseBody.contains("dr. from") ||
                      lowercaseBody.contains("dr from")

        val isCredit = lowercaseBody.contains("credited") ||
                       lowercaseBody.contains("received") ||
                       lowercaseBody.contains("rcvd") ||
                       lowercaseBody.contains("deposited") ||
                       lowercaseBody.contains("added") ||
                       lowercaseBody.contains("refunded") ||
                       // "<Name> paid you"/"<Name> sent you" - real UPI-app wording where
                       // someone else pays the user, confirmed via the sibling SoundBox parser.
                       lowercaseBody.contains("paid you") ||
                       lowercaseBody.contains("sent you") ||
                       lowercaseBody.contains("upi-cr") ||
                       lowercaseBody.contains("upi credit") ||
                       PLUS_CREDIT_PATTERN.matcher(smsBody).find()

        val isMandate = lowercaseBody.contains("mandate") || 
                        lowercaseBody.contains("autopay") || 
                        lowercaseBody.contains("auto pay") || 
                        lowercaseBody.contains("auto-debit") || 
                        lowercaseBody.contains("auto debit") || 
                        lowercaseBody.contains("nach") || 
                        lowercaseBody.contains("standing instruction") || 
                        lowercaseBody.contains("e-mandate") ||
                        lowercaseBody.contains("subscription") ||
                        lowercaseBody.contains("recurring") ||
                        lowercaseBody.contains("sip") ||
                        lowercaseBody.contains("renewal") ||
                        lowercaseBody.contains("renewed")

        // Hard check for actually promoting something to "Current Auto Payments": the loose
        // keywords above (subscription/recurring/sip/...) are too easy to false-positive on
        // unrelated SMS, so only these two exact mandate lifecycle phrases create/cancel one.
        // "created a mandate"/"revoked a mandate" is the bank's own NPCI e-mandate template;
        // "has been setup successfully" is Paytm's own autopay-setup confirmation template -
        // both are exact, low-false-positive phrases, so both count as hard checks.
        val isMandateCreated = lowercaseBody.contains("created a mandate") ||
                                lowercaseBody.contains("has been setup successfully")
        val isMandateRevoked = lowercaseBody.contains("revoked a mandate")

        if (!isDebit && !isCredit && !isMandate && !isMandateCreated && !isMandateRevoked) {
            return null // Not a financial transaction
        }

        // 2. Extract Amount
        var amount = 0.0
        // Keyword-anchored first (tied to the actual debited/credited/deducted/amount verb,
        // with an optional "with"/"of" filler and an optional currency symbol in between -
        // e.g. "credited with INR 60.00", "credited with 200.00", "for amount 299.00"). More
        // reliable than scanning for the first Rs/INR figure anywhere in the message, which
        // can just as easily match the running balance ("AvlBal: Rs9548.54") when the actual
        // transaction amount has no currency prefix of its own.
        val keywordAmountPattern = Pattern.compile("(?i)(?:debited|credited|received|rcvd|deducted|amount(?:\\s+of)?)\\s+(?:with\\s+)?(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
        val keywordAmountMatcher = keywordAmountPattern.matcher(smsBody)
        if (keywordAmountMatcher.find()) {
            val amountStr = keywordAmountMatcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull() ?: 0.0
        }

        if (amount == 0.0) {
            // Fallback: first Rs/INR/₹ figure anywhere (e.g. "Rs.39.09 Dr. from A/C...", which
            // has no debited/credited/deducted/amount keyword before the number at all).
            val genericAmountPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")
            val genericAmountMatcher = genericAmountPattern.matcher(smsBody)
            if (genericAmountMatcher.find()) {
                val amountStr = genericAmountMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull() ?: 0.0
            }
        }

        // 3. Extract Bank Name - checks the SMS sender address first, then falls back to the
        // body text. DLT-registered bank sender headers glue the bank code straight onto the
        // rest with no separator (e.g. "VM-BOBSMS", "AD-BOBTXN"), so a plain substring check is
        // used there - a \b...\b word-boundary match (needed below to avoid misfiring on body
        // prose) would never match "bob" inside "BOBSMS", since there's no boundary between the
        // "B" and the "S".
        var bankName = "Unknown Bank"
        val bankCodes = listOf("hdfc", "icici", "sbi", "axis", "kotak", "pnb", "bob", "hsbc", "citi", "canara", "yesbank", "unionb", "paytm", "phonepe", "iob")
        val senderBankCode = bankCodes.firstOrNull { senderAddress.contains(it, ignoreCase = true) }
        if (senderBankCode != null) {
            bankName = senderBankCode.uppercase()
        } else {
            val bankPattern = Pattern.compile("(?i)\\b(hdfc|icici|sbi|axis|kotak|pnb|bob|hsbc|citi|canara|yesbank|unionb|paytm|phonepe|iob)\\b")
            val bankMatcher = bankPattern.matcher(smsBody)
            if (bankMatcher.find()) {
                bankName = bankMatcher.group(1)?.uppercase() ?: "Unknown Bank"
            }
        }

        // 4. Extract Masked Account Number
        var accountNumber = "XXXX"
        val accPattern = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?\\s*)?x*(\\d{4})")
        val accMatcher = accPattern.matcher(smsBody)
        if (accMatcher.find()) {
            accountNumber = "XX" + accMatcher.group(1)
        } else {
            val altAccPattern = Pattern.compile("(?i)\\b\\*\\*(\\d{4})\\b")
            val altAccMatcher = altAccPattern.matcher(smsBody)
            if (altAccMatcher.find()) {
                accountNumber = "XX" + altAccMatcher.group(1)
            }
        }

        // 5. Extract UPI / Transaction Reference Number
        var refNo = ""
        // Optional "No"/"No." filler (BOB: "UPI Ref No 212942376732", no colon at all)
        val refPattern = Pattern.compile("(?i)(?:upi ref|txn|ref|rrn|transaction id|id)\\s*(?:no\\.?)?\\s*:?\\s*(\\d{8,16})")
        val refMatcher = refPattern.matcher(smsBody)
        if (refMatcher.find()) {
            refNo = refMatcher.group(1) ?: ""
        }

        // 6. Extract Merchant Name & Categorize
        var merchant = "Merchant"
        var category = "Others"

        val merchantCategories = listOf(
            MerchantCategoryRule("netflix", "Netflix", "OTT"),
            MerchantCategoryRule("amazon prime", "Amazon Prime", "OTT"),
            MerchantCategoryRule("prime video", "Amazon Prime", "OTT"),
            MerchantCategoryRule("hotstar", "Disney+ Hotstar", "OTT"),
            MerchantCategoryRule("spotify", "Spotify", "Music"),
            MerchantCategoryRule("youtube", "YouTube Premium", "Music"),
            MerchantCategoryRule("jio", "Reliance Jio", "Mobile"),
            MerchantCategoryRule("airtel", "Airtel", "Mobile"),
            MerchantCategoryRule("vodafone", "Vi (Vodafone Idea)", "Mobile"),
            MerchantCategoryRule("idea", "Vi (Vodafone Idea)", "Mobile"),
            MerchantCategoryRule("emi", "Loan EMI", "EMI"),
            MerchantCategoryRule("loan", "Loan EMI", "EMI"),
            MerchantCategoryRule("icici loan", "ICICI Loan", "EMI"),
            MerchantCategoryRule("hdfc cc", "HDFC Credit Card", "Credit Card"),
            MerchantCategoryRule("credit card", "Credit Card Payment", "Credit Card"),
            MerchantCategoryRule("electricity", "Electricity Bill", "Electricity"),
            MerchantCategoryRule("bescom", "Electricity Bill (BESCOM)", "Electricity"),
            MerchantCategoryRule("torrent power", "Electricity Bill (Torrent)", "Electricity"),
            MerchantCategoryRule("gas", "Gas Utility Bill", "Gas"),
            MerchantCategoryRule("indane", "Indane Gas", "Gas"),
            MerchantCategoryRule("hp gas", "HP Gas", "Gas"),
            MerchantCategoryRule("water", "Water Utility Bill", "Water"),
            MerchantCategoryRule("insurance", "Insurance Premium", "Insurance"),
            MerchantCategoryRule("lic", "LIC Insurance", "Insurance")
        )

        for (rule in merchantCategories) {
            if (lowercaseBody.contains(rule.keyword)) {
                merchant = rule.displayName
                category = rule.categoryName
                break
            }
        }

        if (merchant == "Merchant" && (isMandateCreated || isMandateRevoked)) {
            // NPCI e-mandate template: "...created/revoked a mandate on <merchant> for a
            // frequency of...". Bounded to "for a frequency" so it doesn't spill into that
            // trailing clause the way the generic to/at/for fallback below does.
            val mandateMerchantPattern = Pattern.compile("(?i)mandate\\s+on\\s+(.+?)\\s+for\\s+a\\s+frequency")
            val mandateMerchantMatcher = mandateMerchantPattern.matcher(smsBody)
            if (mandateMerchantMatcher.find()) {
                val candidate = mandateMerchantMatcher.group(1)?.trim() ?: ""
                if (candidate.isNotEmpty()) merchant = candidate
            } else {
                // Paytm template: "Automatic payment of Rs.X for <merchant> has been setup
                // successfully". Bounded to "has been setup successfully" for the same reason.
                val paytmMerchantPattern = Pattern.compile("(?i)for\\s+(.+?)\\s+has\\s+been\\s+setup\\s+successfully")
                val paytmMerchantMatcher = paytmMerchantPattern.matcher(smsBody)
                if (paytmMerchantMatcher.find()) {
                    val candidate = paytmMerchantMatcher.group(1)?.trim() ?: ""
                    if (candidate.isNotEmpty()) merchant = candidate
                }
            }
        }

        if (merchant == "Merchant") {
            // "Cr. to <vpa>." (BOB Dr./Cr. template) - the recipient is a UPI VPA
            // (name@handle), which contains '.'/'@' that the generic word-based fallback
            // below can't capture (it stops at the first '.' and truncates the VPA). Bounded
            // by the sentence-ending ". " so a VPA's own internal '.' (no trailing space)
            // doesn't end the match early.
            val vpaMerchantPattern = Pattern.compile("(?i)cr\\.?\\s+to\\s+(\\S+?)\\.\\s")
            val vpaMerchantMatcher = vpaMerchantPattern.matcher(smsBody)
            if (vpaMerchantMatcher.find()) {
                val candidate = vpaMerchantMatcher.group(1)?.trim() ?: ""
                if (candidate.isNotEmpty()) merchant = candidate
            }
        }

        if (merchant == "Merchant") {
            // "<Name> paid you"/"<Name> sent you" - captures the real payer's name for this
            // direction-reversed UPI wording (confirmed via the sibling SoundBox parser).
            val paidYouPattern = Pattern.compile("(?i)(.+?)\\s+(?:paid|sent)\\s+you\\s+(?:₹|rs\\.?|inr)")
            val paidYouMatcher = paidYouPattern.matcher(smsBody)
            if (paidYouMatcher.find()) {
                val candidate = paidYouMatcher.group(1)?.trim() ?: ""
                if (candidate.isNotEmpty()) merchant = candidate
            }
        }

        if (merchant == "Merchant") {
            val merchantPattern = Pattern.compile("(?i)(?:to|at|for)\\s+([a-zA-Z0-9]+(?:\\s+[a-zA-Z0-9]+){0,2})")
            val merchantMatcher = merchantPattern.matcher(smsBody)
            if (merchantMatcher.find()) {
                val candidate = merchantMatcher.group(1)?.trim() ?: ""
                val lowerCandidate = candidate.lowercase()
                if (candidate.isNotEmpty() && 
                    !lowerCandidate.contains("debited") && 
                    !lowerCandidate.contains("credited") && 
                    !lowerCandidate.contains("account") &&
                    !lowerCandidate.contains("bank") &&
                    !lowerCandidate.contains("card") &&
                    !lowerCandidate.contains("rs") &&
                    !lowerCandidate.contains("inr")) {
                    merchant = candidate
                }
            }
        }

        val txnType = if (isDebit) "DEBIT" else if (isCredit) "CREDIT" else "DEBIT"

        if (merchant == "Merchant" && txnType == "CREDIT") {
            // Some bank credit templates (e.g. BOB: "Your account is credited with INR 60.00
            // ... by UPI Ref No ...") never name the sender at all - only a reference number.
            // "Merchant" is actively misleading there (it implies a payee), so say what's
            // actually known instead of a wrong-sounding guess.
            merchant = "UPI Credit"
        }
        val shouldCreateMandate = isMandateCreated || isMandateRevoked
        val transaction = Transaction(
            smsId = smsId,
            merchant = merchant,
            amount = amount,
            date = smsDate,
            bankName = bankName,
            accountNumber = accountNumber,
            referenceNumber = refNo,
            transactionType = txnType,
            category = category,
            smsBody = smsBody,
            isAutoPay = shouldCreateMandate
        )

        var mandate: Mandate? = null
        if (shouldCreateMandate) {
            val freq = if (lowercaseBody.contains("monthly")) "MONTHLY"
                       else if (lowercaseBody.contains("weekly")) "WEEKLY"
                       else if (lowercaseBody.contains("yearly")) "YEARLY"
                       else "MONTHLY"
            val periodDays = when (freq) {
                "WEEKLY" -> 7L
                "YEARLY" -> 365L
                else -> 30L
            }

            mandate = Mandate(
                merchant = merchant,
                amount = amount,
                frequency = freq,
                nextExpectedDebit = smsDate + (periodDays * 24L * 60L * 60L * 1000L),
                bank = bankName,
                status = if (isMandateRevoked) "CANCELLED" else "ACTIVE",
                referenceNumber = refNo
            )
        }

        return ParsedResult(transaction, mandate)
    }

    private data class MerchantCategoryRule(
        val keyword: String,
        val displayName: String,
        val categoryName: String
    )
}

data class ParsedResult(
    val transaction: Transaction,
    val mandate: Mandate?
)

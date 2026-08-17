package com.uj.appstorysautopaymanager.util

import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import java.util.regex.Pattern

object SmsParser {
    
    fun parseSms(smsBody: String, smsDate: Long, smsId: String): ParsedResult? {
        val lowercaseBody = smsBody.lowercase()
        
        // 1. Detect if it's a financial transaction (credit/debit/autopay/mandate)
        val isDebit = lowercaseBody.contains("debited") || 
                      lowercaseBody.contains("deducted") || 
                      lowercaseBody.contains("spent") || 
                      lowercaseBody.contains("paid") || 
                      lowercaseBody.contains("withdrawn") ||
                      lowercaseBody.contains("debit") ||
                      lowercaseBody.contains("payment to") ||
                      lowercaseBody.contains("auto debit") ||
                      lowercaseBody.contains("mandate executed")
                      
        val isCredit = lowercaseBody.contains("credited") || 
                       lowercaseBody.contains("received") || 
                       lowercaseBody.contains("deposited") || 
                       lowercaseBody.contains("added") || 
                       lowercaseBody.contains("refunded") ||
                       lowercaseBody.contains("credit")
                       
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
                        
        if (!isDebit && !isCredit && !isMandate) {
            return null // Not a financial transaction
        }

        // 2. Extract Amount
        var amount = 0.0
        val amountPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹|rs)\\s*([\\d,]+(?:\\.\\d{1,2})?)")
        val amountMatcher = amountPattern.matcher(smsBody)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull() ?: 0.0
        }
        
        if (amount == 0.0) {
            // Backup search for raw number after Rs/INR
            val backupAmountPattern = Pattern.compile("(?i)(?:debited|credited|deducted|amount of)\\s+([\\d,]+(?:\\.\\d{1,2})?)")
            val backupMatcher = backupAmountPattern.matcher(smsBody)
            if (backupMatcher.find()) {
                val amountStr = backupMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull() ?: 0.0
            }
        }

        // 3. Extract Bank Name
        var bankName = "Unknown Bank"
        val bankPattern = Pattern.compile("(?i)\\b(hdfc|icici|sbi|axis|kotak|pnb|bob|hsbc|citi|canara|yesbank|unionb|paytm|phonepe)\\b")
        val bankMatcher = bankPattern.matcher(smsBody)
        if (bankMatcher.find()) {
            bankName = bankMatcher.group(1)?.uppercase() ?: "Unknown Bank"
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
        val refPattern = Pattern.compile("(?i)(?:upi ref|txn|ref|rrn|transaction id|id)\\s*:?\\s*(\\d{8,16})")
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

        val txnType = if (isCredit) "CREDIT" else "DEBIT"
        val shouldCreateMandate = isMandate || category != "Others"
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
            
            mandate = Mandate(
                merchant = merchant,
                amount = amount,
                frequency = freq,
                nextExpectedDebit = smsDate + (30L * 24L * 60L * 60L * 1000L),
                bank = bankName,
                status = "ACTIVE",
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

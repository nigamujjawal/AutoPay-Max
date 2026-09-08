package com.autopaymax.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

// Same one-param-one-JSON-blob approach as SmsPatternConfig, for UpiNotificationParser's regex
// patterns. Deliberately excludes TRACKED_PACKAGES/GENERIC_TRACKED_PACKAGES (which app packages
// are trusted at all, including the WhatsApp exclusion from the anti-spoofing fix - see
// sms_upi_parsing_pipeline memory) - that's a security invariant, not a template-wording fix.
object UpiPatternConfig {

    data class TxnPattern(val regex: Pattern, val amountGroup: Int, val senderGroup: Int = -1)

    private const val RC_KEY = "upi_regex_patterns"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(mapOf(RC_KEY to DEFAULTS_JSON))
        }
    }

    // Fire-and-forget: call once from AutoPayApplication.onCreate() alongside
    // SmsPatternConfig.refresh(). Until the first fetch completes, every accessor below keeps
    // returning the in-code defaults - parsing never blocks on network.
    fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    val mandateIdPattern get() = pattern("mandate_id")
    val paytmMandateCreatedPattern get() = pattern("paytm_mandate_created")
    val paytmMandateRevokedPattern get() = pattern("paytm_mandate_revoked")
    val paytmCreditWithNamePattern get() = pattern("paytm_credit_with_name")
    val paytmDebitWithNamePattern get() = pattern("paytm_debit_with_name")
    val paytmBankAccountPattern get() = pattern("paytm_bank_account")
    val paytmDailyTransferPattern get() = pattern("paytm_daily_transfer")

    val gpayCreditPatterns get() = patternList("gpay_credit")
    val phonepeCreditPatterns get() = patternList("phonepe_credit")
    val bhimCreditPatterns get() = patternList("bhim_credit")
    val genericCreditPatterns get() = patternList("generic_credit")
    val debitGuessPatterns get() = patternList("debit_guess")

    private fun blob(): JSONObject = JSONObject(remoteConfig.getString(RC_KEY))

    // Same fallback contract as SmsPatternConfig.pattern(): blob missing/malformed, key missing,
    // or a bad regex all fall back to this key's hardcoded default rather than crashing parsing.
    private fun pattern(key: String): Pattern {
        val raw = runCatching { blob().getString(key) }.getOrNull() ?: DEFAULT_STRINGS.getValue(key)
        return runCatching { Pattern.compile(raw) }
            .getOrElse { Pattern.compile(DEFAULT_STRINGS.getValue(key)) }
    }

    // A malformed list falls back to the whole default list for that key, not a partial one -
    // these lists are tried in order as a fallback chain, so a partially-broken list (e.g. one
    // bad entry silently dropped) would silently change matching priority in a way that's harder
    // to notice than "the whole list reverted to default."
    private fun patternList(key: String): List<TxnPattern> {
        val fromRemote = runCatching {
            val arr = blob().getJSONArray(key)
            (0 until arr.length()).map { i ->
                val entry = arr.getJSONObject(i)
                TxnPattern(
                    regex = Pattern.compile(entry.getString("regex")),
                    amountGroup = entry.getInt("amountGroup"),
                    senderGroup = entry.optInt("senderGroup", -1)
                )
            }
        }.getOrNull()
        return if (fromRemote.isNullOrEmpty()) DEFAULT_LISTS.getValue(key) else fromRemote
    }

    // Paytm - mandate templates given directly by the user; transfer templates confirmed
    // on-device (paytm_credit_with_name). See UpiNotificationParser's original comments (git
    // history) for provenance of each.
    private val DEFAULT_STRINGS: Map<String, String> = mapOf(
        "mandate_id" to "(?i)mandate id:\\s*([A-Za-z0-9]+)",
        "paytm_mandate_created" to "(?i)autopay mandate for (.+?) has been successfully created\\.?\\s*amount:\\s*up to inr\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*per\\s*(\\w+)",
        "paytm_mandate_revoked" to "(?i)autopay mandate for (.+?)\\s*\\(mandate id:[^)]*\\)\\s*has been successfully (?:revoked|cancelled)",
        "paytm_credit_with_name" to "(?i)received\\s*(?:₹|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+from\\s+(.*?)(?:\\s+(?:deposited|credited|sent|received|in|on|at|via|to)\\b|$)",
        "paytm_debit_with_name" to "(?i)(?:paid|sent)\\s*(?:₹|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+to\\s+(.*?)(?:\\s+(?:debited|paid|sent|deducted|from|on|at|via)\\b|$)",
        "paytm_bank_account" to "(?i)(?:deposited in|debited from) your (.+?)\\s*-\\s*(\\d{3,6})",
        "paytm_daily_transfer" to "(?i)inr\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*has been (debited|credited)\\s*(?:from|to)\\s*your paytm wallet"
    )

    // Building blocks for the lists below - kept as compile-time constants only to assemble
    // DEFAULT_LISTS, same currency/amount shape SoundBox's own parser uses.
    private const val CURRENCY_PREFIX = """(?:₹|Rs\.?|INR|Rupees)\s*"""
    private const val AMOUNT_PATTERN = """([0-9,]+(?:\.[0-9]{1,2})?)"""

    // Credit direction adopted from SoundBox's real GPAY/PHONEPE/BHIM/GENERIC_PATTERNS (see
    // soundbox_sibling_app_reference memory). debit_guess is still an unverified symmetry guess -
    // SoundBox never needed that direction.
    private val DEFAULT_LISTS: Map<String, List<TxnPattern>> = mapOf(
        "gpay_credit" to listOf(
            TxnPattern(Pattern.compile("""(?i)(.+?)\s+paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
            TxnPattern(Pattern.compile("""(?i)(.+?)\s+sent\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
            TxnPattern(Pattern.compile("""(?i)paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+payment\s+received"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)You\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
            TxnPattern(Pattern.compile("""(?i)Payment\s+of\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2)
        ),
        "phonepe_credit" to listOf(
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
        ),
        "bhim_credit" to listOf(
            TxnPattern(Pattern.compile("""(?i)Transaction\s+successful\.\s*$CURRENCY_PREFIX$AMOUNT_PATTERN\s+credited"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)You\s+have\s+received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2)
        ),
        "generic_credit" to listOf(
            TxnPattern(Pattern.compile("""(?i)(.+?)\s+paid\s+you\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
            TxnPattern(Pattern.compile("""(?i)\+\s*$CURRENCY_PREFIX$AMOUNT_PATTERN\s+UPI\s+transfer\s+from\s+(.+)"""), amountGroup = 1, senderGroup = 2),
            TxnPattern(Pattern.compile("""(?i)received\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)credited\s+with\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+credited"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+received"""), amountGroup = 1),
            TxnPattern(Pattern.compile("""(?i)(.+?)\s+has\s+sent\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1)
        ),
        "debit_guess" to listOf(
            TxnPattern(Pattern.compile("""(?i)you\s+(?:paid|sent)\s+(.+?)\s+$CURRENCY_PREFIX$AMOUNT_PATTERN"""), amountGroup = 2, senderGroup = 1),
            TxnPattern(Pattern.compile("""(?i)$CURRENCY_PREFIX$AMOUNT_PATTERN\s+(?:paid|sent)\s+to\s+(.+)"""), amountGroup = 1, senderGroup = 2)
        )
    )

    // Built from DEFAULT_STRINGS/DEFAULT_LISTS rather than hand-duplicated as a JSON literal, so
    // there's one source of truth - see SmsPatternConfig for the equivalent note. `by lazy`
    // (rather than a plain val, like SmsPatternConfig uses) because this one's assembly loop
    // needs DEFAULT_LISTS' Pattern objects already compiled, and lazy sidesteps any Kotlin
    // object-init-order footgun regardless of where this property sits in the file.
    private val DEFAULTS_JSON: String by lazy {
        val root = JSONObject(DEFAULT_STRINGS)
        DEFAULT_LISTS.forEach { (key, list) ->
            val arr = JSONArray()
            list.forEach { tp ->
                val obj = JSONObject()
                obj.put("regex", tp.regex.pattern())
                obj.put("amountGroup", tp.amountGroup)
                obj.put("senderGroup", tp.senderGroup)
                arr.put(obj)
            }
            root.put(key, arr)
        }
        root.toString()
    }
}

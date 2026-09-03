package com.uj.appstorysautopaymanager.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject
import java.util.regex.Pattern

// A single Firebase Remote Config parameter (one JSON blob, RC_KEY) instead of one key per
// regex - lets a bank/UPI-app SMS template change be fixed by editing one console value instead
// of a Play Store release. Deliberately excludes SmsParser's PLAIN_PHONE_NUMBER_SENDER anti-spoof
// check - that's a security invariant, not a template-wording fix, and has no business being
// remotely editable.
object SmsPatternConfig {

    private const val RC_KEY = "sms_regex_patterns"

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

    // Fire-and-forget: call once from AutoPayApplication.onCreate(). Until the first fetch
    // completes (or on any failure/offline cold start), pattern() below keeps returning the
    // defaults parsed from DEFAULTS_JSON - parsing never blocks on network.
    fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    val plusCreditPattern get() = pattern("plus_credit")
    val keywordAmountPattern get() = pattern("keyword_amount")
    val genericAmountPattern get() = pattern("generic_amount")
    val bankPattern get() = pattern("bank")
    val accountPattern get() = pattern("account")
    val accountAltPattern get() = pattern("account_alt")
    val refPattern get() = pattern("ref")
    val mandateMerchantPattern get() = pattern("mandate_merchant")
    val paytmMerchantPattern get() = pattern("paytm_merchant")
    val vpaMerchantPattern get() = pattern("vpa_merchant")
    val paidYouMerchantPattern get() = pattern("paid_you_merchant")
    val genericMerchantPattern get() = pattern("generic_merchant")

    // Re-parsed and recompiled on every call rather than cached - SmsParser only calls these a
    // handful of times per incoming SMS, so this isn't a hot path, and it means a
    // fetchAndActivate() that completes mid-session is picked up on the very next SMS with no
    // extra wiring. Any failure along the way (blob missing/malformed, key missing inside it, bad
    // regex) falls back to this same key's hardcoded default rather than crashing parsing.
    private fun pattern(key: String): Pattern {
        val raw = runCatching { JSONObject(remoteConfig.getString(RC_KEY)).getString(key) }
            .getOrNull() ?: DEFAULTS.getValue(key)
        return runCatching { Pattern.compile(raw) }
            .getOrElse { Pattern.compile(DEFAULTS.getValue(key)) }
    }

    private val DEFAULTS: Map<String, String> = mapOf(
        "plus_credit" to "(?i)\\+\\s*(?:₹|rs\\.?|inr)",
        "keyword_amount" to "(?i)(?:debited|credited|received|rcvd|deducted|amount(?:\\s+of)?)\\s+(?:with\\s+)?(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        "generic_amount" to "(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        "bank" to "(?i)\\b(hdfc|icici|sbi|axis|kotak|pnb|bob|hsbc|citi|canara|yesbank|unionb|paytm|phonepe|iob)\\b",
        "account" to "(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?\\s*)?x*(\\d{4})",
        "account_alt" to "(?i)\\b\\*\\*(\\d{4})\\b",
        "ref" to "(?i)(?:upi ref|txn|ref|rrn|transaction id|id)\\s*(?:no\\.?)?\\s*:?\\s*(\\d{8,16})",
        "mandate_merchant" to "(?i)mandate\\s+on\\s+(.+?)\\s+for\\s+a\\s+frequency",
        "paytm_merchant" to "(?i)for\\s+(.+?)\\s+has\\s+been\\s+setup\\s+successfully",
        "vpa_merchant" to "(?i)cr\\.?\\s+to\\s+(\\S+?)\\.\\s",
        "paid_you_merchant" to "(?i)(.+?)\\s+(?:paid|sent)\\s+you\\s+(?:₹|rs\\.?|inr)",
        "generic_merchant" to "(?i)(?:to|at|for)\\s+([a-zA-Z0-9]+(?:\\s+[a-zA-Z0-9]+){0,2})"
    )

    // Built from DEFAULTS rather than hand-duplicated as a JSON string literal, so there's one
    // source of truth for the default values and console-side JSON stays byte-for-byte in sync
    // with what's typed above - copy this string (Log.d it once, or read it via
    // FirebaseRemoteConfig's own default) as the starting point when creating sms_regex_patterns
    // in the console.
    private val DEFAULTS_JSON: String = JSONObject(DEFAULTS).toString()
}

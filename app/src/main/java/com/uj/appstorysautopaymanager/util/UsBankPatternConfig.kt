package com.uj.appstorysautopaymanager.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject
import java.util.regex.Pattern

// Same one-param-one-JSON-blob approach as SmsPatternConfig/UpiPatternConfig, for
// UsBankNotificationParser's regex patterns. Starts with EMPTY pattern lists on purpose - per
// this project's own real_capture_before_writing_regex rule, no guessed regex content for
// Chase/BofA/Wells Fargo/Capital One/Venmo/Cash App notification wording exists yet (unlike the
// UPI patterns, which came from a real-world-validated sibling app - see
// soundbox_sibling_app_reference). The point of wiring this up now rather than later: once real
// captured notification text is available, patterns get published via the Firebase console (key
// below) with zero app release needed - see UsBankNotificationParser for why an empty/missing
// list is a silent no-op, never a false match.
object UsBankPatternConfig {

    private const val RC_KEY = "us_bank_regex_patterns"

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
    // SmsPatternConfig/UpiPatternConfig.refresh().
    fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    // One credit/debit pair per tracked source - see UsBankNotificationParser.BANK_PACKAGES for
    // which package maps to which bankKey. e.g. creditPatterns("chase") reads the "chase_credit"
    // array from the published blob.
    fun creditPatterns(bankKey: String): List<UpiPatternConfig.TxnPattern> = patternList("${bankKey}_credit")
    fun debitPatterns(bankKey: String): List<UpiPatternConfig.TxnPattern> = patternList("${bankKey}_debit")

    private fun blob(): JSONObject = JSONObject(remoteConfig.getString(RC_KEY))

    // No DEFAULT fallback map here (unlike Sms/UpiPatternConfig) - there's nothing verified to
    // fall back to. A missing/malformed key just means "nothing published yet for this source" -
    // an empty list, not a crash, and matchPatterns() finding nothing is indistinguishable from
    // "this notification wasn't a transaction," which is the safe failure mode.
    private fun patternList(key: String): List<UpiPatternConfig.TxnPattern> {
        return runCatching {
            val arr = blob().getJSONArray(key)
            (0 until arr.length()).map { i ->
                val entry = arr.getJSONObject(i)
                UpiPatternConfig.TxnPattern(
                    regex = Pattern.compile(entry.getString("regex")),
                    amountGroup = entry.getInt("amountGroup"),
                    senderGroup = entry.optInt("senderGroup", -1)
                )
            }
        }.getOrElse { emptyList() }
    }

    // Empty on purpose - see class doc. Keys ready to add once real captures exist:
    // chase_credit/chase_debit, bofa_credit/bofa_debit, wells_fargo_credit/wells_fargo_debit,
    // capital_one_credit/capital_one_debit, venmo_credit/venmo_debit,
    // cash_app_credit/cash_app_debit - same {"regex", "amountGroup", "senderGroup"} shape as
    // upi_regex_patterns' array entries.
    private val DEFAULTS_JSON: String = JSONObject().toString()
}

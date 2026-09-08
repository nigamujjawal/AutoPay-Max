package com.autopaymax.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import org.json.JSONObject
import java.util.regex.Pattern

// Same one-param-one-JSON-blob Remote Config approach as SmsPatternConfig / UpiPatternConfig /
// UsBankPatternConfig, for EmailParser's regex patterns. RC key `email_regex_patterns`, shape:
//   { "<vendorKey>_mandate": [ {regex, amountGroup, senderGroup}, ... ],
//     "<vendorKey>_debit":   [ ... ], ... }
//
// DEFAULT_LISTS is the source of truth: Google Play patterns are REAL (from captured Play
// receipts, 2026-09 - Claude Pro renewal + Feenko trial). Remote Config is a pure override -
// a key is only taken from RC when it has actually been fetched from the server (source ==
// REMOTE); an unpublished key silently uses the baked default with no Firebase log noise.
object EmailPatternConfig {

    private const val RC_KEY = "email_regex_patterns"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
        }
    }

    // Fire-and-forget: call once from AutoPayApplication.onCreate() alongside the other refreshes.
    fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    // Tried first by EmailParser - a match promotes the email to a Mandate (autopay setup/renewal).
    fun mandatePatterns(vendorKey: String): List<UpiPatternConfig.TxnPattern> = patternList("${vendorKey}_mandate")

    // Tried second - a recurring charge / receipt with no explicit mandate wording.
    fun debitPatterns(vendorKey: String): List<UpiPatternConfig.TxnPattern> = patternList("${vendorKey}_debit")

    // Tried FIRST by EmailParser - a "your <app> subscription will be cancelled" email. Only the
    // senderGroup (app name) matters; no amount.
    fun cancelPatterns(vendorKey: String): List<UpiPatternConfig.TxnPattern> = patternList("${vendorKey}_cancel")

    private fun patternList(key: String): List<UpiPatternConfig.TxnPattern> {
        val fromRemote = publishedBlob()?.let { blob ->
            runCatching {
                val arr = blob.getJSONArray(key)
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    UpiPatternConfig.TxnPattern(
                        regex = Pattern.compile(e.getString("regex")),
                        amountGroup = e.getInt("amountGroup"),
                        senderGroup = e.optInt("senderGroup", -1)
                    )
                }
            }.getOrNull()
        }
        return if (fromRemote.isNullOrEmpty()) DEFAULT_LISTS[key].orEmpty() else fromRemote
    }

    // Only the server-fetched value, never the "static / no value" case - so no
    // "No value of type 'String' exists" warning spam before/without a publish.
    private fun publishedBlob(): JSONObject? {
        val value: FirebaseRemoteConfigValue = runCatching { remoteConfig.getValue(RC_KEY) }.getOrNull() ?: return null
        if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) return null
        return runCatching { JSONObject(value.asString()) }.getOrNull()
    }

    // Real captured Google Play receipt wording. amountGroup is the RECURRING price (goes on the
    // Mandate); EmailParser reads the actual billed amount from the "Total:" line separately.
    // senderGroup is the merchant/company name. `internal` so EmailParserTest can assert these
    // exact regexes against real captured email text without going through Remote Config.
    internal val DEFAULT_LISTS: Map<String, List<UpiPatternConfig.TxnPattern>> = mapOf(
        "google_play_mandate" to listOf(
            // Item line: "Claude Pro Subscription (Claude by Anthropic)   ₹1,999.00/month"
            UpiPatternConfig.TxnPattern(
                Pattern.compile("(?i)Item\\s+Price\\s+(.+?)\\s+(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{2})?)\\s*/\\s*month"),
                amountGroup = 2, senderGroup = 1
            ),
            // Renewal prose fallback: "Your subscription from Anthropic, PBC on Google Play has renewed" + "Total: ₹1,999.00"
            UpiPatternConfig.TxnPattern(
                Pattern.compile("(?is)subscription from (.+?) on Google Play has renewed.*?Total:\\s*(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{2})?)"),
                amountGroup = 2, senderGroup = 1
            ),
            // Free-trial sign-up: "signed up for a trial subscription from GOTYOURBACK ... charged the subscription cost (currently ₹4,550.00/6 months)"
            UpiPatternConfig.TxnPattern(
                Pattern.compile("(?is)signed up for a trial subscription from (.+?) on Google Play.*?charged the subscription cost \\(currently\\s*(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{2})?)"),
                amountGroup = 2, senderGroup = 1
            )
        ),
        // "Your <App Name> subscription will be / has been cancelled" (Google Play cancellation
        // email). App name (group 1) -> AutoPayRepository cancels the matching ACTIVE mandate.
        // ponytail: subject/prose only, not verified against a full captured body yet.
        "google_play_cancel" to listOf(
            UpiPatternConfig.TxnPattern(
                Pattern.compile("(?i)Your (.+?) subscription (?:will be|has been|is|was) cancell?ed"),
                amountGroup = 0, senderGroup = 1
            )
        )
    )
}

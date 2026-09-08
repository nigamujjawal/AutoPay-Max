package com.uj.appstorysautopaymanager.worker

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.remote.GmailApi
import com.uj.appstorysautopaymanager.data.remote.GmailTokenHolder
import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessagePart
import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessageRef
import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessagePayload
import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessageResponse
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.util.EmailParser
import com.uj.appstorysautopaymanager.util.GmailAuthManager
import com.uj.appstorysautopaymanager.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

// Autopay's data source now: reads mandate-confirmation + recurring-charge emails from Gmail,
// parses them (EmailParser) and routes them through the same AutoPayRepository choke points the
// SMS/notification sources used. Self-gates on isGmailConnected - always scheduled, same
// convention as MandateReminderWorker. Not @HiltWorker (no custom WorkerFactory in this project).
class GmailSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GmailSyncWorkerEntryPoint {
        fun repository(): AutoPayRepository
        fun preferenceManager(): PreferenceManager
        fun gmailApi(): GmailApi
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(applicationContext, GmailSyncWorkerEntryPoint::class.java)
        val repository = ep.repository()
        val pref = ep.preferenceManager()
        val gmailApi = ep.gmailApi()

        if (!pref.isGmailConnectedFlow.first()) return Result.success()

        when (val auth = GmailAuthManager.trySilentAuthorize(applicationContext)) {
            GmailAuthManager.AuthResult.ReauthRequired,
            is GmailAuthManager.AuthResult.NeedsResolution -> {
                pref.setGmailReauthNeeded(true)
                return Result.success()
            }
            is GmailAuthManager.AuthResult.Failed -> {
                Log.w("GmailSync", "silent authorize failed: ${auth.error}")
                return Result.retry()
            }
            is GmailAuthManager.AuthResult.Granted -> {
                GmailTokenHolder.accessToken = auth.accessToken
                pref.setGmailReauthNeeded(false)
            }
        }

        // A manual "Gmail Sync" tap forces a deep (180-day) re-scan even if a prior backfill
        // already completed - patterns get added over time (e.g. a new vendor), and a completed
        // backfill would otherwise permanently hide history it couldn't parse the first time.
        val forceDeep = inputData.getBoolean(KEY_DEEP, false)
        val isBackfill = forceDeep || !pref.isGmailBackfillDoneFlow.first()
        val window = if (isBackfill) "newer_than:180d" else "newer_than:2d"
        // (clauseA OR clauseB OR ...) window  - outer parens so the date window AND-s the whole
        // OR group (Gmail binds implicit-AND tighter than OR).
        val query = "(" + EmailParser.gmailQueryClauses().joinToString(" OR ") + ") $window"

        val currencySymbol = pref.currencyFlow.first()
        // `announce` = show a system notification for freshly-detected activity (incremental
        // runs only, not the 180-day backfill). TTS on detection is DEWIRED - see processMessage.
        val announce = !isBackfill

        try {
            // Gmail returns newest-first with no orderBy option - collect every page's refs then
            // process OLDEST-first, so a sign-up email is applied before its later cancellation
            // (same reason the old SMS-inbox backfill scanned DATE ASC).
            val refs = ArrayList<GmailMessageRef>()
            var pageToken: String? = null
            do {
                val page = gmailApi.listMessages(query, pageToken)
                refs.addAll(page.messages.orEmpty())
                pageToken = page.nextPageToken
            } while (pageToken != null)

            var matched = 0
            for (ref in refs.asReversed()) {
                if (processMessage(ref, gmailApi, repository, pref, announce, currencySymbol)) matched++
            }
            Log.d(
                "GmailSync",
                "done: deep=$isBackfill seen=${refs.size} matched=$matched " +
                    "dbMandates=${repository.allMandates.first().size} dbTxns=${repository.allTransactions.first().size}"
            )
        } catch (e: Exception) {
            Log.e("GmailSync", "sync failed", e)
            return Result.retry()
        } finally {
            GmailTokenHolder.accessToken = null
        }

        if (isBackfill) pref.setGmailBackfillDone(true)
        return Result.success()
    }

    // Fetches, parses and applies one message. Returns true if it produced a mandate/transaction.
    private suspend fun processMessage(
        ref: GmailMessageRef,
        gmailApi: GmailApi,
        repository: AutoPayRepository,
        pref: PreferenceManager,
        announce: Boolean,
        currencySymbol: String
    ): Boolean {
        val id = "email_${ref.id}"
        if (repository.exists(id)) return false

        val msg = runCatching { gmailApi.getMessage(ref.id) }.getOrNull() ?: return false
        val from = header(msg, "From") ?: return false
        val vendorKey = EmailParser.vendorKeyFor(from) ?: return false
        val subject = header(msg, "Subject").orEmpty()
        val body = extractPlainText(msg.payload)
        val receivedAt = msg.internalDate?.toLongOrNull() ?: System.currentTimeMillis()

        val result = EmailParser.parse(vendorKey, subject, body, receivedAt, ref.id)
        if (result == null) {
            if (subject.contains("receipt", ignoreCase = true) || subject.contains("cancel", ignoreCase = true)) {
                Log.d("GmailSync", "RECEIPT NO MATCH vendor=$vendorKey \"$subject\" bodyChars=${body.length} body=${body.take(700)}")
            } else {
                Log.v("GmailSync", "no match: vendor=$vendorKey subject=\"$subject\" bodyChars=${body.length}")
            }
            return false
        }

        Log.d("GmailSync", "MATCH vendor=$vendorKey \"$subject\" mandate=${result.mandate?.merchant}@${result.mandate?.amount}/${result.mandate?.status} charge=${result.transaction.amount}")

        result.mandate?.let {
            val isNew = repository.applyMandateEvent(it)
            Log.d("GmailSync", "applyMandateEvent(${it.merchant} @${it.amount} ${it.status}) new=$isNew")
        }
        if (result.transaction.amount > 0.0) {
            val txnId = repository.applyTransactionEvent(result.transaction)
            Log.d("GmailSync", "applyTransactionEvent(${result.transaction.merchant} @${result.transaction.amount}) id=$txnId")
        }

        // Backfill populates history silently; the incremental pass shows a system notification.
        // DEWIRED: detection no longer triggers TTS - the app only speaks on mandate payment
        // reminders (MandateReminderWorker) and manual mandate setup (MandateViewModel.addMandate).
        // To re-enable: rebuild a TextToSpeechHelper here and call speak(AUTOPAY_SET/DEBIT_PAID, ...).
        if (announce) {
            val merchant = result.transaction.merchant
            if (result.mandate?.status == "CANCELLED") {
                NotificationHelper.notify(
                    context = applicationContext, repository = repository, preferenceManager = pref,
                    title = "AutoPay Cancelled", body = "$merchant autopay was cancelled", category = "Payments"
                )
            } else {
                val amount = (result.mandate?.amount ?: result.transaction.amount).toInt()
                NotificationHelper.notify(
                    context = applicationContext, repository = repository, preferenceManager = pref,
                    title = if (result.mandate != null) "AutoPay Detected" else "Subscription Charge",
                    body = "$currencySymbol$amount - $merchant", category = "Payments"
                )
            }
        }
        return true
    }

    private fun header(msg: GmailMessageResponse, name: String): String? =
        msg.payload?.headers?.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value

    // Prefer text/plain; fall back to a tag-stripped text/html (Google's transactional emails are
    // often HTML-only). ponytail: first matching part by DFS, crude HTML strip - enough for the
    // prose + "Item / Total: ₹X" lines the patterns key off.
    private fun extractPlainText(payload: GmailMessagePayload?): String {
        payload ?: return ""
        findPart(payload, "text/plain")?.let { return it }
        findPart(payload, "text/html")?.let { return stripHtml(it) }
        return ""
    }

    private fun findPart(payload: GmailMessagePayload, mime: String): String? {
        if (payload.mimeType == mime) decode(payload.body?.data)?.let { return it }
        return walk(payload.parts, mime)
    }

    private fun walk(parts: List<GmailMessagePart>?, mime: String): String? {
        for (p in parts.orEmpty()) {
            if (p.mimeType == mime) decode(p.body?.data)?.let { return it }
            walk(p.parts, mime)?.let { return it }
        }
        return null
    }

    private fun decode(data: String?): String? {
        if (data.isNullOrBlank()) return null
        return runCatching { String(Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)) }.getOrNull()
    }

    private fun stripHtml(html: String): String = html
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" ?\\n ?"), "\n")
        .trim()

    companion object {
        private const val UNIQUE = "gmail_sync"
        const val KEY_DEEP = "deep"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GmailSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        // One-shot deep re-scan, for the Settings "Gmail Sync" tap. REPLACE so a re-tap always
        // re-runs rather than being dropped as a duplicate.
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<GmailSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(androidx.work.workDataOf(KEY_DEEP to true))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("${UNIQUE}_now", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

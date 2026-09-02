package com.uj.appstorysautopaymanager.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import com.uj.appstorysautopaymanager.domain.payment.usecase.GetPaymentHistoryUseCase
import com.uj.appstorysautopaymanager.domain.payment.usecase.SavePaymentUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Room is the source of truth for transactions (SmsReceiver, UpiNotificationListenerService, and
// TransactionViewModel's inbox backfill all write there first, via AutoPayRepository, which
// enqueues this worker on every new row). This worker's only job is pushing anything not yet
// synced up to the SoundBox backend via POST /payments.
//
// GET /payments is consulted first purely as an idempotency guard: if a prior run POSTed
// successfully but the process died before the local `synced` flag got written, this avoids
// double-posting the same payment on retry. If that check itself fails (offline, server error),
// we fall through to POSTing everything unsynced anyway - still correct, just not deduped against
// a previous partial run.
class PaymentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PaymentSyncWorkerEntryPoint {
        fun repository(): AutoPayRepository
        fun authRepository(): AuthRepository
        fun getPaymentHistoryUseCase(): GetPaymentHistoryUseCase
        fun savePaymentUseCase(): SavePaymentUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PaymentSyncWorkerEntryPoint::class.java
        )
        val repository = entryPoint.repository()
        val authRepository = entryPoint.authRepository()
        val getPaymentHistory = entryPoint.getPaymentHistoryUseCase()
        val savePayment = entryPoint.savePaymentUseCase()

        // The SMS-inbox backfill scan (MainActivity, runs on every cold start) enqueues this
        // worker for every transaction it inserts - including before login, since that scan isn't
        // gated on auth state either. Without this check, a fresh/logged-out run hits /payments
        // with no token, 401s, and (before AuthInterceptor's own fix) forced a spurious
        // navigate-to-Login even mid-OTP-entry. Nothing to sync yet without a session - the next
        // insert after a real login re-enqueues this anyway.
        if (authRepository.getStoredUser() == null) return Result.success()

        val unsynced = repository.getUnsyncedTransactions()
        if (unsynced.isEmpty()) return Result.success()

        val alreadyOnBackend = (getPaymentHistory() as? Resource.Success)?.data
            ?.mapNotNull { it.transactionId.takeIf(String::isNotBlank) }
            ?.toSet()
            ?: emptySet()

        var allSucceeded = true
        var consecutiveFailures = 0
        for (txn in unsynced) {
            // referenceNumber is empty for some transfer results (see UpiNotificationParser) -
            // fall back to smsId, which is always present and unique, so transaction_id is never blank.
            val transactionId = txn.referenceNumber.ifBlank { txn.smsId }

            if (transactionId in alreadyOnBackend) {
                repository.markTransactionSynced(txn.id, transactionId)
                continue
            }

            // bankName doubles as "provider" here - it holds the actual bank name for SMS-sourced
            // transactions but the UPI app name (GPAY/PHONEPE/PAYTM/...) for notification-sourced
            // ones, since that's what UpiNotificationParser stores in it. Best available mapping,
            // not a guess: there's no separate provider field anywhere upstream.
            val result = savePayment(
                amount = txn.amount,
                currency = "INR",
                provider = txn.bankName.lowercase(),
                transactionId = transactionId,
                payeeName = txn.merchant,
                // API timestamp is Unix seconds (doc example is 10 digits); txn.date is stored in
                // milliseconds.
                timestamp = txn.date / 1000
            )
            when (result) {
                is Resource.Success -> {
                    repository.markTransactionSynced(txn.id, result.data?.id ?: transactionId)
                    consecutiveFailures = 0
                }
                is Resource.Error -> {
                    allSucceeded = false
                    // 401/403 means the stored access token is missing/invalid - every remaining
                    // row will fail identically, and retrying won't fix it (only a fresh login
                    // will). Bail out now rather than hammering the endpoint with the rest of the
                    // backlog, and don't schedule a WorkManager retry for the same reason.
                    if (result.code == 401 || result.code == 403) {
                        return Result.failure()
                    }
                    // ponytail: 3 in a row is "the backend is down/broken right now", not "this
                    // one row has bad data" - a real server-side bug fails identically on every
                    // payload, so grinding through the rest of a large backlog just spams the
                    // endpoint for no benefit. Stop this run and let WorkManager's exponential
                    // backoff retry the whole thing later instead.
                    consecutiveFailures++
                    if (consecutiveFailures >= 3) {
                        return Result.retry()
                    }
                }
                is Resource.Loading -> Unit
            }
        }
        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "payment_sync"

        // KEEP, not REPLACE - each run sweeps every unsynced row regardless of which insert
        // triggered it, so a burst of transactions arriving close together only needs one run.
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PaymentSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}

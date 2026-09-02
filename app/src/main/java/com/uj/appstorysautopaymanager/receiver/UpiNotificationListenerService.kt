package com.uj.appstorysautopaymanager.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.tts.AnnouncementKind
import com.uj.appstorysautopaymanager.tts.TextToSpeechHelper
import com.uj.appstorysautopaymanager.util.NotificationHelper
import com.uj.appstorysautopaymanager.util.UpiNotificationParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UpiNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var repository: AutoPayRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!UpiNotificationParser.isTracked(sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(android.app.Notification.EXTRA_TEXT))?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return

        // Same content-based-id reasoning as SmsReceiver/TransactionViewModel: sbn.key is the
        // system-guaranteed unique id for this exact posted notification, so re-posts of the
        // same notification (apps commonly update one in place) don't get processed twice.
        val notifId = "notif_${sbn.key}"
        val postedAt = sbn.postTime
        val packageName = sbn.packageName
        val ttsHelper = TextToSpeechHelper(applicationContext, preferenceManager)

        CoroutineScope(Dispatchers.IO).launch {
            if (repository.exists(notifId)) return@launch

            val result = UpiNotificationParser.parse(packageName, title, text, postedAt, notifId) ?: return@launch

            if (result.mandate != null) {
                // See AutoPayRepository.applyMandateEvent: a reconfirmation of an already-known
                // mandate state doesn't get its own Passbook entry.
                val shouldRecord = repository.applyMandateEvent(result.mandate)
                if (!shouldRecord) return@launch
                repository.insertTransaction(result.transaction)
            } else {
                // Regular (non-autopay) transaction: reconcile against a bank SMS reporting the
                // same real payment, in either order - see AutoPayRepository.applyTransactionEvent.
                repository.applyTransactionEvent(result.transaction)
            }

            val kind = when {
                result.mandate != null || result.transaction.isAutoPay -> AnnouncementKind.AUTOPAY_SET
                result.transaction.transactionType == "CREDIT" -> AnnouncementKind.CREDIT_RECEIVED
                else -> AnnouncementKind.DEBIT_PAID
            }
            ttsHelper.speak(kind, result.transaction.merchant, result.transaction.amount.toInt())

            val isCredit = result.transaction.transactionType == "CREDIT"
            NotificationHelper.notify(
                context = applicationContext,
                repository = repository,
                preferenceManager = preferenceManager,
                title = if (isCredit) "Payment Received" else "Transaction Detected",
                body = if (isCredit) {
                    "₹${result.transaction.amount} credited from ${result.transaction.merchant}"
                } else {
                    "₹${result.transaction.amount} debited for ${result.transaction.merchant}"
                },
                category = "Payments"
            )
        }
    }
}

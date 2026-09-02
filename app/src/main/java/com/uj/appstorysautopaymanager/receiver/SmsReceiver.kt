package com.uj.appstorysautopaymanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.tts.AnnouncementKind
import com.uj.appstorysautopaymanager.tts.TextToSpeechHelper
import com.uj.appstorysautopaymanager.util.NotificationHelper
import com.uj.appstorysautopaymanager.util.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AutoPayRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val ttsHelper = TextToSpeechHelper(context, preferenceManager)

        CoroutineScope(Dispatchers.IO).launch {
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val date = sms.timestampMillis
                val address = sms.originatingAddress ?: "Unknown"
                // Content-based, not timestamp-based: sms.timestampMillis (SMSC send time) and
                // Telephony.Sms.Inbox.DATE (device receipt time, used by the inbox backfill in
                // TransactionViewModel.scanSmsInbox) commonly differ for the same real message,
                // which produced two different ids for one SMS and duplicated it in Passbook.
                val smsId = "${address}_${body}"
                if (repository.exists(smsId)) continue

                val result = SmsParser.parseSms(body, date, smsId, address) ?: continue

                if (result.mandate != null) {
                    // A mandate event that just reconfirms an already-known state (e.g. the
                    // bank's and Paytm's own SMS both confirming the same setup) doesn't get
                    // its own Passbook entry - see AutoPayRepository.applyMandateEvent.
                    val shouldRecord = repository.applyMandateEvent(result.mandate)
                    if (!shouldRecord) continue
                    repository.insertTransaction(result.transaction)
                } else {
                    // Regular (non-autopay) transaction: reconcile against a UPI app
                    // notification reporting the same real payment, in either order - see
                    // AutoPayRepository.applyTransactionEvent.
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
                    context = context,
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
}

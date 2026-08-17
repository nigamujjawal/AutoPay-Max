package com.uj.appstorysautopaymanager.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.uj.appstorysautopaymanager.MainActivity
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.tts.TextToSpeechHelper
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
                val smsId = "${address}_${date}"

                val result = SmsParser.parseSms(body, date, smsId) ?: continue

                repository.insertTransaction(result.transaction)
                result.mandate?.let {
                    repository.insertMandate(it)
                }

                val speakText = if (result.mandate != null || result.transaction.isAutoPay) {
                    "AutoPay set for ${result.transaction.merchant} of ${result.transaction.amount.toInt()} rupees."
                } else if (result.transaction.transactionType == "CREDIT") {
                    "Received ${result.transaction.amount.toInt()} rupees from ${result.transaction.merchant}."
                } else {
                    "Paid ${result.transaction.amount.toInt()} rupees to ${result.transaction.merchant}."
                }
                ttsHelper.speak(speakText)

                showNotification(context, result.transaction.merchant, result.transaction.amount, result.transaction.transactionType)
            }
        }
    }

    private fun showNotification(context: Context, merchant: String, amount: Double, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "autopay_alerts"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (type == "CREDIT") "Payment Received" else "Transaction Detected"
        val text = if (type == "CREDIT") {
            "₹$amount credited from $merchant"
        } else {
            "₹$amount debited for $merchant"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

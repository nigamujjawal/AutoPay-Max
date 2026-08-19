package com.uj.appstorysautopaymanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uj.appstorysautopaymanager.worker.MandateReminderWorker
import com.uj.appstorysautopaymanager.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleBillReminders(context)
            scheduleMandateReminders(context)
        }
    }

    companion object {
        fun scheduleBillReminders(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "bill_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        // Always scheduled - the worker itself checks the "Autopay Reminders" setting and
        // no-ops if it's off, so there's nothing to enqueue/cancel from the Settings toggle.
        fun scheduleMandateReminders(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MandateReminderWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "mandate_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

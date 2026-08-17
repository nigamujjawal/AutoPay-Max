package com.uj.appstorysautopaymanager.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uj.appstorysautopaymanager.MainActivity
import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderWorkerEntryPoint {
        fun billDao(): BillDao
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderWorkerEntryPoint::class.java
        )
        val billDao = entryPoint.billDao()

        val bills = billDao.getAllBills().first()
        val now = System.currentTimeMillis()

        for (bill in bills) {
            if (bill.status != "PENDING") continue

            val diffMillis = bill.dueDate - now
            val daysRemaining = (diffMillis / (24L * 60L * 60L * 1000L)).toInt()

            if (daysRemaining >= 0 && (daysRemaining == bill.reminderDays || daysRemaining == 0)) {
                showBillReminder(bill, daysRemaining)
            }
        }
        return Result.success()
    }

    private fun showBillReminder(bill: Bill, daysRemaining: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "autopay_alerts"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, bill.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Upcoming Bill Alert"
        val text = when (daysRemaining) {
            0 -> "Your bill for ${bill.title} of ₹${bill.amount} is due TODAY!"
            1 -> "Your bill for ${bill.title} of ₹${bill.amount} is due tomorrow!"
            else -> "Your bill for ${bill.title} of ₹${bill.amount} is due in $daysRemaining days."
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(bill.id.toInt(), notification)
    }
}

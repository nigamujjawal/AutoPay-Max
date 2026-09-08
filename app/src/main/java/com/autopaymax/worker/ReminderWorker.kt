package com.autopaymax.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autopaymax.data.local.dao.BillDao
import com.autopaymax.data.local.pref.PreferenceManager
import com.autopaymax.data.repository.AutoPayRepository
import com.autopaymax.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderWorkerEntryPoint {
        fun billDao(): BillDao
        fun repository(): AutoPayRepository
        fun preferenceManager(): PreferenceManager
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderWorkerEntryPoint::class.java
        )
        val billDao = entryPoint.billDao()
        val repository = entryPoint.repository()
        val preferenceManager = entryPoint.preferenceManager()

        val currencySymbol = preferenceManager.currencyFlow.first()
        val bills = billDao.getAllBills().first()
        val now = System.currentTimeMillis()

        for (bill in bills) {
            if (bill.status != "PENDING") continue

            val diffMillis = bill.dueDate - now
            val daysRemaining = (diffMillis / (24L * 60L * 60L * 1000L)).toInt()

            if (daysRemaining >= 0 && (daysRemaining == bill.reminderDays || daysRemaining == 0)) {
                val title = "Upcoming Bill Alert"
                val text = when (daysRemaining) {
                    0 -> "Your bill for ${bill.title} of $currencySymbol${bill.amount} is due TODAY!"
                    1 -> "Your bill for ${bill.title} of $currencySymbol${bill.amount} is due tomorrow!"
                    else -> "Your bill for ${bill.title} of $currencySymbol${bill.amount} is due in $daysRemaining days."
                }
                NotificationHelper.notify(
                    context = applicationContext,
                    repository = repository,
                    preferenceManager = preferenceManager,
                    title = title,
                    body = text,
                    category = "Payments",
                    isWarning = daysRemaining <= 1,
                    notificationId = bill.id.toInt()
                )
            }
        }
        return Result.success()
    }
}

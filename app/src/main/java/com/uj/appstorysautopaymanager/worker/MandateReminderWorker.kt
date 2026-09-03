package com.uj.appstorysautopaymanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uj.appstorysautopaymanager.data.local.dao.MandateDao
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.util.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

// Reminds 2 days before an active mandate's nextExpectedDebit - the same field the Dashboard
// already uses to show "Due in N days" (AutoPaymentRow in DashboardScreen.kt), so this fires in
// lockstep with what the user sees there rather than tracking its own separate due-date logic.
class MandateReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MandateReminderWorkerEntryPoint {
        fun mandateDao(): MandateDao
        fun repository(): AutoPayRepository
        fun preferenceManager(): PreferenceManager
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            MandateReminderWorkerEntryPoint::class.java
        )
        val mandateDao = entryPoint.mandateDao()
        val repository = entryPoint.repository()
        val preferenceManager = entryPoint.preferenceManager()

        if (!preferenceManager.isAutopayRemindersEnabledFlow.first()) return Result.success()

        val currencySymbol = preferenceManager.currencyFlow.first()
        val mandates = mandateDao.getAllMandates().first() // already filtered to status = 'ACTIVE'
        val now = System.currentTimeMillis()

        for (mandate in mandates) {
            val daysRemaining = ((mandate.nextExpectedDebit - now) / (24L * 60L * 60L * 1000L)).toInt()
            if (daysRemaining != 2) continue

            NotificationHelper.notify(
                context = applicationContext,
                repository = repository,
                preferenceManager = preferenceManager,
                title = "Upcoming AutoPay",
                body = "${mandate.merchant} autopay of $currencySymbol${mandate.amount.toInt()} is due in 2 days.",
                category = "Payments",
                isWarning = true,
                notificationId = mandate.id.toInt()
            )
        }
        return Result.success()
    }
}

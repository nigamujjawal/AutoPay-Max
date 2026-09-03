package com.uj.appstorysautopaymanager.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.uj.appstorysautopaymanager.MainActivity
import com.uj.appstorysautopaymanager.R
import com.uj.appstorysautopaymanager.data.local.dao.BillDao
import com.uj.appstorysautopaymanager.data.local.dao.TransactionDao
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AutoPayWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun transactionDao(): TransactionDao
        fun billDao(): BillDao
        fun preferenceManager(): PreferenceManager
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val transactionDao = entryPoint.transactionDao()
        val billDao = entryPoint.billDao()

        CoroutineScope(Dispatchers.IO).launch {
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH)
            val currentYear = now.get(Calendar.YEAR)
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + (24 * 60 * 60 * 1000)

            val txns = transactionDao.getAllTransactions().first()
            val bills = billDao.getAllBills().first()

            val monthSpending = txns.filter { txn ->
                val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
                txnCal.get(Calendar.MONTH) == currentMonth && 
                txnCal.get(Calendar.YEAR) == currentYear &&
                txn.transactionType == "DEBIT"
            }.sumOf { it.amount }

            val upcomingBillsCount = bills.filter { it.status == "PENDING" && !it.isArchived }.size
            val todayTxnsCount = txns.filter { it.date in todayStart until todayEnd }.size

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_monthly_spending, "Monthly Spending: ₹%.2f".format(monthSpending))
                views.setTextViewText(R.id.widget_upcoming_bills, "Upcoming Bills: $upcomingBillsCount")
                views.setTextViewText(R.id.widget_today_txns, "Today's Transactions: $todayTxnsCount")

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_monthly_spending, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

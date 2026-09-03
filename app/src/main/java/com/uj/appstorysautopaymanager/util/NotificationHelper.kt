package com.uj.appstorysautopaymanager.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.uj.appstorysautopaymanager.MainActivity
import com.uj.appstorysautopaymanager.data.local.entity.NotificationEntity
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import kotlinx.coroutines.flow.first

// Single choke point for every real alert-producing source (live SMS, UPI app notifications,
// bill reminders, autopay-mandate reminders): logs an in-app Notifications-screen entry and
// shows the system notification, both gated by the "Push Notifications" setting - that toggle
// is specifically the in-app notification feature's on/off, not just the system banner.
object NotificationHelper {

    suspend fun notify(
        context: Context,
        repository: AutoPayRepository,
        preferenceManager: PreferenceManager,
        title: String,
        body: String,
        category: String,
        isWarning: Boolean = false,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!preferenceManager.isPushNotificationsEnabledFlow.first()) return

        repository.insertNotification(
            NotificationEntity(
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                category = category,
                isWarning = isWarning
            )
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "autopay_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

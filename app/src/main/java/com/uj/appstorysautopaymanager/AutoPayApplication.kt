package com.uj.appstorysautopaymanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.appversal.appstorys.AppStorys
import com.uj.appstorysautopaymanager.domain.auth.usecase.GetStoredUserUseCase
import com.uj.appstorysautopaymanager.util.EmailPatternConfig
import com.uj.appstorysautopaymanager.util.SmsPatternConfig
import com.uj.appstorysautopaymanager.util.UpiPatternConfig
import com.uj.appstorysautopaymanager.util.UsBankPatternConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AutoPayApplication : Application() {

    @Inject
    lateinit var getStoredUserUseCase: GetStoredUserUseCase

    override fun onCreate() {
        super.onCreate()

        // userId is blank on purpose - the app's real per-user identity (Firebase uid) isn't
        // known yet this early (Application.onCreate() runs before login, and reading it would
        // mean a blocking Room query on the main thread). The SDK generates its own anonymous id
        // until one of the two identify paths below actually runs.
        AppStorys.initialize(
            this,
            appId = BuildConfig.APPSTORYS_APP_ID,
            accountId = BuildConfig.APPSTORYS_ACCOUNT_ID,
            userId = "",
            navigateToScreen = {
             navigateToScreen(it)
            }
        )

        // Path 1 (fresh login): AuthRepositoryImpl.signInWithGoogle() calls AppStorys.setUserId()
        // itself right after a new sign-in succeeds.
        // Path 2 (already signed in): a cold start with an existing session never runs the
        // sign-in exchange again, so nothing would otherwise identify a returning user - check
        // Room here instead, off the main thread.
        CoroutineScope(Dispatchers.IO).launch {
            getStoredUserUseCase()?.let { AppStorys.setUserId(it.uid) }
        }

        createNotificationChannel()

        // Fire-and-forget Remote Config refresh for every parser. EmailParser is the live source
        // now (Gmail sync); the SMS/notification parsers are disconnected but their configs are
        // left refreshing (harmless) in case the receivers are re-enabled. All fall back to
        // hardcoded defaults until the fetch completes, so nothing blocks on cold start.
        SmsPatternConfig.refresh()
        UpiPatternConfig.refresh()
        UsBankPatternConfig.refresh()
        EmailPatternConfig.refresh()
    }

    // Application has no NavController of its own, so this just forwards to whatever
    // MainActivity's Composable registered - every navigateToScreen call in the SDK fires from a
    // click inside an already-composed overlay (tooltip/banner/widget tap), so that handler is
    // guaranteed to be set by the time this can ever run; no Activity-restart plumbing needed.
    // The name string must exactly match one of the Screen.*.route values in
    // ui/navigation/Screen.kt - whoever configures campaigns on the AppStorys dashboard needs to
    // use those same route strings.
    fun navigateToScreen(name: String) {
        navigateToScreenHandler?.invoke(name)
    }

    companion object {
        var navigateToScreenHandler: ((String) -> Unit)? = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "autopay_alerts"
            val channelName = "AutoPay & Bill Reminders"
            val descriptionText = "Notifications for upcoming bills and transaction alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

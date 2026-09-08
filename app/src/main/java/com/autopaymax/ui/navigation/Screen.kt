package com.autopaymax.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Star)
    object GoogleConnect : Screen("google_connect", "Connect", Icons.Default.Lock)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Passbook : Screen("passbook", "Passbook", Icons.Default.CreditCard)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AppSettings : Screen("app_settings", "App Settings", Icons.Default.Tune)
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)

    // Tile picker shown when the user taps "Add new subscription" on Home.
    object AddSubscription : Screen("add_subscription", "Add Subscription", Icons.Default.Add)

    // merchant is an optional pre-fill from the picker tile; null = "Other" / manual.
    object AutoPay : Screen("autopay?merchant={merchant}", "AutoPay Mandates", Icons.Default.Autorenew) {
        fun routeFor(merchant: String?) =
            if (merchant.isNullOrBlank()) "autopay" else "autopay?merchant=${android.net.Uri.encode(merchant)}"
    }

    object MandateDetail : Screen("mandate_detail/{mandateId}", "AutoPay Detail", Icons.Default.Autorenew) {
        fun routeFor(mandateId: Long) = "mandate_detail/$mandateId"
    }
}

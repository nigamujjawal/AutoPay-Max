package com.uj.appstorysautopaymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Star)
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.Notifications)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Passbook : Screen("passbook", "Passbook", Icons.Default.CreditCard)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AppSettings : Screen("app_settings", "App Settings", Icons.Default.Tune)
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)

    object AutoPay : Screen("autopay", "AutoPay Mandates", Icons.Default.Autorenew)
}

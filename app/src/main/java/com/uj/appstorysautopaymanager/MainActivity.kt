package com.uj.appstorysautopaymanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uj.appstorysautopaymanager.receiver.BootReceiver
import com.uj.appstorysautopaymanager.ui.autopay.MandateViewModel
import com.uj.appstorysautopaymanager.ui.dashboard.DashboardScreen
import com.uj.appstorysautopaymanager.ui.navigation.Screen
import com.uj.appstorysautopaymanager.ui.onboarding.OnboardingScreen
import com.uj.appstorysautopaymanager.ui.onboarding.PermissionsScreen
import com.uj.appstorysautopaymanager.ui.onboarding.SplashScreen
import com.uj.appstorysautopaymanager.ui.passbook.PassbookScreen
import com.uj.appstorysautopaymanager.ui.passbook.TransactionViewModel
import com.uj.appstorysautopaymanager.ui.notification.NotificationsScreen
import com.uj.appstorysautopaymanager.ui.settings.AppSettingsScreen
import com.uj.appstorysautopaymanager.ui.settings.SettingsScreen
import com.uj.appstorysautopaymanager.ui.settings.SettingsViewModel
import com.uj.appstorysautopaymanager.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @javax.inject.Inject
    lateinit var preferenceManager: com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BootReceiver.scheduleBillReminders(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val mandateViewModel: MandateViewModel = hiltViewModel()
            val transactionViewModel: TransactionViewModel = hiltViewModel()

            val selectedTheme by settingsViewModel.theme.collectAsState()
            val isOnboarded by settingsViewModel.isOnboarded.collectAsState()

            val darkTheme = when (selectedTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            AppStorysAutoPayManagerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(
                        settingsViewModel = settingsViewModel,
                        mandateViewModel = mandateViewModel,
                        transactionViewModel = transactionViewModel,
                        preferenceManager = preferenceManager,
                        isOnboarded = isOnboarded
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    settingsViewModel: SettingsViewModel,
    mandateViewModel: MandateViewModel,
    transactionViewModel: TransactionViewModel,
    preferenceManager: com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager,
    isOnboarded: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // EXACT 3 TABS AS REQUESTED: Home (AutoPay Records), Passbook, Settings
    val navigationItems = listOf(
        Screen.Home,
        Screen.Passbook,
        Screen.Settings
    )

    val isMainTabScreen = currentRoute in navigationItems.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isMainTabScreen) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 12.dp,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(68.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navigationItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) Color(0xFFFF5E00) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = screen.title,
                                        color = if (selected) Color(0xFFFF5E00) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        if (isOnboarded) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Onboarding Screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onStartTrialClick = {
                        navController.navigate(Screen.Permissions.route)
                    }
                )
            }

            // Permissions Screen
            composable(Screen.Permissions.route) {
                PermissionsScreen(
                    onPermissionsCompleted = {
                        settingsViewModel.setIsOnboarded(true)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Tab 1: Home (AutoPay Records)
            composable(Screen.Home.route) {
                DashboardScreen(
                    mandateViewModel = mandateViewModel,
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // Tab 2: Passbook
            composable(Screen.Passbook.route) {
                PassbookScreen(
                    viewModel = transactionViewModel,
                    preferenceManager = preferenceManager,
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // Tab 3: Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLogoutClick = {
                        settingsViewModel.setIsOnboarded(false)
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToAppSettings = {
                        navController.navigate(Screen.AppSettings.route)
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // Sub-screen 1: App Settings (Voice, Language, Alert Customization)
            composable(Screen.AppSettings.route) {
                AppSettingsScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel
                )
            }

            // Sub-screen 2: Notifications List (Alerts, System & Payment Reminders)
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    navController = navController
                )
            }
        }
    }
}
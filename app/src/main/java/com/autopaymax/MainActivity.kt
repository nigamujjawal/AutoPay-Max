package com.autopaymax

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appversal.appstorys.AppStorys
import com.autopaymax.receiver.BootReceiver
import com.autopaymax.ui.auth.AuthViewModel
import com.autopaymax.ui.auth.GoogleConnectScreen
import com.autopaymax.ui.autopay.AddSubscriptionPickerScreen
import com.autopaymax.ui.autopay.AutoPayScreen
import com.autopaymax.ui.autopay.MandateDetailScreen
import com.autopaymax.ui.autopay.MandateViewModel
import com.autopaymax.ui.dashboard.DashboardScreen
import com.autopaymax.ui.navigation.Screen
import com.autopaymax.ui.onboarding.OnboardingScreen
import com.autopaymax.ui.onboarding.SplashScreen
import com.autopaymax.worker.GmailSyncWorker
import com.autopaymax.ui.passbook.PassbookScreen
import com.autopaymax.ui.passbook.TransactionViewModel
import com.autopaymax.ui.notification.NotificationsScreen
import com.autopaymax.ui.notification.NotificationsViewModel
import com.autopaymax.ui.settings.VoiceBehaviour
import com.autopaymax.ui.settings.SettingsScreen
import com.autopaymax.ui.settings.SettingsViewModel
import com.autopaymax.ui.profile.ProfileViewModel
import com.autopaymax.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BootReceiver.scheduleBillReminders(this)
        BootReceiver.scheduleMandateReminders(this)
        GmailSyncWorker.schedule(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val mandateViewModel: MandateViewModel = hiltViewModel()
            val transactionViewModel: TransactionViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()

            val selectedTheme by settingsViewModel.theme.collectAsState()

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
                    // overlayElements() must sit above everything else in the stack (per the SDK
                    // docs) - it's what actually renders Banner/Floater/Modals/BottomSheet/
                    // Tooltips/Spotlight/ScratchCard/Survey AND the test-user "Capture Screen"
                    // button. None of that can ever appear without this being called somewhere -
                    // it wasn't wired in anywhere before now. One Box, drawn last so it's on top;
                    // called once here rather than per-screen since this is a single-Activity app.
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainAppContent(
                            settingsViewModel = settingsViewModel,
                            mandateViewModel = mandateViewModel,
                            transactionViewModel = transactionViewModel,
                            authViewModel = authViewModel
                        )
                        AppStorys.overlayElements(activity = this@MainActivity)
                    }
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
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Set from GoogleConnectScreen's country picker (auto-detected or manually chosen) -
    // Dashboard, Passbook, and AutoPay read this instead of hardcoding a currency symbol.
    val currencySymbol by settingsViewModel.currency.collectAsState()

    // AppStorys campaign navigation. Every navigateToScreen call in the SDK fires from a click
    // inside an actively-composed overlay (tooltip/banner/widget tap) - there's no path where it
    // can happen while this Composable isn't alive, so a direct callback is enough; no Intent/
    // onNewIntent restart plumbing needed. Registered on compose, cleared on dispose so
    // AutoPayApplication never holds a reference to a dead NavController.
    // The name string is whatever's configured on the AppStorys dashboard for that campaign - it
    // must exactly match one of the Screen.*.route values in ui/navigation/Screen.kt.
    DisposableEffect(navController) {
        AutoPayApplication.navigateToScreenHandler = { name -> navController.navigate(name) }
        onDispose { AutoPayApplication.navigateToScreenHandler = null }
    }

    // Forced logout on a 401 from any backend call (see AuthInterceptor/SessionExpiredNotifier) -
    // this can fire from any screen, not just Home, so the whole back stack is cleared rather
    // than reusing the manual logout button's popUpTo(Home).
    LaunchedEffect(Unit) {
        authViewModel.sessionExpired.collect {
            navController.navigate(Screen.GoogleConnect.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Passbook is unwired from the flow (no tab, no "See All" link) - its screen + VM are kept
    // intact in the NavHost below so it can be re-added to this list to bring it back.
    val navigationItems = listOf(
        Screen.Home,
        Screen.Settings
    )

    val isMainTabScreen = currentRoute in navigationItems.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // AnimatedVisibility (not a plain `if`) so this show/hide goes through the same
            // Transition-settling machinery NavHost's destination swap uses — that keeps the
            // bar's appearance in lockstep with the content instead of popping in a frame
            // before/after it, which is what caused the splash→dashboard glitch.
            AnimatedVisibility(
                visible = isMainTabScreen,
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 30.dp , bottomEnd = 30.dp),
                    shadowElevation = 12.dp,
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) Color(0xFFFF5E00) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
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
        floatingActionButton = {
            if (currentRoute == Screen.Home.route) {
                ExtendedFloatingActionButton(
                    shape = RoundedCornerShape(50.dp),
                    onClick = { navController.navigate(Screen.AddSubscription.route) },
                    containerColor = Color(0xFFFF5E00),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        val target = when {
                            !settingsViewModel.isOnboarded.value -> Screen.Onboarding.route
                            !authViewModel.isAuthenticated.value -> Screen.GoogleConnect.route
                            else -> Screen.Home.route
                        }
                        navController.navigate(target) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding Screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onStartTrialClick = {
                        navController.navigate(Screen.GoogleConnect.route){
                            popUpTo(Screen.Onboarding.route){inclusive = true}
                        }
                    }
                )
            }

            // Google connection: in-app sign-in + Gmail read scope, then Home.
            composable(Screen.GoogleConnect.route) {
                GoogleConnectScreen(
                    authViewModel = authViewModel,
                    settingsViewModel = settingsViewModel,
                    onSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.GoogleConnect.route) { inclusive = true }
                        }
                    }
                )
            }


            // Tab 1: Home (AutoPay Records)
            composable(Screen.Home.route) {
                DashboardScreen(
                    mandateViewModel = mandateViewModel,
                    currencySymbol = currencySymbol,
                    onMandateClick = { id ->
                        navController.navigate(Screen.MandateDetail.routeFor(id))
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // Passbook - unwired from the flow (removed from navigationItems + the Dashboard
            // "See All" link). Kept registered so it still resolves for an AppStorys deep link
            // and can be put back in the tab bar with one line.
            composable(Screen.Passbook.route) {
                PassbookScreen(
                    viewModel = transactionViewModel,
                    currencySymbol = currencySymbol,
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // Tab 3: Settings
            composable(Screen.Settings.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    profileViewModel = profileViewModel,
                    onLogoutClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.GoogleConnect.route) {
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
                VoiceBehaviour(
                    navController = navController,
                    settingsViewModel = settingsViewModel
                )
            }

            // Sub-screen 2: Notifications List (Alerts, System & Payment Reminders)
            composable(Screen.Notifications.route) {
                val notificationsViewModel: NotificationsViewModel = hiltViewModel()
                NotificationsScreen(
                    navController = navController,
                    viewModel = notificationsViewModel
                )
            }

            // Add-subscription tile picker (opened by the Home FAB).
            composable(Screen.AddSubscription.route) {
                AddSubscriptionPickerScreen(
                    onPick = { merchant ->
                        navController.navigate(Screen.AutoPay.routeFor(merchant))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AutoPay.route,
                arguments = listOf(navArgument("merchant") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                AutoPayScreen(
                    viewModel = mandateViewModel,
                    navController = navController,
                    currencySymbol = currencySymbol,
                    prefillMerchant = entry.arguments?.getString("merchant")
                )
            }

            composable(
                route = Screen.MandateDetail.route,
                arguments = listOf(navArgument("mandateId") { type = NavType.LongType })
            ) { entry ->
                MandateDetailScreen(
                    mandateId = entry.arguments?.getLong("mandateId") ?: -1L,
                    mandateViewModel = mandateViewModel,
                    currencySymbol = currencySymbol,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
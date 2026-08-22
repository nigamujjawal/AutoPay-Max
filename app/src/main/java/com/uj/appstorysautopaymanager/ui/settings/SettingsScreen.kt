package com.uj.appstorysautopaymanager.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.ui.dashboard.AutoPayTopHeader
import com.uj.appstorysautopaymanager.ui.passbook.TransactionViewModel
import com.uj.appstorysautopaymanager.ui.profile.ProfileUiEvent
import com.uj.appstorysautopaymanager.ui.profile.ProfileViewModel

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    transactionViewModel: TransactionViewModel,
    profileViewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    onNavigateToAppSettings: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val isVoiceAlertsEnabled by viewModel.isVoiceAlertsEnabled.collectAsState()
    val speechSpeed by viewModel.speechSpeed.collectAsState()
    val speechLanguage by viewModel.speechLanguage.collectAsState()
    val pushNotifications by viewModel.isPushNotificationsEnabled.collectAsState()
    val autopayReminders by viewModel.isAutopayRemindersEnabled.collectAsState()
    val isScanning by transactionViewModel.isScanning.collectAsState()

    val profileState by profileViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.event.collect { event ->
            when (event) {
                is ProfileUiEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var profileUpiId by remember { mutableStateOf("") }

    AppStorys.getScreenCampaigns("settings_screen",listOf())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .appstorys("settings_screen")
    ) {
        // App Top Header (Same as Home & Passbook)
        AutoPayTopHeader(onNotificationsClick = onNotificationsClick)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp)
        ) {
            // Section 1: Account
            item {
                SettingsSectionHeader("Account")
            }
            item {
                SettingsCard {
                    SettingsRowItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        subtitle = "Update your name & business info",
                        onClick = {
                            profileName = profileState.profile?.name.orEmpty()
                            profileUpiId = profileState.profile?.upiId.orEmpty()
                            showEditProfileDialog = true
                        }
                    )
                }
            }

            // Section 2: Subscription
            item {
                SettingsSectionHeader("Subscription")
            }
            item {
                // Static placeholder until Razorpay checkout is integrated - do not wire this to
                // SubscriptionViewModel/CreateSubscriptionUseCase yet, there's no real payment
                // collection step behind it.
                SubscriptionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6B00)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AutoPay Premium", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Premium active • Renews Jul 2027", fontSize = 11.sp, color = Color(0xFFFF6B00), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFFFE0D1), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B00))
                        }
                    }
                }
            }

            // Section 3: Preferences
            item {
                SettingsSectionHeader("Preferences")
            }
            item {
                SettingsCard {
                    Column {
                        SettingsSwitchItem(
                            icon = Icons.Default.NotificationsNone,
                            title = "Push Notifications",
                            subtitle = "Payment & system alerts",
                            checked = pushNotifications,
                            onCheckedChange = { viewModel.setPushNotificationsEnabled(it) }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Default.Sync,
                            title = "Re-scan SMS",
                            subtitle = if (isScanning) "Scanning..." else "Tap to re-scan your SMS inbox now",
                            checked = isScanning,
                            onCheckedChange = { checked ->
                                if (checked && !isScanning) transactionViewModel.scanSmsInbox(context)
                            }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Autopay Reminders",
                            subtitle = "Heads-up reminder 2 days before",
                            checked = autopayReminders,
                            onCheckedChange = { viewModel.setAutopayRemindersEnabled(it) }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Default.VolumeUp,
                            title = "Sound Alerts",
                            subtitle = "Audio on payment received",
                            checked = isVoiceAlertsEnabled,
                            onCheckedChange = { viewModel.setVoiceAlertsEnabled(it) }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsRowItem(
                            icon = Icons.Default.Tune,
                            title = "Voice & Behaviour",
                            subtitle = "Voice, speed, language, Audio",
                            onClick = { onNavigateToAppSettings() }
                        )
                    }
                }
            }

            // Section 4: Support
            item {
                SettingsSectionHeader("Support")
            }

            // Help & FAQ Card
            item {
                SettingsCard {
                    SettingsRowItem(
                        icon = Icons.Default.HelpOutline,
                        title = "Help & FAQ",
                        subtitle = "Browse answers on our help center",
                        onClick = { openUrl(context, "https://autopay.com/help") }
                    )
                }
            }

            // Contact Support Card
            item {
                SettingsCard {
                    SettingsRowItem(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Contact Support",
                        subtitle = "Our team typically responds within 2-4 hours.",
                        onClick = { openUrl(context, "https://autopay.com/support") }
                    )
                }
            }

            // Rate the App Card
            item {
                SettingsCard {
                    SettingsRowItem(
                        icon = Icons.Default.ThumbUpOffAlt,
                        title = "Rate the App",
                        subtitle = "Love it? Leave a review!",
                        onClick = { openUrl(context, "https://autopay.com/rate") }
                    )
                }
            }

            // Delete Account Button (same theme as Log Out)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF1F2))
                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(14.dp))
                        .clickable { Toast.makeText(context, "Delete Account clicked", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Account",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delete Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
            }

            // Log Out Button (Matches Image 6)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF1F2))
                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(14.dp))
                        .clickable { onLogoutClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Log Out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color.White,
            title = {
                Text("Edit Profile", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(20.dp),
                        value = profileName,
                        onValueChange = { profileName = it },
                        placeholder = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color.Black
                        )
                    )
                    OutlinedTextField(
                        shape = RoundedCornerShape(20.dp),
                        value = profileUpiId,
                        onValueChange = { profileUpiId = it },
                        placeholder = { Text("UPI ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditProfileDialog = false
                        profileViewModel.saveProfile(name = profileName, upiId = profileUpiId)
                    },
                    enabled = !profileState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
    ) {
        Column(content = content)
    }
}

@Composable
fun SubscriptionCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF9F5))
            .border(1.dp, Color(0xFFFFE0D1), RoundedCornerShape(20.dp))
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0EA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0EA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF6B00),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}

package com.uj.appstorysautopaymanager.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.uj.appstorysautopaymanager.ui.dashboard.AutoPayTopHeader

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogoutClick: () -> Unit,
    onNavigateToAppSettings: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val isVoiceAlertsEnabled by viewModel.isVoiceAlertsEnabled.collectAsState()
    val speechSpeed by viewModel.speechSpeed.collectAsState()
    val speechLanguage by viewModel.speechLanguage.collectAsState()

    var pushNotifications by remember { mutableStateOf(true) }
    var rescanSms by remember { mutableStateOf(true) }
    var autopayReminders by remember { mutableStateOf(true) }

    var expandedFaqIndex by remember { mutableStateOf(0) } // 0 means first question expanded
    var faqSectionExpanded by remember { mutableStateOf(true) }
    var userRating by remember { mutableStateOf(5) }

    val faqs = listOf(
        "How do I set up SoundBox?" to "Install the app, grant notification access, and ensure your UPI apps show notifications. SoundBox will announce every payment automatically.",
        "Why am I not getting payment alerts?" to "Ensure SMS & Notification permissions are granted and your phone's media volume is turned up.",
        "Can I use SoundBox without internet?" to "Yes! Text-to-Speech voice announcements work completely offline without an internet connection.",
        "How do I change announcement language?" to "Navigate to App Settings under Preferences to choose English (US), English (India), or Hindi.",
        "Is my payment data safe?" to "Your financial SMS and mandate records are stored securely on your local device."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
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
                        onClick = { Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // Section 2: Subscription
            item {
                SettingsSectionHeader("Subscription")
            }
            item {
                SettingsCard {
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
                                Text("AutoPay Premium", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Premium active • Renews Jul 2027", fontSize = 11.sp, color = Color(0xFFFF6B00), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFF9F5))
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
                            onCheckedChange = { pushNotifications = it }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Default.Sync,
                            title = "Re-scan SMS",
                            subtitle = "Re-runs the 6-month backfill",
                            checked = rescanSms,
                            onCheckedChange = { rescanSms = it }
                        )

                        Divider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Autopay Reminders",
                            subtitle = "Heads-up reminders 2-3 days before",
                            checked = autopayReminders,
                            onCheckedChange = { autopayReminders = it }
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
                            title = "App Settings",
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
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { faqSectionExpanded = !faqSectionExpanded },
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
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Help & FAQ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                            Icon(
                                imageVector = if (faqSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        if (faqSectionExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            faqs.forEachIndexed { index, (question, answer) ->
                                val isExpanded = expandedFaqIndex == index

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedFaqIndex = if (isExpanded) -1 else index }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = question,
                                            fontSize = 13.sp,
                                            fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isExpanded) Color(0xFFFF6B00) else Color(0xFF334155),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFFCBD5E1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Text(
                                            text = answer,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Contact Support Card
            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF0EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Contact Support", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Our team typically responds within 2-4 hours.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Live Chat Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF6B00))
                                    .clickable { Toast.makeText(context, "Opening Live Chat...", Toast.LENGTH_SHORT).show() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Live Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Instant", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }

                            // Email Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF0EA))
                                    .clickable { Toast.makeText(context, "Email support@autopay.com", Toast.LENGTH_SHORT).show() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MailOutline, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Email", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B00))
                                    Text("support@", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                            }

                            // Call Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF0EA))
                                    .clickable { Toast.makeText(context, "Calling Helpline...", Toast.LENGTH_SHORT).show() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B00))
                                    Text("10am-6pm", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }

            // Rate the App Card
            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF0EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ThumbUpOffAlt, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Rate the App", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Love it? Leave a review!", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("How would you rate SoundBox?", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (star in 1..5) {
                                        Icon(
                                            imageVector = if (star <= userRating) Icons.Default.Star else Icons.Default.StarOutline,
                                            contentDescription = null,
                                            tint = if (star <= userRating) Color(0xFFFFC107) else Color(0xFFCBD5E1),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    userRating = star
                                                    Toast.makeText(context, "Thank you for rating $star stars!", Toast.LENGTH_SHORT).show()
                                                }
                                        )
                                    }
                                }
                            }
                        }
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
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
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

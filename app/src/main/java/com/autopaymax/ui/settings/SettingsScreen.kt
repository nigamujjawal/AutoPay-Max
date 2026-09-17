package com.autopaymax.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.ui.components.StatusBadge
import com.autopaymax.ui.dashboard.AutoPayTopHeader
import com.autopaymax.ui.profile.ProfileUiEvent
import com.autopaymax.ui.profile.ProfileViewModel
import com.autopaymax.ui.theme.*
import com.autopaymax.util.GmailAuthManager
import kotlinx.coroutines.launch

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    onNavigateToAppSettings: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    val isVoiceAlertsEnabled by viewModel.isVoiceAlertsEnabled.collectAsState()
    val speechSpeed by viewModel.speechSpeed.collectAsState()
    val speechLanguage by viewModel.speechLanguage.collectAsState()
    val pushNotifications by viewModel.isPushNotificationsEnabled.collectAsState()
    val autopayReminders by viewModel.isAutopayRemindersEnabled.collectAsState()
    val gmailConnected by viewModel.isGmailConnected.collectAsState()
    val gmailEmail by viewModel.gmailConnectedEmail.collectAsState()
    val gmailReauthNeeded by viewModel.isGmailReauthNeeded.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()

    val profileState by profileViewModel.state.collectAsState()

    val gmailResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        scope.launch {
            if (GmailAuthManager.resultFromIntent(activity, result.data) is GmailAuthManager.AuthResult.Granted) {
                viewModel.setGmailConnected(signedInEmail)
                viewModel.triggerGmailSync(context)
                Toast.makeText(context, "Gmail connected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gmail connection cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun connectGmail() {
        scope.launch {
            when (val r = GmailAuthManager.authorize(activity)) {
                is GmailAuthManager.AuthResult.Granted -> {
                    viewModel.setGmailConnected(signedInEmail)
                    viewModel.triggerGmailSync(context)
                    Toast.makeText(context, "Gmail connected", Toast.LENGTH_SHORT).show()
                }
                is GmailAuthManager.AuthResult.NeedsResolution ->
                    gmailResolutionLauncher.launch(IntentSenderRequest.Builder(r.pendingIntent).build())
                else -> Toast.makeText(context, "Couldn't connect Gmail", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.event.collect { event ->
            when (event) {
                is ProfileUiEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }

    AppStorys.getScreenCampaigns("settings_screen", listOf())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .appstorys("settings_screen")
    ) {
        // App Top Header
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
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    profileState.profile?.name?.takeIf { it.isNotBlank() } ?: "Add your name",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite
                                )
                                if (signedInEmail.isNotBlank()) {
                                    Text(signedInEmail, style = MaterialTheme.typography.bodySmall, color = TextGray)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    profileName = profileState.profile?.name.orEmpty()
                                    showEditProfileDialog = true
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Section 2: Subscription
            item {
                SettingsSectionHeader("Subscription")
            }
            item {
                SubscriptionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    "AutoPay Premium",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Premium active • Renews Jul 2027",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StatusActive,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = "ACTIVE")
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        GmailSyncRow(
                            connected = gmailConnected,
                            email = gmailEmail.ifBlank { signedInEmail },
                            reauthNeeded = gmailReauthNeeded,
                            onSyncNow = {
                                viewModel.triggerGmailSync(context)
                                Toast.makeText(context, "Syncing your email…", Toast.LENGTH_SHORT).show()
                            },
                            onConnect = { connectGmail() },
                            onDisconnect = {
                                viewModel.disconnectGmail()
                                Toast.makeText(context, "Gmail disconnected", Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Autopay Reminders",
                            subtitle = "Heads-up reminder 2 days before",
                            checked = autopayReminders,
                            onCheckedChange = { viewModel.setAutopayRemindersEnabled(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitchItem(
                            icon = Icons.Default.VolumeUp,
                            title = "Sound Alerts",
                            subtitle = "Audio on payment received",
                            checked = isVoiceAlertsEnabled,
                            onCheckedChange = { viewModel.setVoiceAlertsEnabled(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                        onClick = { openUrl(context, "https://autopaymax.com/help") }
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
                        onClick = { openUrl(context, "https://autopaymax.com/support") }
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
                        onClick = { openUrl(context, "https://autopaymax.com/rate") }
                    )
                }
            }

            // App Tour & Onboarding Card
            item {
                SettingsCard {
                    SettingsRowItem(
                        icon = Icons.Default.Explore,
                        title = "App Tour & Onboarding",
                        subtitle = "Replay the feature walkthrough & onboarding sequence",
                        onClick = onNavigateToOnboarding
                    )
                }
            }

            // Delete Account Button — the one truly destructive, irreversible
            // action on this screen; the only control that earns error/red.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Delete Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Log Out Button — routine and reversible, so it reads as a
            // normal control, not a warning.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge, color = TextWhite)
            },
            text = {
                OutlinedTextField(
                    shape = RoundedCornerShape(20.dp),
                    value = profileName,
                    onValueChange = { profileName = it },
                    placeholder = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditProfileDialog = false
                        profileViewModel.saveProfile(name = profileName)
                    },
                    enabled = !profileState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Save", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = InstrumentLabel,
        color = TextGray,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryIndigo,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun GmailSyncRow(
    connected: Boolean,
    email: String,
    reauthNeeded: Boolean,
    onSyncNow: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "gmailChevron")

    val (statusText, statusColor) = when {
        reauthNeeded -> "Reconnect needed" to StatusOverdue
        connected -> "Connected" to StatusActive
        else -> "Not connected" to TextGray
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Gmail Sync", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(statusText, style = MaterialTheme.typography.labelLarge, color = statusColor)
                }
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextGray,
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 62.dp, end = 14.dp, bottom = 14.dp)) {
                if (connected && !reauthNeeded) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        Text(
                            email.ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSyncNow,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) { Text("Sync now", style = MaterialTheme.typography.labelLarge) }
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StatusOverdue.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
                        ) { Text("Disconnect", style = MaterialTheme.typography.labelLarge) }
                    }
                } else {
                    Text(
                        if (reauthNeeded) "Your Gmail access needs to be renewed."
                        else "Connect Gmail so autopays can be read from your subscription emails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) { Text(if (reauthNeeded) "Reconnect" else "Connect Gmail", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
}

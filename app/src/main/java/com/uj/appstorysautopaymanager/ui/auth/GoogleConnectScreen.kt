package com.uj.appstorysautopaymanager.ui.auth

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.ui.settings.SettingsViewModel

private val CreamBg = Color(0xFFFAF3E7)
private val IconBg = Color(0xFFFBE0CB)
private val OrangeAccent = Color(0xFFFF6A00)
private val OrangeButton = Color(0xFFE8935C)
private val TitleColor = Color(0xFF1E293B)
private val SubtitleColor = Color(0xFF7D889A)

// Single-step "connect" screen: signs in with Google (Credential Manager, no browser) and grants
// the Gmail read scope back-to-back, then goes Home. The flow itself lives in AuthViewModel; this
// screen only reacts to ConnectEvents and drives the two Activity-result launchers.
@Composable
fun GoogleConnectScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val isConnecting by authViewModel.isConnecting.collectAsState()
    var errorText by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onSuccess() }

    val gmailResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> authViewModel.onGmailResolutionResult(result.data, activity) }

    LaunchedEffect(Unit) {
        authViewModel.connectEvents.collect { event ->
            when (event) {
                is ConnectEvent.NeedsGmailResolution ->
                    gmailResolutionLauncher.launch(IntentSenderRequest.Builder(event.pendingIntent).build())
                ConnectEvent.Completed -> {
                    settingsViewModel.setIsOnboarded(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onSuccess()
                    }
                }
                is ConnectEvent.Failed -> errorText = event.message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(IconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MailOutline, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("Connect your Google account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TitleColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Sign in and let AutoPay Max read your mandate and subscription emails to track your autopays automatically. Nothing leaves your device.",
            fontSize = 14.sp, color = SubtitleColor, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        CountryCurrencyPicker(
            onSelected = { symbol, code ->
                settingsViewModel.setCurrency(symbol)
                settingsViewModel.setCurrencyCode(code)
            }
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { errorText = null; authViewModel.connect(activity) },
            enabled = !isConnecting,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(29.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        errorText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

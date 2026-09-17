package com.autopaymax.ui.auth

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autopaymax.R
import com.autopaymax.ui.settings.SettingsViewModel
import com.autopaymax.ui.theme.*

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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Badge Container
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(6.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(PrimaryIndigo.copy(alpha = 0.12f))
                .border(1.dp, PrimaryIndigo.copy(alpha = 0.25f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "Mail",
                tint = PrimaryIndigo,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Connect your Google account",
            style = MaterialTheme.typography.headlineLarge,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Sign in to let AutoPay Max securely detect your mandate and subscription emails automatically. Nothing leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Country & Currency Selector
        CountryCurrencyPicker(
            onSelected = { symbol, code ->
                settingsViewModel.setCurrency(symbol)
                settingsViewModel.setCurrencyCode(code)
            }
        )

        Spacer(Modifier.height(20.dp))

        // Security & Privacy Pill
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "100% Private • On-Device Processing",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryIndigo
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Google Sign-In Button
        Button(
            onClick = { errorText = null; authViewModel.connect(activity) },
            enabled = !isConnecting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(4.dp, CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = Color.White
            )
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_google),
                        contentDescription = "Google Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        }

        errorText?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                text = it,
                color = StatusOverdue,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

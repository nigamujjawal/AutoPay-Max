package com.autopaymax.ui.auth

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopaymax.R
import com.autopaymax.ui.settings.SettingsViewModel

private val LightAuthBg = Color(0xFFE6EEF8)
private val DarkTitleColor = Color(0xFF0F172A)
private val SoftSubtextColor = Color(0xFF64748B)
private val PrimaryBlueButton = Color(0xFF2563EB)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightAuthBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Progress Bar (Blue line at top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(PrimaryBlueButton)
            )

            Spacer(modifier = Modifier.weight(1f))

            // White Icon Box with Envelope
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = "Mail",
                        tint = PrimaryBlueButton,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Headline
            Text(
                text = "Connect your Google\naccount",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Sign in to let AutoPay Max securely detect your mandate and subscription emails automatically. Nothing leaves your device.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SoftSubtextColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Auto-detect IP Location & Currency Card
            var detectedCountry by remember { mutableStateOf<com.autopaymax.ui.auth.CountryCode?>(null) }
            var isDetecting by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val response = java.net.URL("https://ipinfo.io/json").readText()
                        val json = org.json.JSONObject(response)
                        val countryIso = json.optString("country", "US")
                        val countryInfo = com.autopaymax.ui.auth.COUNTRY_CODES.firstOrNull { it.iso == countryIso } 
                            ?: com.autopaymax.ui.auth.DEFAULT_COUNTRY
                        
                        android.util.Log.d("IP_DETECT", "Detected Country: ${countryInfo.displayName} (ISO: ${countryInfo.iso}, Currency: ${countryInfo.currencyCode})")

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            detectedCountry = countryInfo
                            settingsViewModel.setCurrency(countryInfo.currencySymbol)
                            settingsViewModel.setCurrencyCode(countryInfo.currencyCode)
                            isDetecting = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val fallback = com.autopaymax.ui.auth.DEFAULT_COUNTRY
                        
                        android.util.Log.e("IP_DETECT", "IP Detection failed, falling back to: ${fallback.displayName}", e)

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            detectedCountry = fallback
                            settingsViewModel.setCurrency(fallback.currencySymbol)
                            settingsViewModel.setCurrencyCode(fallback.currencyCode)
                            isDetecting = false
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isDetecting) {
                            Text(
                                text = "Detecting your region...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftSubtextColor
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryBlueButton,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = detectedCountry?.let { "${it.displayName} ${it.dialCode}" } ?: "Unknown",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTitleColor
                            )
                            Text(
                                text = detectedCountry?.let { "${it.currencyCode} (${it.currencySymbol})" } ?: "",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueButton
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Badge (White Pill)
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryBlueButton,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "100% Private • On-Device Processing",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueButton
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Google Sign-In Button
            Button(
                onClick = { errorText = null; authViewModel.connect(activity) },
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlueButton,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skip for now
            Text(
                text = "Skip for now",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SoftSubtextColor,
                modifier = Modifier
                    .clickable { onSuccess() }
                    .padding(8.dp)
            )

            errorText?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


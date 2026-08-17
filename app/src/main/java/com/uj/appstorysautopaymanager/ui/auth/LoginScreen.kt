package com.uj.appstorysautopaymanager.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.uj.appstorysautopaymanager.ui.theme.*

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit
) {
    val pinCode by authViewModel.pinCode.collectAsState()
    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Auto navigate if no PIN is set
    LaunchedEffect(pinCode) {
        if (pinCode.isEmpty()) {
            authViewModel.setAuthenticated(true)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(SecondaryPurple.copy(alpha = 0.2f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Enter Security PIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Access your secure financial statements",
                fontSize = 14.sp,
                color = TextGray
            )
        }

        // Indicators
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (active) PrimaryIndigo else BorderColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = "Incorrect PIN, please try again",
                    color = AccentCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Keyboard
        Column(
            modifier = Modifier.padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Bio", "0", "Del")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        if (key == "Bio" && !isBiometricEnabled) {
                            Spacer(modifier = Modifier.size(72.dp))
                            continue
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CardBackground)
                                .clickable {
                                    showError = false
                                    when (key) {
                                        "Del" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        "Bio" -> {
                                            // Handle biometric request
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    if (authViewModel.checkPin(enteredPin)) {
                                                        onSuccess()
                                                    } else {
                                                        showError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "Del") {
                                Text("Del", color = AccentCoral, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            } else if (key == "Bio") {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentTeal)
                            } else {
                                Text(key, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.uj.appstorysautopaymanager.ui.autopay

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.util.matchAutoPayApp
import com.uj.appstorysautopaymanager.util.merchantInitials
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MandateDetailScreen(
    mandateId: Long,
    mandateViewModel: MandateViewModel,
    onBack: () -> Unit,
    currencySymbol: String = "₹"
) {
    val context = LocalContext.current
    val mandates by mandateViewModel.mandates.collectAsState()
    val mandate = mandates.find { it.id == mandateId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
            }
            Text("AutoPay Detail", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        if (mandate == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Autopay not found", color = Color(0xFF94A3B8))
            }
            return
        }

        val matchedApp = matchAutoPayApp(mandate.merchant)
        val isActive = mandate.status == "ACTIVE"
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(matchedApp?.color ?: Color(0xFFFF7600)),
                contentAlignment = Alignment.Center
            ) {
                if (matchedApp != null) {
                    Icon(
                        painter = painterResource(id = matchedApp.iconRes),
                        contentDescription = matchedApp.displayName,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(merchantInitials(mandate.merchant), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = mandate.merchant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            StatusPill(isActive)

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            ) {
                DetailRow("Next payment date", if (isActive) df.format(Date(mandate.nextExpectedDebit)) else "—")
                Divider(color = Color(0xFFF1F5F9))
                DetailRow("Bill amount", "$currencySymbol${mandate.amount.toInt()}")
                Divider(color = Color(0xFFF1F5F9))
                DetailRow("Status", if (isActive) "Active" else "Cancelled")
            }
        }

        // Bottom action - opens Google Play's subscriptions screen to cancel there.
        Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = { openPlayStoreSubscriptions(context) },
                    enabled = isActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFF1F1F5),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Text(
                        if (isActive) "Cancel subscription" else "Already cancelled",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Google Play's subscriptions management screen. We don't have the app package/SKU from a
// receipt email, so this opens the full list for the user to cancel the right one.
private fun openPlayStoreSubscriptions(context: android.content.Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending"))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

@Composable
private fun StatusPill(isActive: Boolean) {
    val (bg, fg, label) = if (isActive) {
        Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Active")
    } else {
        Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Cancelled")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
    }
}

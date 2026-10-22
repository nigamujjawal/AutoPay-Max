package com.autopaymax.ui.autopay

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autopaymax.ui.components.StatusBadge
import com.autopaymax.ui.theme.*
import com.autopaymax.util.matchAutoPayApp
import com.autopaymax.util.merchantInitials
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandateDetailScreen(
    mandateId: Long,
    mandateViewModel: MandateViewModel,
    onBack: () -> Unit,
    currencySymbol: String = "$"
) {
    val context = LocalContext.current
    val mandates by mandateViewModel.mandates.collectAsState()
    val mandate = mandates.find { it.id == mandateId }
    var showCancelSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text("AutoPay Detail", style = MaterialTheme.typography.titleLarge, color = TextWhite)
        }

        if (mandate == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Autopay not found", color = TextGray)
            }
            return
        }

        val matchedApp = matchAutoPayApp(mandate.merchant)
        val isActive = mandate.status == "ACTIVE"
        // Play-billed subs have a real cancel destination and the cancellation is picked up from
        // the Play cancellation email on the next sync - send the user straight there, no sheet.
        val isPlayBilled = mandate.source == "GOOGLE_PLAY"
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
                    .background(matchedApp?.color ?: PrimaryIndigo),
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
                    Text(merchantInitials(mandate.merchant), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = mandate.merchant,
                style = MaterialTheme.typography.headlineSmall,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            // mandate.status is literally "ACTIVE" or "CANCELLED" — StatusBadge
            // already renders ACTIVE as the steady-green light and falls back to
            // a neutral dot+label for anything else, which is exactly right for
            // "Cancelled".
            StatusBadge(status = mandate.status)

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                DetailRow("Next payment date", if (isActive) df.format(Date(mandate.nextExpectedDebit)) else "-")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow("Bill amount", "$currencySymbol${mandate.amount.toInt()}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow("Status", if (isActive) "Active" else "Cancelled")
            }
        }

        // Bottom action - Play-billed goes straight to the Play Store; everything else opens the
        // cancellation sheet (guidance + mark-as-cancelled, since there's no auto-detect for it).
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        if (isPlayBilled) openPlayStoreSubscriptions(context) else showCancelSheet = true
                    },
                    enabled = isActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContentColor = TextGray
                    )
                ) {
                    Text(
                        when {
                            !isActive -> "Already cancelled"
                            isPlayBilled -> "Cancel via Google Play"
                            else -> "Cancel subscription"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (showCancelSheet && mandate != null) {
        ModalBottomSheet(
            onDismissRequest = { showCancelSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Cancel ${mandate.merchant}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite
                )
                Text(
                    cancelInstructions(mandate.source, mandate.bank),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )

                Button(
                    onClick = {
                        mandateViewModel.cancelMandate(mandate)
                        showCancelSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Mark as cancelled", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// Instructions for the sheet, which only opens for non-Play sources (Play-billed subs deep-link
// straight to the Play Store instead). `vendor` is Mandate.bank - the vendor display name for
// email-sourced ones ("Netflix", "Paytm", "HDFC Bank"...).
private fun cancelInstructions(source: String, vendor: String): String = when (source) {
    "EMAIL" -> {
        val who = vendor.ifBlank { "the provider" }
        "This autopay was detected from a $who email. If it's a UPI autopay, open your UPI app " +
            "(PhonePe, Google Pay, Paytm...) and cancel it under Autopay or Mandates. Otherwise " +
            "cancel it from $who's own website or app. Then mark it cancelled here."
    }
    else ->
        "Cancel this autopay wherever it was set up, your bank or card issuer, or your payment app " +
            "under Autopay / Mandates. Then mark it cancelled here so it stops showing as active."
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextGray)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextWhite)
    }
}

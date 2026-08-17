package com.uj.appstorysautopaymanager.ui.autopay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.ui.components.*
import com.uj.appstorysautopaymanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutoPayScreen(
    viewModel: MandateViewModel
) {
    val mandates by viewModel.mandates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        AppHeader(
            title = "AutoPay Mandates",
            subtitle = "Detected UPI AutoPay & NACH standing instructions"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (mandates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active mandates detected yet.",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(mandates) { mandate ->
                    MandateRow(
                        mandate = mandate,
                        onToggle = { viewModel.toggleMandateStatus(mandate) },
                        onDelete = { viewModel.deleteMandate(mandate) }
                    )
                }
            }
        }
    }
}

@Composable
fun MandateRow(
    mandate: Mandate,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val nextDebitStr = df.format(Date(mandate.nextExpectedDebit))

    PremiumNormalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mandate.merchant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = mandate.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Freq: ${mandate.frequency} • Bank: ${mandate.bank}",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next Debit: $nextDebitStr",
                    fontSize = 11.sp,
                    color = TextGray.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹%,.2f".format(mandate.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (mandate.status == "ACTIVE") Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = "Toggle Status",
                            tint = if (mandate.status == "ACTIVE") AccentTeal else TextGray
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentCoral
                        )
                    }
                }
            }
        }
    }
}

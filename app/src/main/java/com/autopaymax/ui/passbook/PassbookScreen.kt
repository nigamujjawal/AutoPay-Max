package com.autopaymax.ui.passbook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.data.local.entity.Transaction
import com.autopaymax.ui.components.PremiumNormalCard
import com.autopaymax.ui.dashboard.AutoPayTopHeader
import com.autopaymax.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PassbookScreen(
    viewModel: TransactionViewModel,
    currencySymbol: String = "$",
    onNotificationsClick: () -> Unit = {}
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    var showAmount by remember { mutableStateOf(true) }

    val totalCredits = remember(transactions) {
        transactions.filter { it.transactionType == "CREDIT" }.sumOf { it.amount }
    }
    val totalDebits = remember(transactions) {
        transactions.filter { it.transactionType == "DEBIT" }.sumOf { it.amount }
    }
    val netFlow = totalCredits - totalDebits

    // Group transactions by date string
    val groupedTransactions = remember(transactions) {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val todayStr = df.format(Date())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = df.format(yesterdayCal.time)

        transactions.groupBy { txn ->
            val dateStr = df.format(Date(txn.date))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> dateStr
            }
        }
    }

    AppStorys.getScreenCampaigns("passbook_screen",listOf())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .appstorys("passbook_screen")
    ) {
        // App Top Header (Same as Home & Settings)
        AutoPayTopHeader(onNotificationsClick = onNotificationsClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Net Flow Hero — the second instrument reading. Fixed dark
            // bezel like the Dashboard hero, not a surface that flips
            // with the app theme.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppDarkBackground, NavyPrimary, NavySecondary)
                        )
                    )
                    .padding(20.dp)
            ) {
                // Circle overlay art
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.85f, size.height * 0.25f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = size.width * 0.3f,
                        center = Offset(size.width * 0.95f, size.height * 0.85f)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NET FLOW",
                            style = InstrumentLabel,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "All time",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (showAmount) {
                                if (netFlow < 0) "-$currencySymbol${kotlin.math.abs(netFlow).toInt()}" else "$currencySymbol${netFlow.toInt()}"
                            } else "$currencySymbol ••••••",
                            style = InstrumentValueHero,
                            color = Color.White
                        )

                        Icon(
                            imageVector = if (showAmount) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle visibility",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showAmount = !showAmount }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        // Left Credits metric — steady/active tone
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(StatusActiveDarkTone),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Credits", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                                Text("$currencySymbol${totalCredits.toInt()}", style = InstrumentValueMedium, color = Color.White)
                            }
                        }

                        // Right Debits metric — reserved overdue/error tone
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AppDarkError.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Debits", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                                Text("$currencySymbol${totalDebits.toInt()}", style = InstrumentValueMedium, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Filter Tabs Row (All | Credits | Debits)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Credits", "Debits").forEach { tab ->
                    val mappedType = when (tab) {
                        "Credits" -> "CREDIT"
                        "Debits" -> "DEBIT"
                        else -> "All"
                    }
                    val active = selectedType == mappedType

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, if (active) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { viewModel.setSelectedType(mappedType) }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (active) Color.White else TextGray,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Date Grouped Passbook Items List — only this part scrolls
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
        ) {
            if (groupedTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No passbook transactions recorded.",
                            color = TextGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (dateGroup, txns) ->
                    item {
                        Text(
                            text = dateGroup.uppercase(),
                            style = InstrumentLabel,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(txns, key = { it.id }) { txn ->
                        PassbookTransactionRow(txn = txn, currencySymbol = currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
fun PassbookTransactionRow(
    txn: Transaction,
    currencySymbol: String = "$"
) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = df.format(Date(txn.date))

    val initials = remember(txn.merchant) {
        val clean = txn.merchant.replace("Bank of India", "").replace("Bank", "").trim()
        val parts = clean.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        } else {
            clean.take(2).uppercase()
        }
    }

    PremiumNormalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PremiumNavyGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = txn.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        TransactionTypeLabel(isAutoPay = txn.isAutoPay)
                    }
                    Text(
                        text = "${txn.bankName} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            Text(
                text = "${if (txn.transactionType == "CREDIT") "+" else "-"}$currencySymbol${txn.amount.toInt()}",
                style = InstrumentValueMedium,
                color = if (txn.transactionType == "CREDIT") StatusActive else StatusOverdue
            )
        }
    }
}

@Composable
fun TransactionTypeLabel(isAutoPay: Boolean) {
    // Not a status light — this is a category tag (auto-detected vs.
    // manually logged), so it stays on the brand accent, not a reserved
    // status color.
    val (bg, textColor, label) = if (isAutoPay) {
        Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Automatic")
    } else {
        Triple(MaterialTheme.colorScheme.surfaceContainerHigh, TextGray, "Normal")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

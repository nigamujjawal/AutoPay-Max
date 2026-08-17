package com.uj.appstorysautopaymanager.ui.passbook

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import com.uj.appstorysautopaymanager.ui.dashboard.AutoPayTopHeader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PassbookScreen(
    viewModel: TransactionViewModel,
    preferenceManager: com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager,
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // App Top Header (Same as Home & Settings)
        AutoPayTopHeader(onNotificationsClick = onNotificationsClick)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
        ) {
            // Net Flow Hero Card (Matches Image 2)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5500),
                                    Color(0xFFFF7600),
                                    Color(0xFFFF8500)
                                )
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
                                text = "Net flow",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "All time",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
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
                                    if (netFlow < 0) "-₹${kotlin.math.abs(netFlow).toInt()}" else "₹${netFlow.toInt()}"
                                } else "₹ ••••••",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
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
                            // Left Credits metric
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
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
                                    Text("Credits", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("₹${totalCredits.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Right Debits/Credits metric (Matching Image 2)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.85f)),
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
                                    Text("Debits", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("₹${totalDebits.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Tabs Row (All | Credits | Debits | All time v) (Matches Image 2)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                                .background(if (active) Color(0xFFFF5E00) else Color.White)
                                .border(1.dp, if (active) Color(0xFFFF5E00) else Color(0xFFE2E8F0), CircleShape)
                                .clickable { viewModel.setSelectedType(mappedType) }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                color = if (active) Color.White else Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("All time", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Date Grouped Passbook Items List (Matches Image 2)
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
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (dateGroup, txns) ->
                    item {
                        Text(
                            text = dateGroup,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5E00),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }

                    items(txns) { txn ->
                        PassbookTransactionRow(
                            txn = txn,
                            onDelete = { viewModel.deleteTransaction(txn) },
                            onSetAutoPay = { viewModel.convertToAutoPay(txn, context, preferenceManager) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PassbookTransactionRow(
    txn: Transaction,
    onDelete: () -> Unit,
    onSetAutoPay: () -> Unit = {}
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
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
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF7600), Color(0xFFFF9E40))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = txn.merchant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${txn.bankName} • $formattedDate",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (txn.isAutoPay) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("✔ AutoPay Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF0EA))
                                .clickable { onSetAutoPay() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+ Set AutoPay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5E00))
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${if (txn.transactionType == "CREDIT") "+" else "-"}₹${txn.amount.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (txn.transactionType == "CREDIT") Color(0xFF10B981) else Color(0xFFDC2626)
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0F2))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete transaction",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


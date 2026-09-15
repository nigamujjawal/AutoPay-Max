package com.autopaymax.ui.dashboard

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.data.local.entity.Mandate
import com.autopaymax.ui.autopay.MandateViewModel
import com.autopaymax.ui.theme.NavyPrimary
import com.autopaymax.ui.theme.NavySecondary
import com.autopaymax.ui.theme.PremiumNavyGradient
import com.autopaymax.util.matchAutoPayApp
import com.autopaymax.util.merchantInitials
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    mandateViewModel: MandateViewModel,
    currencySymbol: String = "$",
    onNotificationsClick: () -> Unit = {},
    onMandateClick: (Long) -> Unit = {},
    onAddSubscriptionClick: () -> Unit = {},
    onSelectApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mandates by mandateViewModel.mandates.collectAsState()
    val showMailSyncPrompt by mandateViewModel.showMailSyncPrompt.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var merchantName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("Manual") }

    val activeMandates = mandates.filter { it.status == "ACTIVE" }
    val totalMonthlyCost = activeMandates.sumOf { it.amount }
    val activeCount = activeMandates.size

    val displayTotal = totalMonthlyCost.toInt()
    val displayCount = activeCount

    AppStorys.getScreenCampaigns("DashboardScreen", listOf())
    Log.d("AppStorys", "Active AppStorys userId: ${AppStorys.getUserId()}")

    Box(
        modifier = Modifier
            .appstorys("dashboard_container")
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header
            AutoPayTopHeader(onNotificationsClick = onNotificationsClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Hero Card - Total Monthly Spending (Premium Indigo Slate Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E3A8A),
                                    Color(0xFF2563EB)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    // Decorative background circles
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.85f, size.height * 0.25f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.width * 0.3f,
                            center = Offset(size.width * 0.95f, size.height * 0.85f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Total Monthly Spending",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol $displayTotal",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Across $displayCount autopays",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Section Header: Current Auto Payments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Auto Payments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Auto Payments List or Empty State Exploration View
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp)
            ) {
                if (mandates.isEmpty()) {
                    item {
                        EmptyStateStartExploring(
                            onConnectGmail = {
                                mandateViewModel.syncMailsOnce(context)
                                Toast.makeText(context, "Syncing your inbox…", Toast.LENGTH_SHORT).show()
                            },
                            onImportScreenshot = {
                                onAddSubscriptionClick()
                            },
                            onAddSubscription = onAddSubscriptionClick,
                            onQuickAdd = { name, _ ->
                                onSelectApp(name)
                            }
                        )
                    }
                } else {
                    items(mandates, key = { it.id }) { mandate ->
                        AutoPaymentRow(
                            mandate = mandate,
                            currencySymbol = currencySymbol,
                            onClick = { onMandateClick(mandate.id) }
                        )
                    }
                }
            }
        }

        // One-time "sync your inbox" prompt
        if (showMailSyncPrompt) {
            AlertDialog(
                onDismissRequest = { mandateViewModel.dismissMailSyncPrompt() },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Sync your inbox",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Text(
                        text = "Scan your Gmail once for subscription and autopay emails so your payments show up here automatically.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mandateViewModel.syncMailsOnce(context)
                            Toast.makeText(context, "Syncing your inbox…", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sync now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mandateViewModel.dismissMailSyncPrompt() }) {
                        Text("Not now", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // Add AutoPay Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Add AutoPay Mandate",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = merchantName,
                            onValueChange = { merchantName = it },
                            label = { Text("Service Name (e.g. Netflix)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavySecondary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Monthly Amount ($currencySymbol)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavySecondary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Manual", "Automatic").forEach { freq ->
                                val selected = selectedFrequency == freq
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) NavySecondary else Color(0xFFF1F5F9))
                                        .clickable { selectedFrequency = freq }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = freq,
                                        color = if (selected) Color.White else Color(0xFF64748B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (merchantName.isNotBlank() && amount > 0) {
                                val nextDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                                val mandate = Mandate(
                                    merchant = merchantName,
                                    amount = amount,
                                    frequency = selectedFrequency,
                                    nextExpectedDebit = nextDate,
                                    bank = "HDFC Bank",
                                    status = "ACTIVE",
                                    source = "MANUAL"
                                )
                                mandateViewModel.addMandate(mandate, context)
                                showAddDialog = false
                                merchantName = ""
                                amountText = ""
                                Toast.makeText(context, "AutoPay added successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save AutoPay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

// ==========================================
// EMPTY STATE - START EXPLORING VIEW (SC 6)
// ==========================================
@Composable
private fun EmptyStateStartExploring(
    onConnectGmail: () -> Unit,
    onImportScreenshot: () -> Unit,
    onAddSubscription: () -> Unit,
    onQuickAdd: (String, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stacked Cards Graphic Header
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .offset(x = (-32).dp, y = (-10).dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE50914)),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .offset(x = (-10).dp, y = 0.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1DB954)),
                contentAlignment = Alignment.Center
            ) {
                Text("≈", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .offset(x = 10.dp, y = (-5).dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Text("D", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .offset(x = 32.dp, y = 10.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0066CC)),
                contentAlignment = Alignment.Center
            ) {
                Text("1P", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Start exploring",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add your first subscription to begin your\njourney.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Stack
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Button 1: Connect to Gmail
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onConnectGmail() },
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = "Gmail",
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Connect to Gmail",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Button 2: Import from screenshot
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onImportScreenshot() },
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Screenshot",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Import from screenshot",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Button 3: Add subscription
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onAddSubscription() },
                color = Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add subscription",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Popular Subscriptions Quick Add Cards Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                QuickAddCard(
                    name = "Netflix",
                    price = "$15.49",
                    bgColor = Color(0xFF0F172A),
                    logoChar = "N",
                    logoColor = Color(0xFFE50914),
                    onAdd = { onQuickAdd("Netflix", 15.49) }
                )
            }
            item {
                QuickAddCard(
                    name = "Amazon Prime",
                    price = "$14.99",
                    bgColor = Color(0xFF00A8E1),
                    logoChar = "P",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("Amazon Prime", 14.99) }
                )
            }
            item {
                QuickAddCard(
                    name = "Spotify",
                    price = "$10.99",
                    bgColor = Color(0xFF1DB954),
                    logoChar = "S",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("Spotify", 10.99) }
                )
            }
            item {
                QuickAddCard(
                    name = "Disney+",
                    price = "$13.99",
                    bgColor = Color(0xFF113CCF),
                    logoChar = "D",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("Disney+", 13.99) }
                )
            }
            item {
                QuickAddCard(
                    name = "YouTube",
                    price = "$13.99",
                    bgColor = Color(0xFFFF0000),
                    logoChar = "Y",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("YouTube", 13.99) }
                )
            }
            item {
                QuickAddCard(
                    name = "ChatGPT",
                    price = "$20.00",
                    bgColor = Color(0xFF10A37F),
                    logoChar = "O",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("ChatGPT", 20.00) }
                )
            }
            item {
                QuickAddCard(
                    name = "Claude AI",
                    price = "$20.00",
                    bgColor = Color(0xFFD97757),
                    logoChar = "C",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("Claude AI", 20.00) }
                )
            }
            item {
                QuickAddCard(
                    name = "Apple / iCloud",
                    price = "$9.99",
                    bgColor = Color(0xFF555555),
                    logoChar = "A",
                    logoColor = Color.White,
                    onAdd = { onQuickAdd("Apple / iCloud", 9.99) }
                )
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    name: String,
    price: String,
    bgColor: Color,
    logoChar: String,
    logoColor: Color,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(115.dp)
            .height(154.dp),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(logoChar, color = logoColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = price,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AutoPaymentRow(
    mandate: Mandate,
    currencySymbol: String = "$",
    onClick: () -> Unit = {}
) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dueDateStr = df.format(Date(mandate.nextExpectedDebit))
    val isActive = mandate.status == "ACTIVE"

    val daysUntilDue = remember(mandate.nextExpectedDebit) {
        val diff = mandate.nextExpectedDebit - System.currentTimeMillis()
        (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
    }

    val initials = remember(mandate.merchant) { merchantInitials(mandate.merchant) }
    val matchedApp = remember(mandate.merchant) { matchAutoPayApp(mandate.merchant) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .then(
                            if (matchedApp != null) {
                                Modifier.background(matchedApp.color)
                            } else {
                                Modifier.background(PremiumNavyGradient)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (matchedApp != null) {
                        Icon(
                            painter = painterResource(id = matchedApp.iconRes),
                            contentDescription = matchedApp.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = mandate.merchant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dueDateStr • ${mandate.frequency}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$currencySymbol${mandate.amount.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDC2626)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Transparent)
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = if (isActive)
                            "Due in $daysUntilDue ${if (daysUntilDue == 1L) "day" else "days"}"
                        else "Cancelled",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) NavySecondary else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoPayTopHeader(
    onNotificationsClick: () -> Unit = {}
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title + Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                    contentDescription = "",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "AutoPay Max",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            // Right Icons (Bell with dot)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clickable { onNotificationsClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NavySecondary)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

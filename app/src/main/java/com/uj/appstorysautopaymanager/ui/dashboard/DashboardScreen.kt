package com.uj.appstorysautopaymanager.ui.dashboard

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.ui.autopay.MandateViewModel
import com.uj.appstorysautopaymanager.util.matchAutoPayApp
import com.uj.appstorysautopaymanager.util.merchantInitials
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    mandateViewModel: MandateViewModel,
    currencySymbol: String = "₹",
    onNotificationsClick: () -> Unit = {},
    onMandateClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val mandates by mandateViewModel.mandates.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var merchantName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("Manual") }

    val activeMandates = mandates.filter { it.status == "ACTIVE" }
    val totalMonthlyCost = activeMandates.sumOf { it.amount }
    val activeCount = activeMandates.size

    val displayTotal = totalMonthlyCost.toInt()
    val displayCount = activeCount

    AppStorys.getScreenCampaigns("DashboardScreen",listOf())
    // TEMPORARY - remove once test-user capture is confirmed working. setUserId() assigns
    // internally inside a launched coroutine, so this is only reliable read well after login
    // (which is true here - Dashboard is only reached post-login). Whatever this logs is the
    // exact string that must be registered as the Test User ID on the dashboard.
    Log.d("AppStorys", "Active AppStorys userId: ${AppStorys.getUserId()}")

    Box(
        modifier = Modifier
            .appstorys("dashboard_container")
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header (Matches Image 1 & 2)
            AutoPayTopHeader(onNotificationsClick = onNotificationsClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Hero Card - Total Monthly Spending (Matches Image 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                    // Decorative background circles
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Total Monthly",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol $displayTotal",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Across $displayCount autopays",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Section Header: Current Auto Payments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp, start = 5.dp, end = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Auto Payments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // Auto Payments List — only this part scrolls
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 50.dp)
            ) {
                if (mandates.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No auto payments set up yet.",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
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

        // Add AutoPay Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Add AutoPay Mandate",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
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
                                focusedBorderColor = Color(0xFFFF5E00),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Monthly Amount ($currencySymbol)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF5E00),
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFFF5E00) else Color(0xFFF1F5F9))
                                        .clickable { selectedFrequency = freq }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = freq,
                                        color = if (selected) Color.White else Color(0xFF64748B),
                                        fontSize = 12.sp,
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
                                    status = "ACTIVE"
                                )
                                mandateViewModel.addMandate(mandate, context)
                                showAddDialog = false
                                merchantName = ""
                                amountText = ""
                                Toast.makeText(context, "AutoPay added successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00))
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

@Composable
fun AutoPaymentRow(
    mandate: Mandate,
    currencySymbol: String = "₹",
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

    // High-frequency autopay merchants (streaming/telecom/food/insurance/investing) get a
    // recognizable brand-colored badge; anything else falls back to the generic gradient+initials.
    val matchedApp = remember(mandate.merchant) { matchAutoPayApp(mandate.merchant) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
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
                            .then(
                                if (matchedApp != null) {
                                    Modifier.background(matchedApp.color)
                                } else {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFFF7600).copy(alpha = 0.3f), Color(0xFFFF9E40).copy(alpha = 1.0f))
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (matchedApp != null) {
                            Icon(
                                painter = painterResource(id = matchedApp.iconRes),
                                contentDescription = matchedApp.displayName,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = mandate.merchant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$dueDateStr • ${mandate.frequency}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$currencySymbol${mandate.amount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color(0xFFFF5E00) else Color(0xFF94A3B8)
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
                    painter = painterResource(id = com.uj.appstorysautopaymanager.R.drawable.appicon),
                    contentDescription = "",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

//                AutoPayLogoIcon()
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "AutoPay Max",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5E00)
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
                            .background(Color(0xFFFF5E00))
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoPayLogoIcon() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFFFF5E00), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val strokeWidth = 2.dp.toPx()
            val orange = Color(0xFFFF5E00)
            val w = size.width
            val h = size.height

            // Binder rings
            drawLine(orange, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h * 0.2f), strokeWidth)
            drawLine(orange, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h * 0.2f), strokeWidth)

            // Outer calendar box
            drawRoundRect(
                color = orange,
                topLeft = Offset(0f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.85f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )

            // Header dividing line
            drawLine(orange, Offset(0f, h * 0.4f), Offset(w, h * 0.4f), strokeWidth)

            // Grid dots inside calendar
            drawCircle(orange, radius = 1.5.dp.toPx(), center = Offset(w * 0.28f, h * 0.6f))
            drawCircle(orange, radius = 1.5.dp.toPx(), center = Offset(w * 0.55f, h * 0.6f))
            drawCircle(orange, radius = 1.5.dp.toPx(), center = Offset(w * 0.28f, h * 0.8f))
        }

        // ₹ coin badge at bottom-right of logo
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF5E00)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "₹",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


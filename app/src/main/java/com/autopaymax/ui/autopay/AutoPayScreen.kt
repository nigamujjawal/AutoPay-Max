package com.autopaymax.ui.autopay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.appversal.appstorys.AppStorys
import com.autopaymax.ui.navigation.Screen
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.data.local.entity.Mandate
import com.autopaymax.util.frequencyOffsetMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val PaymentApps = listOf("GPay", "PhonePe", "Paytm", "Amazon Pay", "Other")
private val Frequencies = listOf("Weekly", "Monthly", "Quarterly", "Yearly")

private val DarkTitleColor = Color(0xFF0F172A)
private val SoftSubtextColor = Color(0xFF64748B)
private val LightBorderColor = Color(0xFFE2E8F0)
private val DarkNavyButton = Color(0xFF1E3A8A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPayScreen(
    viewModel: MandateViewModel,
    navController: NavController,
    currencySymbol: String = "₹",
    prefillMerchant: String? = null
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    var merchant by remember { mutableStateOf(prefillMerchant.orEmpty()) }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    var nextDueDate by remember { mutableLongStateOf(System.currentTimeMillis() + frequencyOffsetMillis("Monthly")) }
    var userPickedDate by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var paymentApp by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(frequency) {
        if (!userPickedDate) {
            nextDueDate = System.currentTimeMillis() + frequencyOffsetMillis(frequency)
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isValid = merchant.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0

    AppStorys.getScreenCampaigns("autopay_screen", listOf())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .appstorys("autopay_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = DarkTitleColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Add Autopay",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTitleColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manually track an autopay we couldn't detect from your SMS.",
                fontSize = 14.sp,
                color = SoftSubtextColor,
                lineHeight = 20.sp
            )

            val matchedApp = remember(merchant) { com.autopaymax.util.matchAutoPayApp(merchant) }
            if (matchedApp != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(matchedApp.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = matchedApp.iconRes),
                            contentDescription = matchedApp.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = matchedApp.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Name Field
            AutoPayField(
                value = merchant,
                onValueChange = { merchant = it },
                placeholder = "App Name"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Field
            AutoPayField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "Amount ($currencySymbol)",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Frequency Label & Pills
            FieldLabel("Frequency")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Frequencies.forEach { freq ->
                    FrequencyChip(
                        label = freq,
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Next Due Date
            FieldLabel("Next due date")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, LightBorderColor, RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(nextDueDate)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTitleColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category (optional)
            FieldLabel("Category (optional)", muted = true)
            Spacer(modifier = Modifier.height(10.dp))
            AutoPayDropdown(
                selected = category,
                placeholder = "Category (optional)",
                options = categories.map { it.name },
                onSelect = { category = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Payment App (optional)
            FieldLabel("Payment App (optional)", muted = true)
            Spacer(modifier = Modifier.height(10.dp))
            AutoPayDropdown(
                selected = paymentApp,
                placeholder = "Payment App (optional)",
                options = PaymentApps,
                onSelect = { paymentApp = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save Autopay Button
            Button(
                onClick = {
                    viewModel.addMandate(
                        Mandate(
                            merchant = merchant,
                            amount = amountText.toDouble(),
                            frequency = frequency,
                            nextExpectedDebit = nextDueDate,
                            bank = "",
                            status = "ACTIVE",
                            category = category,
                            paymentApp = paymentApp,
                            source = "MANUAL"
                        ),
                        context
                    )
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkNavyButton,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFF1F5F9),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Text(
                    text = "Save Autopay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val initialUtcMillis = remember(nextDueDate) {
            val local = Calendar.getInstance().apply { timeInMillis = nextDueDate }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        nextDueDate = Calendar.getInstance().apply {
                            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                        userPickedDate = true
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = DarkNavyButton, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = SoftSubtextColor, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Select date", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = DarkNavyButton,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = DarkNavyButton,
                    todayContentColor = DarkNavyButton
                )
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String, muted: Boolean = false) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = if (muted) FontWeight.Medium else FontWeight.Bold,
        color = DarkTitleColor
    )
}

@Composable
private fun AutoPayField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF94A3B8), fontSize = 15.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedIndicatorColor = LightBorderColor,
            focusedIndicatorColor = DarkNavyButton,
            focusedTextColor = DarkTitleColor,
            unfocusedTextColor = DarkTitleColor
        )
    )
}

@Composable
private fun FrequencyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) DarkNavyButton else Color.White)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = LightBorderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else DarkTitleColor
        )
    }
}

@Composable
private fun AutoPayDropdown(
    selected: String,
    placeholder: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, LightBorderColor, RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.ifBlank { placeholder },
                    fontSize = 15.sp,
                    color = if (selected.isBlank()) Color(0xFF94A3B8) else DarkTitleColor
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SoftSubtextColor)
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, LightBorderColor, RoundedCornerShape(14.dp))
            ) {
                options.forEach { option ->
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        color = DarkTitleColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                expanded = false
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}


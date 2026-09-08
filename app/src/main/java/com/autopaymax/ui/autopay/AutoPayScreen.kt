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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val OrangeAccent = Color(0xFFFF5E00)
private val FieldBackground = Color(0xFFF7F7FA)
private val LabelDark = Color(0xFF1E293B)
private val LabelMuted = Color(0xFF64748B)
private val Placeholder = Color(0xFFAEAEBE)

private val PaymentApps = listOf("GPay", "PhonePe", "Paytm", "Amazon Pay", "Other")
private val Frequencies = listOf("Weekly", "Monthly", "Quarterly", "Yearly")

private fun frequencyOffsetMillis(frequency: String): Long {
    val days = when (frequency) {
        "Weekly" -> 7L
        "Monthly" -> 30L
        "Quarterly" -> 90L
        "Yearly" -> 365L
        else -> 30L
    }
    return days * 24 * 60 * 60 * 1000
}

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
    // Once the user has actually picked a date themselves, switching frequency shouldn't silently
    // discard that choice - only the auto-computed placeholder should track frequency.
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

    AppStorys.getScreenCampaigns("autopay_screen",listOf())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .appstorys("autopay_screen")
    ) {
        // Orange header
        Surface(color = OrangeAccent, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Add Autopay",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Fill in the subscription details below.",
                fontSize = 14.sp,
                color = LabelMuted
            )

            val matchedApp = remember(merchant) { com.autopaymax.util.matchAutoPayApp(merchant) }
            if (matchedApp != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                    Text(matchedApp.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LabelDark)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            AutoPayField(
                value = merchant,
                onValueChange = { merchant = it },
                placeholder = "Merchant"
            )

            Spacer(modifier = Modifier.height(14.dp))
            AutoPayField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = "Amount ($currencySymbol)",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(15.dp))
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

            Spacer(modifier = Modifier.height(24.dp))
            FieldLabel("Next due date")
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FieldBackground)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(nextDueDate)),
                    fontSize = 15.sp,
                    color = LabelDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            FieldLabel("Category (optional)", muted = true)
            Spacer(modifier = Modifier.height(10.dp))
            AutoPayDropdown(
                selected = category,
                options = categories.map { it.name },
                onSelect = { category = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
            FieldLabel("Payment App (optional)", muted = true)
            Spacer(modifier = Modifier.height(10.dp))
            AutoPayDropdown(
                selected = paymentApp,
                options = PaymentApps,
                onSelect = { paymentApp = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
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
                    // Back past the tile picker straight to Home.
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFF1F1F5),
                    disabledContentColor = Color(0xFFAEAEBE)
                )
            ) {
                Text("Save Autopay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    Text("OK", color = OrangeAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = OrangeAccent, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Select date", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = OrangeAccent,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = OrangeAccent,
                    todayContentColor = OrangeAccent
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
        color = if (muted) LabelMuted else LabelDark
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
        placeholder = { Text(placeholder, color = Placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = FieldBackground,
            focusedContainerColor = FieldBackground,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            focusedTextColor = LabelDark,
            unfocusedTextColor = LabelDark
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
            .background(if (selected) OrangeAccent else FieldBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else LabelMuted
        )
    }
}

@Composable
private fun AutoPayDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    // Not ExposedDropdownMenu: that renders in a separately-positioned Popup that picks above vs.
    // below the field based on available screen space, and flips to "above" the moment there's
    // less room below than above - which is routinely the case for a field this far down a form.
    // A plain inline expand is just the next sibling in this Column, so it can only ever appear
    // below the field, no popup-positioning heuristics to fight.
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FieldBackground)
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.ifBlank { "Select" },
                    fontSize = 15.sp,
                    color = if (selected.isBlank()) Placeholder else LabelDark
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = LabelMuted)
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .widthIn(min = 140.dp)
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, FieldBackground, RoundedCornerShape(14.dp))
            ) {
                options.forEach { option ->
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        color = LabelDark,
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

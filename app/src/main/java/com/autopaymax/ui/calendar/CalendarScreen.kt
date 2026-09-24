package com.autopaymax.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autopaymax.ui.autopay.MandateViewModel
import com.autopaymax.ui.components.PremiumNormalCard
import com.autopaymax.ui.dashboard.AutoPaymentRow
import com.autopaymax.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Midnight (device-local) for [millis] - the key mandates are grouped by day on. */
private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun CalendarScreen(
    mandateViewModel: MandateViewModel,
    currencySymbol: String = "$",
    onBack: () -> Unit = {},
    onMandateClick: (Long) -> Unit = {}
) {
    val mandates by mandateViewModel.mandates.collectAsState()
    val activeMandates = remember(mandates) { mandates.filter { it.status == "ACTIVE" } }
    val mandatesByDay = remember(activeMandates) {
        activeMandates.groupBy { startOfDay(it.nextExpectedDebit) }
    }

    val today = remember { startOfDay(System.currentTimeMillis()) }
    // Closest upcoming due date, not an overdue one - that's what "next autopay" means.
    val nextAutoPayDay = remember(mandatesByDay) {
        mandatesByDay.keys.filter { it >= today }.minOrNull()
    }

    val initialCal = remember { Calendar.getInstance().apply { timeInMillis = nextAutoPayDay ?: today } }
    var visibleYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var visibleMonthIdx by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(nextAutoPayDay) }

    val monthLabel = remember(visibleYear, visibleMonthIdx) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(Calendar.getInstance().apply { set(visibleYear, visibleMonthIdx, 1) }.time)
    }
    val daysInMonth = remember(visibleYear, visibleMonthIdx) {
        Calendar.getInstance().apply { set(visibleYear, visibleMonthIdx, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val leadingBlanks = remember(visibleYear, visibleMonthIdx) {
        Calendar.getInstance().apply { set(visibleYear, visibleMonthIdx, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    }
    val cells = remember(daysInMonth, leadingBlanks) {
        val days = List<Int?>(leadingBlanks) { null } + (1..daysInMonth).toList()
        days + List((7 - days.size % 7) % 7) { null }
    }
    fun dayMillis(day: Int) = Calendar.getInstance().apply {
        set(visibleYear, visibleMonthIdx, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "AutoPay Calendar",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(colors = listOf(NavyAccent.copy(alpha = 0.45f), NavyPrimary.copy(alpha = 0.35f))))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (visibleMonthIdx == 0) { visibleMonthIdx = 11; visibleYear-- } else visibleMonthIdx--
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = TextWhite)
                    }
                    Text(monthLabel, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    IconButton(onClick = {
                        if (visibleMonthIdx == 11) { visibleMonthIdx = 0; visibleYear++ } else visibleMonthIdx++
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = TextWhite)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGray
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cells.chunked(7).forEachIndexed { index, week ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                if (day == null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val millis = dayMillis(day)
                                    DayTile(
                                        day = day,
                                        isToday = millis == today,
                                        isNextAutoPay = millis == nextAutoPayDay,
                                        isSelected = millis == selectedDay,
                                        hasMandates = mandatesByDay.containsKey(millis),
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedDay = millis }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(4.dp))

            // Focused box - the day tapped (or the next autopay day, by default).
            selectedDay?.let { day ->
                val dueMandates = mandatesByDay[day].orEmpty()
                val dateLabel = remember(day) {
                    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(day))
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateLabel, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                        if (day == nextAutoPayDay) {
                            Text("Next autopay", style = MaterialTheme.typography.labelLarge, color = NavySecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (dueMandates.isEmpty()) {
                        PremiumNormalCard {
                            Text(
                                text = "No autopay scheduled on this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            dueMandates.forEach { mandate ->
                                AutoPaymentRow(
                                    mandate = mandate,
                                    currencySymbol = currencySymbol,
                                    onClick = { onMandateClick(mandate.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DayTile(
    day: Int,
    isToday: Boolean,
    isNextAutoPay: Boolean,
    isSelected: Boolean,
    hasMandates: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isNextAutoPay) NavySecondary else Color.Transparent)
                .border(
                    width = if (isNextAutoPay) 0.dp else if (isSelected) 2.dp else if (isToday) 1.dp else 0.dp,
                    color = if (isSelected) NavySecondary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                color = if (isNextAutoPay) Color.White else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isNextAutoPay || isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasMandates) (if (isNextAutoPay) NavySecondary else StatusActive) else Color.Transparent)
        )
    }
}

package com.uj.appstorysautopaymanager.ui.bill

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.ui.components.*
import com.uj.appstorysautopaymanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillManagerScreen(
    viewModel: BillViewModel,
    currencySymbol: String = "₹"
) {
    val bills by viewModel.filteredBills.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBillForEdit by remember { mutableStateOf<Bill?>(null) }

    AppStorys.getScreenCampaigns("bill_manager_screen",listOf())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryIndigo,
                contentColor = TextWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .appstorys("bill_manager_screen")
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            AppHeader(
                title = "Bill Manager",
                subtitle = "Schedule alerts & repeat patterns"
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search bills...", color = TextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderColor
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories LazyRow
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val selected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) PrimaryIndigo else CardBackground)
                            .clickable { viewModel.setSelectedCategory(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (selected) TextWhite else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by: $sortBy", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Due Date", "Amount", "Title").forEach { opt ->
                        val active = opt == sortBy
                        Text(
                            text = opt,
                            color = if (active) AccentTeal else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.setSortBy(opt) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bills List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (bills.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No bills found. Tap '+' to add one.", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(bills) { bill ->
                        BillRow(
                            bill = bill,
                            currencySymbol = currencySymbol,
                            onMarkPaid = { viewModel.markPaid(bill) },
                            onDuplicate = { viewModel.duplicateBill(bill) },
                            onDelete = { viewModel.deleteBill(bill) },
                            onEdit = { selectedBillForEdit = bill }
                        )
                    }
                }
            }
        }

        // Add Bill Dialog
        if (showAddDialog) {
            BillDialog(
                onDismiss = { showAddDialog = false },
                onSave = { bill ->
                    viewModel.addBill(bill)
                    showAddDialog = false
                }
            )
        }

        // Edit Bill Dialog
        if (selectedBillForEdit != null) {
            BillDialog(
                bill = selectedBillForEdit,
                onDismiss = { selectedBillForEdit = null },
                onSave = { updated ->
                    viewModel.updateBill(updated)
                    selectedBillForEdit = null
                }
            )
        }
    }
}

@Composable
fun BillRow(
    bill: Bill,
    currencySymbol: String = "₹",
    onMarkPaid: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dueDateStr = df.format(Date(bill.dueDate))

    PremiumNormalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = bill.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Due: $dueDateStr • Repeats: ${bill.repeatType}",
                    fontSize = 12.sp,
                    color = TextGray
                )
                if (bill.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = bill.notes,
                        fontSize = 11.sp,
                        color = TextGray.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol%,.2f".format(bill.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onMarkPaid,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (bill.status == "PAID") Icons.Default.Undo else Icons.Default.Check,
                            contentDescription = "Mark Paid/Unpaid",
                            tint = if (bill.status == "PAID") TextGray else AccentEmerald
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = AccentTeal
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryIndigo
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDialog(
    bill: Bill? = null,
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(bill?.title ?: "") }
    var amount by remember { mutableStateOf(bill?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(bill?.notes ?: "") }
    var repeatType by remember { mutableStateOf(bill?.repeatType ?: "MONTHLY") }
    var category by remember { mutableStateOf(bill?.category ?: "OTT") }
    var reminderDays by remember { mutableStateOf(bill?.reminderDays ?: 1) }
    var selectedDate by remember { mutableStateOf(bill?.dueDate ?: System.currentTimeMillis()) }

    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (bill == null) "Add New Bill" else "Edit Bill", color = TextWhite) },
        containerColor = CardBackground,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderColor)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d)
                                    }
                                    selectedDate = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Due Date: ", color = TextGray)
                    Text(formattedDate, color = TextWhite, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderColor)
                )

                // Category & Repeat dropdown elements simplified as Text buttons for design cleanliness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Category", color = TextGray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("OTT", "EMI", "Mobile").forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (category == cat) PrimaryIndigo else BorderColor)
                                        .clickable { category = cat }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(cat, color = TextWhite, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Frequency", color = TextGray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("NONE", "MONTHLY", "YEARLY").forEach { freq ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (repeatType == freq) PrimaryIndigo else BorderColor)
                                        .clickable { repeatType = freq }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(freq, color = TextWhite, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && amountVal > 0) {
                        onSave(
                            Bill(
                                id = bill?.id ?: 0,
                                title = title,
                                amount = amountVal,
                                dueDate = selectedDate,
                                repeatType = repeatType,
                                reminderDays = reminderDays,
                                notes = note,
                                category = category,
                                status = bill?.status ?: "PENDING",
                                isArchived = bill?.isArchived ?: false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("Save", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

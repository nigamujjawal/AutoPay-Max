package com.autopaymax.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material3.Text
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.data.local.entity.NotificationEntity
import com.autopaymax.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class NotificationItem(
    val id: Long,
    val title: String,
    val body: String,
    val timestamp: String,
    val category: String, // "Payments" or "System"
    val dateGroup: String, // "Today", "Yesterday", "Earlier"
    val isWarning: Boolean = false,
    val isUnread: Boolean = true
)

private fun NotificationEntity.toItem(): NotificationItem {
    val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val todayStr = dayFormat.format(Date())
    val yesterdayStr = dayFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
    val entryDayStr = dayFormat.format(Date(timestamp))
    val dateGroup = when (entryDayStr) {
        todayStr -> "Today"
        yesterdayStr -> "Yesterday"
        else -> "Earlier"
    }
    return NotificationItem(
        id = id,
        title = title,
        body = body,
        timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp)),
        category = category,
        dateGroup = dateGroup,
        isWarning = isWarning,
        isUnread = isUnread
    )
}

@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val notifications by viewModel.notifications.collectAsState()
    val items = remember(notifications) { notifications.map { it.toItem() } }

    val unreadCount = items.count { it.isUnread }

    val filteredList = remember(selectedFilter, items) {
        if (selectedFilter == "All") items else items.filter { it.category == selectedFilter }
    }

    val groupedList = remember(filteredList) {
        filteredList.groupBy { it.dateGroup }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .appstorys("notifications_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Back Button, Title + Badge, and Mark all read
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite
                            )

                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Mark all read",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryIndigo,
                        modifier = Modifier.clickable { viewModel.markAllRead() }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
            ) {
                // Filter Tabs (All | Payments | System)
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Payments", "System").forEach { tab ->
                            val active = selectedFilter == tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(1.dp, if (active) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable { selectedFilter = tab }
                                    .padding(horizontal = 22.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (active) Color.White else TextGray,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Date Grouped Notifications
                if (groupedList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No notifications found.", color = TextGray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    groupedList.forEach { (dateGroup, items) ->
                        item {
                            Text(
                                text = dateGroup.uppercase(),
                                style = InstrumentLabel,
                                color = TextGray,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }

                        items(items) { item ->
                            NotificationCardRow(
                                item = item,
                                onDismiss = { viewModel.dismiss(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardRow(
    item: NotificationItem,
    onDismiss: () -> Unit
) {
    // isWarning is "needs a look", not a hard failure — amber (pending),
    // not red (overdue). A confirmed/info notification reads steady green.
    val statusColor = if (item.isWarning) StatusPending else StatusActive
    val cardBg = if (item.isWarning) StatusPending.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circle Icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.isWarning) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )

                    Column (
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        if (item.isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo)
                            )
                        }

                        Text(
                            text = item.timestamp,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

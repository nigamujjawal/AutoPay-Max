package com.uj.appstorysautopaymanager.ui.notification

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material3.Text
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.data.local.entity.NotificationEntity
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
            .background(Color.White)
            .appstorys("notifications_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Back Button, Title + Badge, and Mark all read (Matches Screenshot 2)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )

                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5E00)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5E00),
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
                // Filter Tabs (All | Payments | System) (Matches Screenshot 2)
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
                                    .background(if (active) Color(0xFFFF5E00) else Color.White)
                                    .border(2.dp, if (active) Color(0xFFFF5E00) else Color(0xFFFFF8F0), CircleShape)
                                    .clickable { selectedFilter = tab }
                                    .padding(horizontal = 22.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (active) Color.White else Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
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
                            Text("No notifications found.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        }
                    }
                } else {
                    groupedList.forEach { (dateGroup, items) ->
                        item {
                            Text(
                                text = dateGroup,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5E00),
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
    val cardBg = if (item.isWarning) Color(0xFFFFF8F0) else Color.White
    val cardBorder = if (item.isWarning) Color(0xFFFFE8D6) else Color(0xFFF1F5F9)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
//            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
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
                    .background(if (item.isWarning) Color(0xFFFFEAD5) else Color(0xFFE8F8EE)),
                contentAlignment = Alignment.Center
            ) {
                if (item.isWarning) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = Color(0xFFFF5E00),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
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
                                    .background(Color(0xFFFF5E00))
                            )
                        }

                        Text(
                            text = item.timestamp,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
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
                        lineHeight = 15.sp,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

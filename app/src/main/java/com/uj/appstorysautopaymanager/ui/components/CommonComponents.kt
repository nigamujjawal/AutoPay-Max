package com.uj.appstorysautopaymanager.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun PremiumGradientCard(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(PrimaryIndigo, SecondaryPurple),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(colors))
            .padding(24.dp),
        content = content
    )
}

@Composable
fun PremiumNormalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    }

    Column(
        modifier = cardModifier,
        content = content
    )
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status.uppercase()) {
        "PAID" -> AccentEmerald to "Paid"
        "PENDING" -> Color(0xFFF59E0B) to "Pending"
        "OVERDUE" -> AccentCoral to "Overdue"
        "ACTIVE" -> AccentTeal to "Active"
        else -> TextGray to status
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Not built on Material3's SwipeToDismissBox or AnchoredDraggableState. Both were tried first:
// SwipeToDismissBox hardcodes an internal velocity threshold of 125dp/s (not exposed on its
// public API), which any normal swipe clears. Building directly on AnchoredDraggableState to
// raise that bar looked right but had a sharper bug: its live currentValue flips to the "delete"
// anchor via a crude "past the halfway point between anchors" check that ignores
// positionalThreshold/velocityThreshold entirely - and the real settle-on-release decision
// (computeTarget) measures its threshold *relative to currentValue*, so once that live flip has
// already happened, the "80% threshold" ends up measured backwards (from the delete anchor back
// toward settled), not forward from the start. Net effect either way: any swipe past ~50% deletes.
// This version is a plain Modifier.draggable with one explicit decision made once, at release,
// off the raw offset and velocity - no hidden intermediate state to get out of sync with.
@Composable
fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFEF4444),
    iconTint: Color = Color.White,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    // ponytail: 1200dp/s is a reasoned "fast flick" bar (a normal deliberate swipe rarely nears
    // it), tune higher/lower if it still feels too easy/too strict to trigger.
    val velocityThresholdPxPerSec = with(density) { 1200.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(0f, rowWidthPx)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = iconTint
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> rowWidthPx = size.width.toFloat() }
                .offset { IntOffset(x = offsetX.roundToInt(), y = 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val distanceThresholdPx = rowWidthPx * 0.8f
                        val shouldDelete =
                            offsetX >= distanceThresholdPx || velocity >= velocityThresholdPxPerSec
                        if (shouldDelete) {
                            animate(offsetX, rowWidthPx, initialVelocity = velocity) { value, _ ->
                                offsetX = value
                            }
                            onDelete()
                        } else {
                            animate(offsetX, 0f, initialVelocity = velocity) { value, _ ->
                                offsetX = value
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
fun AppHeader(
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = TextGray
            )
        }
        action?.invoke()
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = PrimaryIndigo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CustomDatePicker(){
//    var showDatePicker by mutableStateOf(false)
//    val datePickerState = rememberDatePickerState()
//}
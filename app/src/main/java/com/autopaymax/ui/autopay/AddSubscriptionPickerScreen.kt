package com.autopaymax.ui.autopay

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopaymax.util.AutoPayApp
import com.autopaymax.util.KnownAutoPayApps

// Tap "Add new subscription" -> pick the app here -> AutoPayScreen opens pre-filled with that
// name. The last tile ("Not listed") opens the form blank for a manual entry.
//
// Deliberately NOT a LazyVerticalGrid: ~28 lightweight tiles fit a plain verticalScroll, which
// composes them once and then just translates on scroll - no lazy re-measure/recycle jank, and
// no per-item graphics layers.
@Composable
fun AddSubscriptionPickerScreen(
    onPick: (merchant: String?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F7))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
            }
            Text("Add new subscription", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        Text(
            "Pick the service, or choose “Not listed” to add it manually.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // null slot = the "Not listed" tile, appended after the catalog.
            (KnownAutoPayApps + null).chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { app ->
                        Box(Modifier.weight(1f)) {
                            if (app != null) AppTile(app) { onPick(app.displayName) }
                            else OtherTile { onPick(null) }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TileCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE6E8EF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun AppTile(app: AutoPayApp, onClick: () -> Unit) {
    TileCard(onClick = onClick) {
        IconBadge(bg = app.color) {
            Icon(
                painter = painterResource(id = app.iconRes),
                contentDescription = app.displayName,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        TileLabel(app.displayName)
    }
}

@Composable
private fun OtherTile(onClick: () -> Unit) {
    TileCard(onClick = onClick) {
        IconBadge(bg = Color(0xFFFFF0EA)) {
            Icon(Icons.Default.Add, contentDescription = "Not listed", tint = Color(0xFFFF5E00), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        TileLabel("Not listed")
    }
}

@Composable
private fun IconBadge(bg: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

@Composable
private fun TileLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1E293B),
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

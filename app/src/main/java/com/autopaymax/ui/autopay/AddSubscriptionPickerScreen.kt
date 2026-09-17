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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autopaymax.ui.theme.NavyPrimary
import com.autopaymax.ui.theme.PrimaryIndigo
import com.autopaymax.ui.theme.TextGray
import com.autopaymax.ui.theme.TextWhite
import com.autopaymax.util.AutoPayApp
import com.autopaymax.util.KnownAutoPayApps
import androidx.compose.material3.MaterialTheme

// Tap "Add new subscription" -> pick the app here -> AutoPayScreen opens pre-filled with that
// name. The last tile ("Not listed") opens the form blank for a manual entry.
//
// Deliberately NOT a LazyVerticalGrid: ~28 lightweight tiles fit a plain verticalScroll, which
// composes them once and then just translates on scroll - no lazy re-measure/recycle jank, and
// no per-item graphics layers.
@Composable
fun AddSubscriptionPickerScreen(
    onPick: (merchant: String?) -> Unit,
    onBack: () -> Unit,
    onImportScreenshot: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text("Add new subscription", style = MaterialTheme.typography.titleLarge, color = TextWhite)
        }

        Text(
            "Pick the service, or choose “Not listed” to add it manually.",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Always reachable here (unlike the Home empty-state version, this row shows up
            // whether or not the user already has mandates).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) { ScreenshotImportTile(onClick = onImportScreenshot) }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

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

// Fixed dark navy, like the Dashboard/Passbook hero cards - a deliberately darker blue than a
// theme-reactive tonal container, and the same fixed color in both app themes.
private val TileCardBackground = NavyPrimary

@Composable
private fun TileCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TileCardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
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
        IconBadge(bg = app.color, showAddBadge = true) {
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
        IconBadge(bg = PrimaryIndigo) {
            Icon(Icons.Default.Add, contentDescription = "Not listed", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        TileLabel("Add Custom")
    }
}

@Composable
private fun ScreenshotImportTile(onClick: () -> Unit) {
    TileCard(onClick = onClick) {
        IconBadge(bg = PrimaryIndigo) {
            Icon(Icons.Default.Image, contentDescription = "Import from screenshot", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        TileLabel("Import from screenshot")
    }
}

@Composable
private fun IconBadge(bg: Color, showAddBadge: Boolean = false, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(46.dp)) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
            content = { content() }
        )
        if (showAddBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(TileCardBackground)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun TileLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

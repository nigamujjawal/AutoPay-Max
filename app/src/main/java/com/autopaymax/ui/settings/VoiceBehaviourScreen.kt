package com.autopaymax.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autopaymax.data.local.pref.PreferenceManager
import com.autopaymax.tts.AnnouncementKind
import com.autopaymax.tts.TextToSpeechHelper
import com.autopaymax.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
private fun sliderFraction(state: SliderState): Float =
    (state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceBehaviour(
    navController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current

    val currentSpeechLanguage by settingsViewModel.speechLanguage.collectAsState()
    val voiceEngine by settingsViewModel.voiceEngine.collectAsState()
    val voiceVolume by settingsViewModel.voiceVolume.collectAsState()
    val speechSpeed by settingsViewModel.speechSpeed.collectAsState()
    val playChimeFirst by settingsViewModel.playChimeFirst.collectAsState()
    val selectedAlertTone by settingsViewModel.alertTone.collectAsState()

    var volumeValue by remember(voiceVolume) { mutableFloatStateOf(voiceVolume) }
    var speedValue by remember(speechSpeed) { mutableFloatStateOf(speechSpeed) }

    val languages = remember {
        listOf(
            "English" to "en", "Español" to "es", "Français" to "fr", "Deutsch" to "de",
            "Italiano" to "it", "Português" to "pt", "Nederlands" to "nl", "Русский" to "ru",
            "العربية" to "ar", "日本語" to "ja", "한국어" to "ko", "中文" to "zh"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header with Back Button
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
                    verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice & Behaviour",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
            ) {
                // Section 1: Voice & Language
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Voice & Language",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextWhite
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "LANGUAGE",
                                    style = InstrumentLabel,
                                    color = TextGray
                                )

                                // 2-column language grid
                                val rows = languages.chunked(2)
                                rows.forEach { rowLangs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowLangs.forEach { (displayName, code) ->
                                            val isSelected = currentSpeechLanguage == code
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                    .clickable { settingsViewModel.setSpeechLanguage(code) }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = displayName,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = if (isSelected) Color.White else TextWhite,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        if (rowLangs.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "VOICE ENGINE",
                                    style = InstrumentLabel,
                                    color = TextGray
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf("Male Voice", "Female Voice").forEach { engine ->
                                        val isSelected = voiceEngine == engine
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                .clickable { settingsViewModel.setVoiceEngine(engine) }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = engine,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (isSelected) Color.White else TextWhite
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Volume & Alert Customization
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Volume & Alert Customization",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextWhite
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                // Voice Volume
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Voice Volume",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("0", style = MaterialTheme.typography.bodySmall, color = TextGray, modifier = Modifier.width(34.dp))
                                        Slider(
                                            value = volumeValue,
                                            onValueChange = { volumeValue = it },
                                            onValueChangeFinished = { settingsViewModel.setVoiceVolume(volumeValue) },
                                            valueRange = 0f..100f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            thumb = {
                                                Spacer(
                                                    Modifier
                                                        .size(16.dp)
                                                        .background(PrimaryIndigo, CircleShape)
                                                )
                                            },
                                            track = { state ->
                                                val trackBg = MaterialTheme.colorScheme.outlineVariant
                                                Canvas(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                ) {
                                                    val centerY = size.height / 2f
                                                    val fraction = sliderFraction(state)
                                                    drawLine(
                                                        color = trackBg,
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                    drawLine(
                                                        color = NavyPrimary,
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width * fraction, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                        )
                                        Text("${volumeValue.toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextGray, modifier = Modifier.width(42.dp))
                                    }
                                }

                                // Speech Speed
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Speech Speed",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("0.5x", style = MaterialTheme.typography.bodySmall, color = TextGray, modifier = Modifier.width(34.dp))
                                        Slider(
                                            value = speedValue,
                                            onValueChange = { speedValue = it },
                                            onValueChangeFinished = { settingsViewModel.setSpeechSpeed(speedValue) },
                                            valueRange = 0.5f..2.0f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            thumb = {
                                                Spacer(
                                                    Modifier
                                                        .size(16.dp)
                                                        .background(PrimaryIndigo, CircleShape)
                                                )
                                            },
                                            track = { state ->
                                                val trackBg = MaterialTheme.colorScheme.outlineVariant
                                                Canvas(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                ) {
                                                    val centerY = size.height / 2f
                                                    val fraction = sliderFraction(state)
                                                    drawLine(
                                                        color = trackBg,
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                    drawLine(
                                                        color = NavyPrimary,
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width * fraction, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                        )
                                        Text(String.format("%.1fx", speedValue), style = MaterialTheme.typography.bodySmall, color = TextGray, modifier = Modifier.width(42.dp))
                                    }
                                }

                                // Play Chime Beep First
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Play Chime Beep First",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Plays a chime sound before the voice announcement.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }
                                    Switch(
                                        checked = playChimeFirst,
                                        onCheckedChange = { settingsViewModel.setPlayChimeFirst(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryIndigo,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Alert Behaviour
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Alert Behaviour",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextWhite
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf(
                                    "Cashier" to "Plays a classic cash registered sound",
                                    "Gun Shot" to "Plays a sharp attention-grabbing sound",
                                    "Temple Bell" to "Plays a traditional temple bell sound"
                                ).forEach { (title, subtitle) ->
                                    val isSelected = selectedAlertTone == title
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { settingsViewModel.setAlertTone(title) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextWhite
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextGray
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { settingsViewModel.setAlertTone(title) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = PrimaryIndigo,
                                                unselectedColor = PrimaryIndigo
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Diagnostics & Permissions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Diagnostics & Permissions",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextWhite
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Notification Access",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Required to capture payments from notifications.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(StatusActive.copy(alpha = 0.15f))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Enabled",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StatusActive
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val ttsHelper = TextToSpeechHelper(context, PreferenceManager(context))
                                        ttsHelper.speak(AnnouncementKind.AUTOPAY_DUE, "Netflix", 499)
                                        settingsViewModel.sendTestNotification(context)
                                        Toast.makeText(context, "Playing test alert tone!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = CircleShape
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Play Test Alert Tone",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

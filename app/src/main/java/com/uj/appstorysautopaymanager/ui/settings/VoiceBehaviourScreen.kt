package com.uj.appstorysautopaymanager.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.tts.AnnouncementKind
import com.uj.appstorysautopaymanager.tts.TextToSpeechHelper

// SliderState.value is the actual value within valueRange, not a 0..1 fraction -
// normalize it so the active-track width is correct for ranges that don't start at 0.
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

    // Paired with its Locale/TTS language code (what TextToSpeechHelper actually needs) - the
    // picker used to store the display string itself via setLanguage(), a different preference
    // TextToSpeechHelper never read, so picking a language here never changed the spoken voice.
    val languages = remember {
        listOf(
            "English" to "en", "Hindi (हिंदी)" to "hi", "Marathi (मराठी)" to "mr", "Gujarati (ગુજરાતી)" to "gu",
            "Tamil (தமிழ்)" to "ta", "Telugu (తెలుగు)" to "te", "Kannada (ಕನ್ನಡ)" to "kn", "Bengali (বাংলা)" to "bn",
            "Punjabi (ਪੰਜਾਬੀ)" to "pa"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header with Back Button (Matches Screenshot 1)
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
                    verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice & Behaviour",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Language",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
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
                                                    .background(if (isSelected) Color(0xFFFF5E00) else Color(0xFFF8FAFC))
                                                    .border(1.dp, if (isSelected) Color(0xFFFF5E00) else Color(0xFFF1F5F9), CircleShape)
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
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else Color(0xFF334155),
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
                                    text = "Voice Engine",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf("Female Voice", "Male Voice").forEach { engine ->
                                        val isSelected = voiceEngine == engine
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFFFF5E00) else Color(0xFFF8FAFC))
                                                .border(1.dp, if (isSelected) Color(0xFFFF5E00) else Color(0xFFF1F5F9), CircleShape)
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
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else Color(0xFF334155)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
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
                                            tint = Color(0xFFFF5E00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Voice Volume",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("0", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(34.dp))
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
                                                        .background(Color(0xFF0F172A), CircleShape)
                                                )
                                            },
                                            track = { state ->
                                                Canvas(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                ) {
                                                    val centerY = size.height / 2f
                                                    val fraction = sliderFraction(state)
                                                    drawLine(
                                                        color = Color(0xFFFFE0D1),
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                    drawLine(
                                                        color = Color(0xFFFF5E00),
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width * fraction, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                        )
                                        Text("${volumeValue.toInt()}%", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(42.dp))
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
                                            tint = Color(0xFFFF5E00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Speech Speed",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("0.5x", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(34.dp))
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
                                                        .background(Color(0xFF0F172A), CircleShape)
                                                )
                                            },
                                            track = { state ->
                                                Canvas(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp)
                                                ) {
                                                    val centerY = size.height / 2f
                                                    val fraction = sliderFraction(state)
                                                    drawLine(
                                                        color = Color(0xFFFFE0D1),
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                    drawLine(
                                                        color = Color(0xFFFF5E00),
                                                        start = Offset(0f, centerY),
                                                        end = Offset(size.width * fraction, centerY),
                                                        strokeWidth = size.height * 4f,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                        )
                                        Text(String.format("%.1fx", speedValue), fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(42.dp))
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
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Plays a chime sound before the voice announcement.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Switch(
                                        checked = playChimeFirst,
                                        onCheckedChange = { settingsViewModel.setPlayChimeFirst(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFFF5E00),
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color(0xFFCBD5E1)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
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
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = subtitle,
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { settingsViewModel.setAlertTone(title) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFFFF5E00),
                                                unselectedColor = Color(0xFFFF5E00)
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
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
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Required to capture payments from notifications.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Enabled",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val ttsHelper = TextToSpeechHelper(context, PreferenceManager(context))
                                        ttsHelper.speak(AnnouncementKind.CREDIT_RECEIVED, "AutoPay Max", 100)
                                        settingsViewModel.sendTestNotification(context)
                                        Toast.makeText(context, "Playing test alert tone!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
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
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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

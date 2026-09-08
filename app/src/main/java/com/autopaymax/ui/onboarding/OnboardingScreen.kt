package com.autopaymax.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Social-proof pill on the onboarding screen - picked once per screen visit so it doesn't show
// the exact same name/city to every single user.
private val RECENT_ACTIVATIONS = listOf(
    "Vikram Singh" to "Mumbai",
    "Priya Sharma" to "Delhi",
    "Rahul Verma" to "Bengaluru",
    "Ananya Iyer" to "Chennai",
    "Arjun Mehta" to "Pune",
    "Sneha Reddy" to "Hyderabad",
    "Karan Malhotra" to "Ahmedabad",
    "Neha Kapoor" to "Kolkata",
    "Rohan Gupta" to "Jaipur",
    "Divya Nair" to "Kochi"
)

@Composable
fun OnboardingScreen(
    onStartTrialClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val (activationName, activationCity) = remember { RECENT_ACTIVATIONS.random() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Scrollable Inner Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top notification pill banner
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFFFFB074), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFFFF7600),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$activationName just activated from $activationCity",
                        color = Color(0xFFFF7600),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main headline
            Row(

            ) {
                Text(
                    text = "Start your FREE trial for ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight(500),
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "₹299",
                    textDecoration = TextDecoration.LineThrough,
                    fontSize = 22.sp,
                    fontWeight = FontWeight(500),
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Giant ₹2
            Text(
                text = "₹2",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF6B00)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rating Stars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "4.7 Highly Rated",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Media Presentation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFF0EA)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE0D1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Smart AutoPay SoundBox Alerts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B00)
                    )
                    Text(
                        text = "Real-time payment speech & reminders",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Feature Grid Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeatureItem(
                    icon = Icons.Default.History,
                    title = "Auto-\ndetect all\nUPI",
                    modifier = Modifier.weight(1f)
                )
                FeatureItem(
                    icon = Icons.Default.NotificationsNone,
                    title = "Reminder 1.3.7\ndays before\ndue",
                    modifier = Modifier.weight(1f)
                )
                FeatureItem(
                    icon = Icons.Default.TouchApp,
                    title = "Cancel\nmandates\nwith one tap",
                    modifier = Modifier.weight(1f)
                )
                FeatureItem(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud sync\nacross\ndevices",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Supports all UPI Apps Section
            Text(
                text = "Supports all UPI Apps",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B00)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment App Logos Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UpiBadge(name = "G Pay", brandColor = Color(0xFF4285F4))
                UpiBadge(name = "Paytm", brandColor = Color(0xFF00BAF2))
                UpiBadge(name = "PhonePe", brandColor = Color(0xFF5F259F))
                UpiBadge(name = "MobiKwik", brandColor = Color(0xFF00B0FF))
            }
        }

        // STICKY Bottom Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onStartTrialClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text(
                        text = "Start Your Free Trial",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFFFD4B8), RoundedCornerShape(14.dp)),
//                .background(Color(0xFFFFF9F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF6B00),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF6B00),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun UpiBadge(name: String, brandColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clip(shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = brandColor
        )
    }
}

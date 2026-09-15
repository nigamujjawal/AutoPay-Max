package com.autopaymax.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopaymax.ui.theme.NavyPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateNext()
    }

    val navyGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF2C4375),
            Color(0xFF365380)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(navyGradient)
    ) {
        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.7f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.5f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = size.width * 0.2f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.7f)
            )
        }

        // Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Box
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title & Subtitle
            Text(
                text = "AutoPay Max",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your Smart Auto Payment Manager",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 3 Pill Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Instant Alerts", "Multi-App", "Analytics").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = NavyPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Loading Progress Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Loading...",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.autopaymax.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2200)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Decorative background circles at top-left
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFDBEAFE).copy(alpha = 0.5f),
                radius = size.width * 0.45f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.02f)
            )
            drawCircle(
                color = Color(0xFFE0E7FF).copy(alpha = 0.4f),
                radius = size.width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.08f)
            )
        }

        // Top & Center Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 90.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            Image(
                painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Subtitle
            Text(
                text = "AutoPay Max",
                color = Color(0xFF1E293B),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Smart Auto Payment Manager",
                color = Color(0xFF64748B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3 Outlined Pill Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Instant Alerts", "Multi-App", "Analytics").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFF93C5FD), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color(0xFF1E40AF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Curved Wave Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.BottomCenter)
        ) {
            // Blue Curved Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.32f)
                    quadraticTo(
                        size.width * 0.5f, -size.height * 0.08f,
                        size.width, size.height * 0.32f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2563EB),
                            Color(0xFF1D4ED8),
                            Color(0xFF1E40AF)
                        )
                    )
                )

                // Bottom Right Decorative Circles inside blue section
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = size.width * 0.32f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.88f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = size.width * 0.28f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.94f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Loading Progress Bar + Text
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 40.dp)
                    .fillMaxWidth(0.82f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Loading...",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


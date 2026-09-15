package com.autopaymax.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopaymax.ui.theme.NavyPrimary
import com.autopaymax.ui.theme.NavySecondary
import com.autopaymax.ui.theme.PremiumNavyGradient

@Composable
fun OnboardingScreen(
    onStartTrialClick: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                1 -> OnboardingStep1Welcome(onNext = { currentStep = 2 })
                2 -> OnboardingStep2Spending(onNext = { currentStep = 3 })
                3 -> OnboardingStep3HelpGoals(
                    onBack = { currentStep = 2 },
                    onNext = { currentStep = 4 }
                )
                4 -> OnboardingStep4ValueNotifications(
                    onBack = { currentStep = 3 },
                    onNext = onStartTrialClick
                )
            }
        }
    }
}

// ==========================================
// STEP 1: Welcome & Subscriptions Grid Hero
// ==========================================
@Composable
private fun OnboardingStep1Welcome(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Background brand grid wall
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BrandTile("N", Color(0xFFE50914), Color.Black)
                        BrandTile("hulu", Color(0xFF1CE783), Color(0xFF0F172A))
                        BrandTile("prime", Color(0xFF00A8E1), Color(0xFF00A8E1))
                        BrandTile("HBO", Color(0xFF9933FF), Color(0xFF9933FF))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BrandTile("tv+", Color.White, Color(0xFF334155))
                        BrandTile("▶", Color.Red, Color(0xFFEF4444))
                        BrandTile("≈", Color(0xFF1DB954), Color(0xFF1DB954))
                        BrandTile("Cc", Color(0xFFFF9A00), Color(0xFFF43F5E))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BrandTile("aws", Color(0xFFFF9900), Color(0xFFF8FAFC))
                        BrandTile("a", Color(0xFFFF9900), Color(0xFFFED7AA))
                        BrandTile("P", Color(0xFF0066CC), Color(0xFFCBD5E1))
                        BrandTile("aMC+", Color(0xFF00B0FF), Color(0xFF94A3B8))
                    }
                }

                // Central App Logo Badge Overlaid
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                            contentDescription = "AutoPay Max Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Name Headline
            Text(
                text = "AutoPay Max",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Keep track of your\nsubscriptions in one place",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }

        // Bottom CTA Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PremiumNavyGradient)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Get started",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 2: Spending Breakdown Screen
// ==========================================
@Composable
private fun OnboardingStep2Spending(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step 2 App Logo Badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                    contentDescription = "AutoPay Max Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You're spending more\nthan you think",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Find forgotten subscriptions, cut what you\ndon't need, and start saving today.",
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Subscription Cards List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpendBrandCard(
                    name = "Netflix",
                    price = "£12.99",
                    backgroundColor = Color(0xFF0F172A),
                    logoChar = "N",
                    logoColor = Color(0xFFE50914)
                )
                SpendBrandCard(
                    name = "Spotify",
                    price = "£10.99",
                    backgroundColor = Color(0xFF1DB954),
                    logoChar = "≈",
                    logoColor = Color.White
                )
                SpendBrandCard(
                    name = "Youtube Premium",
                    price = "£11.99",
                    backgroundColor = Color(0xFFEF4444),
                    logoChar = "▶",
                    logoColor = Color.White
                )
                SpendBrandCard(
                    name = "Amazon Prime",
                    price = "£14.99",
                    backgroundColor = Color(0xFF00A8E1),
                    logoChar = "prime",
                    logoColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Total Spend Box
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "£611.52",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "per year",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Bottom Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PremiumNavyGradient)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Find my savings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 3: Goals Questionnaire Screen
// ==========================================
@Composable
private fun OnboardingStep3HelpGoals(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var selectedOption by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 110.dp)
        ) {
            // Top Navigation & Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { 0.33f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NavySecondary,
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                    contentDescription = "AutoPay Max Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "What do you want help\nwith most?",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3 Goal Questionnaire Cards
            GoalCard(
                icon = Icons.Default.Search,
                title = "Find hidden subscriptions",
                subtitle = "Catch subscriptions hiding in receipts, renewals, and old trials.",
                isSelected = selectedOption == 0,
                onClick = { selectedOption = 0 }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoalCard(
                icon = Icons.Default.Savings,
                title = "Lower my bills",
                subtitle = "Find cheaper plans, sharing options, and better deals.",
                isSelected = selectedOption == 1,
                onClick = { selectedOption = 1 }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GoalCard(
                icon = Icons.Default.Notifications,
                title = "Never miss a renewal",
                subtitle = "Stay ahead of upcoming charges, reminders, and plan changes.",
                isSelected = selectedOption == 2,
                onClick = { selectedOption = 2 }
            )
        }

        // Bottom Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PremiumNavyGradient)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 4: Value & Stacked Notifications Screen
// ==========================================
@Composable
private fun OnboardingStep4ValueNotifications(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { 0.66f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NavySecondary,
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = com.autopaymax.R.drawable.appicon),
                    contentDescription = "AutoPay Max Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "We watch. You save.",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Price hikes, upcoming renewals, expiring\ntrials — we catch them so you don't have\nto.",
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stacked Notification Cards Visual
            Column(
                verticalArrangement = Arrangement.spacedBy((-14).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NotificationStackCard(
                    brandChar = "N",
                    brandColor = Color(0xFFE50914),
                    title = "Netflix",
                    time = "Today",
                    description = "Found Netflix in your email — £12.99/mo",
                    elevation = 10.dp
                )
                NotificationStackCard(
                    brandChar = "≈",
                    brandColor = Color(0xFF1DB954),
                    title = "Spotify",
                    time = "Today",
                    description = "Spotify renews in 3 days",
                    elevation = 8.dp
                )
                NotificationStackCard(
                    brandChar = "▶",
                    brandColor = Color(0xFFEF4444),
                    title = "Youtube Premium",
                    time = "Yesterday",
                    description = "YouTube Premium price increased — cheaper alternatives found",
                    elevation = 6.dp
                )
                NotificationStackCard(
                    brandChar = "prime",
                    brandColor = Color(0xFF00A8E1),
                    title = "Amazon Prime",
                    time = "5 days",
                    description = "Amazon Prime free trial ends in 5 days",
                    elevation = 4.dp
                )
            }
        }

        // Bottom Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PremiumNavyGradient)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// HELPER COMPONENTS
// ==========================================
@Composable
private fun BrandTile(text: String, textColor: Color, tileBg: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(tileBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SpendBrandCard(
    name: String,
    price: String,
    backgroundColor: Color,
    logoChar: String,
    logoColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = logoChar,
                        color = logoColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = price,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoalCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NavySecondary else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) NavySecondary.copy(alpha = 0.12f) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) NavySecondary else Color(0xFF0F172A),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun NotificationStackCard(
    brandChar: String,
    brandColor: Color,
    title: String,
    time: String,
    description: String,
    elevation: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = brandChar,
                    color = brandColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

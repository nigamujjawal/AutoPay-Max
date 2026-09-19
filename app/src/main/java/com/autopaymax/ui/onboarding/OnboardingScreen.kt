package com.autopaymax.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopaymax.R

private val LightOnboardingBg = Color(0xFFE6EEF8)
private val DarkTitleColor = Color(0xFF0F172A)
private val SoftSubtextColor = Color(0xFF64748B)
private val PrimaryBlueButton = Color(0xFF2563EB)

@Composable
fun OnboardingScreen(
    onStartTrialClick: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightOnboardingBg)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightOnboardingBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Logos Wall (3 Rows of authentic launcher icons)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: Prime Video, Disney+ Hotstar, Spotify, Netflix
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandLauncherIcon(brand = BrandType.PRIME_VIDEO)
                    BrandLauncherIcon(brand = BrandType.DISNEY_HOTSTAR)
                    BrandLauncherIcon(brand = BrandType.SPOTIFY)
                    BrandLauncherIcon(brand = BrandType.NETFLIX)
                }
                // Row 2: Hulu, Spotify, Netflix, YouTube
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandLauncherIcon(brand = BrandType.HULU)
                    BrandLauncherIcon(brand = BrandType.SPOTIFY)
                    BrandLauncherIcon(brand = BrandType.NETFLIX)
                    BrandLauncherIcon(brand = BrandType.YOUTUBE)
                }
                // Row 3: Apple TV+, Canva, OpenAI/ChatGPT, Prime Video
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandLauncherIcon(brand = BrandType.APPLE_TV)
                    BrandLauncherIcon(brand = BrandType.CANVA)
                    BrandLauncherIcon(brand = BrandType.OPENAI)
                    BrandLauncherIcon(brand = BrandType.PRIME_VIDEO)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Headline
            Text(
                text = "AutoPay Max",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Keep track of your subscriptions\nin one place",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = SoftSubtextColor,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        // Bottom CTA Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = LightOnboardingBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlueButton,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Get started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightOnboardingBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Credit Card Icon at Top
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(28.dp, 18.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF8C00)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.CenterStart)
                                .padding(start = 2.dp)
                                .background(Color(0xFFFFD700), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Headline
            Text(
                text = "You're spending more\nthan you think",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Find forgotten subscriptions, cut what you\ndon't need, and start saving today.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SoftSubtextColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // List of 4 White Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpendItemCard(
                    brand = BrandType.NETFLIX,
                    name = "Netflix",
                    price = "₹1,299"
                )
                SpendItemCard(
                    brand = BrandType.SPOTIFY,
                    name = "Spotify",
                    price = "₹119"
                )
                SpendItemCard(
                    brand = BrandType.YOUTUBE,
                    name = "YouTube Premium",
                    price = "₹189"
                )
                SpendItemCard(
                    brand = BrandType.PRIME_VIDEO,
                    name = "Amazon Prime",
                    price = "₹179"
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Total Spend Readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "₹21,551.52",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkTitleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "per year",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Bottom CTA Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = LightOnboardingBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlueButton,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Find my savings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightOnboardingBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 120.dp)
        ) {
            // Top Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(PrimaryBlueButton)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFCBD5E1))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Headline
            Text(
                text = "What do you want help\nwith most?",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Option 1
            HelpGoalOptionCard(
                title = "Find hidden subscriptions",
                subtitle = "Catch subscriptions hiding in receipts, renewals, and old trials.",
                isSelected = selectedOption == 0,
                onClick = { selectedOption = 0 }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2
            HelpGoalOptionCard(
                title = "Lower my bills",
                subtitle = "Find cheaper plans, sharing options, and better deals.",
                isSelected = selectedOption == 1,
                onClick = { selectedOption = 1 }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 3
            HelpGoalOptionCard(
                title = "Never miss a renewal",
                subtitle = "Stay ahead of upcoming charges, reminders, and plan changes.",
                isSelected = selectedOption == 2,
                onClick = { selectedOption = 2 }
            )
        }

        // Bottom CTA Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = LightOnboardingBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlueButton,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 4: Value & Notifications Screen
// ==========================================
@Composable
private fun OnboardingStep4ValueNotifications(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightOnboardingBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Progress Bar (100% completed on final step)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlueButton)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Headline
            Text(
                text = "We watch. You save.",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Price hikes, upcoming renewals, expiring trials. We catch them so you don't have to.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SoftSubtextColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Single White Notification Container Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    NotificationRowItem(
                        brand = BrandType.NETFLIX,
                        title = "Netflix",
                        time = "Today",
                        subtitle = "Found Netflix in your email: ₹1,299/month"
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    NotificationRowItem(
                        brand = BrandType.SPOTIFY,
                        title = "Spotify",
                        time = "Today",
                        subtitle = "Spotify renews in 3 days"
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    NotificationRowItem(
                        brand = BrandType.YOUTUBE,
                        title = "YouTube Premium",
                        time = "Yesterday",
                        subtitle = "Youtube Premium free trial ends in 5 days"
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    NotificationRowItem(
                        brand = BrandType.PRIME_VIDEO,
                        title = "Amazon Prime",
                        time = "5 days",
                        subtitle = "Amazon Prime free trial ends in 5 days"
                    )
                }
            }
        }

        // Bottom CTA Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = LightOnboardingBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlueButton,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// BRAND LOGO HELPER COMPONENTS & TYPES
// ==========================================

private enum class BrandType {
    PRIME_VIDEO,
    DISNEY_HOTSTAR,
    SPOTIFY,
    NETFLIX,
    HULU,
    YOUTUBE,
    APPLE_TV,
    CANVA,
    OPENAI
}

@Composable
private fun BrandLauncherIcon(
    brand: BrandType,
    size: androidx.compose.ui.unit.Dp = 60.dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(18.dp),
        color = when (brand) {
            BrandType.PRIME_VIDEO -> Color(0xFF00A8E1)
            BrandType.DISNEY_HOTSTAR -> Color(0xFF092A38)
            BrandType.SPOTIFY -> Color(0xFF1DB954)
            BrandType.NETFLIX -> Color(0xFF000000)
            BrandType.HULU -> Color(0xFF0F172A)
            BrandType.YOUTUBE -> Color(0xFFEF4444)
            BrandType.APPLE_TV -> Color(0xFF000000)
            BrandType.CANVA -> Color(0xFF00C4CC)
            BrandType.OPENAI -> Color(0xFF10A37F)
        },
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (brand) {
                BrandType.PRIME_VIDEO -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_primevideo),
                        contentDescription = "Prime Video",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                BrandType.DISNEY_HOTSTAR -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_disney),
                        contentDescription = "Disney+",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                BrandType.SPOTIFY -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_spotify),
                        contentDescription = "Spotify",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }
                BrandType.NETFLIX -> {
                    Text(
                        text = "N",
                        color = Color(0xFFE50914),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                BrandType.HULU -> {
                    Text(
                        text = "hulu",
                        color = Color(0xFF1CE783),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                BrandType.YOUTUBE -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_youtube),
                        contentDescription = "YouTube",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                BrandType.APPLE_TV -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_brand_apple),
                            contentDescription = "Apple TV+",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "tv+",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                BrandType.CANVA -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_canva),
                        contentDescription = "Canva",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                BrandType.OPENAI -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_brand_openai),
                        contentDescription = "OpenAI",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendItemCard(
    brand: BrandType,
    name: String,
    price: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BrandLauncherIcon(brand = brand, size = 44.dp)
                Text(
                    text = name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTitleColor
                )
            }
            Text(
                text = price,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkTitleColor
            )
        }
    }
}

@Composable
private fun HelpGoalOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color(0xFFF1F5F9),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlueButton) else null,
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Blue Icon Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlueButton),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTitleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = SoftSubtextColor,
                    lineHeight = 18.sp
                )
            }

            // Custom Radio Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) PrimaryBlueButton else Color(0xFFCBD5E1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueButton)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRowItem(
    brand: BrandType,
    title: String,
    time: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrandLauncherIcon(brand = brand, size = 46.dp)
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
                    color = DarkTitleColor
                )
                Text(
                    text = time,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = SoftSubtextColor,
                maxLines = 2
            )
        }
    }
}


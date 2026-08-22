package com.uj.appstorysautopaymanager.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.ui.onboarding.UpiBadge
import com.uj.appstorysautopaymanager.ui.theme.AppStorysAutoPayManagerTheme

// Dummy screen only - not wired into any nav graph. Mirrors OnboardingScreen.kt's structure
// (scrollable Column, same spacing/typography conventions) but themed to the app's orange
// palette instead of the reference mock's green. Star rating / download-count line intentionally
// left out per request.
private val RECENT_ACTIVATIONS = listOf(
    "Sunita" to "Surat",
    "Rakesh" to "Nagpur",
    "Meera" to "Indore",
    "Vikram" to "Mumbai",
    "Anjali" to "Lucknow",
    "Suresh" to "Coimbatore"
)

private val OrangeAccent = Color(0xFFFF6B00)

@Composable
fun SubscriptionOfferScreen(
    onSubscribeClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val (activationName, activationCity) = remember { RECENT_ACTIVATIONS.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp)
            .appstorys("subscription_offer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand line
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(20.dp).height(1.dp).background(OrangeAccent))
            Spacer(modifier = Modifier.width(8.dp))
            Text("•", color = OrangeAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "YOUR POCKET SOUNDBOX",
                color = OrangeAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("•", color = OrangeAccent, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(20.dp).height(1.dp).background(OrangeAccent))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Social-proof pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$activationName from $activationCity just activated alerts",
                    color = Color(0xFF1E293B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Unlock premium features",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "₹199",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = OrangeAccent
        )
        Text(
            text = "/month",
            fontSize = 16.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Testimonial / video placeholder card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFF0EA))
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Watch how AutoPay announces every payment",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(OrangeAccent)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAYMENT ALERTS, LOUD & CLEAR",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Autopays ₹199 every month, cancel anytime",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PremiumFeatureItem(icon = Icons.Default.Bolt, title = "Instant\nUPI Alerts", modifier = Modifier.weight(1f))
            PremiumFeatureItem(icon = Icons.Default.PhoneAndroid, title = "No Hardware\nNeeded", modifier = Modifier.weight(1f))
            PremiumFeatureItem(icon = Icons.Default.VerifiedUser, title = "No KYC\nRequired", modifier = Modifier.weight(1f))
            PremiumFeatureItem(icon = Icons.Default.Translate, title = "Alerts in\nYour Language", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Supports all UPI Apps",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeAccent
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pay via", fontSize = 14.sp, color = Color(0xFF64748B))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpiBadge(name = "Paytm", brandColor = Color(0xFF00BAF2))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(OrangeAccent)
            )
            Text(
                text = "Hurry up! Limited-time launch pricing",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF7600), Color(0xFFFF9E40))))
                .clickable { onSubscribeClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SUBSCRIBE ₹199/MONTH",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    icon: ImageVector,
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
                .clip(CircleShape)
                .background(Color(0xFFFFF0EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SubscriptionOfferScreenPreview() {
    AppStorysAutoPayManagerTheme {
        SubscriptionOfferScreen()
    }
}

package com.autopaymax.ui.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appversal.appstorys.utils.appstorys
import com.autopaymax.ui.theme.*

@Composable
fun SubscriptionOfferScreen(
    onSubscribeClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
                .appstorys("subscription_offer_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Close Bar & Brand Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onContinueClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Header Brand Icon Wall visual — each keeps its own real
                // brand color; these are third-party marks, not app theme.
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandHeaderBadge("N", Color(0xFFE50914), Color.Black)
                        BrandHeaderBadge("a", Color(0xFFFF9900), Color(0xFFFED7AA))
                        BrandHeaderBadge("≈", Color(0xFF1DB954), Color(0xFF1DB954))
                        BrandHeaderBadge("P", Color(0xFF0066CC), Color(0xFFCBD5E1))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandHeaderBadge("▶", Color.Red, Color(0xFFEF4444))
                        BrandHeaderBadge("tv+", Color.White, Color(0xFF334155))
                        BrandHeaderBadge("prime", Color(0xFF00A8E1), Color(0xFF00A8E1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Titles
            Text(
                text = "Choose your plan",
                style = MaterialTheme.typography.headlineLarge,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Take full control of your subscriptions",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureCheckRow("Unlimited subscriptions")
                FeatureCheckRow("See exactly where your money goes")
                FeatureCheckRow("AI assistant for cancellation instructions")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Plan Cards Selection Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Lifetime Option
                PlanOptionCard(
                    title = "Lifetime, pay once",
                    priceText = state.lifetimePackage?.product?.price?.formatted ?: "₹5,629.00",
                    subtitleText = "no recurring payments",
                    badgeText = null,
                    isSelected = state.selectedPlanType == PlanType.LIFETIME,
                    onClick = { viewModel.selectPlanType(PlanType.LIFETIME) }
                )

                // Card 2: Save on Annual Option (Selected by Default)
                PlanOptionCard(
                    title = "Save on Annual",
                    priceText = state.yearlyPackage?.product?.price?.formatted ?: "₹2,029.00 per year",
                    originalPriceText = "₹4,058.00",
                    subtitleText = "Cancel anytime on Google Play",
                    badgeText = "-50% ENDS SOON",
                    isSelected = state.selectedPlanType == PlanType.YEARLY,
                    onClick = { viewModel.selectPlanType(PlanType.YEARLY) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Get Premium Button — a steady status readout once the
            // purchase already went through, a call to action until then.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (state.isProUser) Modifier.background(StatusActive)
                        else Modifier.background(PremiumNavyGradient)
                    )
                    .clickable(enabled = !state.isProcessing) {
                        viewModel.subscribe(activity)
                        onSubscribeClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (state.isProUser) "PRO MEMBER ACTIVE" else "Get Premium",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Restore Purchases Link
            TextButton(
                onClick = { viewModel.restorePurchases() },
                enabled = !state.isProcessing
            ) {
                Text(
                    text = "Restore purchases",
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline
                )
            }
        }

        // RevenueCat Native Paywall Overlay
        if (state.showRevenueCatPaywall) {
            com.revenuecat.purchases.ui.revenuecatui.Paywall(
                options = com.revenuecat.purchases.ui.revenuecatui.PaywallOptions.Builder(
                    dismissRequest = { viewModel.toggleRevenueCatPaywall(false) }
                ).build()
            )
        }

        // RevenueCat Customer Center Overlay
        if (state.showCustomerCenter) {
            com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter(
                onDismiss = { viewModel.toggleCustomerCenter(false) }
            )
        }
    }
}

@Composable
private fun BrandHeaderBadge(text: String, textColor: Color, tileBg: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tileBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeatureCheckRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite
        )
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    priceText: String,
    originalPriceText: String? = null,
    subtitleText: String,
    badgeText: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { onClick() }
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Radio checkmark indicator on left
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (originalPriceText != null) {
                            Text(
                                text = originalPriceText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusOverdue,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = priceText,
                            style = InstrumentValueLarge,
                            color = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }

        // Top right discount badge pill
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PremiumNavyGradient)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    style = InstrumentLabel
                )
            }
        }
    }
}

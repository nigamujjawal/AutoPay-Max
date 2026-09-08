package com.uj.appstorysautopaymanager.util

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.uj.appstorysautopaymanager.R

// The merchants that show up most often as real autopay mandates (streaming, telecom, food
// delivery, UPI wallets, bank EMIs) - not exhaustive, just common enough to earn a recognizable
// badge on the Dashboard/Home autopay list instead of the generic plain-initials avatar.
//
// Icon shapes are from the Simple Icons project (CC0-licensed monochrome brand glyphs, meant
// exactly for this kind of "identify a real-world service in your own UI" use) - not scraped
// logo images, and each is rendered white-on-brand-color the same way the plain initials badge
// already was. The trademarks themselves still belong to their respective companies; this is
// referential use (labeling the user's own subscription), not an endorsement claim.
data class AutoPayApp(
    val displayName: String,
    val initials: String,
    val color: Color,
    @param:DrawableRes val iconRes: Int,
    private val keywords: List<String>
) {
    fun matches(merchant: String): Boolean {
        val lower = merchant.lowercase()
        return keywords.any { lower.contains(it) }
    }
}

val KnownAutoPayApps = listOf(
    AutoPayApp("Netflix", "N", Color(0xFFE50914), R.drawable.ic_brand_netflix, listOf("netflix")),
    AutoPayApp("Amazon Prime", "A", Color(0xFF00A8E1), R.drawable.ic_brand_primevideo, listOf("amazon prime", "prime video", "amazon")),
    AutoPayApp("Spotify", "S", Color(0xFF1DB954), R.drawable.ic_brand_spotify, listOf("spotify")),
    AutoPayApp("YouTube", "Y", Color(0xFFFF0000), R.drawable.ic_brand_youtube, listOf("youtube")),
    AutoPayApp("Google Play", "G", Color(0xFF00C853), R.drawable.ic_brand_googleplay, listOf("google play", "play store")),
    AutoPayApp("Google Pay", "G", Color(0xFF4285F4), R.drawable.ic_brand_googlepay, listOf("google pay", "gpay")),
    AutoPayApp("PhonePe", "P", Color(0xFF5F259F), R.drawable.ic_brand_phonepe, listOf("phonepe")),
    AutoPayApp("Paytm", "P", Color(0xFF00BAF2), R.drawable.ic_brand_paytm, listOf("paytm")),
    AutoPayApp("LinkedIn", "L", Color(0xFF0A66C2), R.drawable.ic_brand_linkedin, listOf("linkedin")),
    AutoPayApp("Jio", "J", Color(0xFF0F2C6B), R.drawable.ic_brand_jio, listOf("jio")),
    AutoPayApp("Airtel", "A", Color(0xFFED1C24), R.drawable.ic_brand_airtel, listOf("airtel")),
    AutoPayApp("Vodafone Idea", "VI", Color(0xFFEE2737), R.drawable.ic_brand_vodafone, listOf("vodafone")),
    AutoPayApp("Zomato", "Z", Color(0xFFE23744), R.drawable.ic_brand_zomato, listOf("zomato")),
    AutoPayApp("Swiggy", "S", Color(0xFFFC8019), R.drawable.ic_brand_swiggy, listOf("swiggy")),
    AutoPayApp("Instagram", "I", Color(0xFFC13584), R.drawable.ic_brand_instagram, listOf("instagram")),
    AutoPayApp("Facebook", "F", Color(0xFF1877F2), R.drawable.ic_brand_facebook, listOf("facebook")),
    AutoPayApp("Apple / iCloud", "A", Color(0xFF555555), R.drawable.ic_brand_apple, listOf("apple", "icloud", "itunes")),
    AutoPayApp("Microsoft 365", "M", Color(0xFF00A4EF), R.drawable.ic_brand_microsoft, listOf("microsoft", "office 365")),
    AutoPayApp("Adobe", "A", Color(0xFFFF0000), R.drawable.ic_brand_adobe, listOf("adobe")),
    AutoPayApp("Uber", "U", Color(0xFF000000), R.drawable.ic_brand_uber, listOf("uber")),
    AutoPayApp("Slack", "S", Color(0xFF4A154B), R.drawable.ic_brand_slack, listOf("slack")),
    AutoPayApp("Flipkart", "F", Color(0xFF2874F0), R.drawable.ic_brand_flipkart, listOf("flipkart")),
    AutoPayApp("HDFC Bank", "H", Color(0xFF004C8F), R.drawable.ic_brand_hdfcbank, listOf("hdfc")),
    AutoPayApp("Axis Bank", "A", Color(0xFF97144D), R.drawable.ic_brand_axisbank, listOf("axis bank", "axis")),
    AutoPayApp("OpenAI", "O", Color(0xFF10A37F), R.drawable.ic_brand_openai, listOf("openai", "chatgpt", "chat gpt")),
    AutoPayApp("Claude", "C", Color(0xFFD97757), R.drawable.ic_brand_claude, listOf("claude", "anthropic")),
    // Generic Google catch-all - kept last so the more specific Google Play/Google Pay entries
    // above always match first for merchant strings that mention those by name.
    AutoPayApp("Google", "G", Color(0xFF4285F4), R.drawable.ic_brand_google, listOf("google"))
)

fun matchAutoPayApp(merchant: String): AutoPayApp? = KnownAutoPayApps.firstOrNull { it.matches(merchant) }

// Fallback avatar text when no brand icon matches - first letters of the first two words, else
// the first two characters.
fun merchantInitials(merchant: String): String {
    val clean = merchant.replace("Bank of India", "").replace("Bank", "").trim()
    val parts = clean.split(" ").filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
    } else {
        clean.take(2).uppercase()
    }
}

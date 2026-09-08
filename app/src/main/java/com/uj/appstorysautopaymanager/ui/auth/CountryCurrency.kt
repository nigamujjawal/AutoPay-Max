package com.uj.appstorysautopaymanager.ui.auth

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Relocated verbatim from the deleted phone-OTP LoginScreen. This picker is the ONLY thing that
// sets the global currency preference (SettingsViewModel.setCurrency/setCurrencyCode, which
// Dashboard/Passbook/etc. read) - see the currency-preference system. The GoogleConnectScreen
// renders CountryCurrencyPicker so that dependency survives the auth rewrite.
//
// currencySymbol drives what Dashboard/Passbook display; several countries deliberately share a
// plain symbol ("$" = US/Australia/Mexico) the way real apps do. currencyCode is the actual ISO
// 4217 code (unambiguous) that PaymentSyncWorker sends to the backend. The single combined
// US/Canada row is a known simplification carried over from the +1 dial-code merge: both get
// "USD", so a Canadian user's payments sync as USD, not CAD.
data class CountryCode(
    val iso: String,
    val dialCode: String,
    val displayName: String,
    val digitCount: Int,
    val currencySymbol: String,
    val currencyCode: String
)

val COUNTRY_CODES = listOf(
    CountryCode("IN", "+91", "India", 10, "₹", "INR"),
    CountryCode("AU", "+61", "Australia", 9, "$", "AUD"),
    CountryCode("BH", "+973", "Bahrain", 8, "BHD", "BHD"),
    CountryCode("BD", "+880", "Bangladesh", 10, "৳", "BDT"),
    CountryCode("BR", "+55", "Brazil", 11, "R$", "BRL"),
    CountryCode("CN", "+86", "China", 11, "¥", "CNY"),
    CountryCode("EG", "+20", "Egypt", 10, "E£", "EGP"),
    CountryCode("FR", "+33", "France", 9, "€", "EUR"),
    CountryCode("DE", "+49", "Germany", 10, "€", "EUR"),
    CountryCode("HK", "+852", "Hong Kong", 8, "HK$", "HKD"),
    CountryCode("ID", "+62", "Indonesia", 10, "Rp", "IDR"),
    CountryCode("IE", "+353", "Ireland", 9, "€", "EUR"),
    CountryCode("IT", "+39", "Italy", 10, "€", "EUR"),
    CountryCode("JP", "+81", "Japan", 10, "¥", "JPY"),
    CountryCode("KE", "+254", "Kenya", 9, "KSh", "KES"),
    CountryCode("KW", "+965", "Kuwait", 8, "KWD", "KWD"),
    CountryCode("MY", "+60", "Malaysia", 9, "RM", "MYR"),
    CountryCode("MX", "+52", "Mexico", 10, "$", "MXN"),
    CountryCode("NP", "+977", "Nepal", 10, "NPR", "NPR"),
    CountryCode("NL", "+31", "Netherlands", 9, "€", "EUR"),
    CountryCode("NZ", "+64", "New Zealand", 9, "NZ$", "NZD"),
    CountryCode("NG", "+234", "Nigeria", 10, "₦", "NGN"),
    CountryCode("OM", "+968", "Oman", 8, "OMR", "OMR"),
    CountryCode("PK", "+92", "Pakistan", 10, "Rs", "PKR"),
    CountryCode("PH", "+63", "Philippines", 10, "₱", "PHP"),
    CountryCode("QA", "+974", "Qatar", 8, "QAR", "QAR"),
    CountryCode("SA", "+966", "Saudi Arabia", 9, "SAR", "SAR"),
    CountryCode("SG", "+65", "Singapore", 8, "S$", "SGD"),
    CountryCode("ZA", "+27", "South Africa", 9, "R", "ZAR"),
    CountryCode("KR", "+82", "South Korea", 10, "₩", "KRW"),
    CountryCode("ES", "+34", "Spain", 9, "€", "EUR"),
    CountryCode("LK", "+94", "Sri Lanka", 9, "Rs", "LKR"),
    CountryCode("TH", "+66", "Thailand", 9, "฿", "THB"),
    CountryCode("AE", "+971", "United Arab Emirates", 9, "AED", "AED"),
    CountryCode("GB", "+44", "United Kingdom", 10, "£", "GBP"),
    CountryCode("US", "+1", "United States / Canada", 10, "$", "USD")
)

val DEFAULT_COUNTRY = COUNTRY_CODES.first { it.iso == "IN" }

// simCountryIso/networkCountryIso don't need READ_PHONE_STATE - carrier/locale info, not device
// or subscriber identity. Falls back to device locale, then to India (home market).
fun detectCountryCode(context: Context): CountryCode {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val isoCode = telephonyManager?.simCountryIso?.uppercase()?.takeIf { it.isNotBlank() }
        ?: telephonyManager?.networkCountryIso?.uppercase()?.takeIf { it.isNotBlank() }
        ?: context.resources.configuration.locales[0].country.uppercase()
    return COUNTRY_CODES.find { it.iso == isoCode } ?: DEFAULT_COUNTRY
}

private val FieldBorder = Color(0xFFEDE4D6)
private val TitleColor = Color(0xFF1E293B)
private val SubtitleColor = Color(0xFF7D889A)

// Auto-detects on first composition and reports (symbol, code) via onSelected - the caller wires
// that to SettingsViewModel. Manual override via the dropdown re-fires onSelected.
@Composable
fun CountryCurrencyPicker(
    onSelected: (currencySymbol: String, currencyCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(detectCountryCode(context)) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(selected) { onSelected(selected.currencySymbol, selected.currencyCode) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, FieldBorder, RoundedCornerShape(18.dp))
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("${selected.displayName}  ${selected.dialCode}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TitleColor)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Change country", tint = SubtitleColor, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White,
            border = BorderStroke(1.dp, FieldBorder),
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            COUNTRY_CODES.forEachIndexed { index, country ->
                DropdownMenuItem(
                    text = { Text("${country.displayName}   ${country.dialCode}", fontSize = 15.sp, color = TitleColor) },
                    onClick = {
                        selected = country
                        expanded = false
                    }
                )
                if (index != COUNTRY_CODES.lastIndex) HorizontalDivider(color = FieldBorder, thickness = 1.dp)
            }
        }
    }
}

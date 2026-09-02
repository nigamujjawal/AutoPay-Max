package com.uj.appstorysautopaymanager.ui.auth

import android.app.Activity
import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appversal.appstorys.AppStorys
import com.appversal.appstorys.utils.appstorys
import com.uj.appstorysautopaymanager.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay

private val CreamBg = Color(0xFFFAF3E7)
private val IconBg = Color(0xFFFBE0CB)
private val OrangeAccent = Color(0xFFFF6A00)
private val OrangeButton = Color(0xFFE8935C)
private val TitleColor = Color(0xFF1E293B)
private val SubtitleColor = Color(0xFF7D889A)
private val FieldBorder = Color(0xFFEDE4D6)
private val PlaceholderColor = Color(0xFFAEAEBE)

// Not every country's local mobile number is 10 digits (India/US happen to match, but UAE/
// Australia/Singapore don't) - each entry carries its own expected length so validation doesn't
// silently assume India's shape everywhere. US and Canada share the same +1 dial code, so a
// single entry covers both rather than showing a confusing duplicate.
// ponytail: digit counts are each country's common/typical mobile length, not independently
// verified against every real numbering-plan edge case - a soft client-side hint, not the real
// validation (Firebase itself is the actual authority on whether a number is valid).
// currencySymbol drives what Dashboard/Passbook display, not just this screen - several
// countries deliberately share a plain symbol (US/Australia/Mexico all show "$") the same way
// real apps do, rather than inventing disambiguating prefixes nobody local actually uses; Gulf
// currencies (Bahrain/Kuwait/Oman/Qatar/Saudi) use their ISO code instead since none of them
// have a widely-recognized single-character symbol.
private data class CountryCode(
    val iso: String,
    val dialCode: String,
    val displayName: String,
    val digitCount: Int,
    val currencySymbol: String
)

private val COUNTRY_CODES = listOf(
    CountryCode("IN", "+91", "India", 10, "₹"),
    CountryCode("AU", "+61", "Australia", 9, "$"),
    CountryCode("BH", "+973", "Bahrain", 8, "BHD"),
    CountryCode("BD", "+880", "Bangladesh", 10, "৳"),
    CountryCode("BR", "+55", "Brazil", 11, "R$"),
    CountryCode("CN", "+86", "China", 11, "¥"),
    CountryCode("EG", "+20", "Egypt", 10, "E£"),
    CountryCode("FR", "+33", "France", 9, "€"),
    CountryCode("DE", "+49", "Germany", 10, "€"),
    CountryCode("HK", "+852", "Hong Kong", 8, "HK$"),
    CountryCode("ID", "+62", "Indonesia", 10, "Rp"),
    CountryCode("IE", "+353", "Ireland", 9, "€"),
    CountryCode("IT", "+39", "Italy", 10, "€"),
    CountryCode("JP", "+81", "Japan", 10, "¥"),
    CountryCode("KE", "+254", "Kenya", 9, "KSh"),
    CountryCode("KW", "+965", "Kuwait", 8, "KWD"),
    CountryCode("MY", "+60", "Malaysia", 9, "RM"),
    CountryCode("MX", "+52", "Mexico", 10, "$"),
    CountryCode("NP", "+977", "Nepal", 10, "NPR"),
    CountryCode("NL", "+31", "Netherlands", 9, "€"),
    CountryCode("NZ", "+64", "New Zealand", 9, "NZ$"),
    CountryCode("NG", "+234", "Nigeria", 10, "₦"),
    CountryCode("OM", "+968", "Oman", 8, "OMR"),
    CountryCode("PK", "+92", "Pakistan", 10, "Rs"),
    CountryCode("PH", "+63", "Philippines", 10, "₱"),
    CountryCode("QA", "+974", "Qatar", 8, "QAR"),
    CountryCode("SA", "+966", "Saudi Arabia", 9, "SAR"),
    CountryCode("SG", "+65", "Singapore", 8, "S$"),
    CountryCode("ZA", "+27", "South Africa", 9, "R"),
    CountryCode("KR", "+82", "South Korea", 10, "₩"),
    CountryCode("ES", "+34", "Spain", 9, "€"),
    CountryCode("LK", "+94", "Sri Lanka", 9, "Rs"),
    CountryCode("TH", "+66", "Thailand", 9, "฿"),
    CountryCode("AE", "+971", "United Arab Emirates", 9, "AED"),
    CountryCode("GB", "+44", "United Kingdom", 10, "£"),
    CountryCode("US", "+1", "United States / Canada", 10, "$")
)
private val DEFAULT_COUNTRY = COUNTRY_CODES.first { it.iso == "IN" }

// simCountryIso/networkCountryIso don't need READ_PHONE_STATE - they're carrier/locale info, not
// device or subscriber identity. Falls back to device locale (no SIM, e.g. WiFi-only tablet),
// then to India since that's this app's home market.
private fun detectCountryCode(context: Context): CountryCode {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val isoCode = telephonyManager?.simCountryIso?.uppercase()?.takeIf { it.isNotBlank() }
        ?: telephonyManager?.networkCountryIso?.uppercase()?.takeIf { it.isNotBlank() }
        ?: context.resources.configuration.locales[0].country.uppercase()
    return COUNTRY_CODES.find { it.iso == isoCode } ?: DEFAULT_COUNTRY
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val step by authViewModel.step.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) onSuccess()
    }

    AppStorys.getScreenCampaigns("login_screen",listOf())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .padding(top = 100.dp)
            .appstorys("login_screen")
    ) {
        if (step is LoginStep.EnterOtp) {
            IconButton(
                onClick = { authViewModel.backToPhoneEntry() },
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TitleColor)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(if (step is LoginStep.EnterOtp) 24.dp else 96.dp))

            when (val currentStep = step) {
                is LoginStep.EnterPhone -> PhoneEntryStep(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onSendOtp = { phoneNumber -> authViewModel.sendOtp(phoneNumber, activity) },
                    onCountrySelected = { currencySymbol -> settingsViewModel.setCurrency(currencySymbol) }
                )
                is LoginStep.EnterOtp -> OtpEntryStep(
                    phoneNumber = currentStep.phoneNumber,
                    verificationId = currentStep.verificationId,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onVerify = { code -> authViewModel.verifyOtp(code) },
                    onResend = { authViewModel.resendOtp(activity) }
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(IconBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OrangeAccent,
            modifier = Modifier.size(44.dp)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun PhoneEntryStep(
    isLoading: Boolean,
    errorMessage: String?,
    onSendOtp: (String) -> Unit,
    onCountrySelected: (currencySymbol: String) -> Unit = {}
) {
    val context = LocalContext.current
    var phoneDigits by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(detectCountryCode(context)) }
    var showCountryMenu by remember { mutableStateOf(false) }

    // Fires on the auto-detected country too, not just a manual pick - Dashboard/Passbook read
    // this currency setting, so it needs a value from the moment this screen is first shown.
    LaunchedEffect(selectedCountry) {
        onCountrySelected(selectedCountry.currencySymbol)
    }

    IconBadge(Icons.Default.Phone)

    Text(
        text = "Sign In with Phone",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TitleColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Enter your phone number to receive a secure OTP code for verification",
        fontSize = 14.sp,
        color = SubtitleColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, FieldBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clickable { showCountryMenu = true }
            ) {
                Text(selectedCountry.dialCode, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TitleColor)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change country code",
                    tint = SubtitleColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showCountryMenu,
                onDismissRequest = { showCountryMenu = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color.White,
                border = BorderStroke(1.dp, FieldBorder),
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                COUNTRY_CODES.forEachIndexed { index, country ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${country.displayName}   ${country.dialCode}",
                                fontSize = 15.sp,
                                color = TitleColor
                            )
                        },
                        onClick = {
                            if (country != selectedCountry) phoneDigits = ""
                            selectedCountry = country
                            showCountryMenu = false
                        }
                    )
                    if (index != COUNTRY_CODES.lastIndex) {
                        HorizontalDivider(color = FieldBorder, thickness = 1.dp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(FieldBorder)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (phoneDigits.isEmpty()) {
                Text("Enter phone number", fontSize = 16.sp, color = PlaceholderColor)
            }
            BasicTextField(
                value = phoneDigits,
                onValueChange = { if (it.length <= selectedCountry.digitCount && it.all(Char::isDigit)) phoneDigits = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = TitleColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(OrangeAccent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(errorMessage, color = Color(0xFFDC2626), fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = { onSendOtp("${selectedCountry.dialCode}$phoneDigits") },
        enabled = phoneDigits.length == selectedCountry.digitCount && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(29.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Send Verification OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun OtpEntryStep(
    phoneNumber: String,
    verificationId: String,
    isLoading: Boolean,
    errorMessage: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableIntStateOf(30) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(verificationId) {
        resendCooldown = 30
        while (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    IconBadge(Icons.Default.Lock)

    Text(
        text = "Verify OTP Code",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TitleColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "We sent a 6-digit verification code to\n$phoneNumber",
        fontSize = 14.sp,
        color = SubtitleColor,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }
        ) {
            for (i in 0 until 6) {
                val digit = code.getOrNull(i)?.toString() ?: ""
                val isActive = i == code.length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) OrangeAccent else FieldBorder,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(digit, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TitleColor)
                }
            }
        }
    }

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(errorMessage, color = Color(0xFFDC2626), fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = { onVerify(code) },
        enabled = code.length == 6 && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(29.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OrangeButton)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Verify & Proceed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (resendCooldown > 0) {
            Text("Resend OTP in ", fontSize = 13.sp, color = SubtitleColor)
            Text("${resendCooldown}s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OrangeAccent)
        } else {
            Text(
                "Resend OTP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OrangeAccent,
                modifier = Modifier.clickable { resendCooldown = 30; onResend() }
            )
        }
    }
}

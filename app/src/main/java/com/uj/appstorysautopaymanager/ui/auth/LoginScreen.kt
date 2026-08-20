package com.uj.appstorysautopaymanager.ui.auth

import android.app.Activity
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
import kotlinx.coroutines.delay

private val CreamBg = Color(0xFFFAF3E7)
private val IconBg = Color(0xFFFBE0CB)
private val OrangeAccent = Color(0xFFFF6A00)
private val OrangeButton = Color(0xFFE8935C)
private val TitleColor = Color(0xFF1E293B)
private val SubtitleColor = Color(0xFF7D889A)
private val FieldBorder = Color(0xFFEDE4D6)
private val PlaceholderColor = Color(0xFFAEAEBE)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .padding(top = 100.dp)
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
                    onSendOtp = { phoneNumber -> authViewModel.sendOtp(phoneNumber, activity) }
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
    onSendOtp: (String) -> Unit
) {
    var phoneDigits by remember { mutableStateOf("") }

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
        Text("+91", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TitleColor)
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
                onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) phoneDigits = it },
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
        onClick = { onSendOtp("+91$phoneDigits") },
        enabled = phoneDigits.length == 10 && !isLoading,
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

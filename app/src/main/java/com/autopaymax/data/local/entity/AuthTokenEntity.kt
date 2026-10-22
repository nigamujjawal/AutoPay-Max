package com.autopaymax.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single-row-in-practice table (no multi-account handling) - the auth token lives only here for
// now, not in secure storage. Deliberate, temporary simplification, not an oversight.
@Entity(tableName = "auth_token")
data class AuthTokenEntity(
    @PrimaryKey val uid: String,
    // Legacy column from the phone-OTP era - kept (written "") so the v8->v9 migration stays
    // additive. Google sign-in fills `email` instead.
    val phoneNumber: String = "",
    val email: String = "",
    val idToken: String,
    // SoundBox backend session, obtained by exchanging idToken via POST /auth/firebase - this is
    // what's actually sent as the Bearer token for every other backend call, not idToken.
    val accessToken: String,
    val refreshToken: String,
    val issuedAt: Long,
    // Local-only display name, edited from Settings > Edit Profile - never sent to or read from
    // the backend (that dropped merchant_name/email from the profile concern entirely).
    val name: String = ""
)

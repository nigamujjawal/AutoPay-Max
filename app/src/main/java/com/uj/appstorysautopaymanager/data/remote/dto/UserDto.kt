package com.uj.appstorysautopaymanager.data.remote.dto

// email/merchant_name intentionally not modeled here - the app dropped them from the profile
// concern (name is now a local-only field, see AuthTokenEntity.name); Gson just ignores the
// extra keys the backend still returns for them.
data class UserDto(
    val id: String,
    val firebase_uid: String,
    val phone_number: String,
    val upi_id: String,
    val provider: String,
    val subscription_id: String?,
    val subscription_status: String,
    val subscription_endsAt: String?,
    val last_login: String?,
    val created_at: String,
    val updated_at: String
)

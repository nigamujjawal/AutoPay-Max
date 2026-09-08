package com.autopaymax.domain.auth.model

data class AuthUser(
    val uid: String,
    val email: String,
    val idToken: String
)

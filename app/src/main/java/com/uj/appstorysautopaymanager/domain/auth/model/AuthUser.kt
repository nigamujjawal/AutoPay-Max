package com.uj.appstorysautopaymanager.domain.auth.model

data class AuthUser(
    val uid: String,
    val email: String,
    val idToken: String
)

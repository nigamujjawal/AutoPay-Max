package com.uj.appstorysautopaymanager.domain.auth.model

data class AuthUser(
    val uid: String,
    val phoneNumber: String,
    val idToken: String
)

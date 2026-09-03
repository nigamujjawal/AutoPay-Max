package com.uj.appstorysautopaymanager.data.remote.dto

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val newUser: Boolean,
    val user: UserDto?
)

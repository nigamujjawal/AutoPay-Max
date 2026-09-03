package com.uj.appstorysautopaymanager.domain.profile.model

data class UserProfile(
    val id: String,
    val firebaseUid: String,
    val phoneNumber: String,
    // Local-only (Room AuthTokenEntity.name), not part of the backend User model.
    val name: String,
    val upiId: String,
    val provider: String,
    val subscriptionId: String?,
    val subscriptionStatus: String,
    val subscriptionEndsAt: String?,
    val lastLogin: String?,
    val createdAt: String,
    val updatedAt: String
)

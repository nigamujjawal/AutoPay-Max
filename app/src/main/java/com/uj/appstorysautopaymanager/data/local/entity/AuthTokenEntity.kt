package com.uj.appstorysautopaymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single-row-in-practice table (no multi-account handling) - the auth token lives only here for
// now, not in secure storage. Deliberate, temporary simplification, not an oversight.
@Entity(tableName = "auth_token")
data class AuthTokenEntity(
    @PrimaryKey val uid: String,
    val phoneNumber: String,
    val idToken: String,
    val issuedAt: Long
)

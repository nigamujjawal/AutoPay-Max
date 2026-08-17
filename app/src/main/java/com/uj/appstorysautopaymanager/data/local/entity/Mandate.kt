package com.uj.appstorysautopaymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mandates")
data class Mandate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val frequency: String,
    val nextExpectedDebit: Long,
    val bank: String,
    val status: String,
    val referenceNumber: String = ""
)

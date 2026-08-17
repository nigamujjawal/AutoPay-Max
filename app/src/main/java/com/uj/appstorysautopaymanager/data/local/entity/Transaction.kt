package com.uj.appstorysautopaymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsId: String,
    val merchant: String,
    val amount: Double,
    val date: Long,
    val bankName: String,
    val accountNumber: String,
    val referenceNumber: String,
    val transactionType: String,
    val category: String,
    val smsBody: String,
    val isAutoPay: Boolean = false
)

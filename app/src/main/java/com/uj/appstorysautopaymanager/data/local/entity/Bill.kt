package com.uj.appstorysautopaymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val repeatType: String,
    val reminderDays: Int,
    val notes: String = "",
    val category: String,
    val status: String,
    val isArchived: Boolean = false
)
